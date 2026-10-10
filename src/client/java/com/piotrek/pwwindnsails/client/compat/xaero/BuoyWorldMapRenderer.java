package com.piotrek.pwwindnsails.client.compat.xaero;

import com.piotrek.pwwindnsails.entity.BuoyEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import xaero.lib.client.graphics.XaeroBufferProvider;
import xaero.map.element.MapElementGraphics;
import xaero.map.element.render.ElementReader;
import xaero.map.element.render.ElementRenderInfo;
import xaero.map.element.render.ElementRenderLocation;
import xaero.map.element.render.ElementRenderProvider;
import xaero.map.element.render.ElementRenderer;
import xaero.map.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider;

import java.util.Iterator;
import java.util.List;

public final class BuoyWorldMapRenderer extends ElementRenderer<BuoyTracker.BuoyMapTarget, BuoyWorldMapRenderer.Context, BuoyWorldMapRenderer> {
	public static final class Context {}

	public BuoyWorldMapRenderer() {
		super(new Context(), new Provider(), new Reader());
	}

	@Override
	public int getOrder() {
		return 2;
	}

	@Override
	public boolean shouldRender(ElementRenderLocation location, boolean hovered) {
		return location == ElementRenderLocation.WORLD_MAP || location == ElementRenderLocation.OVER_MINIMAP;
	}

	@Override
	public void preRender(
		ElementRenderInfo renderInfo,
		XaeroBufferProvider bufferProvider,
		MultiTextureRenderTypeRendererProvider rendererProvider,
		boolean hovered
	) {}

	@Override
	public void postRender(
		ElementRenderInfo renderInfo,
		XaeroBufferProvider bufferProvider,
		MultiTextureRenderTypeRendererProvider rendererProvider,
		boolean hovered
	) {}

	@Override
	public void renderElementShadow(
		BuoyTracker.BuoyMapTarget element,
		boolean hovered,
		float scale,
		double renderX,
		double renderZ,
		ElementRenderInfo renderInfo,
		MapElementGraphics graphics,
		XaeroBufferProvider bufferProvider,
		MultiTextureRenderTypeRendererProvider rendererProvider
	) {}

	@Override
	public boolean renderElement(
		BuoyTracker.BuoyMapTarget element,
		boolean hovered,
		double optionalDepth,
		float optionalScale,
		double partialX,
		double partialY,
		ElementRenderInfo renderInfo,
		MapElementGraphics graphics,
		XaeroBufferProvider bufferProvider,
		MultiTextureRenderTypeRendererProvider rendererProvider
	) {
		graphics.pose().pushPose();
		graphics.pose().translate(partialX, partialY, 0.0);
		float scale = optionalScale > 0.0F ? optionalScale : 1.0F;
		graphics.pose().scale(scale, scale, 1.0F);

		if (element.type() == BuoyEntity.TYPE_RED_SPHERICAL) {
			// Red Spherical Buoy: distinctive round marine marker with reflective stripe
			graphics.fill(-6, -6, 7, 7, 0xEE1A0000);  // Dark outer outline
			graphics.fill(-5, -5, 6, 6, 0xFFFF2525);  // Nautical red core
			graphics.fill(-5, -1, 6, 2, 0xFFFFFFFF);  // White reflective stripe
			graphics.fill(-2, -8, 3, -6, 0xFF353535); // Metal mooring shackle
		} else {
			// Blue Cylindrical Buoy: tall nautical cylinder marker
			graphics.fill(-5, -8, 6, 9, 0xEE00142A);  // Dark outer outline
			graphics.fill(-4, -7, 5, 8, 0xFF1872E8);  // Deep marine blue body
			graphics.fill(-4, -1, 5, 2, 0xFFFFFFFF);  // White reflective stripe
			graphics.fill(-4, -6, 5, -4, 0xFFFFDD00); // Yellow high-vis signal band
			graphics.fill(-2, -10, 3, -8, 0xFF353535);// Metal mooring shackle
		}

		// Text label display
		Font font = Minecraft.getInstance().font;
		if (element.name() != null) {
			String label = element.name().getString();
			int w = font.width(label);
			int y = 9;
			graphics.fill(-w / 2 - 2, y - 1, w / 2 + 2, y + font.lineHeight, 0xB0000000);
			graphics.drawCenteredString(font, label, 0, y, 0xFFFFFFFF);
		} else if (hovered) {
			String label = element.type() == BuoyEntity.TYPE_RED_SPHERICAL ? "Boja (czerwona)" : "Boja (niebieska)";
			int w = font.width(label);
			int y = 9;
			graphics.fill(-w / 2 - 2, y - 1, w / 2 + 2, y + font.lineHeight, 0xB0000000);
			graphics.drawCenteredString(font, label, 0, y, 0xFFFFFFFF);
		}

		graphics.pose().popPose();
		graphics.flush();
		return false;
	}

