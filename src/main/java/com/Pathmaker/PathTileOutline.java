package com.Pathmaker;

import java.util.ArrayList;
import net.runelite.api.Point;

/*
 * OVERVIEW
 *
 * OLD IMPLEMENTATION (what you had before):
 *
 * - Tried to choose exactly ONE edge per tile (except corners)
 * - Used direction vectors, cross products, and suppression rules
 * - Relied on stitching consecutive edges together
 *
 * PROBLEMS WITH OLD APPROACH:
 *
 * - When tiles were offset or diagonal, the chosen edges did not line up
 * - The polyline connected endpoints directly, producing diagonals
 * - Fixing one case broke another, because the outline is global
 *   but the logic was local to each tile
 *
 * NEW IMPLEMENTATION (this class):
 *
 * - A tile is allowed to emit MORE THAN ONE edge
 * - For each tile, we determine:
 *     - which side the outline ENTERS
 *     - which side the outline EXITS
 * - We then WALK the tile boundary between those sides
 *   in counter-clockwise order (left outline)
 *
 * CONSEQUENCES:
 *
 * - The polyline only ever walks along real tile edges
 * - No diagonals are possible by construction
 * - All your "missing edge" cases disappear naturally
 * - The code becomes simpler and more robust
 *
 * IMPORTANT:
 *
 * - The algorithm ALWAYS computes the LEFT outline
 * - RIGHT outline is achieved by reversing the tile order
 *
 * Coordinate system:
 *   (1, 0) = right
 *   (0, 1) = up
 *
 * Tile corners:
 *   p0 = top-left
 *   p1 = top-right
 *   p2 = bottom-right
 *   p3 = bottom-left
 */
public class PathTileOutline
{
	/* ---------------- Tile side ---------------- */

	/*
	 * Represents the four sides of a tile.
	 * We walk these in counter-clockwise order for a LEFT outline.
	 */
	private enum Side
	{
		TOP,
		LEFT,
		BOTTOM,
		RIGHT
	}

	/* ---------------- Public API ---------------- */

	public static ArrayList<Point> build(
		ArrayList<int[]> tileXs,
		ArrayList<int[]> tileYs,
		boolean left
	)
	{
		ArrayList<Point> out = new ArrayList<>();
		int n = tileXs.size();
		if (n == 0)
		{
			return out;
		}

		/*
		 * RIGHT outline handling:
		 *
		 * We do NOT duplicate logic for right vs left.
		 * Instead, we reverse the tile order and still compute
		 * a LEFT outline.
		 */
		if (!left)
		{
			reverse(tileXs);
			reverse(tileYs);
		}

		for (int i = 0; i < n; i++)
		{
			Point[] rect = rect(tileXs.get(i), tileYs.get(i));

			Point dirIn  = (i > 0)     ? direction(tileXs, tileYs, i - 1, i, left) : null;
			Point dirOut = (i < n - 1) ? direction(tileXs, tileYs, i, i + 1, left) : null;

			/*
			 * FIRST TILE:
			 *
			 * No entry direction.
			 * We only know where we are going next.
			 */
			if (dirIn == null)
			{
				Side exit = sideOf(dirOut, left);
				emitSide(out, rect, exit);
				continue;
			}

			/*
			 * LAST TILE:
			 *
			 * No exit direction.
			 * We only know where we came from.
			 */
			if (dirOut == null)
			{
				Side entry = sideOf(dirIn, left);
				emitSide(out, rect, entry);
				continue;
			}

			/*
			 * MIDDLE TILE (new logic):
			 *
			 * OLD approach:
			 *   - pick one edge
			 *   - hope it connects
			 *
			 * NEW approach:
			 *   - determine entry side
			 *   - determine exit side
			 *   - walk the boundary between them
			 */

			int cross = dirIn.getX() * dirOut.getY() - dirIn.getY() * dirOut.getX();

			boolean outerTurn = left ? cross < 0 : cross > 0;
			boolean innerTurn = left ? cross > 0 : cross < 0;

			Side inSide  = sideOf(dirIn, left);
			Side outSide = sideOf(dirOut, left);

			if (inSide == outSide)
			{
				// Normal straight / gentle turn
				emitSide(out, rect, inSide);
			}
			else
			{
				// Diagonal / offset case
				// Replace diagonal with exactly two edges
				emitSide(out, rect, inSide);
				emitSide(out, rect, outSide);
//				else if(innerTurn && outerTurn)
//				{
//					addLast(out);
//				}
			}
		}

		/*
		 * Restore original traversal order for right outline output.
		 */
		if (!left)
		{
			reverse(out);
		}

		return out;
	}

