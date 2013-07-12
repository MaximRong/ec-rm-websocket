package org.n3r.server.factory;

import static org.jboss.netty.handler.codec.http.HttpHeaders.isKeepAlive;
import static org.jboss.netty.handler.codec.http.HttpHeaders.setContentLength;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.OK;
import static org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.util.CharsetUtil;
import org.n3r.core.lang.RClassPath;
import org.n3r.server.HandlerHelper;
import org.springframework.core.io.Resource;

public class ResFileHandler implements RequestHandler {

    private String uri;

    public ResFileHandler(String uri) {
        this.uri = uri;
    }

    @Override
    public void process(ChannelHandlerContext ctx, HttpRequest req, MessageEvent e) throws Exception {
        HttpResponse res = new DefaultHttpResponse(HTTP_1_1, OK);

        ChannelBuffer content = getContent();

        res.setHeader(CONTENT_TYPE, getContentType());
        setContentLength(res, content.readableBytes());

        res.setContent(content);
        sendHttpResponse(ctx, req, res);
    }

    private ChannelBuffer getContent() throws IOException {
        if (StringUtils.endsWith(uri, ".css") || StringUtils.endsWith(uri, ".js")) {
            return ChannelBuffers.copiedBuffer(getResourceContent(), CharsetUtil.UTF_8);
        }

        ByteArrayOutputStream bos = null;
        try {
            bos = new ByteArrayOutputStream();
            Resource resource = getResource();
            BufferedImage image = ImageIO.read(resource.getURL());
            ImageIO.write(image, StringUtils.substringAfterLast(uri, "."), bos);
            return ChannelBuffers.copiedBuffer(bos.toByteArray());
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
        finally {
            IOUtils.closeQuietly(bos);
        }
    }

    private String getContentType() {
        if (StringUtils.endsWith(uri, ".css"))
            return "text/css";
        if (StringUtils.endsWith(uri, ".js"))
            return "application/javascript";
        return "image/" + StringUtils.substringAfterLast(uri, ".");
    }

    private CharSequence getResourceContent() throws IOException {
        Resource resource = getResource();
        byte[] byteArray = IOUtils.toByteArray(resource.getInputStream());
        return  new String(byteArray, CharsetUtil.UTF_8);
    }

    private Resource getResource() {
        String decodePath = HandlerHelper.decodePath(uri);

        String filePath = StringUtils.split(decodePath, '?')[1];
        Resource resource = RClassPath.getResource(filePath);
        return resource;
    }

    private static void sendHttpResponse(ChannelHandlerContext ctx, HttpRequest req, HttpResponse res) {
        // Generate an error page if response status code is not OK (200).
        if (res.getStatus().getCode() != 200) {
            res.setContent(ChannelBuffers.copiedBuffer(res.getStatus().toString(), CharsetUtil.UTF_8));
            setContentLength(res, res.getContent().readableBytes());
        }

        // Send the response and close the connection if necessary.
        ChannelFuture f = ctx.getChannel().write(res);
        if (!isKeepAlive(req) || res.getStatus().getCode() != 200) {
            f.addListener(ChannelFutureListener.CLOSE);
        }
    }

}
