/**
 * License: GPL
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License 2
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * An execption is the FFT implementation of Dave Hale which we use as a library,
 * wich is released under the terms of the Common Public License - v1.0, which is
 * available at http://www.eclipse.org/legal/cpl-v10.html
 *
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 *
 */
package mpicbg.stitching.math.model;

/**
 * An n-dimensional point.
 *
 * local coordinates are thought to be immutable, application of a model changes
 * the world coordinates of the point.
 */
public class Point {
    /**
     * World coordinates
     */
    final private float[] w;

    final public float[] getW() {
        return w;
    }

    /**
     * Local coordinates
     */
    final private float[] l;

    final public float[] getL() {
        return l;
    }

    /**
     * Constructor
     *
     * Sets member l to the given float[] reference.
     *
     * @param l
     *            reference to the local coordinates of the {@link Point}
     */
    public Point(final float[] l) {
        this.l = l;
        w = l.clone();
    }

    /**
     * Apply a {@link Model} to the {@link Point}.
     *
     * Transfers the local coordinates to new world coordinates}.
     *
     * @param model
     */
    final public void apply(final Model model) {
        System.arraycopy(l, 0, w, 0, l.length);
        model.applyInPlace(w);
    }

    /**
     * Apply a {@link Model} to the {@link Point} by a given amount.
     *
     * Transfers the local coordinates to new world coordinates}.
     *
     * @param model
     * @param amount
     *            0.0 -> no application, 1.0 -> full application
     */
    final public void apply(final Model model, final float amount) {
        final float[] a = model.apply(l);
        for (int i = 0; i < a.length; ++i) {
            w[i] += amount * (a[i] - w[i]);
        }
    }

    /**
     * Apply the inverse of a {@link InvertibleModel} to the {@link Point}.
     *
     * Transfers the local coordinates to new world coordinates}.
     *
     * @param model
     */
    final public void applyInverse(final InvertibleModel model)
            throws NoninvertibleModelException {
        System.arraycopy(l, 0, w, 0, l.length);
        model.applyInverseInPlace(w);
    }

    /**
     * Estimate the square distance of two {@link Point Points} in the world.
     *
     * @param p1
     * @param p2
     * @return square distance
     */
    final static public float squareDistance(final Point p1, final Point p2) {
        assert p1.l.length == p2.l.length : "Both points have to have the same dimensionality.";

        double sum = 0.0;
        for (int i = 0; i < p1.w.length; ++i) {
            final double d = p1.w[i] - p2.w[i];
            sum += d * d;
        }
        return (float) sum;
    }

    /**
     * Estimate the Euclidean distance of two {@link Point Points} in the world.
     *
     * @param p1
     * @param p2
     * @return Euclidean distance
     */
    final static public float distance(final Point p1, final Point p2) {
        assert p1.l.length == p2.l.length : "Both points have to have the same dimensionality.";

        return (float) Math.sqrt(squareDistance(p1, p2));
    }

    /**
     * Clone this {@link Point} instance.
     */
    @Override
    public Point clone() {
        final Point p = new Point(l.clone());
        for (int i = 0; i < w.length; ++i) {
            p.w[i] = w[i];
        }
        return p;
    }
}
