package com.Pathmaker;

import java.util.ArrayList;
import net.runelite.api.Point;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;

/* Coordinate system:
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

	private enum Side
	{
		TOP,
		LEFT,
		BOTTOM,
		RIGHT
	}

	/* ---------------- Public API ---------------- */

	public static ArrayList<LocalPoint> build(
		ArrayList<WorldView> tileWVs,
		ArrayList<int[]> tileXs,
		ArrayList<int[]> tileYs,
		boolean left
	)
	{
		ArrayList<LocalPoint> out = new ArrayList<>();
		int n = tileXs.size();
		if (n == 0)
		{
			return out;
		}

		boolean isLooped = tileXs.get(0)[0] == tileXs.get(n - 1)[0] && tileYs.get(0)[0] == tileYs.get(n - 1)[0];

		for (int i = 0; i < n; i++)
		{
			LocalPoint[] rect = rect(tileXs.get(i), tileYs.get(i), tileWVs.get(i));

			Point dirIn  = (i > 0)     ? direction(tileXs, tileYs, i - 1, i) : null;
			Point dirOut = i < n - 1 ? direction(tileXs, tileYs, i, i + 1) : null;

			/* FIRST TILE:
			 * No entry direction.
			 * We only know where we are going next. */

			if (dirIn == null)
			{
				Side exit = sideOf(dirOut, left);
				addSide(out, rect, exit, left);
				continue;
			}

			/* LAST TILE:
			 * No exit direction.
			 * We only know where we came from. */

			if (dirOut == null)
			{
				Side inSide  = sideOf(dirIn, left);
				if (isLooped)
				{
					dirOut = direction(tileXs, tileYs, i,1);
					int cross = dirIn.getX() * dirOut.getY() - dirIn.getY() * dirOut.getX();
					boolean innerTurn = left ?  cross > 0 : cross < 0;
					Side outSide = sideOf(dirOut, left);

					if (inSide == outSide)
					{
						out.add(out.get(0));
					}
					else if(innerTurn)
					{
						out.remove(0);
						out.add(out.get(0));
					}
					else
					{
						out.remove(0);
						addSide(out, rect, inSide, left);
						addSide(out, rect, outSide, left);
					}
				}
				else
				{
					addSide(out, rect, inSide, left);
				}
				continue;
			}

			// MIDDLE TILE (new logic):

			int cross = dirIn.getX() * dirOut.getY() - dirIn.getY() * dirOut.getX();
			boolean innerTurn = left ?  cross > 0 : cross < 0;

			Side inSide  = sideOf(dirIn, left);
			Side outSide = sideOf(dirOut, left);

			if (inSide == outSide)
			{
				// Normal straight / gentle turn
				addSide(out, rect, inSide, left);
			}
			else if(innerTurn)
			{
				if(left)
					addSideStartPoint(out,rect,inSide);
				else
					addSideEndPoint(out,rect,inSide);
			}
			else
			{
				// Diagonal / offset case
				// Replace diagonal with exactly two edges
				addSide(out, rect, inSide, left);
				addSide(out, rect, outSide, left);
			}

		}

		return out;
	}

	/* ---------------- Geometry helpers ---------------- */

	// Computes cardinal direction between tile centers.
	private static Point direction(
		ArrayList<int[]> xs,
		ArrayList<int[]> ys,
		int a,
		int b
	)
	{
		int ax = (xs.get(a)[0] + xs.get(a)[2]) / 2;
		int ay = (ys.get(a)[0] + ys.get(a)[2]) / 2;
		int bx = (xs.get(b)[0] + xs.get(b)[2]) / 2;
		int by = (ys.get(b)[0] + ys.get(b)[2]) / 2;

			return new Point(
				Integer.signum(bx - ax),
				Integer.signum(by - ay)
			);
	}

	private static LocalPoint[] rect(int[] xs, int[] ys, WorldView wv)
	{
		return new LocalPoint[]{
			new LocalPoint(xs[0], ys[0], wv), // p0 top-left
			new LocalPoint(xs[1], ys[1], wv), // p1 top-right
			new LocalPoint(xs[2], ys[2], wv), // p2 bottom-right
			new LocalPoint(xs[3], ys[3], wv)  // p3 bottom-left
		};
	}

	/* ---------------- Side logic ---------------- */

	// Determines which SIDE of a tile is on the LEFT/RIGHT of a given movement direction.
	private static Side sideOf(Point dir, boolean left)
	{
		if (left)
		{
			if (dir.getX() == 1 && dir.getY() == 0) return Side.TOP;
			if (dir.getX() == -1 && dir.getY() == 0) return Side.BOTTOM;
			if (dir.getX() == 0 && dir.getY() == 1) return Side.LEFT;
			if (dir.getX() == 0 && dir.getY() == -1) return Side.RIGHT;

			/* Diagonal case:
			 * Use the dominant component implicitly via boundary walking.
			 * We still return a side so entry/exit always exist. */
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

	/* ---------------- Emission ---------------- */

	private static void addSide(ArrayList<LocalPoint> out, LocalPoint[] r, Side s, boolean left)
	{
		if(left)
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
		else
		{
			switch (s)
			{
				case TOP:
					add(out, r[1]);
					add(out, r[0]);
					break;
				case RIGHT:
					add(out, r[2]);
					add(out, r[1]);
					break;
				case BOTTOM:
					add(out, r[3]);
					add(out, r[2]);
					break;
				case LEFT:
					add(out, r[0]);
					add(out, r[3]);
					break;
			}
		}

	}

	private static void addSideStartPoint(ArrayList<LocalPoint> out, LocalPoint[] r, Side s)
	{
		switch (s)
		{
			case TOP:
				add(out, r[0]);
				break;
			case RIGHT:
				add(out, r[1]);
				break;
			case BOTTOM:
				add(out, r[2]);
				break;
			case LEFT:
				add(out, r[3]);
				break;
		}
	}

	private static void addSideEndPoint(ArrayList<LocalPoint> out, LocalPoint[] r, Side s)
	{
		switch (s)
		{
			case TOP:
				add(out, r[1]);
				break;
			case RIGHT:
				add(out, r[2]);
				break;
			case BOTTOM:
				add(out, r[3]);
				break;
			case LEFT:
				add(out, r[0]);
				break;
		}
	}

	private static boolean sidesAreOpposites(Side a, Side b)
	{
		if (a.equals(Side.TOP) &&  b.equals(Side.BOTTOM))
			return true;
		if (a.equals(Side.LEFT) &&  b.equals(Side.RIGHT))
			return true;
		if (a.equals(Side.RIGHT) &&  b.equals(Side.LEFT))
			return true;
		if (a.equals(Side.BOTTOM) &&  b.equals(Side.TOP))
			return true;

		return false;
	}

	private static void add(ArrayList<LocalPoint> out, LocalPoint p)
	{
		if (out.isEmpty() || !out.get(out.size() - 1).equals(p))
		{
			out.add(p);
		}
	}
}
