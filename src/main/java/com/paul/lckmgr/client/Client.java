package com.paul.lckmgr.client;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

/**
 * Created by paul on 5/15/16.
 */
public class Client {
    private SocketChannel client;
    private Selector selctor = getSelector();

    private List<Object> messageQueue = new LinkedList<>();

    private Set<String> set = new HashSet<String>();
    private ExecutorService threadPool = Executors.newFixedThreadPool(20);

    private boolean isClose = false;
    private volatile boolean run = true;

    public Selector getSelector() {
        try {
            return Selector.open();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Client() {
        try {
            client = SocketChannel.open();
            client.configureBlocking(false);
            client.connect(new InetSocketAddress(InetAddress.getLocalHost(), 5555));
            client.register(selctor, SelectionKey.OP_CONNECT);
        } catch (IOException e) {
            e.printStackTrace();
            isClose = true;
            return;
        }
        threadPool.execute(new Runnable() {

            @Override
            public void run() {
                while (run) {
                    try {
//                        if (selctor.select(20) == 0) {
//                            continue;
//                        }
                        selctor.select();
                        Iterator<SelectionKey> iterator = selctor.selectedKeys().iterator();
                        while (iterator.hasNext()) {
                            SelectionKey selectionKey = iterator.next();
                            iterator.remove();
                            if (selectionKey.isConnectable()) {
                                SocketChannel sc = (SocketChannel) selectionKey.channel();
                                sc.finishConnect();
                                sc.register(selctor, SelectionKey.OP_READ);
                            } else if (selectionKey.isWritable()) {
                                selectionKey.interestOps(SelectionKey.OP_READ);
                                SocketChannel writeSocketChannel = (SocketChannel) selectionKey.channel();
                                Object requestMessage = null;
                                while (messageQueue.size() > 0) {
                                    requestMessage = messageQueue.remove(0);
                                    threadPool
                                            .execute(new WriteClientSocketHandler(writeSocketChannel, requestMessage));

                                }
                            } else if (selectionKey.isReadable()) {
                                SocketChannel readSocketChannel = (SocketChannel) selectionKey.channel();
                                ByteBuffer tmp = ByteBuffer.allocate(1024);
                                int len = -1;
                                byte[] data = new byte[0];
                                if ((len = readSocketChannel.read(tmp)) > 0) {
                                    data = Arrays.copyOf(data, data.length + len);
                                    System.arraycopy(tmp.array(), 0, data, data.length - len, len);
                                    tmp.rewind();
                                }
                                if (data.length > 0) {
                                    String receiveData = new String(data);
                                    set.add(receiveData.substring(receiveData.length() - 3));
                                    System.out.println("received data:" + new String(data));
                                }
                            }
                        }
                    } catch (IOException e1) {
                        e1.printStackTrace();
                        close();
                    }
                }
            }
        });
    }

    private class WriteClientSocketHandler implements Runnable {
        SocketChannel client;
        Object respnoseMessage;

        WriteClientSocketHandler(SocketChannel client, Object respnoseMessage) {
            this.client = client;
            this.respnoseMessage = respnoseMessage;
        }

        @Override
        public void run() {
            byte[] responseByteData = null;
            String logResponseString = "";
            if (respnoseMessage instanceof byte[]) {
                responseByteData = (byte[]) respnoseMessage;
                logResponseString = new String(responseByteData);
            } else if (respnoseMessage instanceof String) {
                logResponseString = (String) respnoseMessage;
                responseByteData = logResponseString.getBytes();
            }
            if (responseByteData == null || responseByteData.length == 0) {
                System.out.println("no data");
                return;
            }
            try {
                client.write(ByteBuffer.wrap(responseByteData));
            } catch (IOException e) {
                e.printStackTrace();
                close();
            }
        }
    }

    public boolean isClose() {
        return isClose;
    }

    public void close() {
        try {
            run = false;
            isClose = true;
            selctor.close();
            client.close();
            threadPool.shutdown();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeData(String data) {
        try {
            if (client.isOpen()) {
                client.register(selctor, SelectionKey.OP_WRITE);
            }
        } catch (ClosedChannelException e1) {
            e1.printStackTrace();
        }
        messageQueue.add(data);
        try {
            Thread.sleep(40);
        } catch (InterruptedException e) {

            e.printStackTrace();
        }
    }

    public int receiveSize() {
        return set.size();
    }

    public boolean hasElement(Object obj) {
        return set.contains(obj);
    }

    public static void main(String[] args) {

        Client client = new Client();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {

            e.printStackTrace();
        }
        long t1 = System.currentTimeMillis();
        for (int i = 0; i < 500; i++) {
            String ii = "00" + i;
            ii = ii.substring(ii.length() - 3);
            client.writeData(ii + "testing...." + i);
        }
        long t2 = System.currentTimeMillis();
        System.out.println("total：" + (t2 - t1) + "ms");
        System.out.println("data: " + client.receiveSize());
        if (client.receiveSize() < 500) {
            for (int i = 0; i < 500; i++) {
                String ii = "00" + i;
                ii = ii.substring(ii.length() - 3);
                if (!client.hasElement(ii)) {
                    System.out.println("missing: " + ii);
                }
            }
        }
        client.close();
    }
}