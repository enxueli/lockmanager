package com.paul.lckmgr.server;

import io.netty.channel.ChannelHandlerContext;

/**
 * Created by paul on 5/13/16.
 */
public interface Lock {
    void lock(final ChannelHandlerContext ctx,
              final String lockName,
              final String lockHolder);

    boolean tryLock(final ChannelHandlerContext ctx,
                    final String lockName,
                    final String lockHolder);

    void unLock(final ChannelHandlerContext ctx,
                final String lockName,
                final String lockHolder);
}