package org.eclipse.draw2d.examples;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.NativeGC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.SkijaGC;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.opengl.GLCanvas;
import org.eclipse.swt.opengl.GLData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.LightweightSystem;
import org.eclipse.draw2d.MouseEvent;
import org.eclipse.draw2d.MouseListener;
import org.eclipse.draw2d.MouseMotionListener;
import org.eclipse.draw2d.RectangleFigure;
import org.eclipse.draw2d.StackLayout;

import io.github.humbleui.skija.BackendRenderTarget;
import io.github.humbleui.skija.ColorSpace;
import io.github.humbleui.skija.DirectContext;
import io.github.humbleui.skija.FramebufferFormat;
import io.github.humbleui.skija.PixelGeometry;
import io.github.humbleui.skija.Surface;
import io.github.humbleui.skija.SurfaceColorFormat;
import io.github.humbleui.skija.SurfaceOrigin;
import io.github.humbleui.skija.SurfaceProps;

public class Stickman {
	public static void main(String[] args) throws Exception {
		new Stickman().run();
	}

	private Display display;
	private GLCanvas glCanvas;
	private DirectContext context;
	private Surface surface;
	private BackendRenderTarget renderTarget;
	private StickmanFigure stickmanFigure;

	// Center position of the stickman
	int positionX = 300;
	int positionY = 300;

	// Drag offsets relative to the stickman's center.
	int dragOffsetX = 0;
	int dragOffsetY = 0;

	int animationInterval = 50;

	private class StickmanFigure extends Figure {
		int headMax = 50;
		int headMin = 20;
		int headRadius = headMin;

		@Override
		public void paintFigure(Graphics g) {
			super.paintFigure(g);
			// Use the global positionX and positionY as the center of the stickman.
			int centerX = positionX;
			int centerY = positionY;

			// Define dimensions.
			int headCenterY = centerY - 80; // head above center
			int bodyStartY = headCenterY + headRadius;
			int bodyEndY = centerY;
			int armLength = 40;
			int legLength = 40;

			// Draw head (circle).
			g.drawOval(centerX - headRadius, headCenterY - headRadius, headRadius * 2, headRadius * 2);
			// Draw body (vertical line).
			g.drawLine(centerX, bodyStartY, centerX, bodyEndY);
			// Draw arms (diagonal lines).
			int armY = bodyStartY + (bodyEndY - bodyStartY) / 3;
			g.drawLine(centerX, armY, centerX - armLength, armY + 10);
			g.drawLine(centerX, armY, centerX + armLength, armY + 10);
			// Draw legs (diagonal lines).
			g.drawLine(centerX, bodyEndY, centerX - 20, bodyEndY + legLength);
			g.drawLine(centerX, bodyEndY, centerX + 20, bodyEndY + legLength);
		}

		int inc = 1;

		public void nextFrame() {
			headRadius += inc;

			if (headRadius > headMax) {
				inc = inc * -1;
				headRadius = headMax + inc;
			} else if (headRadius < headMin) {
				inc = inc * -1;
				headRadius = headMin + inc;
			}
		}

	}

	protected void run() throws Exception {
		display = new Display();

		Shell shell = new Shell(display);
		shell.setText("Draw2d with OpenGL - Drag the Stickman"); //$NON-NLS-1$
		shell.setLayout(new FillLayout());
		shell.setSize(new Point(1024, 768));

		GLData data = new GLData();
		data.doubleBuffer = true;

		glCanvas = new GLCanvas(shell, SWT.NO_BACKGROUND | SWT.NO_REDRAW_RESIZE, data);
		glCanvas.setCurrent();
		context = DirectContext.makeGL();

		LightweightSystem lws = new LightweightSystem(glCanvas) {
			@Override
			public void paint(org.eclipse.swt.graphics.GC gc) {
				if (surface == null || glCanvas.getBounds().width != surface.getWidth()
						|| glCanvas.getBounds().height != surface.getHeight()) {
					release();
					Rectangle rect = glCanvas.getClientArea();
					renderTarget = BackendRenderTarget.makeGL(rect.width, rect.height, /* samples */ 0, /* stencil */ 8,
							/* fbid */ 0, FramebufferFormat.GR_GL_RGBA8);
					surface = Surface.makeFromBackendRenderTarget(context, renderTarget, SurfaceOrigin.BOTTOM_LEFT,
							SurfaceColorFormat.RGBA_8888, ColorSpace.getDisplayP3(),
							new SurfaceProps(PixelGeometry.RGB_H));
				}
				gc.innerGC = new SkijaGC((NativeGC) gc.innerGC, surface);
				surface.getCanvas().clear(0xFFFFFFFF);

				super.paint(gc);
			}
		};

		Listener listener = event -> {
			if (event.type == SWT.Dispose) {
				onDispose();
			}
		};

		// Create a container figure.
		Figure figure = new RectangleFigure() {
			@Override
			public void paint(Graphics graphics) {
				super.paint(graphics);
				onPaint();
			}
		};

		figure.setOpaque(true);
		figure.setBackgroundColor(ColorConstants.lightBlue);

		stickmanFigure = new StickmanFigure();
		figure.add(stickmanFigure);

		// Add mouse listeners to implement dragging.
		stickmanFigure.addMouseListener(new MouseListener.Stub() {
			@Override
			public void mousePressed(MouseEvent me) {
				// Capture the offset between the mouse position and the stickman's center.
				dragOffsetX = me.getLocation().x - positionX;
				dragOffsetY = me.getLocation().y - positionY;
			}
		});
		stickmanFigure.addMouseMotionListener(new MouseMotionListener.Stub() {
			@Override
			public void mouseDragged(MouseEvent me) {
				// Update stickman center based on mouse position and initial offset.
				positionX = me.getLocation().x - dragOffsetX;
				positionY = me.getLocation().y - dragOffsetY;
				stickmanFigure.repaint();
			}
		});

		stickmanFigure.setForegroundColor(ColorConstants.blue);

		figure.setLayoutManager(new StackLayout());
		lws.setContents(figure);

		shell.addListener(SWT.Dispose, listener);

		shell.open();

		display.timerExec(animationInterval, new Runnable() {
			@Override
			public void run() {
				stickmanFigure.nextFrame();
				stickmanFigure.repaint();
				display.timerExec(animationInterval, this);
			}
		});

		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}

		display.dispose();
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

	protected void onPaint() {
		if (surface == null) {
			return;
		}
		context.flush();
		glCanvas.swapBuffers();
	}

	protected void onDispose() {
		release();
		context.close();
	}

}
