package org.n3r.server;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import org.n3r.server.factory.LinuxLogContentCollector;
import org.n3r.server.factory.WindowLogContentCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;

public class WebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(WebSocketHandler.class);

    private ChannelHandlerContext ctx;
    private WebSocketFrame frame;

    private WebSocketServerHandshaker handshaker;

    private static ExecutorService pool = Executors.newSingleThreadExecutor();
//    private static List<Future<?>> futures = Lists.newArrayList();
    private static Map<Integer, Future<?>> futures = Maps.newConcurrentMap();


    public WebSocketHandler(ChannelHandlerContext ctx, WebSocketFrame frame, WebSocketServerHandshaker handshaker) {
        this.ctx = ctx;
        this.frame = frame;
        this.handshaker = handshaker;
    }

    public void process() {
        // Check for closing frame
        if (frame instanceof CloseWebSocketFrame) {
            handshaker.close(ctx.getChannel(), (CloseWebSocketFrame) frame);
            return;
        }
        if (frame instanceof PingWebSocketFrame) {
            ctx.getChannel().write(new PongWebSocketFrame(frame.getBinaryData()));
            return;
        }
        if (!(frame instanceof TextWebSocketFrame)) {
            throw new UnsupportedOperationException(String.format("%s frame types not supported", frame.getClass()
                    .getName()));
        }

        log.info("start cmd....");
        boolean l = isWindow() ? runOnWindow() : runOnLinux();
        log.info("end cmd...." + l);
    }

    private boolean isWindow() {
        return System.getProperty("os.name").startsWith("Windows");
    }

    private boolean runOnWindow() {
        cancelTheLastTask();

        String reqText = ((TextWebSocketFrame) frame).getText();
        WindowLogContentCollector collector = new WindowLogContentCollector(ctx, reqText);
        Future<?> future = pool.submit(collector);
        futures.put(ctx.getChannel().getId(), future);

        return true;
    }

    private boolean runOnLinux() {
        cancelTheLastTask();
        String reqText = ((TextWebSocketFrame) frame).getText();
        LinuxLogContentCollector collector = new LinuxLogContentCollector(ctx, reqText);
        Future<?> future = pool.submit(collector);
        futures.put(ctx.getChannel().getId(), future);

        return false;
    }

    private void cancelTheLastTask() {
        Integer channelId = ctx.getChannel().getId();
        if(futures.containsKey(channelId)) {
            Future<?> future = futures.get(channelId);
            future.cancel(true);
            futures.remove(channelId);
            log.info("the future is done ? " + future.isDone());
        }
    }


}
