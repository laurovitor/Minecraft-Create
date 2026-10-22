package com.simibubi.create.infrastructure.ponder.scenes;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.electricity.connector.SmallConnectorBlockEntity;
import com.simibubi.create.content.electricity.energy.IWireNode;
import com.simibubi.create.content.electricity.energy.WireType;
import com.simibubi.create.content.electricity.portable.PortableEnergyInterfaceBlockEntity;
import com.simibubi.create.content.electricity.pump.ElectricPumpBlockEntity;
import com.simibubi.create.content.electricity.tesla.TeslaCoilBlock;
import com.simibubi.create.content.fluids.pump.PumpBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.redstone.analogLever.AnalogLeverBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;

import net.createmod.catnip.api.math.Pointing;
import net.createmod.ponder.api.client.PonderPalette;
import net.createmod.ponder.api.client.element.ElementLink;
import net.createmod.ponder.api.client.element.WorldSectionElement;
import net.createmod.ponder.api.client.scene.SceneBuilder;
import net.createmod.ponder.api.client.scene.SceneBuildingUtil;
import net.createmod.ponder.api.client.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Ponder scenes for the Tesla Coil, Liquid Blaze Burner, Portable Energy Interface, Electric Pump and Servo
 * Motor, ported from Create Crafts &amp; Additions (MIT, by MRH0).
 */
public class ElectricityScenes2 {

	public static void teslaCoil(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("tesla_coil", "Charging Items with the Tesla Coil");
		scene.configureBasePlate(0, 0, 5);
		scene.showBasePlate();
		scene.idle(5);

		BlockPos depot = util.grid().at(2, 1, 2);
		scene.world().showSection(util.select().position(depot), Direction.DOWN);
		scene.idle(5);
		scene.world().showSection(util.select().position(2, 3, 2), Direction.DOWN);
		scene.idle(5);
		Vec3 topOf = util.vector().topOf(depot);
		scene.overlay().showText(50)
			.attachKeyFrame()
			.text("A Tesla Coil charges items held on a Belt or Depot below it")
			.placeNearTarget()
			.pointAt(topOf);
		scene.idle(60);

		scene.world().createItemOnBeltLike(depot, Direction.NORTH, AllItems.CAPACITOR.asStack());
		scene.idle(10);
		scene.world().setBlock(util.grid().at(2, 3, 2), AllBlocks.TESLA_COIL.getDefaultState()
			.setValue(TeslaCoilBlock.FACING, Direction.UP)
			.setValue(TeslaCoilBlock.POWERED, true), false);
		scene.overlay().showText(70)
			.attachKeyFrame()
			.text("It fills items that store energy (FE) and processes Charging recipes")
			.placeNearTarget()
			.pointAt(topOf);
		scene.idle(80);
		scene.markAsFinished();
	}

	public static void teslaCoilHurt(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("tesla_coil_hurt", "Dangerous Tesla Coils");
		scene.configureBasePlate(0, 0, 5);
		scene.showBasePlate();
		scene.idle(5);

		BlockPos coil = util.grid().at(2, 1, 2);
		BlockPos lever = util.grid().at(2, 1, 1);
		scene.world().showSection(util.select().position(coil), Direction.DOWN);
		scene.idle(5);
		scene.overlay().showText(70)
			.attachKeyFrame()
			.text("The Tesla Coil can also shock nearby players and mobs")
			.placeNearTarget()
			.pointAt(util.vector().topOf(coil));
		scene.idle(80);
		scene.world().showSection(util.select().position(lever), Direction.SOUTH);

		scene.idle(5);
		scene.overlay().showText(50)
			.attachKeyFrame()
			.text("This is triggered by a Redstone signal")
			.placeNearTarget()
			.pointAt(util.vector().centerOf(lever));
		scene.idle(60);
		scene.world().setBlock(lever, Blocks.LEVER.defaultBlockState()
			.setValue(LeverBlock.POWERED, true)
			.setValue(LeverBlock.FACING, Direction.SOUTH)
			.setValue(LeverBlock.FACE, AttachFace.FLOOR), false);
		scene.idle(5);
		scene.world().setBlock(coil, AllBlocks.TESLA_COIL.getDefaultState()
			.setValue(TeslaCoilBlock.FACING, Direction.DOWN)
			.setValue(TeslaCoilBlock.POWERED, true), false);
		scene.idle(5);
		scene.overlay().showText(70)
			.attachKeyFrame()
			.text("A full set of chainmail armor protects against the shock")
			.placeNearTarget()
			.pointAt(util.vector().topOf(coil));
		scene.idle(80);
		scene.markAsFinished();
	}

