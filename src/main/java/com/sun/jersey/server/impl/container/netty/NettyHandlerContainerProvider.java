package com.sun.jersey.server.impl.container.netty;

import com.sun.jersey.spi.container.ContainerProvider;
import com.sun.jersey.spi.container.WebApplication;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.api.container.ContainerException;

public class NettyHandlerContainerProvider implements ContainerProvider<NettyHandlerContainer>
{
	public NettyHandlerContainer createContainer(Class<NettyHandlerContainer> type, ResourceConfig config, WebApplication application) throws ContainerException
	{
		if (type != NettyHandlerContainer.class)
			return null;

		return new NettyHandlerContainer(application, config);
	}
}
