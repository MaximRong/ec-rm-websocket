package org.n3r.server.factory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LinuxLogContentCollector extends Thread {

    private static final Logger log = LoggerFactory.getLogger(LinuxLogContentCollector.class);
    private ChannelHandlerContext ctx;
    private String reqText;
    private static final int ErrorStream = 0;
    private static final int NormalStream = 1;

    public LinuxLogContentCollector(ChannelHandlerContext ctx, String reqText) {
        this.ctx = ctx;
        this.reqText = reqText;
    }

    @Override
    public void run() {
        try {
            String[] cmd = new String[] { "tail", "-100f", reqText };

            Process process = new ProcessBuilder(cmd).start();
            new Thread(new ListenerThread(Thread.currentThread(), process)).start();
            BufferedReader infoBr = null;
            BufferedReader errorBr = null;

            printProcessInfo(ctx, process, infoBr, NormalStream);

            printProcessInfo(ctx, process, errorBr, ErrorStream);

            int val = process.waitFor();
            process.destroy();

            if (val != 0) {

            }
        }
        catch (Exception e) {
            log.error("cmd excute error [IOException] : ", e);
        }
    }


    private void printProcessInfo(ChannelHandlerContext ctx, Process process, BufferedReader buffer, int streamTpye)
            throws IOException {
        String s;
        try {
            buffer = new BufferedReader(new InputStreamReader(ErrorStream == streamTpye ? process.getErrorStream()
                    : process.getInputStream()));
            while ((s = buffer.readLine()) != null)
                ctx.getChannel().write(new TextWebSocketFrame(s));
        }
        finally {
            if (null != buffer) {
                buffer.close();
            }
            log.info("collector '{}' has run over!", this.getClass().getSimpleName());
        }
    }


}