	public static void liquidBlazeBurner(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("liquid_blaze_burner", "Burning Liquid Fuels");
		scene.configureBasePlate(0, 0, 5);
		scene.showBasePlate();
		scene.idle(5);

		BlockPos burner = util.grid().at(2, 1, 2);
		BlockPos[] pipes = { util.grid().at(1, 1, 2), util.grid().at(0, 1, 2), util.grid().at(0, 2, 2),
			util.grid().at(0, 3, 2) };
		scene.world().showSection(util.select().position(burner), Direction.DOWN);
		scene.idle(5);
		scene.overlay().showText(50)
			.attachKeyFrame()
			.text("Giving a Blaze Burner a Straw")
			.placeNearTarget()
			.pointAt(util.vector().topOf(burner));
		scene.idle(10);
		scene.overlay().showControls(util.vector().topOf(burner), Pointing.DOWN, 40)
			.rightClick()
			.withItem(AllItems.STRAW.asStack());
		scene.world().setBlock(burner, AllBlocks.LIQUID_BLAZE_BURNER.getDefaultState()
			.setValue(BlazeBurnerBlock.HEAT_LEVEL, BlazeBurnerBlock.HeatLevel.SMOULDERING), false);
		scene.idle(60);
		scene.overlay().showText(50)
			.attachKeyFrame()
			.text("lets it accept liquid fuels from buckets...")
			.placeNearTarget()
			.pointAt(util.vector().topOf(burner));
		scene.idle(10);
		scene.overlay().showControls(util.vector().topOf(burner), Pointing.DOWN, 40)
			.rightClick()
			.withItem(new ItemStack(Items.LAVA_BUCKET));
		scene.idle(60);
		scene.overlay().showText(50)
			.text("...or from pipes")
			.placeNearTarget()
			.pointAt(util.vector().topOf(burner));
		scene.idle(10);

		for (BlockPos pipe : pipes) {
			scene.idle(5);
			scene.world().showSection(util.select().position(pipe), Direction.EAST);
		}
		scene.idle(20);
		scene.markAsFinished();
	}

