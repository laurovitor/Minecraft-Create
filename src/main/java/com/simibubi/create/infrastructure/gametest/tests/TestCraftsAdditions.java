package com.simibubi.create.infrastructure.gametest.tests;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllFluids;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.electricity.connector.base.AbstractConnectorBlock;
import com.simibubi.create.content.electricity.energy.IWireNode;
import com.simibubi.create.infrastructure.gametest.CreateGameTestHelper;
import com.simibubi.create.content.logistics.packagePort.frogport.FrogportBlockEntity;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.infrastructure.gametest.GameTest;
import com.simibubi.create.infrastructure.gametest.GameTestGroup;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * Regression tests for the Crafts & Additions content merged into Create, built on an empty platform.
 */
@GameTestGroup(path = "cca")
public class TestCraftsAdditions {

	private static final BlockPos CENTER = new BlockPos(3, 1, 3);

	@GameTest(template = "empty_platform")
	public static void basinBucketBioethanol(CreateGameTestHelper helper) {
		basinBucket(helper, AllFluids.BIOETHANOL.getSource(), AllFluids.BIOETHANOL.getBucket().orElseThrow());
	}

	@GameTest(template = "empty_platform")
	public static void basinBucketSeedOil(CreateGameTestHelper helper) {
		basinBucket(helper, AllFluids.SEED_OIL.getSource(), AllFluids.SEED_OIL.getBucket().orElseThrow());
	}

	@GameTest(template = "empty_platform")
	public static void basinBucketHoney(CreateGameTestHelper helper) {
		basinBucket(helper, AllFluids.HONEY.getSource(), AllFluids.HONEY.getBucket().orElseThrow());
	}

	@GameTest(template = "empty_platform")
	public static void basinBucketCreative(CreateGameTestHelper helper) {
		basinBucket(helper, AllFluids.BIOETHANOL.getSource(), AllFluids.BIOETHANOL.getBucket().orElseThrow(),
			GameType.CREATIVE);
	}

	/**
	 * Right-clicking a basin holding a bucket of fluid with an empty bucket must hand back the filled
	 * bucket and leave the basin empty, never consume the fluid for an empty bucket. In creative the
	 * empty bucket stays in hand and the filled one is added, as vanilla does with a cauldron.
	 */
	private static void basinBucket(CreateGameTestHelper helper, Fluid fluid, Item filledBucket) {
		basinBucket(helper, fluid, filledBucket, GameType.SURVIVAL);
	}

	private static void basinBucket(CreateGameTestHelper helper, Fluid fluid, Item filledBucket, GameType mode) {
		helper.setBlock(CENTER, AllBlocks.BASIN.getDefaultState());
		try (Transaction t = Transaction.openRoot()) {
			int inserted = helper.fluidStorageAt(CENTER).insert(FluidResource.of(fluid), 1000, t);
			t.commit();
			if (inserted != 1000)
				helper.fail("basin accepted " + inserted + " mB of " + fluid);
		}
		Player player = helper.makeMockPlayer(mode);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));
		helper.useBlock(CENTER, player);
		helper.succeedIf(() -> {
			if (!player.getInventory().contains(new ItemStack(filledBucket)))
				helper.fail("no " + filledBucket + " handed back; held " + player.getMainHandItem());
			helper.assertTankEmpty(CENTER);
		});
	}

	/**
	 * Clicking one connector and then another with a spool links them with a wire.
	 */
	@GameTest(template = "empty_platform")
	public static void spoolConnectsConnectors(CreateGameTestHelper helper) {
		BlockPos a = new BlockPos(1, 1, 3);
		BlockPos b = new BlockPos(5, 1, 3);
		BlockState connector = AllBlocks.SMALL_CONNECTOR.getDefaultState()
			.setValue(AbstractConnectorBlock.FACING, Direction.DOWN);
		helper.setBlock(a, connector);
		helper.setBlock(b, connector);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, AllItems.COPPER_SPOOL.asStack());
		helper.useBlock(a, player);
		helper.useBlock(b, player);
		helper.succeedIf(() -> {
			IWireNode node = (IWireNode) helper.getBlockEntity(a, BlockEntity.class);
			if (!node.hasConnectionTo(helper.absolutePos(b)))
				helper.fail("connectors not linked; spool now " + player.getMainHandItem() + " "
					+ player.getMainHandItem().get(DataComponents.CUSTOM_DATA));
		});
	}

	@GameTest(template = "empty_platform")
	public static void spoolVariantsConnect(CreateGameTestHelper helper) {
		BlockPos[] positions = { new BlockPos(1, 1, 1), new BlockPos(5, 1, 1),
			new BlockPos(1, 1, 3), new BlockPos(5, 1, 3),
			new BlockPos(1, 1, 5), new BlockPos(5, 1, 5) };
		ItemStack[] spools = { AllItems.COPPER_SPOOL.asStack(), AllItems.IRON_SPOOL.asStack(),
			AllItems.GOLD_SPOOL.asStack() };
		for (int pair = 0; pair < spools.length; pair++) {
			BlockPos a = positions[pair * 2];
			BlockPos b = positions[pair * 2 + 1];
			BlockState connector = AllBlocks.SMALL_CONNECTOR.getDefaultState()
				.setValue(AbstractConnectorBlock.FACING, Direction.DOWN);
			helper.setBlock(a, connector);
			helper.setBlock(b, connector);
			Player player = helper.makeMockPlayer(GameType.SURVIVAL);
			player.setItemInHand(InteractionHand.MAIN_HAND, spools[pair]);
			helper.useBlock(a, player);
			helper.useBlock(b, player);
			IWireNode node = (IWireNode) helper.getBlockEntity(a, BlockEntity.class);
			if (!node.hasConnectionTo(helper.absolutePos(b)))
				helper.fail("spool did not connect: " + spools[pair].getItem());
		}
		helper.succeed();
	}
	@GameTest(template = "empty_platform")
	public static void blankFrogportAddressIsWildcard(CreateGameTestHelper helper) {
		helper.setBlock(CENTER, AllBlocks.PACKAGE_FROGPORT.getDefaultState());
		FrogportBlockEntity port = helper.getBlockEntity(CENTER, FrogportBlockEntity.class);
		port.acceptsPackages = true;
		port.addressFilter = "";
		if (!PackageItem.matchAddress("Some Destination", port.getFilterString()))
			helper.fail("blank frogport did not accept an addressed package");
		port.acceptsPackages = false;
		if (port.getFilterString() != null)
			helper.fail("disabled frogport still accepts packages");
		helper.succeed();
	}

}
