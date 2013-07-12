package org.n3r.server.factory;

import static org.jboss.netty.handler.codec.http.HttpHeaders.isKeepAlive;
import static org.jboss.netty.handler.codec.http.HttpHeaders.setContentLength;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.HOST;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.OK;
import static org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
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
import org.n3r.freemarker.FreemarkerTemplateEngine;
import org.springframework.core.io.Resource;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import freemarker.template.Template;

public class IndexHandler implements RequestHandler {

    private String uri;

    public IndexHandler(String uri) {
        this.setUri(uri);
    }

    @Override
    public void process(ChannelHandlerContext ctx, HttpRequest req, MessageEvent e) throws Exception {
        HttpResponse res = new DefaultHttpResponse(HTTP_1_1, OK);
        Resource resource = RClassPath.getResource("org/n3r/ftl/index.ftl");

//        String templateSource = FileUtils.readFileToString(resource[0].getFile());
        byte[] byteArray = IOUtils.toByteArray(resource.getInputStream());
        String string = new String(byteArray, CharsetUtil.UTF_8);
        // TODO : 有缓存 要调试下
        Template template = FreemarkerTemplateEngine.putTemplate("index", string);
        ArrayList<Object> folderLst = genFolderList();
        Map root = ImmutableMap.of("contextPath", req.getHeader(HOST), "folderLst", folderLst);
        String result = FreemarkerTemplateEngine.process(root, template);

        ChannelBuffer content = ChannelBuffers.copiedBuffer(result, CharsetUtil.UTF_8);

        res.setHeader(CONTENT_TYPE, "text/html; charset=UTF-8");
        setContentLength(res, content.readableBytes());

        res.setContent(content);
        sendHttpResponse(ctx, req, res);
    }

    private ArrayList<Object> genFolderList() throws IOException {
        ArrayList<Object> folderLst = Lists.newArrayList();
        File file = new File("log.properties");
        if(!file.exists()) {
            throw new RuntimeException("must create log.properties file in jar folder!");
        }
        String logfolders = FileUtils.readFileToString(new File("log.properties"));
        Iterable<String> iterable = Splitter.on(';').omitEmptyStrings().trimResults().split(logfolders);
        for (String folderPath : iterable) {
            File folder = new File(folderPath);
            if(!folder.exists()) {
                throw new RuntimeException("the " + folderPath + "is not exist!");
            }

            if(!folder.isDirectory()) continue;

            HashMap<Object, Object> folderMap = Maps.newHashMap();
            folderMap.put("folder", folderPath);
            ArrayList<Map> fileLst = Lists.newArrayList();
            File[] listFiles = folder.listFiles();
            for (File f : listFiles) {
                HashMap<String, String> fileMap = Maps.newHashMap();
                fileMap.put("file", f.getName());
                fileMap.put("fileCanonicalPath", f.getCanonicalPath());
                fileLst.add(fileMap);
            }
            folderMap.put("fileLst", fileLst);
            folderLst.add(folderMap);
        }

        return folderLst;
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

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

}
