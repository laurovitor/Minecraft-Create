package com.simibubi.create.content.redstone.displayLink.target;

import java.util.List;

import com.simibubi.create.api.behaviour.display.DisplayTarget;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;

import net.createmod.catnip.api.data.Couple;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.entity.SignTextSlot;

public class SignDisplayTarget extends DisplayTarget {

	@Override
	public void acceptText(int line, List<MutableComponent> text, DisplayLinkContext context) {
		BlockEntity be = context.getTargetBlockEntity();
		if (!(be instanceof SignBlockEntity sign))
			return;

		boolean changed = false;
		Couple<SignText> signText = Couple.createWithContext(front -> sign.getText(front ? SignTextSlot.FRONT : SignTextSlot.BACK));
		for (int i = 0; i < text.size() && i + line < 4; i++) {
			if (i == 0)
				reserve(i + line, sign, context);
			if (i > 0 && isReserved(i + line, sign, context))
				break;

			final int iFinal = i;
			String content = text.get(iFinal)
				.getString(sign.getMaxTextLineWidth());
			signText = signText.map(st -> st.asMutable().setLine(iFinal + line, Component.literal(content)).asImmutable());
			changed = true;
		}

		if (changed) {
			signText.forEachWithContext((st, front) -> sign.setText(st, front ? SignTextSlot.FRONT : SignTextSlot.BACK));
			context.level()
				.sendBlockUpdated(context.getTargetPos(), sign.getBlockState(), sign.getBlockState(), 2);
		}
	}

	@Override
	public DisplayTargetStats provideStats(DisplayLinkContext context) {
		return new DisplayTargetStats(4, 15, this);
	}

	@Override
	public boolean requiresComponentSanitization() {
		return true;
	}

}
