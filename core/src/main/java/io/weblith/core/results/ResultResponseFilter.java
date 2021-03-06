package io.weblith.core.results;

import java.io.IOException;
import java.util.Map.Entry;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.ext.Provider;

import io.weblith.core.request.RequestContext;
import io.weblith.core.results.Result.ConfigureResponse;
import io.weblith.core.results.Result.RenderResponse;
import io.weblith.core.scopes.CookieBuilder;

@Provider
@ApplicationScoped
public class ResultResponseFilter implements ContainerResponseFilter {

    @Inject
    RequestContext context;

    @Inject
    HttpCacheHelper httpCache;

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {

        if (!responseContext.hasEntity() || !Result.class.isAssignableFrom(responseContext.getEntityClass())) {
            return;
        }

        Result result = (Result) responseContext.getEntity();

        httpCache.setCachingPolicy(requestContext, result);

        for (Entry<String, Object> header : result.getHeaders().entrySet()) {
            responseContext.getHeaders().add(header.getKey(), header.getValue());
        }

        if (ConfigureResponse.class.isAssignableFrom(result.getClass())) {
            ((ConfigureResponse) result).filter(context, responseContext);
        } else {
            setDefaultResponseConfiguration(result, responseContext);
        }

        if (RenderResponse.class.isAssignableFrom(result.getClass())) {
            responseContext.setEntity(null);
            try {
                ((RenderResponse) result).write(responseContext.getEntityStream());
            } catch (WebApplicationException e) {
                throw e;
            } catch (Exception e) {
                throw new WebApplicationException(e.getMessage(), e);
            }
        }

        manageCookies(result, responseContext);

    }

    protected void setDefaultResponseConfiguration(Result result, ContainerResponseContext responseContext) {
        responseContext.setStatus(result.getStatus().getStatusCode());

        if (result.getContentType() == null) {
            // Resteasy absolutely needs a header to be filled
            responseContext.getHeaders().putSingle(HttpHeaders.CONTENT_TYPE, "");
            responseContext.setEntity(null);
        } else if (result.getCharset() != null) {
            responseContext.getHeaders().putSingle(HttpHeaders.CONTENT_TYPE,
                    String.format("%s; charset=%s", result.getContentType(), result.getCharset()));
        } else {
            responseContext.getHeaders().putSingle(HttpHeaders.CONTENT_TYPE, result.getContentType());
        }
    }

    protected void manageCookies(Result result, ContainerResponseContext responseContext) throws IOException {
        if (result.isIncludeScopeCookies()) {
            context.flash().save(result);
            context.session().save(result);
        }
        for (NewCookie cookie : result.getCookies()) {
            CookieBuilder.save(responseContext, cookie);
        }
    }

}
