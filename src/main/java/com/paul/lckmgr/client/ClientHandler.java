package com.paul.lckmgr.client;

import com.paul.lckmgr.common.Response;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by paul on 5/13/16.
 */
public class ClientHandler extends ChannelInboundHandlerAdapter {
    private static final Logger _logger = LogManager.getLogger(ClientHandler.class);

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ctx.close();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof Response) {
            Response response = (Response) msg;
            if (response.getResponseStatus().equals(Response.ResponseStatus.OK)) {
                _logger.info(String.format("Client sends request success for lock [%s]", response.getLockName()));
            } else {
                _logger.warn(String.format("Client sends request failed for lock [%s]", response.getLockName()));
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
    }
}
