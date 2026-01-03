/*
 * Copyright (c) 2025, JosteinE
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *	  list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *	  this list of conditions and the following disclaimer in the documentation
 *	  and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.Pathmaker;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.border.Border;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.components.FlatTextField;

@Slf4j
public class PathGroup extends JPanel
{
	FlatTextField groupTextField = new FlatTextField();
	JPanel memberPanel = new JPanel();
	boolean beingDragged = false;

	PathGroup(PathmakerPlugin plugin, JPanel parentPanel, PathPanel firstPathEntry, String groupName, int parentPanelIndex)
	{
		JPanel groupPanel = this;

		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBackground(Color.BLUE);

		groupTextField.setText(groupName);
		groupTextField.getTextField().setEnabled(false);
		setBorder(createDefaultBorder());
		groupTextField.setBackground(Color.BLUE);

		int PANEL_MARGIN = 20;
		// Add drag and drop adapters, but also extra logic to allow for renaming on click.
		MouseMotionAdapter dragAdapter = new DragAdapter(parentPanel, groupPanel, PANEL_MARGIN);
		groupTextField.getTextField().addMouseMotionListener(new MouseMotionAdapter()
		{
			@Override
			public void mouseDragged(MouseEvent e)
			{
				super.mouseDragged(e);
				beingDragged = true;
				dragAdapter.mouseDragged(e);
			}
		});

		DropAdapter dropAdapter = new DropAdapter(plugin, parentPanel, groupPanel, parentPanelIndex, null, PANEL_MARGIN);
		groupTextField.getTextField().addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				super.mouseClicked(e);
				log.debug("mouse clicked");
				groupTextField.getTextField().setEnabled(true);
				groupTextField.requestFocusInWindow();
				groupTextField.getTextField().selectAll();
				groupTextField.setBackground(Color.DARK_GRAY);
				beingDragged = false;
			}

			@Override
			public void mouseReleased(MouseEvent e)
			{
				super.mouseReleased(e);
				if (beingDragged) // need to do this, otherwise mouseClicked doesn't get called
				{
					dropAdapter.mouseReleased(e);
					beingDragged = false;
				}
			}
		});
		groupTextField.getTextField().addFocusListener(new FocusAdapter()
		{
			@Override
			public void focusLost(FocusEvent e)
			{
				beingDragged = false;
				groupTextField.getTextField().setText(groupName);
				finalizeEditing();
			}
		});
		groupTextField.getTextField().addKeyListener(new KeyAdapter()
		{
			@Override
			public void keyPressed(KeyEvent e)
			{
				super.keyPressed(e);

				if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
				{
					groupTextField.getTextField().setText(groupName);
					finalizeEditing();
				}

				if (e.getKeyCode() == KeyEvent.VK_ENTER)
				{
					// If name changed: rename entries group name
					if (!groupTextField.getText().equals(groupName))
					{
						log.debug("finalizeEditing: group name changed from {} to {}", groupName, groupTextField.getText());
						for (Component c : memberPanel.getComponents())
						{
							String pathLabel = ((PathPanel) c).getPathLabel().getText();
							plugin.getStoredPaths().get(pathLabel).pathGroup = groupTextField.getTextField().getText();
						}
						plugin.rebuildPanel(true);
						return;
					}

					finalizeEditing();
				}
			}
		});

		add(groupTextField);

		memberPanel.setLayout(new BoxLayout(memberPanel, BoxLayout.Y_AXIS));
		memberPanel.setBackground(Color.BLUE);
		memberPanel.add(firstPathEntry);
		add(memberPanel);
	}

	private void finalizeEditing()
	{
		groupTextField.getTextField().setEnabled(false);
		groupTextField.setBackground(Color.BLUE);
	}

	Border createDefaultBorder()
	{
		return BorderFactory.createEmptyBorder(2, 2, 2, 2);
	}

	void addPathPanel(PathPanel panel)
	{
		memberPanel.add(panel);
	}

	Component[] getPathPanels()
	{
		return memberPanel.getComponents();
	}

	int getPathPanelCount()
	{
		return memberPanel.getComponentCount();
	}

	PathPanel getPathPanel(int index)
	{
		return (PathPanel) memberPanel.getComponent(index);
	}

	String getGroupName()
	{
		return groupTextField.getText();
	}
}
