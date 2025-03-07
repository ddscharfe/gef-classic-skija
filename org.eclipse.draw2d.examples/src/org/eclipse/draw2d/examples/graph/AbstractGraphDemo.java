/*******************************************************************************
 * Copyright (c) 2005, 2023 IBM Corporation and others.
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
package org.eclipse.draw2d.examples.graph;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.NativeGC;
import org.eclipse.swt.graphics.SkijaGC;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.opengl.GLCanvas;
import org.eclipse.swt.opengl.GLData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.draw2d.AbsoluteBendpoint;
import org.eclipse.draw2d.BendpointConnectionRouter;
import org.eclipse.draw2d.ChopboxAnchor;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.DeferredUpdateManager;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.LightweightSystem;
import org.eclipse.draw2d.LineBorder;
import org.eclipse.draw2d.PolygonDecoration;
import org.eclipse.draw2d.PolylineConnection;
import org.eclipse.draw2d.PositionConstants;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.draw2d.graph.Edge;
import org.eclipse.draw2d.graph.Node;
import org.eclipse.draw2d.graph.NodeList;

import io.github.humbleui.skija.BackendRenderTarget;
import io.github.humbleui.skija.ColorSpace;
import io.github.humbleui.skija.DirectContext;
import io.github.humbleui.skija.FramebufferFormat;
import io.github.humbleui.skija.PixelGeometry;
import io.github.humbleui.skija.Surface;
import io.github.humbleui.skija.SurfaceColorFormat;
import io.github.humbleui.skija.SurfaceOrigin;
import io.github.humbleui.skija.SurfaceProps;

/**
 * @author Daniel Lee
 */
public abstract class AbstractGraphDemo {

	// /** Indicates whether the prime graph should be built */
	// protected static boolean buildPrime = false;
	/** Contents of the demo */
	protected IFigure contents;
	/** Name of graph test method to run */
	protected static String graphMethod;
	/** direction to use for the graph */
	protected static int graphDirection = PositionConstants.SOUTH;
	/** Demo shell */
	protected Shell shell;

	private GLCanvas fc;

	static class TopOrBottomAnchor extends ChopboxAnchor {
		public TopOrBottomAnchor(IFigure owner) {
			super(owner);
		}

		@Override
		public Point getLocation(Point reference) {
			Point p;
			p = getOwner().getBounds().getCenter();
			getOwner().translateToAbsolute(p);
			if (reference.y < p.y) {
				p = getOwner().getBounds().getTop();
			} else {
				p = getOwner().getBounds().getBottom();
			}
			getOwner().translateToAbsolute(p);
			return p;
		}
	}

	static class LeftOrRightAnchor extends ChopboxAnchor {
		public LeftOrRightAnchor(IFigure owner) {
			super(owner);
		}

		@Override
		public Point getLocation(Point reference) {
			Point p;
			p = getOwner().getBounds().getCenter();
			getOwner().translateToAbsolute(p);
			if (reference.x() < p.x()) {
				p = getOwner().getBounds().getLeft();
			} else {
				p = getOwner().getBounds().getRight();
			}
			getOwner().translateToAbsolute(p);
			return p;
		}
	}

	/**
	 * Builds a figure for the given edge and adds it to contents
	 *
	 * @param contents the parent figure to add the edge to
	 * @param edge     the edge
	 */
	static void buildEdgeFigure(Figure contents, Edge edge) {
		PolylineConnection conn = connection(edge);
		conn.setForegroundColor(ColorConstants.gray);
		PolygonDecoration dec = new PolygonDecoration();
		conn.setTargetDecoration(dec);
		conn.setPoints(edge.getPoints());
		contents.add(conn);
	}

	/**
	 * Builds a Figure for the given node and adds it to contents
	 *
	 * @param contents the parent Figure to add the node to
	 * @param node     the node to add
	 */
	static void buildNodeFigure(Figure contents, Node node) {
		Label label;
		label = new Label();
		label.setBackgroundColor(ColorConstants.lightGray);
		label.setOpaque(true);
		label.setBorder(new LineBorder());
		if (node.incoming.isEmpty()) {
			label.setBorder(new LineBorder(2));
		}
		String text = node.data.toString();// + "(" + node.index
		// +","+node.sortValue+ ")";
		label.setText(text);
		node.data = label;
		contents.add(label, new Rectangle(node.x, node.y, node.width, node.height));
	}

	// /**
	// * Builds a Figure for the given prime edge
	// * @param e the prime edge
	// * @return the Figure for the prime edge
	// */
	// static PolylineConnection buildPrimeEdge(Edge e) {
	// PolylineConnection line = new PolylineConnection();
	//
	// if (e.tree) {
	// PolygonDecoration dec = new PolygonDecoration();
	// dec.setLineWidth(2);
	//
	// line.setLineWidth(3);
	// Label l = new Label (e.cut + "");
	// l.setOpaque(true);
	// line.add(l, new ConnectionLocator(line));
	// } else {
	// line.setLineStyle(Graphics.LINE_DOT);
	// Label l = new Label (e.getSlack() + "");
	// l.setOpaque(true);
	// line.add(l, new ConnectionLocator(line));
	// }
	// return line;
	// }

	/**
	 * Builds a connection for the given edge
	 *
	 * @param e the edge
	 * @return the connection
	 */
	static PolylineConnection connection(Edge e) {
		PolylineConnection conn = new PolylineConnection();
		conn.setConnectionRouter(new BendpointConnectionRouter());
		List<AbsoluteBendpoint> bends = new ArrayList<>();
		NodeList nodes = e.vNodes;
		if (nodes != null) {
			for (Node n : nodes) {
				int x = n.x;
				int y = n.y;
				bends.add(new AbsoluteBendpoint(x, y));
				bends.add(new AbsoluteBendpoint(x, y + n.height));
			}
		}
		conn.setRoutingConstraint(bends);
		return conn;
	}

