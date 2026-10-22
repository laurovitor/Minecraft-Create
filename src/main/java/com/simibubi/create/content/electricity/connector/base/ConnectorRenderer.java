package com.simibubi.create.content.electricity.connector.base;

import com.simibubi.create.content.electricity.render.WireNodeRenderer;

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;


public class ConnectorRenderer extends WireNodeRenderer<AbstractConnectorBlockEntity> {

	public ConnectorRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}
}

