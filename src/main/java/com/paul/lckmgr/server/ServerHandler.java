package com.paul.lckmgr.server;

import com.paul.lckmgr.common.Request;
import com.paul.lckmgr.common.Response;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * Created by paul on 5/13/16.
 */
public class ServerHandler extends ChannelInboundHandlerAdapter implements Lock {

    /**
     * All locks and the locks current holder
     */
    private final Map<String /*the lock name*/, String /*the lock holder*/> currentHolder = new HashMap<>();

    /**
     * All waiting holders
     */
    private final Map<String /*the lock name*/, LinkedList<String> /*the lock holder list*/> waitingHoldersList = new HashMap<>();

    private static final Logger _logger = LogManager.getFormatterLogger(ServerHandler.class);

    @Override
    public void channelRead(final ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!(msg instanceof Request)) {
            ctx.fireChannelRead(msg);
            _logger.error("Not invalid request received.");
        }

        Request request = (Request) msg;
        switch (request.getRequestType()) {
            case LOCK:
                lock(ctx, request.getLockName(), request.getLockHolder());
                break;

            case TRYLOCK:
                tryLock(ctx, request.getLockName(), request.getLockHolder());
                break;

            case UNLOCK:
                unLock(ctx, request.getLockName(), request.getLockHolder());
                break;

            default:
                _logger.error("Unsupported request operation.");
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        synchronized (currentHolder) {
            for (Map.Entry<String, String> entry : currentHolder.entrySet()) {
                unLock(ctx, entry.getKey(), entry.getValue());
            }
            currentHolder.clear();
        }
    }

    /**
     * Lock logic
     * @param ctx        the channel handler context
     * @param lockName   the name of lock
     * @param lockHolder the holder wants to get the lock
     */
    public synchronized void lock(final ChannelHandlerContext ctx,
                                  final String lockName,
                                  final String lockHolder) {
        if (null == currentHolder.get(lockName)) {
            currentHolder.put(lockName, lockHolder);
            _sendResponse(ctx, new Response(lockName, lockHolder, Response.ResponseStatus.OK));
        }
        if (currentHolder.get(lockName).equals(lockHolder)) {
            _sendResponse(ctx, new Response(lockName, lockHolder, Response.ResponseStatus.OK));
        } else {
            LinkedList<String> waitingHolders;
            if (!waitingHoldersList.containsKey(lockName)) {
                waitingHolders = new LinkedList<>();
                waitingHoldersList.put(lockName, waitingHolders);
            } else {
                waitingHolders = waitingHoldersList.get(lockName);
            }
            waitingHolders.add(lockHolder);
        }
    }

    /**
     * Try to get lock logic
     * @param ctx        the channel handler context
     * @param lockName   the name of lock
     * @param lockHolder the holder wants to get the lock
     * @return true if get the lock immediately and
     *         false if can not get the lock immediately
     */
    public synchronized boolean tryLock(final ChannelHandlerContext ctx,
                                        final String lockName,
                                        final String lockHolder) {
        if (null == currentHolder.get(lockName)) {
            currentHolder.put(lockName, lockHolder);
            _sendResponse(ctx, new Response(lockName, lockHolder, Response.ResponseStatus.OK));
            return true;
        }
        if (currentHolder.get(lockName).equals(lockHolder)) {
            _sendResponse(ctx, new Response(lockName, lockHolder, Response.ResponseStatus.OK));
            return true;
        } else {
            _sendResponse(ctx, new Response(lockName, lockHolder, Response.ResponseStatus.FAIL));
            return false;
        }
    }

    /**
     * Unlock logic
     * @param ctx        the channel handler context
     * @param lockName   the name of lock
     * @param lockHolder the holder wants to get the lock
     */
    public synchronized void unLock(final ChannelHandlerContext ctx,
                                    final String lockName,
                                    final String lockHolder) {
        if (null == currentHolder.get(lockName) ||
                !currentHolder.get(lockName).equals(lockHolder)) {
            _sendResponse(ctx, new Response(lockName, lockHolder, Response.ResponseStatus.FAIL));
            return;
        }
        if (null == waitingHoldersList.get(lockName)) {
            currentHolder.remove(lockName);
            _sendResponse(ctx, new Response(lockName, lockHolder, Response.ResponseStatus.OK));
        } else {
            currentHolder.remove(lockName);
            LinkedList<String> waitingHolder = waitingHoldersList.get(lockName);
            String nextWaitingHolder = waitingHolder.removeFirst();
            if (waitingHolder.size() == 0)
                waitingHoldersList.remove(lockName);
            currentHolder.put(lockName, nextWaitingHolder);
            _sendResponse(ctx, new Response(lockName, lockHolder, Response.ResponseStatus.OK));
        }
    }

    /**
     * Send response to client
     * @param ctx      the channel handler context
     * @param response the response to client
     */
    private synchronized void _sendResponse(final ChannelHandlerContext ctx,
                                            final Response response) {
        ctx.writeAndFlush(response);
    }
}
