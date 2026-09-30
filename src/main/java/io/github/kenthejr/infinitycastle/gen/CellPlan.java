package io.github.kenthejr.infinitycastle.gen;

/**
 * Everything needed to build one cell.
 *
 * @param type     the module to build
 * @param openings doorways to neighbouring cells
 * @param variant  hash used for decorative choices inside the module
 */
public record CellPlan(ModuleType type, Openings openings, long variant) {
	public static final CellPlan VOID = new CellPlan(ModuleType.VOID, Openings.NONE, 0L);
}
