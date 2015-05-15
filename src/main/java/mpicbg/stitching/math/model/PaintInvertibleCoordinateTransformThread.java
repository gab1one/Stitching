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

import ij.ImagePlus;
import ij.process.ImageProcessor;

import java.util.concurrent.atomic.AtomicBoolean;

import mpicbg.stitching.utils.Log;

public class PaintInvertibleCoordinateTransformThread extends Thread {
    final protected ImagePlus imp;
    final protected ImageProcessor source;
    final protected ImageProcessor target;
    final protected AtomicBoolean pleaseRepaint;
    final protected InvertibleCoordinateTransform transform;

    public PaintInvertibleCoordinateTransformThread(final ImagePlus imp,
            final ImageProcessor source, final ImageProcessor target,
            final AtomicBoolean pleaseRepaint,
            final InvertibleCoordinateTransform transform) {
        this.imp = imp;
        this.source = source;
        this.target = target;
        this.pleaseRepaint = pleaseRepaint;
        this.transform = transform;
        setName("PaintInvertibleCoordinateTransformThread");
    }

    @Override
    public void run() {
        while (!isInterrupted()) {
            try {
                if (pleaseRepaint.compareAndSet(true, false)) {
                    for (int y = 0; y < target.getHeight(); ++y) {
                        for (int x = 0; x < target.getWidth(); ++x) {
                            final float[] t = new float[] { x, y };
                            try {
                                transform.applyInverseInPlace(t);
                                target.putPixel(x, y,
                                        source.getPixel((int) t[0], (int) t[1]));
                            } catch (final NoninvertibleModelException e) {
                                Log.error(e);
                            }
                        }
                        imp.updateAndDraw();
                    }
                } else {
                    synchronized (this) {
                        wait();
                    }
                }
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
