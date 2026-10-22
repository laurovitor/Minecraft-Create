package com.simibubi.create.infrastructure.ponder.scenes;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.electricity.connector.SmallConnectorBlockEntity;
import com.simibubi.create.content.electricity.connector.base.AbstractConnectorBlock;
import com.simibubi.create.content.electricity.connector.base.ConnectorMode;
import com.simibubi.create.content.electricity.energy.IWireNode;
import com.simibubi.create.content.electricity.energy.WireType;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Ponder scenes for electricity and the Rolling Mill, ported from Create Crafts &amp; Additions (MIT, by MRH0).
 */
public class ElectricityScenes {

	public static void electricMotor(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("electric_motor", "Generating Rotational Force using Electric Motors");
		scene.configureBasePlate(0, 0, 5);
		scene.world().showSection(util.select().layer(0), Direction.UP);

		BlockPos motor = util.grid().at(3, 1, 2);
		for (int i = 0; i < 3; i++) {
			scene.idle(5);
			scene.world().showSection(util.select().position(1 + i, 1, 2), Direction.DOWN);
		}

		scene.idle(10);
		scene.effects().rotationDirectionIndicator(motor);
		scene.overlay().showText(50)
			.text("Electric Motors are a compact and configurable source of Rotational Force")
			.placeNearTarget()
			.pointAt(util.vector().topOf(motor));
		scene.idle(50);

		scene.rotateCameraY(90);
		scene.idle(20);

		Vec3 blockSurface = util.vector().blockSurface(motor, Direction.EAST);
		AABB point = new AABB(blockSurface, blockSurface);
		AABB expanded = point.inflate(1 / 16f, 1 / 5f, 1 / 5f);
		scene.overlay().chaseBoundingBoxOutline(PonderPalette.WHITE, blockSurface, point, 1);
		scene.idle(1);
		scene.overlay().chaseBoundingBoxOutline(PonderPalette.WHITE, blockSurface, expanded, 60);
		scene.overlay().showControls(blockSurface, Pointing.DOWN, 60).scroll();
		scene.idle(20);

		scene.overlay().showText(70)
			.attachKeyFrame()
			.text("Scrolling on the back panel changes the RPM of the motor's rotational output")
			.placeNearTarget()
			.pointAt(blockSurface);
		scene.idle(10);
		scene.world().modifyKineticSpeed(util.select().fromTo(1, 1, 2, 3, 1, 2), f -> 4 * f);
		scene.effects().rotationSpeedIndicator(motor);
		scene.idle(70);

		scene.overlay().showText(70)
			.attachKeyFrame()
			.text("The Electric Motor requires a source of energy")
			.placeNearTarget()
			.pointAt(blockSurface);
		scene.idle(80);

		scene.overlay().showText(70)
			.text("The motor's energy consumption is determined by the set RPM")
			.placeNearTarget()
			.pointAt(blockSurface);
		scene.idle(80);
		scene.markAsFinished();
		scene.rotateCameraY(-90);
	}

	public static void alternator(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("alternator", "Generating Electric energy using an Alternator");
		scene.configureBasePlate(1, 0, 4);
		scene.world().showSection(util.select().layer(0), Direction.UP);

		BlockPos generator = util.grid().at(3, 1, 2);
		for (int i = 0; i < 6; i++) {
			scene.idle(5);
			scene.world().showSection(util.select().position(i, 1, 2), Direction.DOWN);
		}

		scene.idle(10);
		scene.overlay().showText(50)
			.text("It generates electric energy from rotational force")
			.placeNearTarget()
			.pointAt(util.vector().topOf(generator));
		scene.idle(60);

		scene.overlay().showText(50)
			.text("Its energy production is determined by the input RPM")
			.placeNearTarget()
			.pointAt(util.vector().topOf(generator));
		scene.idle(60);

		scene.overlay().showText(50)
			.text("Use a connector in Pull mode to take the energy out")
			.placeNearTarget()
			.pointAt(util.vector().topOf(generator));
		scene.idle(60);
		scene.markAsFinished();
	}