	public static void peiTransfer(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("pei_transfer", "Contraption Energy Exchange");
		scene.configureBasePlate(0, 0, 6);
		scene.scaleSceneView(0.95f);
		scene.setSceneOffsetY(-1);
		scene.world().showSection(util.select().layer(0), Direction.UP);
		scene.idle(5);

		BlockPos bearing = util.grid().at(5, 1, 2);
		scene.world().showSection(util.select().position(bearing), Direction.DOWN);
		scene.idle(5);
		ElementLink<WorldSectionElement> contraption =
			scene.world().showIndependentSection(util.select().fromTo(5, 2, 2, 6, 4, 2), Direction.DOWN);
		scene.world().configureCenterOfRotation(contraption, util.vector().centerOf(bearing));
		scene.idle(10);
		scene.world().rotateBearing(bearing, 360, 70);
		scene.world().rotateSection(contraption, 0, 360, 0, 70);
		scene.overlay().showText(60)
			.pointAt(util.vector().topOf(bearing.above(2)))
			.colored(PonderPalette.RED)
			.placeNearTarget()
			.attachKeyFrame()
			.text("Accumulators on moving contraptions cannot be wired up");

		scene.idle(70);
		BlockPos pei = util.grid().at(4, 2, 2);
		scene.world().showSectionAndMerge(util.select().position(pei), Direction.EAST, contraption);
		scene.idle(13);
		scene.effects().superGlue(pei, Direction.EAST, true);

		scene.overlay().showText(80)
			.pointAt(util.vector().topOf(pei))
			.colored(PonderPalette.GREEN)
			.placeNearTarget()
			.attachKeyFrame()
			.text("This component can exchange energy without the need to stop the contraption");
		scene.idle(90);

		BlockPos pei2 = pei.west(2);
		scene.world().showSection(util.select().position(pei2), Direction.DOWN);
		scene.overlay().showOutlineWithText(util.select().position(pei.west()), 50)
			.colored(PonderPalette.RED)
			.placeNearTarget()
			.attachKeyFrame()
			.text("Place a second one with a gap of 1 or 2 blocks in between");
		scene.idle(55);

		scene.world().rotateBearing(bearing, 360, 60);
		scene.world().rotateSection(contraption, 0, 360, 0, 60);
		scene.idle(20);

		scene.overlay().showText(40)
			.placeNearTarget()
			.pointAt(util.vector().of(3, 3, 2.5))
			.text("Whenever they pass by each other, they will engage in a connection");
		scene.idle(35);

		Selection both = util.select().fromTo(2, 2, 2, 4, 2, 2);
		Class<PortableEnergyInterfaceBlockEntity> peiClass = PortableEnergyInterfaceBlockEntity.class;
		scene.world().modifyBlockEntityNBT(both, peiClass, nbt -> {
			nbt.putFloat("Distance", 1);
			nbt.putFloat("Timer", 40);
		});

		scene.idle(20);
		scene.overlay().showOutline(PonderPalette.GREEN, pei, util.select().fromTo(5, 3, 2, 6, 4, 2), 80);
		scene.idle(10);

		scene.overlay().showOutlineWithText(util.select().position(pei2), 70)
			.placeNearTarget()
			.colored(PonderPalette.GREEN)
			.attachKeyFrame()
			.text("While engaged, the stationary interface will represent ALL Accumulators on the contraption");
		scene.idle(80);

		BlockPos connector = util.grid().at(2, 3, 2);
		scene.world().showSection(util.select().position(connector), Direction.DOWN);
		scene.overlay().showText(70)
			.placeNearTarget()
			.pointAt(util.vector().centerOf(connector))
			.attachKeyFrame()
			.text("Energy can now be pushed in or pulled out...");
		scene.idle(80);

		scene.overlay().showText(120)
			.placeNearTarget()
			.pointAt(util.vector().centerOf(pei2))
			.text("After no energy has been exchanged for a while, the contraption will continue on its way");
		scene.world().modifyBlockEntityNBT(both, peiClass, nbt -> nbt.putFloat("Timer", 9));

		scene.idle(15);
		scene.world().rotateBearing(bearing, 270, 120);
		scene.world().rotateSection(contraption, 0, 270, 0, 120);
		scene.markAsFinished();
	}

	public static void peiRedstone(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("pei_redstone", "Redstone Control");
		scene.configureBasePlate(0, 0, 5);
		scene.setSceneOffsetY(-1);

		Class<PortableEnergyInterfaceBlockEntity> peiClass = PortableEnergyInterfaceBlockEntity.class;
		Selection peis = util.select().fromTo(1, 1, 3, 1, 3, 3);
		scene.world().modifyBlockEntityNBT(peis, peiClass, nbt -> {
			nbt.putFloat("Distance", 1);
			nbt.putFloat("Timer", 40);
		});

		scene.world().showSection(util.select().layer(0), Direction.UP);
		scene.idle(5);
		scene.world().showSection(util.select().layer(1), Direction.DOWN);
		scene.idle(5);

		ElementLink<WorldSectionElement> contraption =
			scene.world().showIndependentSection(util.select().layersFrom(2), Direction.DOWN);
		BlockPos bearing = util.grid().at(3, 1, 3);
		scene.world().configureCenterOfRotation(contraption, util.vector().topOf(bearing));
		scene.idle(20);
		scene.world().modifyBlockEntityNBT(peis, peiClass, nbt -> nbt.putFloat("Timer", 9));
		scene.idle(20);
		scene.world().rotateBearing(bearing, 360 * 3 + 270, 240 + 60);
		scene.world().rotateSection(contraption, 0, 360 * 3 + 270, 0, 240 + 60);
		scene.idle(20);

		scene.world().toggleRedstonePower(util.select().fromTo(1, 1, 1, 1, 1, 2));
		scene.effects().indicateRedstone(util.grid().at(1, 1, 1));
		scene.idle(10);

		scene.overlay().showOutlineWithText(util.select().position(1, 1, 3), 120)
			.colored(PonderPalette.RED)
			.text("Redstone power will prevent the stationary interface from engaging");
		scene.idle(20);
		scene.markAsFinished();
	}