	@SuppressWarnings("static-method")
	protected IFigure getContents() {
		return null;
	}

	/**
	 * Returns the FigureCanvas
	 *
	 * @return this demo's FigureCanvas
	 */
	protected GLCanvas getFigureCanvas() {
		return fc;
	}

	/**
	 * Returns an array of strings that represent the names of the methods which
	 * build graphs for this graph demo
	 *
	 * @return array of graph building method names
	 */
	protected abstract String[] getGraphMethods();

	protected void hookShell() {
		Composite composite = new Composite(shell, 0);
		composite.setLayoutData(new GridData(GridData.FILL_VERTICAL));

		composite.setLayout(new GridLayout());
		final org.eclipse.swt.widgets.Label nodesLabel = new org.eclipse.swt.widgets.Label(composite, SWT.NONE);
		nodesLabel.setText("Graph"); //$NON-NLS-1$
		final Combo graphList = new Combo(composite, SWT.DROP_DOWN);

		String[] graphMethods = getGraphMethods();
		for (String graphMethod2 : graphMethods) {
			if (graphMethod2 != null) {
				graphList.add(graphMethod2);
			}
		}
		setGraphMethod(graphMethods[0]);
		graphList.setText(graphMethod);
		graphList.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setGraphMethod(graphList.getItem(graphList.getSelectionIndex()));
				lws.setContents(getContents());
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				graphList.setText(graphMethod);
			}
		});

		// final org.eclipse.swt.widgets.Label seedLabel = new
		// org.eclipse.swt.widgets.Label(
		// composite, SWT.NONE);
		// seedLabel.setText("Build Prime Graph");
		// final Button primeGraphButton = new Button(composite, SWT.CHECK);
		//
		// primeGraphButton.addSelectionListener(new SelectionListener() {
		// public void widgetSelected(SelectionEvent e) {
		// buildPrime = !buildPrime;
		// getFigureCanvas().setContents(getContents());
		// }
		//
		// public void widgetDefaultSelected(SelectionEvent e) {
		// }
		// });

		final org.eclipse.swt.widgets.Label directionLabel = new org.eclipse.swt.widgets.Label(composite, SWT.NONE);
		directionLabel.setText("Graph Direction"); //$NON-NLS-1$
		final Combo directionCombo = new Combo(composite, SWT.DEFAULT);
		directionCombo.setItems("SOUTH", "EAST"); //$NON-NLS-1$ //$NON-NLS-2$
		directionCombo.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				int index = directionCombo.getSelectionIndex();
				switch (index) {
				case 0:
					graphDirection = PositionConstants.SOUTH;
					break;
				case 1:
					graphDirection = PositionConstants.EAST;
					break;
				}
				lws.setContents(getContents());
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		directionCombo.select(0);
	}

	private Display display;
	private GLCanvas glCanvas;
	private DirectContext context;
	private Surface surface;
	private BackendRenderTarget renderTarget;
	private LightweightSystem lws;

	/**
	 * Runs the demo.
	 */
	protected void run() {
		Display d = Display.getDefault();
		shell = new Shell(d);
		String appName = getClass().getName();
		appName = appName.substring(appName.lastIndexOf('.') + 1);
		hookShell();
		shell.setText(appName);
		shell.setLayout(new GridLayout(2, false));
		GLData data = new GLData();
		data.doubleBuffer = true;

		GLCanvas glCanvas = new GLCanvas(shell, SWT.NO_BACKGROUND | SWT.NO_REDRAW_RESIZE, data);
		glCanvas.setCurrent();
		DirectContext context = DirectContext.makeGL();

		lws = new LightweightSystem(glCanvas);
		lws.setControl(glCanvas);
		lws.setContents(getContents());
		lws.setUpdateManager(new DeferredUpdateManager() {
			@Override
			protected void paint(GC gc) {
				if (!validating) {
					if (surface == null || glCanvas.getBounds().width != surface.getWidth()
							|| glCanvas.getBounds().height != surface.getHeight()) {
						release();
						org.eclipse.swt.graphics.Rectangle rect = glCanvas.getClientArea();
						renderTarget = BackendRenderTarget.makeGL(rect.width, rect.height, /* samples */ 0,
								/* stencil */ 8, /* fbid */ 0, FramebufferFormat.GR_GL_RGBA8);
						surface = Surface.makeFromBackendRenderTarget(context, renderTarget, SurfaceOrigin.BOTTOM_LEFT,
								SurfaceColorFormat.RGBA_8888, ColorSpace.getDisplayP3(),
								new SurfaceProps(PixelGeometry.RGB_H));
					}
					gc.innerGC = new SkijaGC((NativeGC) gc.innerGC, surface);
					surface.getCanvas().clear(0xFFFFFFFF);
					gc.setAlpha(255);
					super.paint(gc);
					context.flush();
					glCanvas.swapBuffers();
				} else {
					super.paint(gc);
				}
			}
		});

		setFigureCanvas(glCanvas);
		getFigureCanvas().setLayoutData(new GridData(GridData.FILL_BOTH));
		shell.setSize(1100, 700);
		shell.open();
		while (!shell.isDisposed()) {
			while (!d.readAndDispatch()) {
				d.sleep();
			}
		}
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

	/**
	 * Sets this demo's FigureCanvas
	 *
	 * @param canvas this demo's FigureCanvas
	 */
	protected void setFigureCanvas(GLCanvas canvas) {
		this.fc = canvas;
	}

	/**
	 * Sets the name of the method to call to build the graph
	 *
	 * @param method name of the method used to build the graph
	 */
	public static void setGraphMethod(String method) {
		graphMethod = method;
	}

}