	public static final class Provider extends ElementRenderProvider<BuoyTracker.BuoyMapTarget, Context> {
		private Iterator<BuoyTracker.BuoyMapTarget> iterator;

		@Override
		public void begin(ElementRenderLocation location, Context context) {
			Minecraft mc = Minecraft.getInstance();
			if (mc.level != null) {
				List<BuoyTracker.BuoyMapTarget> targets = BuoyTracker.getTargetsForDimension(mc.level.dimension());
				this.iterator = targets.iterator();
			} else {
				this.iterator = null;
			}
		}

		@Override
		public boolean hasNext(ElementRenderLocation location, Context context) {
			return this.iterator != null && this.iterator.hasNext();
		}

		@Override
		public BuoyTracker.BuoyMapTarget getNext(ElementRenderLocation location, Context context) {
			return this.iterator.next();
		}

		@Override
		public void end(ElementRenderLocation location, Context context) {
			this.iterator = null;
		}
	}

	public static final class Reader extends ElementReader<BuoyTracker.BuoyMapTarget, Context, BuoyWorldMapRenderer> {
		@Override
		public boolean isHidden(BuoyTracker.BuoyMapTarget element, Context context) {
			return false;
		}

		@Override
		public double getRenderX(BuoyTracker.BuoyMapTarget element, Context context, float partialTicks) {
			return element.pos().x;
		}

		@Override
		public double getRenderY(BuoyTracker.BuoyMapTarget element, Context context, float partialTicks) {
			return element.pos().y;
		}

		@Override
		public boolean hasYCoordinate() {
			return true;
		}

		@Override
		public double getRenderZ(BuoyTracker.BuoyMapTarget element, Context context, float partialTicks) {
			return element.pos().z;
		}

		@Override
		public int getInteractionBoxLeft(BuoyTracker.BuoyMapTarget element, Context context, float partialTicks) {
			return -6;
		}

		@Override
		public int getInteractionBoxRight(BuoyTracker.BuoyMapTarget element, Context context, float partialTicks) {
			return 6;
		}

		@Override
		public int getInteractionBoxTop(BuoyTracker.BuoyMapTarget element, Context context, float partialTicks) {
			return -6;
		}

		@Override
		public int getInteractionBoxBottom(BuoyTracker.BuoyMapTarget element, Context context, float partialTicks) {
			return 6;
		}

		@Override
		public int getRenderBoxLeft(BuoyTracker.BuoyMapTarget element, Context context, float partialTicks) {
			return -32;
		}

		@Override
		public int getRenderBoxRight(BuoyTracker.BuoyMapTarget element, Context context, float partialTicks) {
			return 32;
		}

		@Override
		public int getRenderBoxTop(BuoyTracker.BuoyMapTarget element, Context context, float partialTicks) {
			return -8;
		}

		@Override
		public int getRenderBoxBottom(BuoyTracker.BuoyMapTarget element, Context context, float partialTicks) {
			return 16;
		}

		@Override
		public int getLeftSideLength(BuoyTracker.BuoyMapTarget element, Minecraft mc) {
			return mc.font.width(element.getDisplayName());
		}

		@Override
		public String getMenuName(BuoyTracker.BuoyMapTarget element) {
			return element.getDisplayName();
		}

		@Override
		public String getFilterName(BuoyTracker.BuoyMapTarget element) {
			return element.getDisplayName();
		}

		@Override
		public int getMenuTextFillLeftPadding(BuoyTracker.BuoyMapTarget element) {
			return 0;
		}

		@Override
		public int getRightClickTitleBackgroundColor(BuoyTracker.BuoyMapTarget element) {
			return 0xAA000000;
		}

		@Override
		public boolean shouldScaleBoxWithOptionalScale() {
			return true;
		}

		@Override
		public boolean isInteractable(ElementRenderLocation location, BuoyTracker.BuoyMapTarget element) {
			return false;
		}

		@Override
		public boolean isRightClickValid(BuoyTracker.BuoyMapTarget element) {
			return false;
		}
	}
}
