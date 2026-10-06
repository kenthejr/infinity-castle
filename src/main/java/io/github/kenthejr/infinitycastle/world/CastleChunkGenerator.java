package io.github.kenthejr.infinitycastle.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.kenthejr.infinitycastle.gen.CastleGeometry;
import io.github.kenthejr.infinitycastle.gen.CastleLayout;
import io.github.kenthejr.infinitycastle.gen.CellPlacer;
import io.github.kenthejr.infinitycastle.gen.CellPlan;
import io.github.kenthejr.infinitycastle.gen.Half;
import io.github.kenthejr.infinitycastle.gravity.FloorProfile;
import io.github.kenthejr.infinitycastle.gravity.GravityRules;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import org.jspecify.annotations.Nullable;

/**
 * Generates the castle. Everything is decided by {@link CastleLayout} from the world seed, one cell per two-by-two
 * block of chunks, so there are no structures, features or noise involved. Each chunk builds the whole cell it belongs
 * to and keeps its own quarter.
 */
public class CastleChunkGenerator extends ChunkGenerator {
	public static final MapCodec<CastleChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance.group(BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource))
			.apply(instance, instance.stable(CastleChunkGenerator::new))
	);

	public CastleChunkGenerator(BiomeSource biomeSource) {
		super(biomeSource);
	}

	@Override
	protected MapCodec<? extends ChunkGenerator> codec() {
		return CODEC;
	}

	public static CastleLayout layout(RandomState randomState) {
		return new CastleLayout(randomState.seed());
	}

	@Override
	public CompletableFuture<ChunkAccess> buildTerrain(
		ChunkAccess chunk,
		Blender blender,
		RandomState randomState,
		StructureManager structureManager,
		BiomeManager biomeManager,
		@Nullable WorldGenRegion carverBiomeRegion,
		Set<Holder<Biome>> possibleBiomes
	) {
		ChunkPos pos = chunk.getPos();
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
		Heightmap oceanFloor = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
		Heightmap worldSurface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
		int originX = SectionPos.sectionToBlockCoord(pos.x());
		int originZ = SectionPos.sectionToBlockCoord(pos.z());

		CellPlacer.placeChunk(layout(randomState), pos.x(), pos.z(), (x, y, z, piece) -> {
			if (chunk.isOutsideBuildHeight(y)) {
				return;
			}
			BlockState state = CastlePalette.state(piece);
			cursor.set(originX + x, y, originZ + z);
			chunk.setBlockState(cursor, state);
			oceanFloor.update(x, y, z, state);
			worldSurface.update(x, y, z, state);
			if (CastlePalette.needsPostProcessing(piece)) {
				chunk.markPosForPostProcessing(cursor);
			}
		});
		return CompletableFuture.completedFuture(chunk);
	}

	@Override
	public void spawnOriginalMobs(WorldGenRegion region) {
	}

	@Override
	public int getGenDepth() {
		return CastleGeometry.HEIGHT;
	}

	@Override
	public int getSeaLevel() {
		return CastleGeometry.MIN_Y;
	}

	@Override
	public int getMinY() {
		return CastleGeometry.MIN_Y;
	}

	@Override
	public int getSpawnHeight(LevelHeightAccessor heightAccessor) {
		return CastleGeometry.entranceStandY();
	}

	@Override
	public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor heightAccessor, RandomState randomState) {
		return heightAccessor.getMinY();
	}

	@Override
	public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor heightAccessor, RandomState randomState) {
		return new NoiseColumn(heightAccessor.getMinY(), new BlockState[0]);
	}

	@Override
	public void addDebugScreenInfo(List<String> result, RandomState randomState, BlockPos feetPos, SamplerContext samplerContext) {
		int floor = CastleGeometry.floorOf(feetPos.getY());
		Half half = CastleGeometry.halfOf(feetPos.getY());
		CellPlan plan = layout(randomState).plan(floor, half, CastleGeometry.cellOf(feetPos.getX()), CastleGeometry.cellOf(feetPos.getZ()));
		FloorProfile profile = FloorProfile.of(floor);
		result.add("Castle: floor " + profile.number() + " " + half + " " + plan.type() + " " + plan.openings());
		result.add("Castle: gravity x" + profile.gravityScale() + ", tilt " + profile.cameraTilt() + ", band " + GravityRules.equatorBand(feetPos.getY()));
	}
}
