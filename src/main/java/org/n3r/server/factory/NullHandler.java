package org.n3r.server.factory;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.HttpRequest;

public class NullHandler implements RequestHandler {

    private String uri;

    public NullHandler(String uri) {
        this.uri = uri;
    }

    @Override
    public void process(ChannelHandlerContext ctx, HttpRequest req, MessageEvent e) throws Exception {
        System.out.println(uri);
    }

}