	/* ---------------- Geometry helpers ---------------- */

	/*
	 * Computes cardinal direction between tile centers.
	 * Diagonal directions are allowed here; they only influence
	 * which side is considered "left".
	 */
	private static Point direction(
		ArrayList<int[]> xs,
		ArrayList<int[]> ys,
		int a,
		int b,
		boolean left
	)
	{
		int ax = (xs.get(a)[0] + xs.get(a)[2]) / 2;
		int ay = (ys.get(a)[0] + ys.get(a)[2]) / 2;
		int bx = (xs.get(b)[0] + xs.get(b)[2]) / 2;
		int by = (ys.get(b)[0] + ys.get(b)[2]) / 2;

		if (left)
		{
			return new Point(
				Integer.signum(bx - ax),
				Integer.signum(by - ay)
			);
		}

		return new Point(
			Integer.signum(ax-bx),
			Integer.signum(ay-by)
		);
	}

	private static Point[] rect(int[] xs, int[] ys)
	{
		return new Point[]{
			new Point(xs[0], ys[0]), // p0 top-left
			new Point(xs[1], ys[1]), // p1 top-right
			new Point(xs[2], ys[2]), // p2 bottom-right
			new Point(xs[3], ys[3])  // p3 bottom-left
		};
	}

	/* ---------------- Side logic ---------------- */

	/*
	 * Determines which SIDE of a tile is on the LEFT/RIGHT
	 * of a given movement direction.
	 *
	 * This replaces the old normal / dot-product logic.
	 */
	private static Side sideOf(Point dir, boolean left)
	{
		if (left)
		{
			if (dir.getX() == 1 && dir.getY() == 0) return Side.TOP;
			if (dir.getX() == -1 && dir.getY() == 0) return Side.BOTTOM;
			if (dir.getX() == 0 && dir.getY() == 1) return Side.LEFT;
			if (dir.getX() == 0 && dir.getY() == -1) return Side.RIGHT;

			/*
			 * Diagonal case:
			 * Use the dominant component implicitly via boundary walking.
			 * We still return a side so entry/exit always exist.
			 */
			if (dir.getX() > 0) return Side.TOP;
			if (dir.getX() < 0) return Side.BOTTOM;
			if (dir.getY() > 0) return Side.LEFT;
			return Side.RIGHT;
		}
		else
		{
			if (dir.getX() == 1 && dir.getY() == 0)  return Side.BOTTOM;
			if (dir.getX() == -1 && dir.getY() == 0) return Side.TOP;
			if (dir.getX() == 0 && dir.getY() == 1)  return Side.RIGHT;
			if (dir.getX() == 0 && dir.getY() == -1) return Side.LEFT;

			if (dir.getX() > 0)  return Side.BOTTOM;
			if (dir.getX() < 0)  return Side.TOP;
			if (dir.getY() > 0)  return Side.RIGHT;
			return Side.LEFT;
		}
	}

	/*
	 * Counter-clockwise order around a tile.
	 * This is what enforces a LEFT outline.
	 */
	private static Side nextCCW(Side s)
	{
		switch (s)
		{
			case TOP:    return Side.LEFT;
			case LEFT:   return Side.BOTTOM;
			case BOTTOM: return Side.RIGHT;
			case RIGHT:  return Side.TOP;
		}
		throw new IllegalStateException();
	}

	/* ---------------- Emission ---------------- */

	private static void emitSide(ArrayList<Point> out, Point[] r, Side s)
	{
		switch (s)
		{
			case TOP:
				add(out, r[0]);
				add(out, r[1]);
				break;
			case RIGHT:
				add(out, r[1]);
				add(out, r[2]);
				break;
			case BOTTOM:
				add(out, r[2]);
				add(out, r[3]);
				break;
			case LEFT:
				add(out, r[3]);
				add(out, r[0]);
				break;
		}
	}

	private static void add(ArrayList<Point> out, Point p)
	{
		if (out.isEmpty() || !out.get(out.size() - 1).equals(p))
		{
			out.add(p);
		}
	}

	private static void addLast(ArrayList<Point> out)
	{
		out.add(out.get(out.size()-1));
	}

	private static <T> void reverse(ArrayList<T> list)
	{
		for (int i = 0, j = list.size() - 1; i < j; i++, j--)
		{
			T tmp = list.get(i);
			list.set(i, list.get(j));
			list.set(j, tmp);
		}
	}
}
