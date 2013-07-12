package org.n3r.server;

import static org.jboss.netty.handler.codec.http.HttpMethod.GET;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.METHOD_NOT_ALLOWED;

import org.apache.commons.lang3.StringUtils;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import org.n3r.server.factory.HandlerFactory;
import org.n3r.server.factory.RequestHandler;
import org.n3r.server.factory.WebSocketStartUpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebSocketServerHandler extends SimpleChannelUpstreamHandler {
    private static final Logger log = LoggerFactory.getLogger(WebSocketServerHandler.class);

    private WebSocketServerHandshaker handshaker;


    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        Object msg = e.getMessage();
        if (msg instanceof HttpRequest) {
            handleHttpRequest(ctx, (HttpRequest) msg, e);
        }
        else if (msg instanceof WebSocketFrame) {
            handleWebSocketFrame(ctx, (WebSocketFrame) msg);
        }
    }

    private void handleHttpRequest(ChannelHandlerContext ctx, HttpRequest req, MessageEvent e) throws Exception {
        // Allow only GET methods.
        if (req.getMethod() != GET) {
            HandlerHelper.sendError(ctx, METHOD_NOT_ALLOWED);
            return;
        }

        final String uri = req.getUri();
        log.info("the uri is {}", uri);
        log.info(".....................................");
        if(StringUtils.equals("/websocket", uri)) {
            new WebSocketStartUpHandler(handshaker).process(ctx, req, e);
            log.info("the websocket sever is startUp!");
            return;
        }

        RequestHandler handler = new HandlerFactory(uri).create();
        handler.process(ctx, req, e);
        log.info("handler {} process has bean finished!", handler.getClass().getSimpleName());
    }


    private void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {
        new WebSocketHandler(ctx,  frame, handshaker).process();
    }

}
