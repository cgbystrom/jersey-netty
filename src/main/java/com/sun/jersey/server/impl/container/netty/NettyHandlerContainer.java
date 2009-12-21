package com.sun.jersey.server.impl.container.netty;

import org.jboss.netty.channel.*;
import org.jboss.netty.handler.codec.http.*;
import org.jboss.netty.buffer.ChannelBufferInputStream;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferOutputStream;
import com.sun.jersey.spi.container.WebApplication;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseWriter;
import com.sun.jersey.core.header.InBoundHeaders;
import com.sun.jersey.api.core.ResourceConfig;

import java.net.URI;
import java.io.OutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/**
 * @author Carl Bystršm
 */
@ChannelPipelineCoverage("all")
public class NettyHandlerContainer extends SimpleChannelUpstreamHandler
{
	public static final String PROPERTY_BASE_URI = "com.sun.jersey.server.impl.container.netty.baseUri";

	private WebApplication application;
	private String baseUri;


	public NettyHandlerContainer(WebApplication application, ResourceConfig resourceConfig)
	{
		this.application = application;
		this.baseUri = (String)resourceConfig.getProperty(PROPERTY_BASE_URI);
	}

	private final static class Writer implements ContainerResponseWriter
	{
		private final Channel channel;
		private HttpResponse response;

		private Writer(Channel channel)
		{
			this.channel = channel;
		}

		public OutputStream writeStatusAndHeaders(long contentLength, ContainerResponse cResponse) throws IOException
		{
			response = new DefaultHttpResponse(HttpVersion.HTTP_1_0, HttpResponseStatus.valueOf(cResponse.getStatus()));

			for (Map.Entry<String, List<Object>> e : cResponse.getHttpHeaders().entrySet())
			{
				List<String> values = new ArrayList<String>();
				for (Object v : e.getValue())
					values.add(ContainerResponse.getHeaderValue(v));
				response.setHeader(e.getKey(), values);
			}

			ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
			response.setContent(buffer);
			return new ChannelBufferOutputStream(buffer);
		}

		public void finish() throws IOException
		{
			// Streaming is not supported. Entire response will be written downstream once finish() is called.
			channel.write(response).addListener(ChannelFutureListener.CLOSE);
		}
	}

	@Override
	public void messageReceived(ChannelHandlerContext context, MessageEvent e) throws Exception
	{
		HttpRequest request = (HttpRequest)e.getMessage();

		String base = getBaseUri(request);
		final URI baseUri = new URI(base);
		final URI requestUri = new URI(base.substring(0, base.length() - 1) + request.getUri());

		final ContainerRequest cRequest = new ContainerRequest(
				application,
				request.getMethod().getName(),
				baseUri,
				requestUri,
				getHeaders(request),
				new ChannelBufferInputStream(request.getContent())
			);

		application.handleRequest(cRequest, new Writer(e.getChannel()));
	}

	private String getBaseUri(HttpRequest request)
	{
		if (baseUri != null)
		{
			return baseUri;
		}

		return "http://" + request.getHeader(HttpHeaders.Names.HOST) + "/";
	}

	private InBoundHeaders getHeaders(HttpRequest request)
	{
		InBoundHeaders headers = new InBoundHeaders();

		for (String name : request.getHeaderNames())
		{
			headers.put(name, request.getHeaders(name));
		}

		return headers;
	}
}
