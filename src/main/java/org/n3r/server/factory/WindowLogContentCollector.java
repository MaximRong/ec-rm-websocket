package org.n3r.server.factory;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.jboss.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WindowLogContentCollector implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(WindowLogContentCollector.class);
    private String reqText;
    private ChannelHandlerContext ctx;

    public WindowLogContentCollector(ChannelHandlerContext ctx, String reqText) {
        this.ctx = ctx;
        this.reqText = reqText;
    }

    @Override
    public void run() {
        while(!Thread.currentThread().isInterrupted()) {
            try {
                String fileContent = FileUtils.readFileToString(new File(reqText), CharsetUtil.UTF_8);
                TextWebSocketFrame textWebSocketFrame = new TextWebSocketFrame(fileContent);
                ctx.getChannel().write(textWebSocketFrame);
                Thread.sleep(5000);
            }
            catch (Exception e) {
                log.error("error!", e);
                break;
            }
        }
        log.info("collector has run over!");
    }

    public String getReqText() {
        return reqText;
    }

    public void setReqText(String reqText) {
        this.reqText = reqText;
    }

}