	public static void rollingMill(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("rolling_mill", "Rolling metals into Rods and Wires");
		scene.configureBasePlate(1, 0, 4);
		scene.world().showSection(util.select().layer(0), Direction.UP);

		BlockPos mill = util.grid().at(3, 1, 2);
		for (int i = 0; i < 6; i++) {
			scene.idle(5);
			scene.world().showSection(util.select().position(i, 1, 2), Direction.DOWN);
		}

		scene.idle(10);
		scene.overlay().showText(50)
			.text("The Rolling Mill uses rotational force to roll metals into Rods and Wires")
			.placeNearTarget()
			.pointAt(util.vector().topOf(mill));
		scene.idle(60);

		scene.overlay().showText(50)
			.text("To manually input items, drop Ingots or Plates on the top of the Mill")
			.placeNearTarget()
			.pointAt(util.vector().topOf(mill));
		scene.idle(60);

		scene.overlay().showControls(util.vector().topOf(mill), Pointing.DOWN, 50).rightClick();
		scene.overlay().showText(50)
			.attachKeyFrame()
			.text("Right-click the Mill to manually retrieve the output items")
			.placeNearTarget()
			.pointAt(util.vector().topOf(mill));
		scene.idle(60);
		scene.markAsFinished();
	}

	public static void automateRollingMill(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("automated_rolling_mill", "Automating the Rolling Mill");
		scene.configureBasePlate(1, 0, 4);
		scene.world().showSection(util.select().layer(0), Direction.UP);

		BlockPos mill = util.grid().at(3, 2, 2);
		BlockPos in = util.grid().at(3, 2, 3);
		BlockPos out = util.grid().at(3, 2, 1);

		for (int i = 0; i < 3; i++) {
			scene.idle(5);
			scene.world().showSection(util.select().position(i, 1, 4), Direction.DOWN);
		}
		for (int i = 5; i >= 0; i--) {
			scene.idle(5);
			scene.world().showSection(util.select().position(3, 1, i), Direction.DOWN);
			scene.world().showSection(util.select().position(4, 1, i), Direction.DOWN);
			scene.world().showSection(util.select().position(4, 2, i), Direction.DOWN);
		}
		scene.world().showSection(util.select().position(mill), Direction.DOWN);

		scene.overlay().showText(50)
			.attachKeyFrame()
			.text("The Rolling Mill can be automated using a Belt and two Funnels")
			.placeNearTarget()
			.pointAt(util.vector().topOf(mill));
		scene.idle(60);

		scene.world().showSection(util.select().position(in), Direction.NORTH);
		scene.overlay().showText(40)
			.text("Input Funnel")
			.placeNearTarget()
			.pointAt(util.vector().centerOf(in));
		scene.idle(50);
		scene.world().showSection(util.select().position(out), Direction.SOUTH);
		scene.overlay().showText(40)
			.text("Output Funnel")
			.placeNearTarget()
			.pointAt(util.vector().centerOf(out));
		scene.idle(50);
		scene.markAsFinished();
	}

	public static void modularAccumulator(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("modular_accumulator", "The Multi-block Accumulator");
		scene.configureBasePlate(0, 0, 4);
		scene.showBasePlate();
		scene.idle(15);

		BlockPos cIn = new BlockPos(1, 3, 1);
		BlockPos cOut = new BlockPos(2, 3, 2);

		Selection accumulator = util.select().fromTo(1, 1, 1, 2, 2, 2);
		ElementLink<WorldSectionElement> accumulatorLink =
			scene.world().showIndependentSection(accumulator, Direction.EAST);
		scene.idle(15);
		scene.overlay().showOutline(PonderPalette.GREEN, accumulatorLink, accumulator, 50);

		scene.overlay().showText(50)
			.text("The Accumulator is a multi-block structure")
			.placeNearTarget()
			.pointAt(util.vector().centerOf(cIn));
		scene.idle(60);
		scene.overlay().showText(50)
			.text("It can store large amounts of electric energy")
			.placeNearTarget()
			.pointAt(util.vector().centerOf(cIn));
		scene.idle(60);
		scene.world().showSection(util.select().position(cIn), Direction.DOWN);
		scene.idle(5);
		scene.world().showSection(util.select().position(cOut), Direction.DOWN);

		scene.idle(15);
		scene.overlay().showControls(util.vector().centerOf(cIn), Pointing.DOWN, 40)
			.rightClick()
			.withItem(AllItems.WRENCH.asStack());
		scene.world().setBlock(cIn, connector(ConnectorMode.Push), false);
		scene.overlay().showText(50)
			.attachKeyFrame()
			.text("Configure an input connector")
			.placeNearTarget()
			.pointAt(util.vector().centerOf(cIn));
		scene.idle(60);

		scene.overlay().showControls(util.vector().centerOf(cOut), Pointing.DOWN, 40)
			.rightClick()
			.withItem(AllItems.WRENCH.asStack());
		scene.world().setBlock(cOut, connector(ConnectorMode.Pull), false);
		scene.overlay().showText(50)
			.text("and an output connector.")
			.placeNearTarget()
			.pointAt(util.vector().centerOf(cOut));
		scene.idle(60);

		scene.overlay().showText(110)
			.text("For compatibility with other mods, the Accumulator also pushes energy into blocks placed on its top or bottom")
			.placeNearTarget()
			.pointAt(util.vector().centerOf(cOut));
		scene.idle(120);
		scene.markAsFinished();
	}

