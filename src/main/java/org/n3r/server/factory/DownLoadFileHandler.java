package org.n3r.server.factory;

import static org.jboss.netty.handler.codec.http.HttpHeaders.isKeepAlive;
import static org.jboss.netty.handler.codec.http.HttpHeaders.setContentLength;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CACHE_CONTROL;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.DATE;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.EXPIRES;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.IF_MODIFIED_SINCE;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.LAST_MODIFIED;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.NOT_MODIFIED;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.OK;
import static org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import javax.activation.MimetypesFileTypeMap;

import org.apache.commons.lang3.StringUtils;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelFutureProgressListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.DefaultFileRegion;
import org.jboss.netty.channel.FileRegion;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.ssl.SslHandler;
import org.jboss.netty.handler.stream.ChunkedFile;
import org.n3r.server.HandlerHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DownLoadFileHandler implements RequestHandler {
    private static final Logger log = LoggerFactory.getLogger(DownLoadFileHandler.class);
    private static final String HTTP_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz";
    private static final String HTTP_DATE_GMT_TIMEZONE = "GMT";
    private static final int HTTP_CACHE_SECONDS = 60;

    private String uri;

    public DownLoadFileHandler(String uri) {
        this.uri = uri;
    }

    @Override
    public void process(ChannelHandlerContext ctx, HttpRequest req, MessageEvent e) throws Exception {
        log.info("run in download file handler...");
        final String path = analyzeFilePath(uri);

        File file = new File(path);
        if (file.isHidden() || !file.exists()) {
            HandlerHelper.sendError(ctx, NOT_FOUND);
            return;
        }

        if (!file.isFile()) {
            HandlerHelper.sendError(ctx, FORBIDDEN);
            return;
        }

        if (!validateIfModifiedSince(ctx, req, file))
            return;

        log.info("start to access file : {}", path);
        RandomAccessFile raf;
        try {
            raf = new RandomAccessFile(file, "r");
        }
        catch (FileNotFoundException fnfe) {
            HandlerHelper.sendError(ctx, NOT_FOUND);
            return;
        }

        long fileLength = raf.length();
        HttpResponse resp = new DefaultHttpResponse(HTTP_1_1, OK);
        setContentLength(resp, fileLength);
        setContentTypeHeader(resp, file);
        setDateAndCacheHeaders(resp, file);

        Channel ch = e.getChannel();

        log.info("start to write file...");
        // Write the initial line and the header.
        ch.write(resp);

        // Write the content.
        ChannelFuture writeFuture;
        writeFuture = writeFile(path, raf, fileLength, ch);

        // Decide whether to close the connection or not.
        if (!isKeepAlive(req)) {
            // Close the connection when the whole content is written out.
            writeFuture.addListener(ChannelFutureListener.CLOSE);
        }

        log.info("download file finish, close connection...");
    }

    private boolean validateIfModifiedSince(ChannelHandlerContext ctx, HttpRequest req, File file)
            throws ParseException {
        log.info("validation http header, 'if-modified-since'...");
        // Cache Validation
        String ifModifiedSince = req.getHeader(IF_MODIFIED_SINCE);
        if (ifModifiedSince != null && ifModifiedSince.length() != 0) {
            SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
            Date ifModifiedSinceDate = dateFormatter.parse(ifModifiedSince);

            // Only compare up to the second because the datetime format we send to the client does
            // not have milliseconds
            long ifModifiedSinceDateSeconds = ifModifiedSinceDate.getTime() / 1000;
            long fileLastModifiedSeconds = file.lastModified() / 1000;
            if (ifModifiedSinceDateSeconds == fileLastModifiedSeconds) {
                sendNotModified(ctx);
                return false;
            }
        }

        return true;
    }

    /**
     * Sets the Date and Cache headers for the HTTP Response
     *
     * @param response
     *            HTTP response
     * @param fileToCache
     *            file to extract content type
     */
    private static void setDateAndCacheHeaders(HttpResponse response, File fileToCache) {
        SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
        dateFormatter.setTimeZone(TimeZone.getTimeZone(HTTP_DATE_GMT_TIMEZONE));

        // Date header
        Calendar time = new GregorianCalendar();
        response.setHeader(DATE, dateFormatter.format(time.getTime()));

        // Add cache headers
        time.add(Calendar.SECOND, HTTP_CACHE_SECONDS);
        response.setHeader(EXPIRES, dateFormatter.format(time.getTime()));
        response.setHeader(CACHE_CONTROL, "private, max-age=" + HTTP_CACHE_SECONDS);
        response.setHeader(
                LAST_MODIFIED, dateFormatter.format(new Date(fileToCache.lastModified())));
    }

    private void setContentTypeHeader(HttpResponse response, File file) {
        MimetypesFileTypeMap mimeTypesMap = new MimetypesFileTypeMap();
        response.setHeader(CONTENT_TYPE, mimeTypesMap.getContentType(file.getPath()));
        response.addHeader("content-disposition", "attachment; filename=" + file.getName());
    }

    private void sendNotModified(ChannelHandlerContext ctx) {
        HttpResponse response = new DefaultHttpResponse(HTTP_1_1, NOT_MODIFIED);
        setDateHeader(response);

        // Close the connection as soon as the error message is sent.
        ctx.getChannel().write(response).addListener(ChannelFutureListener.CLOSE);
    }

    /**
     * Sets the Date header for the HTTP response
     *
     * @param response
     *            HTTP response
     */
    private void setDateHeader(HttpResponse response) {
        SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
        dateFormatter.setTimeZone(TimeZone.getTimeZone(HTTP_DATE_GMT_TIMEZONE));

        Calendar time = new GregorianCalendar();
        response.setHeader(DATE, dateFormatter.format(time.getTime()));
    }

    private String analyzeFilePath(String uri) {
        log.info("start to analyze file path, the uri is {}.", uri);
        return getParams(HandlerHelper.decodePath(uri));
    }

    private String getParams(String decodePath) {
        if (!StringUtils.containsAny(decodePath, '?')) {
            throw new RuntimeException("the uri must has param");
        }
        String backUri = StringUtils.split(decodePath, '?')[1];
        if (!StringUtils.startsWith(backUri, "file=")) {
            throw new RuntimeException("the param key must be file");
        }

        // Simplistic dumb security check.
        // You will have to do something serious in the production environment.
        String filePath = StringUtils.substringBetween(backUri, "file=", "&").replace('/', File.separatorChar);
        ;
        if (filePath.contains(File.separator + '.') ||
                filePath.contains('.' + File.separator) ||
                filePath.startsWith(".") || filePath.endsWith(".")) {
            throw new RuntimeException("you path is not security");
        }

        return filePath;
    }


    private ChannelFuture writeFile(final String path, RandomAccessFile raf, long fileLength, Channel ch)
            throws IOException {
        ChannelFuture writeFuture;
        if (ch.getPipeline().get(SslHandler.class) != null) {
            // Cannot use zero-copy with HTTPS.
            writeFuture = ch.write(new ChunkedFile(raf, 0, fileLength, 8192));
        }
        else {
            // No encryption - use zero-copy.
            final FileRegion region =
                    new DefaultFileRegion(raf.getChannel(), 0, fileLength);
            writeFuture = ch.write(region);
            writeFuture.addListener(new ChannelFutureProgressListener() {
                @Override
                public void operationComplete(ChannelFuture future) {
                    region.releaseExternalResources();
                }

                @Override
                public void operationProgressed(
                        ChannelFuture future, long amount, long current, long total) {
                    log.info("%s: %d / %d (+%d)%n", path, current, total, amount);
                }
            });
        }
        return writeFuture;
    }

}
