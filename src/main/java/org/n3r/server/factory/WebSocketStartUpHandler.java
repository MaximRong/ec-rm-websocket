package org.n3r.server.factory;

import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.HOST;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;

public class WebSocketStartUpHandler implements RequestHandler {

    private static final String WEBSOCKET_PATH = "/websocket";
    private WebSocketServerHandshaker handshaker;

    public WebSocketStartUpHandler(WebSocketServerHandshaker handshaker) {
        this.handshaker = handshaker;
    }

    @Override
    public void process(ChannelHandlerContext ctx, HttpRequest req, MessageEvent e) throws Exception {
        // Handshake
        WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(
                getWebSocketLocation(req), null, false);
        handshaker = wsFactory.newHandshaker(req);
        if (handshaker == null) {
            wsFactory.sendUnsupportedWebSocketVersionResponse(ctx.getChannel());
        }
        else {
            handshaker.handshake(ctx.getChannel(), req).addListener(WebSocketServerHandshaker.HANDSHAKE_LISTENER);
        }
    }

    private String getWebSocketLocation(HttpRequest req) {
        return "ws://" + req.getHeader(HOST) + WEBSOCKET_PATH;
    }

}
