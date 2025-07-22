/*******************************************************************************
 * Copyright (c) 2025 COSEDA Technologies GmbH and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.gef.test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.eclipse.jface.viewers.CellEditor;

import org.eclipse.draw2d.IFigure;

import org.eclipse.gef.EditDomain;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartListener;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.LayerConstants;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.gef.editparts.LayerManager;
import org.eclipse.gef.requests.DirectEditRequest;
import org.eclipse.gef.tools.CellEditorLocator;
import org.eclipse.gef.tools.DirectEditManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class DirectEditManagerWithMockTest {
	/// Manager Under Test
	TestDirectEditManager mut;

	@Mock
	GraphicalEditPart sourceEditPart;

	@Mock
	CellEditorLocator locator;

	@Mock
	EditPartViewer viewer;

	@Mock
	Composite viewerControl;

	@Mock
	CellEditor cellEditor;

	@Mock
	IFigure sourceFigure;

	@Mock
	Control cellEditorControl;

	@Mock
	TestRootEditPart rootEditPart;

	@Mock
	IFigure feedbackLayer;

	@Mock
	EditDomain editDomain;

	@Mock
	CommandStack commandStack;

	@Mock
	Command directEditCommand;

	/**
	 * Inner class for using CellEditor mock and making methods public for testing
	 */
	private class TestDirectEditManager extends DirectEditManager {
		public TestDirectEditManager(GraphicalEditPart source, Class<? extends CellEditor> editorType,
				CellEditorLocator locator, Object feature) {
			super(source, editorType, locator, feature);
		}

		@Override
		protected void initCellEditor() {
		}

		@Override
		protected CellEditor createCellEditorOn(Composite composite) {
			return cellEditor;
		}

		@Override
		public void commit() {
			super.commit();
		}

		@Override
		public void bringDown() {
			super.bringDown();
		}
	}

	public static interface TestRootEditPart extends EditPart, LayerManager {
	}

	@BeforeEach
	public void setup() {
		mut = new TestDirectEditManager(sourceEditPart, CellEditor.class, locator, new Object());
		when(rootEditPart.getLayer(LayerConstants.FEEDBACK_LAYER)).thenReturn(feedbackLayer);
		when(viewer.getEditPartForModel(LayerManager.ID)).thenReturn(rootEditPart);
		when(viewer.getControl()).thenReturn(viewerControl);
		when(cellEditor.getControl()).thenReturn(cellEditorControl);
		when(cellEditorControl.getBounds()).thenReturn(new Rectangle(100, 200, 400, 50));
		when(viewer.getEditDomain()).thenReturn(editDomain);
		when(editDomain.getCommandStack()).thenReturn(commandStack);

		when(sourceEditPart.getViewer()).thenReturn(viewer);
		when(sourceEditPart.getFigure()).thenReturn(sourceFigure);
		when(sourceEditPart.getCommand(any(DirectEditRequest.class))).thenReturn(directEditCommand);
	}

	/***
	 * Showcase: If {@link EditPart#removeEditPartListener(EditPartListener)}
	 * accepts null, no exception is thrown.
	 */
	@Test
	public void showAndCommitWithoutException() {
		// Emulate that the direct edit command leads to calling bringDown()
		doAnswer(invocation -> {
			mut.bringDown();
			return null;
		}).when(commandStack).execute(directEditCommand);

		mut.show();

		// Commit only executes the command when the cell editor is dirty
		when(cellEditor.isDirty()).thenReturn(true);

		mut.commit();
	}

	/***
	 * Showcase: If {@link EditPart#removeEditPartListener(EditPartListener)} throws
	 * an {@link IllegalArgumentException} when called with null,
	 * {@link DirectEditManager#commit()} will also throw an exception.
	 */
	@Test
	public void showAndCommitWithException() {
		// removeEditPartListener is called with EditPartListener -> OK
		doNothing().when(sourceEditPart).removeEditPartListener(any(EditPartListener.class));

		// removeEditPartListener is called with null -> throw exception
		doThrow(new IllegalArgumentException("Listener must not be null")).when(sourceEditPart) //$NON-NLS-1$
				.removeEditPartListener(isNull());

		// Emulate that the direct edit command leads to calling bringDown()
		doAnswer(invocation -> {
			mut.bringDown();
			return null;
		}).when(commandStack).execute(directEditCommand);

		mut.show();

		// Commit only executes the command when the cell editor is dirty
		when(cellEditor.isDirty()).thenReturn(true);

		mut.commit();
	}
}
