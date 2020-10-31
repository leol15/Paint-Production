package com.oreo.paint.help;


import android.util.Log;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * keeps track of 2D points
 *  use this class to query those points that are close to a given point
 */
public class InterestingPoints {
    static final String TAG = "-=-= InterestingPoints";
    int SNAP_RADIUS = 20;

    HashMap<Point, Integer> pointCounts;
    HashMap<Object, Set<Point>> pointStorage;
    // implementation to speed up query
    PointCloud pointCloud;
    public InterestingPoints() {
        pointCounts = new HashMap<>();
        pointStorage = new HashMap<>();
        pointCloud = new NaiveCloud();
    }

    public Point query(Object o, float x, float y) {
        // linear implementation
        Point p = pointCloud.closest(x, y);
        // ownership test: don't snap to yourself
        if (pointStorage.containsKey(o)) {
            for (Point myPoints : pointStorage.get(o)) {
                if (myPoints.equals(p)) {
                    return null;
                }
            }
        }
        if (p != null && Calculator.DIST(x, y, p.x, p.y) < SNAP_RADIUS) {
            return p;
        }
        return null;
    }

    public void addPoint(Object o, float x, float y) {
        Point n = new Point(x, y);
        if (!pointStorage.containsKey(o)) {
            pointStorage.put(o, new HashSet<Point>());
        }
        pointStorage.get(o).add(n);
        pointCounts.put(n, pointCounts.getOrDefault(n, 0) + 1);
        if (pointCounts.get(n) == 1) {
            // only add a point once
            pointCloud.add(n);
        }
    }

    public void removePoint(Object o, float x, float y) {
        Point n = new Point(x, y);
        if (pointStorage.containsKey(o)) {
            pointStorage.get(o).remove(n);
            if (pointCounts.containsKey(n)) {
                if (pointCounts.get(n) == 1) {
                    pointCounts.remove(n);
                    pointCloud.remove(n);
                    pointCloud.remove(n);
                } else {
                    pointCounts.put(n, pointCounts.get(n) - 1);
                }
            }
        }
    }

    public void removeAllPoints(Object o) {
        if (pointStorage.containsKey(o)) {
            for (Point p : pointStorage.get(o)) {
                if (pointCounts.containsKey(p)) {
                    if (pointCounts.get(p) == 1) {
                        pointCounts.remove(p);
                        pointCloud.remove(p);
                    } else {
                        pointCounts.put(p, pointCounts.get(p) - 1);
                    }
                } else {
                    Log.d(TAG, "removeAllPoints: nothing!!!");
                }
            }
            pointStorage.remove(o);
        }
    }





    // debug
    public Set<Point> allPoints() {
        return pointCounts.keySet();
    }


    /**
     * keep track of a point
     */
    public static class Point {
        public float x;
        public float y;

        public Point(float x, float y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Point point = (Point) o;
            return (int) x == (int) point.x &&
                    (int) y == (int) point.y;
        }

        @Override
        public int hashCode() {
            return Objects.hash((int) x, (int) y);
        }
    }


    /**
     * interface of PointCloud implementations
     */
    private interface PointCloud {
        void add(Point point);
        void remove(Point point);
        InterestingPoints.Point closest(float x, float y);
    }

    /**
     * linear solution
     */
    private static class NaiveCloud implements PointCloud {

        HashSet<Point> pool;
        NaiveCloud() {
            pool = new HashSet<>();
        }

        @Override
        public void add(Point point) {
            pool.add(point);
        }

        @Override
        public void remove(Point point) {
            if (!pool.remove(point)) {
                Log.e(TAG, "remove: point to remove is not found " + point.x + ", " + point.y);
            }
        }

        @Override
        public Point closest(float x, float y) {
            double min = Double.POSITIVE_INFINITY;
            Point re = null;
            for (Point p : pool) {
                double dist = Calculator.DIST(x, y, p.x, p.y);
                if (dist < min) {
                    min = dist;
                    re = p;
                }
            }
            return re;
        }
    }

    /**
     * kd-tree implementation
     */
    private static class KDTree implements PointCloud {

        private static class TreeNode {
            Point p;
            TreeNode left;
            TreeNode right;
            TreeNode(Point point) {
                p = point;
                left = null;
                right = null;
            }
        }

        TreeNode overallRoot;
        public KDTree() {

        }


        @Override
        public void add(Point point) {
            // call add
            overallRoot = addNode(new TreeNode(point), overallRoot, false);
        }

        private TreeNode addNode(TreeNode newNode, TreeNode current, boolean isX) {
            if (current == null) {
                return newNode;
            }

            float val = 0;
            float split = 0;

            if (isX) {
                val = newNode.p.x;
                split = current.p.x;
            } else {
                val = newNode.p.y;
                split = current.p.y;
            }


            if (val > split) {
                current.right = addNode(newNode, current.right, !isX);
            } else {
                current.left = addNode(newNode, current.left, !isX);
            }

            return current;
        }


        @Override
        public void remove(Point point) {

            TreeNode p = overallRoot;
            boolean isX = false;
            while (p != null && !p.p.equals(point)) {
                if (isX) {
                    if (point.x > p.p.x) {
                        p = p.right;
                    } else {
                        p = p.left;
                    }
                } else {
                    if (point.y > p.p.y) {
                        p = p.right;
                    } else {
                        p = p.left;
                    }
                }
            }
            if (p == null) {
                Log.e(TAG, "remove: no point found " + point.x + ", " + point.y);
                return;
            }
            // need to remove p
            overallRoot = removeNode(p, overallRoot, false);
        }

        private TreeNode removeNode(TreeNode tar, TreeNode curr, boolean isX) {
            if (curr == null) {
                return curr;
            }
            if (curr.left == null && curr.right == null) {
                return null;
            }
            return null;
        }

        @Override
        public Point closest(float x, float y) {
            return null;
        }


    }

    /**
     * spacial hash implementation
     */
    private static class SpacialHash implements PointCloud {


        SpacialHash() {

        }

        @Override
        public void add(Point point) {

        }

        @Override
        public void remove(Point point) {

        }

        @Override
        public Point closest(float x, float y) {
            return null;
        }
    }

}
