package org.n3r.server.factory;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.HttpRequest;

public interface RequestHandler {

    void process(ChannelHandlerContext ctx, HttpRequest req, MessageEvent e) throws Exception;

}
