package org.n3r.server.factory;

import org.apache.commons.lang3.StringUtils;

public class HandlerFactory {

    private String uri;

    public HandlerFactory(String uri) {
        this.uri = uri;
    }

    public RequestHandler create() {
        if (StringUtils.startsWith(uri, "/downloads")) {
            return new DownLoadFileHandler(uri);
        }
        if(StringUtils.startsWith(uri, "/res")) {
            return new ResFileHandler(uri);
        }
        if (StringUtils.equals("/", uri)) {
            return new IndexHandler(uri);
        }
        return new NullHandler(uri);
    }


}
