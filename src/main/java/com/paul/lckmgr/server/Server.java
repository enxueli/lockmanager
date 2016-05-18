package com.paul.lckmgr.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * Created by paul on 5/13/16.
 */
public class Server {

    /**
     * The acceptor threads to deal with the
     * network event and created from EventLoopGroup thread pool
     */
    private EventLoopGroup acceptorThreads;

    /**
     * The threads to deal with IO
     * and created from EventLoopGroup thread pool
     */
    private EventLoopGroup ioHandleThreads;

    /**
     * The channel future and can get channel object from channel() method
     */
    private ChannelFuture channelFuture;

    /**
     * The SocketAddress object
     */
    private SocketAddress socketAddress;

    private static final Logger _logger = LogManager.getFormatterLogger(Server.class);

    public Server(SocketAddress socketAddress) throws Exception {
        this.socketAddress = socketAddress;
    }

    /**
     * Initial netty and start the server
     * @throws Exception
     */
    public void start() throws Exception {
        acceptorThreads = new NioEventLoopGroup(1);
        ioHandleThreads = new NioEventLoopGroup(20);

        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(acceptorThreads, ioHandleThreads)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 200) //the maximum queue length for incoming connections
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    public void initChannel(SocketChannel channel) throws Exception {
                        channel.pipeline()
                                .addLast(new ObjectDecoder(ClassResolvers.softCachingConcurrentResolver(Server.class.getClassLoader())))
                                .addLast(new ObjectEncoder())
                                .addLast(new ServerHandler());
                    }
                });

        channelFuture = bootstrap.bind(socketAddress).syncUninterruptibly();
        if(channelFuture.isSuccess()){
            _logger.info("Server start.");
        }
    }

    /**
     * Stop server
     */
    public void stop() {
        if (channelFuture != null) {
            channelFuture.channel().close().syncUninterruptibly();
            channelFuture = null;
        }

        if (acceptorThreads != null) {
            acceptorThreads.shutdownGracefully();
            acceptorThreads = null;
        }

        if (ioHandleThreads != null) {
            ioHandleThreads.shutdownGracefully();
            ioHandleThreads = null;
        }

        _logger.info("Server stop.");
    }

    public static void main(String[] args) throws Exception {
        Server server = new Server(new InetSocketAddress(InetAddress.getLocalHost(), 8088));
        try {
            server.start();
            Thread.currentThread().suspend();
        }
        finally {
            server.stop();
        }
    }
}