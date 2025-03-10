/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.gef.ui.parts;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.NativeGC;
import org.eclipse.swt.graphics.SkijaGC;
import org.eclipse.swt.opengl.GLCanvas;
import org.eclipse.swt.widgets.Composite;

import org.eclipse.draw2d.DeferredUpdateManager;
import org.eclipse.draw2d.FigureCanvas;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.GraphicsSource;
import org.eclipse.draw2d.LightweightSystem;
import org.eclipse.draw2d.geometry.Rectangle;

import io.github.humbleui.skija.BackendRenderTarget;
import io.github.humbleui.skija.ColorSpace;
import io.github.humbleui.skija.DirectContext;
import io.github.humbleui.skija.FramebufferFormat;
import io.github.humbleui.skija.PixelGeometry;
import io.github.humbleui.skija.Surface;
import io.github.humbleui.skija.SurfaceColorFormat;
import io.github.humbleui.skija.SurfaceOrigin;
import io.github.humbleui.skija.SurfaceProps;

public class ScrollingGraphicalGLViewer extends ScrollingGraphicalViewer {

	private DirectContext context;
	private Surface surface;
	private BackendRenderTarget renderTarget;
	GLCanvas canvas;

	@Override
	protected FigureCanvas createCanvas(Composite parent) {
		return new FigureCanvas(parent, getLightweightSystem());
	}

	@Override
	protected LightweightSystem createLightweightSystem() {
		LightweightSystem lws = super.createLightweightSystem();

		GraphicsSource graphicsSource = new GraphicsSource() {
			@Override
			public Graphics getGraphics(Rectangle region) {
				if (canvas == null) {

					canvas = (GLCanvas) getControl();
					canvas.setCurrent();
					context = DirectContext.makeGL();

					canvas.addListener(SWT.Dispose, event -> {
						if (event.type == SWT.Dispose) {
							release();
							context.close();
						}
					});
					lws.setControl(canvas);
				}
				canvas.redraw();
				return null;
			}

			@Override
			public void flushGraphics(Rectangle region) {
			}
		};

		lws.setUpdateManager(new DeferredUpdateManager() {
			@Override
			public void setGraphicsSource(GraphicsSource gs) {
				super.setGraphicsSource(graphicsSource);
			}

			@Override
			protected void paint(GC gc) {
				if (canvas != null && !validating) {
					if (surface == null || canvas.getBounds().width != surface.getWidth()
							|| canvas.getBounds().height != surface.getHeight()) {
						release();
						org.eclipse.swt.graphics.Rectangle rect = canvas.getClientArea();
						renderTarget = BackendRenderTarget.makeGL(rect.width, rect.height, /* samples */ 0,
								/* stencil */ 8, /* fbid */ 0, FramebufferFormat.GR_GL_RGBA8);
						surface = Surface.makeFromBackendRenderTarget(context, renderTarget, SurfaceOrigin.BOTTOM_LEFT,
								SurfaceColorFormat.RGBA_8888, ColorSpace.getDisplayP3(),
								new SurfaceProps(PixelGeometry.RGB_H));
					}
					if (gc.innerGC instanceof NativeGC nat) {
						gc.innerGC = new SkijaGC(nat, surface);
					}
					gc.setAlpha(255);
					super.paint(gc);
					context.flush();
					canvas.swapBuffers();
				} else {
					super.paint(gc);
				}
			}
		});

		return lws;
	}

	protected void release() {
		if (surface != null) {
			surface.close();
			surface = null;
		}
		if (renderTarget != null) {
			renderTarget.close();
			renderTarget = null;
		}
	}

}
