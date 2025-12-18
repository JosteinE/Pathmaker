package com.Pathmaker;

import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Float;
import java.util.ArrayList;
import net.runelite.api.Point;

public class PathTileOutline
{
	/* ---------------- Direction ---------------- */

	private static final class Dir {
		final int dx, dy;

		Dir(float ax, float ay, float bx, float by) {
			dx = Integer.signum(Math.round(bx - ax));
			dy = Integer.signum(Math.round(by - ay));
		}
	}

	/* ---------------- Edge ---------------- */

	private static final class Edge {
		final Point a, b;   // ordered
		final int nx, ny;   // outward normal

		Edge(Point a, Point b, int nx, int ny) {
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
	) {
		ArrayList<Point> out = new ArrayList<>();
		int n = tileXs.size();
		if (n == 0) return out;

		// Single tile case
		if (n == 1) {
			Point[] p = rect(tileXs.get(0), tileYs.get(0));
			if (left) {
				add(out, p[3]);
				add(out, p[0]);
			} else {
				add(out, p[1]);
				add(out, p[2]);
			}
			return out;
		}

		Dir prevDir = null;
		Edge prevEdge = null;

		for (int i = 0; i < n; i++) {
			Point[] rect = rect(tileXs.get(i), tileYs.get(i));

			Dir dirIn  = (i > 0)     ? direction(tileXs, tileYs, i - 1, i) : null;
			Dir dirOut = (i < n - 1) ? direction(tileXs, tileYs, i, i + 1) : null;

			// First tile: always emit one edge
			if (i == 0) {
				Edge e = pickEdge(dirOut, left, rect);
				emitEdge(out, e);
				prevEdge = e;
				continue;
			}

			// Last tile: always emit one edge
			if (i == n - 1) {
				Edge e = pickEdge(dirIn, left, rect);
				emitEdge(out, e);
				continue;
			}

			int cross =
				dirIn.dx * dirOut.dy -
					dirIn.dy * dirOut.dx;

			boolean outerTurn =
				left ? cross > 0 : cross < 0;
			boolean innerTurn =
				left ? cross < 0 : cross > 0;

			if (cross == 0) {
				// Straight
				Edge e = pickEdge(dirOut, left, rect);
				emitEdge(out, e);
				prevEdge = e;
			}
			else if (outerTurn) {
				// Two edges, joined by corner
				Edge e1 = pickEdge(dirIn, left, rect);
				Edge e2 = pickEdge(dirOut, left, rect);

				Point corner = sharedCorner(e1, e2);
				add(out, e1.a);
				add(out, corner);
				add(out, e2.b);

				prevEdge = e2;
			}
			// inner turn: emit nothing
		}

		return out;
	}

	/* ---------------- Geometry ---------------- */

	private static Point[] rect(int[] xs, int[] ys) {
		return new Point[]{
			new Point(xs[0], ys[0]), // p0
			new Point(xs[1], ys[1]), // p1
			new Point(xs[2], ys[2]), // p2
			new Point(xs[3], ys[3])  // p3
		};
	}

	private static Dir direction(
		ArrayList<int[]> xs,
		ArrayList<int[]> ys,
		int a, int b
	) {
		float ax = (xs.get(a)[0] + xs.get(a)[2]) * 0.5f;
		float ay = (ys.get(a)[0] + ys.get(a)[2]) * 0.5f;
		float bx = (xs.get(b)[0] + xs.get(b)[2]) * 0.5f;
		float by = (ys.get(b)[0] + ys.get(b)[2]) * 0.5f;
		return new Dir(ax, ay, bx, by);
	}

	private static Edge pickEdge(Dir d, boolean left, Point[] p) {
		int nx = left ? -d.dy : d.dy;
		int ny = left ?  d.dx : -d.dx;

		Edge[] edges = new Edge[]{
			new Edge(p[0], p[1],  0,  1),  // top
			new Edge(p[1], p[2],  1,  0),  // right
			new Edge(p[2], p[3],  0, -1),  // bottom
			new Edge(p[3], p[0], -1,  0)   // left
		};

		Edge best = null;
		int bestDot = Integer.MIN_VALUE;

		for (Edge e : edges) {
			int dot = nx * e.nx + ny * e.ny;
			if (dot > bestDot) {
				bestDot = dot;
				best = e;
			}
		}
		return best;
	}

	private static Point sharedCorner(Edge a, Edge b) {
		if (a.a.equals(b.a) || a.a.equals(b.b)) return a.a;
		return a.b;
	}

	/* ---------------- Output helpers ---------------- */

	private static void emitEdge(ArrayList<Point> out, Edge e) {
		add(out, e.a);
		add(out, e.b);
	}

	private static void add(ArrayList<Point> out, Point p) {
		if (out.isEmpty() || !out.get(out.size() - 1).equals(p)) {
			out.add(p);
		}
	}
}