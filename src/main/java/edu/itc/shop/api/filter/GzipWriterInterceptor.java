package edu.itc.shop.api.filter;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Locale;
import java.util.zip.GZIPOutputStream;

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.ext.WriterInterceptor;
import jakarta.ws.rs.ext.WriterInterceptorContext;

/**
 * Compresses {@link Compress}-bound responses when the client sends {@code Accept-Encoding: gzip}.
 */
@Provider
@Compress
public class GzipWriterInterceptor implements WriterInterceptor {

    @Context
    private HttpHeaders requestHeaders;

    @Override
    public void aroundWriteTo(WriterInterceptorContext context) throws IOException {
        String encoding = requestHeaders.getHeaderString(HttpHeaders.ACCEPT_ENCODING);
        if (encoding != null && encoding.toLowerCase(Locale.ROOT).contains("gzip")) {
            MultivaluedMap<String, Object> headers = context.getHeaders();
            headers.putSingle(HttpHeaders.CONTENT_ENCODING, "gzip");
            headers.putSingle(HttpHeaders.VARY, HttpHeaders.ACCEPT_ENCODING);
            headers.remove(HttpHeaders.CONTENT_LENGTH);

            OutputStream original = context.getOutputStream();
            GZIPOutputStream gzip = new GZIPOutputStream(original);
            context.setOutputStream(gzip);
            context.proceed();
            gzip.finish();
            return;
        }
        context.proceed();
    }
}