	private static net.minecraft.world.level.block.state.BlockState connector(ConnectorMode mode) {
		return AllBlocks.SMALL_CONNECTOR.getDefaultState()
			.setValue(AbstractConnectorBlock.FACING, Direction.DOWN)
			.setValue(AbstractConnectorBlock.MODE, mode);
	}

	public static void connector(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("connector", "Transferring Energy using Connectors");
		scene.configureBasePlate(0, 0, 5);
		scene.showBasePlate();

		BlockPos alternator = util.grid().at(4, 1, 2);
		BlockPos motor = util.grid().at(1, 1, 1);
		BlockPos pull = alternator.above();
		BlockPos push = motor.above();
		BlockPos relay = util.grid().at(2, 3, 3);

		Selection largeCog = util.select().position(5, 0, 1);
		Selection alternatorKinetics = util.select().fromTo(4, 1, 2, 5, 1, 2);
		Selection motorKinetics = util.select().fromTo(1, 1, 0, 1, 1, 1);

		// The structure is saved with its wires attached; take them down until the relay is shown.
		scene.world().modifyBlockEntity(relay, SmallConnectorBlockEntity.class, be -> {
			Level level = be.getLevel();
			if (level == null)
				return;
			IWireNode.disconnect(level, relay, pull);
			IWireNode.disconnect(level, relay, push);
		});
		scene.world().setKineticSpeed(largeCog, 32);
		scene.world().setKineticSpeed(alternatorKinetics, -64);
		scene.idle(5);

		scene.world().showSection(alternatorKinetics.add(largeCog), Direction.DOWN);
		scene.idle(5);
		scene.world().showSection(motorKinetics, Direction.DOWN);
		scene.idle(10);

		scene.overlay().showText(60)
			.text("The Alternator is a producer of energy...")
			.placeNearTarget()
			.pointAt(util.vector().topOf(alternator));
		scene.idle(70);

		scene.overlay().showText(60)
			.text("...while the Electric Motor is a consumer of energy")
			.placeNearTarget()
			.pointAt(util.vector().topOf(motor));
		scene.idle(70);

		scene.world().showSection(util.select().position(pull), Direction.DOWN);
		scene.idle(10);
		scene.overlay().showText(80)
			.attachKeyFrame()
			.text("Producers need a connector in Pull mode, which pulls energy out of the block it is attached to")
			.placeNearTarget()
			.pointAt(util.vector().centerOf(pull));
		scene.idle(90);

		scene.world().showSection(util.select().position(push), Direction.DOWN);
		scene.idle(10);
		scene.overlay().showText(80)
			.attachKeyFrame()
			.text("Consumers need a connector in Push mode, which pushes energy into the block it is attached to")
			.placeNearTarget()
			.pointAt(util.vector().centerOf(push));
		scene.idle(90);

		scene.overlay().showControls(util.vector().topOf(push), Pointing.DOWN, 50)
			.rightClick()
			.withItem(AllItems.WRENCH.asStack());
		scene.overlay().showText(50)
			.text("Use a Wrench on a connector to cycle its mode")
			.placeNearTarget()
			.pointAt(util.vector().centerOf(push));
		scene.idle(60);

		scene.world().showSection(util.select().fromTo(2, 1, 3, 2, 2, 3), Direction.DOWN);
		scene.idle(5);
		scene.world().showSection(util.select().position(relay), Direction.DOWN);
		scene.idle(10);
		scene.overlay().showText(70)
			.attachKeyFrame()
			.text("Connectors in None mode do not interact with the block they are attached to...")
			.placeNearTarget()
			.pointAt(util.vector().centerOf(relay));
		scene.idle(80);

		scene.overlay().showText(70)
			.text("...they only relay energy through their wires, so they can be placed on any block")
			.placeNearTarget()
			.pointAt(util.vector().centerOf(relay));
		scene.idle(80);

		scene.overlay().showText(110)
			.attachKeyFrame()
			.text("Connect two connectors with a wire by right-clicking both using a Copper Spool")
			.placeNearTarget()
			.pointAt(util.vector().centerOf(relay));
		scene.idle(10);

		wire(scene, util, relay, 1, pull, 0);
		wire(scene, util, relay, 0, push, 0);

		scene.world().setKineticSpeed(motorKinetics, -32);
		scene.effects().rotationDirectionIndicator(motor.north());
		scene.overlay().showText(60)
			.attachKeyFrame()
			.text("Energy now flows from the Alternator to the Electric Motor")
			.placeNearTarget()
			.pointAt(util.vector().topOf(motor));
		scene.idle(70);
		scene.markAsFinished();
	}

