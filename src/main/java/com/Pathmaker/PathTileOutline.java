package com.Pathmaker;

import java.util.ArrayList;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Point;

@Slf4j
public class PathTileOutline
{
	/* ---------------- Direction ---------------- */

	private static final class Dir
	{
		final int dx, dy;

		Dir(float ax, float ay, float bx, float by)
		{
			dx = Integer.signum(Math.round(bx - ax));
			dy = Integer.signum(Math.round(by - ay));
		}
	}

	/* ---------------- Edge ---------------- */

	private static final class Edge
	{
		final Point a, b;   // ordered endpoints
		final int nx, ny;   // outward normal

		Edge(Point a, Point b, int nx, int ny)
		{
			this.a = a;
			this.b = b;
			this.nx = nx;
			this.ny = ny;
		}
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

		// Single tile case
		if (n == 1)
		{
			Point[] p = rect(tileXs.get(0), tileYs.get(0));
			if (left)
			{
				add(out, p[3]);
				add(out, p[0]);
			}
			else
			{
				add(out, p[1]);
				add(out, p[2]);
			}
			return out;
		}

		for (int i = 0; i < n; i++)
		{
			Point[] rect = rect(tileXs.get(i), tileYs.get(i));

			Dir dirIn = (i > 0) ? direction(tileXs, tileYs, i - 1, i) : null;
			Dir dirOut = (i < n - 1) ? direction(tileXs, tileYs, i, i + 1) : null;

			/* -------- First tile -------- */
			if (i == 0)
			{
				Edge e = orientEdge(
					pickEdge(dirOut, left, rect),
					dirOut
				);
				emitEdge(out, e);
				continue;
			}

			/* -------- Last tile -------- */
			if (i == n - 1)
			{
				Edge e = orientEdge(
					pickEdge(dirIn, left, rect),
					dirIn
				);
				emitEdge(out, e);
				continue;
			}

			/* -------- Middle tiles -------- */

			int cross =
				dirIn.dx * dirOut.dy -
					dirIn.dy * dirOut.dx;

			boolean outerTurn = left ? cross < 0 : cross > 0;

			if (cross == 0)
			{

				// STRAIGHT: emit exactly one edge
				Edge e = orientEdge(
					pickEdge(dirOut, left, rect),
					dirOut
				);
				emitEdge(out, e);
			}
			else if (outerTurn)
			{
				// OUTER TURN: emit exactly ONE corner vertex
				Point corner = outerCorner(rect, dirIn, dirOut, left);
				if (corner != null)
				{
					add(out, corner);
				}
			}
			// INNER TURN: emit nothing (collapses the corner)
		}

		return out;
	}

	/* ---------------- Geometry ---------------- */

	private static Point[] rect(int[] xs, int[] ys)
	{
		return new Point[]{
			new Point(xs[0], ys[0]), // p0 top-left
			new Point(xs[1], ys[1]), // p1 top-right
			new Point(xs[2], ys[2]), // p2 bottom-right
			new Point(xs[3], ys[3])  // p3 bottom-left
		};
	}

	private static Dir direction(
		ArrayList<int[]> xs,
		ArrayList<int[]> ys,
		int a, int b
	)
	{
		float ax = (xs.get(a)[0] + xs.get(a)[2]) * 0.5f;
		float ay = (ys.get(a)[0] + ys.get(a)[2]) * 0.5f;
		float bx = (xs.get(b)[0] + xs.get(b)[2]) * 0.5f;
		float by = (ys.get(b)[0] + ys.get(b)[2]) * 0.5f;
		return new Dir(ax, ay, bx, by);
	}

	private static Edge pickEdge(Dir d, boolean left, Point[] p)
	{
		int nx = left ? -d.dy : d.dy;
		int ny = left ? d.dx : -d.dx;

		Edge[] edges = new Edge[]{
			new Edge(p[0], p[1], 0, 1),  // top
			new Edge(p[1], p[2], 1, 0),  // right
			new Edge(p[2], p[3], 0, -1),  // bottom
			new Edge(p[3], p[0], -1, 0)   // left
		};

		Edge best = null;
		int bestDot = Integer.MIN_VALUE;

		for (Edge e : edges)
		{
			int dot = nx * e.nx + ny * e.ny;
			if (dot > bestDot)
			{
				bestDot = dot;
				best = e;
			}
		}
		return best;
	}

	/* ---------------- Edge orientation (unchanged) ---------------- */

	private static Edge orientEdge(Edge e, Dir travel)
	{
		int ex = Integer.signum(e.b.getX() - e.a.getX());
		int ey = Integer.signum(e.b.getY() - e.a.getY());

		if (ex == -travel.dx && ey == -travel.dy)
		{
			return new Edge(e.b, e.a, e.nx, e.ny);
		}
		return e;
	}

	/* ---------------- NEW: explicit outer corner ---------------- */

	private static Point outerCorner(Point[] r, Dir in, Dir out, boolean left)
	{
		if (left)
		{
			if (in.dy == 1 && out.dx == 1)
			{
				return r[0]; // up → right
			}
			if (in.dx == 1 && out.dy == -1)
			{
				return r[1]; // right → down
			}
			if (in.dy == -1 && out.dx == -1)
			{
				return r[2]; // down → left
			}
			if (in.dx == -1 && out.dy == 1)
			{
				return r[3]; // left → up
			}
		}
		else
		{
			if (in.dy == 1 && out.dx == -1)
			{
				return r[1]; // up → left
			}
			if (in.dx == -1 && out.dy == -1)
			{
				return r[0]; // left → down
			}
			if (in.dy == -1 && out.dx == 1)
			{
				return r[3]; // down → right
			}
			if (in.dx == 1 && out.dy == 1)
			{
				return r[2]; // right → up
			}
		}
		return null;
	}

	/* ---------------- Output helpers ---------------- */

	private static void emitEdge(ArrayList<Point> out, Edge e)
	{
		add(out, e.a);
		add(out, e.b);
	}

	private static void add(ArrayList<Point> out, Point p)
	{
		if (out.isEmpty() || !out.get(out.size() - 1).equals(p))
		{
			out.add(p);
		}
	}
}
