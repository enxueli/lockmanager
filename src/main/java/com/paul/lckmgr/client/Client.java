package com.paul.lckmgr.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Created by paul on 5/13/16.
 */
public class Client {
    /**
     * The client channels and one client can have multiple channels
     */
    private EventLoopGroup clientGroup;

    /**
     * The client channel future
     */
    private ChannelFuture channelFuture;

    /**
     * The server address to connect
     */
    private SocketAddress serverAddress;

    public Client(SocketAddress serverAddress) {
        this.serverAddress = serverAddress;
    }

    /**
     * Start client
     */
    public void start() throws Exception {
        int clientNumbers = 10;

        // set one channel for a client
        clientGroup = new NioEventLoopGroup(1);

        // start multiple clients
        ExecutorService service = Executors.newFixedThreadPool(clientNumbers);

        try {
            Bootstrap bootstrap = new Bootstrap()
                    .group(clientGroup)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel channel) throws Exception {
                            channel.pipeline()
                                    .addLast(new ObjectDecoder(ClassResolvers.softCachingConcurrentResolver(Client.class.getClassLoader())))
                                    .addLast(new ObjectEncoder())
                                    .addLast(new ClientHandler());
                        }
                    });

            channelFuture = bootstrap.connect(serverAddress).syncUninterruptibly();

            List<Future<Void>> futures = new ArrayList<>(clientNumbers);

            for (int i = 0; i < clientNumbers; i++) {
                futures.add(service.submit(new Callable<Void>() {
                    public Void call() {
                        channelFuture.channel().writeAndFlush("Client data.");
                        System.out.println("Client output data.");
                        return null;
                    }
                }));
            }

            for (Future<Void> future : futures) {
                try {
                    future.get();
                }
                catch (ExecutionException e) {
                    e.printStackTrace();
                }
            }
        }
        finally {
            service.shutdownNow();
        }
    }

    /**
     * Stop client
     */
    public void stop() {
        if (channelFuture != null) {
            channelFuture.channel().close().syncUninterruptibly();
            channelFuture = null;
        }

        if (clientGroup != null) {
            clientGroup.shutdownGracefully();
            clientGroup = null;
        }

        System.out.println("client stop.");
    }

    /**
     * The input of client
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        Client client = new Client(new InetSocketAddress(InetAddress.getLocalHost(), 8088));
        try {
            client.start();
        }
        finally {
            client.stop();
        }
    }
}