	public static void electricPumpFlow(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("electric_pump_flow", "Fluid Transportation using Electric Pumps");
		scene.configureBasePlate(0, 0, 5);
		scene.showBasePlate();
		scene.world().multiplyKineticSpeed(util.select().everywhere(), -1);
		scene.idle(5);

		BlockPos pumpPos = util.grid().at(2, 1, 1);
		Selection tank1 = util.select().fromTo(0, 2, 3, 0, 1, 3);
		Selection tank2 = util.select().fromTo(4, 2, 3, 4, 1, 3);
		Selection pipes = util.select().fromTo(3, 1, 3, 1, 1, 1);
		Selection pump = util.select().position(pumpPos);
		BlockPos connectorPos = pumpPos.above();
		BlockPos connector2Pos = connectorPos.offset(2, 0, -1);
		BlockPos accumulatorPos = connector2Pos.below();

		scene.world().setBlock(pumpPos, AllBlocks.FLUID_PIPE.get()
			.getAxisState(Axis.X), false);

		scene.world().showSection(tank1, Direction.DOWN);
		scene.idle(5);
		scene.world().showSection(tank2, Direction.DOWN);
		scene.idle(5);
		scene.world().showSection(pipes, Direction.NORTH);
		scene.idle(5);

		scene.world().destroyBlock(pumpPos);
		scene.world().restoreBlocks(pump);
		scene.world().modifyBlock(pumpPos, s -> s.setValue(PumpBlock.FACING, s.getValue(PumpBlock.FACING)
			.getOpposite()), false);
		scene.world().setKineticSpeed(pump, 0);
		scene.idle(15);

		scene.overlay().showText(60)
			.text("Electric Pumps govern the flow of their attached pipe networks")
			.attachKeyFrame()
			.placeNearTarget()
			.pointAt(util.vector().topOf(pumpPos));
		scene.idle(70);

		scene.world().showSection(util.select().position(connectorPos), Direction.DOWN);
		scene.world().showSection(util.select().position(connector2Pos), Direction.DOWN);
		scene.world().showSection(util.select().position(accumulatorPos), Direction.DOWN);
		scene.world().modifyBlockEntity(connectorPos, SmallConnectorBlockEntity.class, be -> {
			Level level = be.getLevel();
			if (level != null)
				IWireNode.connect(level, connectorPos, be.getAvailableNode(), connector2Pos, 0, WireType.COPPER);
		});
		scene.idle(15);
		scene.overlay().showText(60)
			.text("They run on energy instead of Rotational Force; scroll on their side to set the speed")
			.attachKeyFrame()
			.placeNearTarget()
			.pointAt(util.vector().blockSurface(pumpPos, Direction.NORTH));
		scene.idle(70);

		scene.world().setKineticSpeed(pump, 64);
		scene.world().modifyBlockEntity(pumpPos, ElectricPumpBlockEntity.class, be -> {
			be.getBehaviour(ScrollValueBehaviour.TYPE)
				.setValue(64);
			be.setActive(true);
		});
		scene.world().propagatePipeChange(pumpPos);
		scene.effects().rotationDirectionIndicator(pumpPos.north());
		scene.idle(15);

		scene.overlay().showText(60)
			.text("Their arrow indicates the direction of flow")
			.attachKeyFrame()
			.placeNearTarget()
			.pointAt(util.vector().topOf(pumpPos)
				.subtract(0.5f, 0.125f, 0));

		AABB bb1 = new AABB(Vec3.ZERO, Vec3.ZERO).inflate(.25, .25, 0)
			.move(0, 0, .25);
		AABB bb2 = new AABB(Vec3.ZERO, Vec3.ZERO).inflate(.25, .25, 1.25);
		scene.idle(65);

		Object in = new Object();
		Object out = new Object();

		scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, in, bb1.move(util.vector().centerOf(3, 1, 3)), 3);
		scene.idle(2);
		scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, in, bb2.move(util.vector().centerOf(3, 1, 2)), 50);
		scene.idle(10);

		scene.overlay().showText(50)
			.text("The network behind is now pulling fluids...")
			.attachKeyFrame()
			.placeNearTarget()
			.colored(PonderPalette.INPUT)
			.pointAt(util.vector().centerOf(3, 1, 2));
		scene.idle(60);

		scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, out, bb1.move(util.vector().centerOf(1, 1, 1)
			.add(0, 0, -.5)), 3);
		scene.idle(2);
		scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, out, bb2.move(util.vector().centerOf(1, 1, 2)), 50);
		scene.idle(10);

		scene.overlay().showText(50)
			.text("...while the network in front is transferring it outward")
			.placeNearTarget()
			.colored(PonderPalette.OUTPUT)
			.pointAt(util.vector().centerOf(1, 1, 2));
		scene.idle(25);

		scene.overlay().showControls(util.vector().topOf(pumpPos), Pointing.DOWN, 40)
			.rightClick()
			.withItem(AllItems.WRENCH.asStack());
		scene.idle(7);
		scene.world().modifyBlock(pumpPos, s -> s.setValue(PumpBlock.FACING, Direction.EAST), true);
		scene.overlay().showText(70)
			.attachKeyFrame()
			.pointAt(util.vector().centerOf(2, 1, 1))
			.placeNearTarget()
			.text("A Wrench can be used to reverse the direction");
		scene.world().propagatePipeChange(pumpPos);
		scene.idle(40);

		scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, in, bb1.move(util.vector().centerOf(3, 1, 3)), 3);
		scene.idle(2);
		scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, in, bb2.move(util.vector().centerOf(3, 1, 2)), 30);
		scene.idle(15);
		scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, out, bb1.move(util.vector().centerOf(1, 1, 1)
			.add(0, 0, -.5)), 3);
		scene.idle(2);
		scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, out, bb2.move(util.vector().centerOf(1, 1, 2)), 30);
		scene.idle(25);
		scene.markAsFinished();
	}

	public static void servoMotor(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("servo_motor", "Rotating Blocks to precise Angles using the Servo Motor");
		scene.configureBasePlate(0, 0, 5);
		scene.showBasePlate();
		scene.idle(5);

		BlockPos servo = util.grid().at(2, 1, 1);
		BlockPos connector = util.grid().at(2, 1, 2);
		Selection minLever = util.select().position(1, 1, 1);
		Selection maxLever = util.select().position(3, 1, 1);
		Vec3 minLeverVec = util.vector().centerOf(1, 1, 1).add(0, -.25, 0);
		Vec3 maxLeverVec = util.vector().centerOf(3, 1, 1).add(0, -.25, 0);

		scene.world().showSection(util.select().position(servo), Direction.DOWN);
		scene.idle(15);

		scene.overlay().showText(60)
			.text("The Servo Motor uses redstone to rotate attached blocks to a precise angle")
			.placeNearTarget()
			.pointAt(util.vector().topOf(servo));
		scene.idle(70);

		scene.world().showSection(util.select().position(connector)
			.add(util.select().fromTo(4, 1, 3, 4, 2, 3)), Direction.DOWN);
		scene.idle(15);
		scene.overlay().showText(50)
			.text("It needs a source of electric energy (FE) to operate")
			.placeNearTarget()
			.pointAt(util.vector().centerOf(connector));
		scene.idle(60);

		ElementLink<WorldSectionElement> observer =
			scene.world().showIndependentSection(util.select().position(servo.above()), Direction.DOWN);
		scene.world().configureCenterOfRotation(observer, util.vector().centerOf(servo));
		scene.idle(15);

		scene.world().showSection(minLever, Direction.DOWN);
		scene.idle(5);
		scene.world().showSection(maxLever, Direction.DOWN);
		scene.idle(15);

		// Max side 15, min side 0: 0° -> 90°
		scene.addKeyframe();
		scene.overlay().showControls(maxLeverVec, Pointing.DOWN, 30).rightClick();
		scene.idle(7);
		rotateServo(scene, servo, observer, 90, 30);
		turnLever(scene, maxLever, 0, 15);
		scene.overlay().showText(60)
			.text("Redstone power on this side turns the Servo towards its Max Angle...")
			.placeNearTarget()
			.pointAt(maxLeverVec);
		scene.idle(70);

		// Max side 0: back to 0°
		scene.overlay().showControls(maxLeverVec, Pointing.DOWN, 30).rightClick().whileSneaking();
		scene.idle(7);
		rotateServo(scene, servo, observer, -90, 30);
		turnLever(scene, maxLever, 15, 0);
		scene.idle(10);

		// Min side 15: 0° -> -90°
		scene.addKeyframe();
		scene.overlay().showControls(minLeverVec, Pointing.DOWN, 30).rightClick();
		scene.idle(7);
		rotateServo(scene, servo, observer, -90, 30);
		turnLever(scene, minLever, 0, 15);
		scene.overlay().showText(60)
			.text("...while power on the opposite side turns it towards its Min Angle")
			.placeNearTarget()
			.pointAt(minLeverVec);
		scene.idle(70);

		// Both sides 15, net 0: -90° -> 0°
		scene.addKeyframe();
		scene.overlay().showControls(maxLeverVec, Pointing.DOWN, 30).rightClick();
		scene.idle(7);
		rotateServo(scene, servo, observer, 90, 30);
		turnLever(scene, maxLever, 0, 15);
		scene.overlay().showText(80)
			.text("The signals from both sides are summed, with the Min side counting as negative, so equal signals cancel each other out")
			.placeNearTarget()
			.pointAt(util.vector().topOf(servo.above()));
		scene.idle(90);

		// Min side 5, net 10: 0° -> 60°
		scene.addKeyframe();
		scene.overlay().showControls(minLeverVec, Pointing.DOWN, 30).rightClick().whileSneaking();
		scene.idle(7);
		rotateServo(scene, servo, observer, 60, 20);
		turnLever(scene, minLever, 15, 5);
		scene.overlay().showText(70)
			.text("How far the Servo turns depends on the strength of the summed signal")
			.placeNearTarget()
			.pointAt(util.vector().topOf(servo.above()));
		scene.idle(80);

		scene.rotateCameraY(90);
		scene.idle(20);

		Vec3 maxAngleSlot = util.vector().blockSurface(servo, Direction.EAST).add(0, -2 / 16f, 0);
		AABB point = new AABB(maxAngleSlot, maxAngleSlot);
		AABB expanded = point.inflate(1 / 16f, 1 / 5f, 1 / 5f);

		scene.addKeyframe();
		scene.overlay().chaseBoundingBoxOutline(PonderPalette.WHITE, maxAngleSlot, point, 1);
		scene.idle(1);
		scene.overlay().chaseBoundingBoxOutline(PonderPalette.WHITE, maxAngleSlot, expanded, 80);
		scene.overlay().showControls(maxAngleSlot, Pointing.DOWN, 80).scroll();
		scene.overlay().showText(80)
			.text("Scroll on the sides of the Servo to adjust its Max and Min Angles between 0° and 180°")
			.placeNearTarget()
			.pointAt(maxAngleSlot);
		scene.idle(90);

		// Max angle 90° -> 180°, same net 10: 60° -> 120°
		rotateServo(scene, servo, observer, 60, 20);
		scene.idle(20);
		scene.overlay().showText(70)
			.text("With the Max Angle raised to 180°, the same signal now turns it twice as far")
			.placeNearTarget()
			.pointAt(util.vector().topOf(servo.above()));
		scene.idle(80);
		scene.markAsFinished();
	}

	private static void rotateServo(CreateSceneBuilder scene, BlockPos servo,
		ElementLink<WorldSectionElement> attached, float angle, int duration) {
		scene.world().rotateBearing(servo, angle, duration);
		scene.world().rotateSection(attached, 0, angle, 0, duration);
	}

	/** Steps an Analog Lever one power level every 2 ticks. */
	private static void turnLever(CreateSceneBuilder scene, Selection lever, int from, int to) {
		int step = Integer.signum(to - from);
		for (int state = from + step; state != to + step; state += step) {
			final int power = state;
			scene.idle(2);
			scene.world().modifyBlockEntityNBT(lever, AnalogLeverBlockEntity.class, nbt -> nbt.putInt("State", power));
		}
	}
}