	private static void wire(CreateSceneBuilder scene, SceneBuildingUtil util, BlockPos from, int fromNode, BlockPos to,
		int toNode) {
		scene.overlay().showControls(util.vector().topOf(from), Pointing.DOWN, 20)
			.rightClick()
			.withItem(AllItems.COPPER_SPOOL.asStack());
		scene.idle(25);
		scene.overlay().showControls(util.vector().topOf(to), Pointing.DOWN, 20)
			.rightClick()
			.withItem(AllItems.COPPER_SPOOL.asStack());
		scene.idle(5);
		scene.world().modifyBlockEntity(from, SmallConnectorBlockEntity.class, be -> {
			Level level = be.getLevel();
			if (level != null)
				IWireNode.connect(level, from, fromNode, to, toNode, WireType.COPPER);
		});
		scene.idle(20);
	}

	public static void connectorRemove(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("connector_remove", "Removing Wires using an Empty Spool");
		scene.configureBasePlate(0, 0, 5);
		scene.showBasePlate();

		BlockPos motor = util.grid().at(1, 1, 1);
		BlockPos push = motor.above();
		BlockPos relay = util.grid().at(2, 3, 3);

		Selection largeCog = util.select().position(5, 0, 1);
		Selection motorKinetics = util.select().fromTo(1, 1, 0, 1, 1, 1);

		scene.world().setKineticSpeed(largeCog, 32);
		scene.world().setKineticSpeed(util.select().fromTo(4, 1, 2, 5, 1, 2), -64);
		scene.world().setKineticSpeed(motorKinetics, -32);
		scene.idle(5);

		scene.world().showSection(util.select().layersFrom(1).add(largeCog), Direction.DOWN);
		scene.idle(20);

		scene.overlay().showText(90)
			.text("Right-click both ends of a wire with an Empty Spool to remove it")
			.placeNearTarget()
			.pointAt(util.vector().centerOf(push));
		scene.idle(20);

		scene.overlay().showControls(util.vector().topOf(push), Pointing.DOWN, 20)
			.rightClick()
			.withItem(AllItems.SPOOL.asStack());
		scene.idle(25);
		scene.overlay().showControls(util.vector().topOf(relay), Pointing.DOWN, 20)
			.rightClick()
			.withItem(AllItems.SPOOL.asStack());
		scene.idle(5);
		scene.world().modifyBlockEntity(relay, SmallConnectorBlockEntity.class, be -> {
			Level level = be.getLevel();
			if (level != null)
				IWireNode.disconnect(level, relay, push);
		});
		scene.idle(20);
		scene.world().setKineticSpeed(motorKinetics, 0);
		scene.idle(30);

		scene.overlay().showText(60)
			.text("The wire is returned as a filled Spool")
			.placeNearTarget()
			.pointAt(util.vector().centerOf(relay));
		scene.idle(70);
		scene.markAsFinished();
	}
}
