package com.Pathmaker;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.components.FlatTextField;

@Slf4j
public class PathGroup extends JPanel
{
	FlatTextField groupTextField = new FlatTextField();
	JPanel memberPanel = new JPanel();
	boolean beingDragged = false;

	PathGroup(PathmakerPlugin plugin, JPanel parentPanel, PathPanel firstPathEntry, ArrayList<String> groupNames,  String groupName, int parentPanelIndex)
	{
		JPanel groupPanel = this;

		int PANEL_MARGIN = 10;

		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
		setBackground(Color.BLUE);

		groupTextField.setText(groupName);
		groupTextField.getTextField().setEnabled(false);
		groupTextField.setBackground(Color.BLUE);

		// Add drag and drop adapters, but also extra logic to allow for renaming on click.
		MouseMotionAdapter dragAdapter = new DragAdapter(parentPanel, groupPanel, true, PANEL_MARGIN);
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
		MouseAdapter dropAdapter = new DropAdapter(plugin, groupNames, parentPanel, groupPanel, parentPanelIndex, null, PANEL_MARGIN);
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
