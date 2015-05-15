/**
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
 * @author Stephan Preibisch
 */
package mpicbg.stitching.math;

public class FloatArray2D extends FloatArray {
    public int width = 0;
    public int height = 0;

    public FloatArray2D(final int width, final int height) {
        data = new float[width * height];
        this.width = width;
        this.height = height;
    }

    public FloatArray2D(final float[] data, final int width, final int height) {
        this.data = data;
        this.width = width;
        this.height = height;
    }

    @Override
    public FloatArray2D clone() {
        final FloatArray2D clone = new FloatArray2D(width, height);
        System.arraycopy(data, 0, clone.data, 0, data.length);
        return clone;
    }

    public int getPos(final int x, final int y) {
        return x + width * y;
    }

    public float get(final int x, final int y) {
        return data[getPos(x, y)];
    }

    public float getMirror(int x, int y) {
        if (x >= width) {
            x = width - (x - width + 2);
        }

        if (y >= height) {
            y = height - (y - height + 2);
        }

        if (x < 0) {
            int tmp = 0;
            int dir = 1;

            while (x < 0) {
                tmp += dir;
                if (tmp == width - 1 || tmp == 0) {
                    dir *= -1;
                }
                x++;
            }
            x = tmp;
        }

        if (y < 0) {
            int tmp = 0;
            int dir = 1;

            while (y < 0) {
                tmp += dir;
                if (tmp == height - 1 || tmp == 0) {
                    dir *= -1;
                }
                y++;
            }
            y = tmp;
        }

        return data[getPos(x, y)];
    }

    public float getZero(final int x, final int y) {
        if (x >= width) {
            return 0;
        }

        if (y >= height) {
            return 0;
        }

        if (x < 0) {
            return 0;
        }

        if (y < 0) {
            return 0;
        }

        return data[getPos(x, y)];
    }

    public float getZero(final double x, final double y) {
        if (x >= width) {
            return 0;
        }

        if (y >= height) {
            return 0;
        }

        if (x < 0) {
            return 0;
        }

        if (y < 0) {
            return 0;
        }

        final int xbase = (int) x;
        final int ybase = (int) y;
        final double xFraction = x - xbase;
        final double yFraction = y - ybase;

        final float lowerLeft = getZero(xbase, ybase);
        final float lowerRight = getZero(xbase + 1, ybase);
        final float upperRight = getZero(xbase + 1, ybase + 1);
        final float upperLeft = getZero(xbase, ybase + 1);
        final double upperAverage =
                upperLeft + xFraction * (upperRight - upperLeft);
        final double lowerAverage =
                lowerLeft + xFraction * (lowerRight - lowerLeft);

        return (float) (lowerAverage + yFraction
                * (upperAverage - lowerAverage));
    }

    public void set(final float value, final int x, final int y) {
        data[getPos(x, y)] = value;
    }
}
