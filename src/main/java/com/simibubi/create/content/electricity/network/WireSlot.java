package com.simibubi.create.content.electricity.network;

import com.simibubi.create.content.electricity.energy.WireType;

record WireSlot(int index, long otherPos, int otherIndex, WireType type) {}
