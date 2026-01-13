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
import java.util.function.Consumer;
import net.runelite.api.ChatMessageType;
import net.runelite.api.MenuEntry;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.Text;

public class MenuEntryUtil
{
	static Runnable getAddPointMenuEntryMethod(PathmakerPlugin plugin, String pathName, PathPoint point, String targetEntityString)
	{
		return () -> {
			plugin.createOrAddToPath(Text.removeTags(pathName), point); // Also sets the created path as the active path
			String targetLabel = Text.removeTags(targetEntityString);
			if (!targetLabel.equals("Tile"))
				point.setLabel(targetLabel);
			plugin.savePath(plugin.getActivePathName());
			plugin.rebuildPanel(false);
		};
	}

	static Consumer<MenuEntry> getAddPointMenuEntry(PathmakerPlugin plugin, String pathName, PathPoint point, String targetEntityString)
	{
		return menuEntry ->
		{
			getAddPointMenuEntryMethod(plugin, pathName, point, targetEntityString).run();
		};
	}

	static Runnable getAddPathMenuEntryMethod(PathmakerPlugin plugin, String prompt, String pathName, PathPoint point, String targetEntityString)
	{
		return () ->
		{
			plugin.chatboxPanelManager.openTextInput(prompt)
				.value(pathName)
				.onDone(label ->
				{
					if (label.length() > plugin.pluginPanel.MAX_PATH_NAME_LENGTH)
						label = label.substring(0, plugin.pluginPanel.MAX_PATH_NAME_LENGTH);

					if (!plugin.getStoredPaths().isEmpty() && plugin.getStoredPaths().containsKey(label))
					{
						String chatMessage =  ColorUtil.wrapWithColorTag(Text.removeTags("Path name \"" + label + "\" is already in use!"), Color.RED);
						plugin.chatMessageManager.queue(QueuedMessage.builder()
							.type(ChatMessageType.GAMEMESSAGE)
							.runeLiteFormattedMessage(chatMessage)
							.build());
					}
					else
						getAddPointMenuEntryMethod(plugin, label, point, targetEntityString).run();

				}).build();
		};
	}

	static Consumer<MenuEntry> getAddPathMenuEntry(PathmakerPlugin plugin, String pathName, PathPoint point, String targetEntityString)
	{
		return menuEntry ->
		{
			getAddPathMenuEntryMethod(plugin,"Path name", pathName, point, targetEntityString).run();
		};
	}

	static Consumer<MenuEntry> getLabelRenameMenuEntryMethod(PathmakerPlugin plugin, PathPoint point, String targetEntityString, int labelLength)
	{
		return menuEntry -> {
			String currentLabel = point.getLabel() == null ? "" : point.getLabel();

			plugin.chatboxPanelManager.openTextInput(targetEntityString + " label")
				.value(currentLabel)
				.onDone(label ->
				{
					if (label.length() > labelLength)
						label = label.substring(0, labelLength);
					point.setLabel(label); // From
					plugin.rebuildPanel(true);
				})
				.build();
		};
	}

	static Consumer<MenuEntry> getLoopMenuEntryMethod(PathmakerPlugin plugin, PathmakerPath path, String pathName, PathPoint point)
	{
		return menuEntry ->
		{
			// Reverse and unloop if target point is second to last in draw order (this preserves the path structure)
			if (path.loopPath &&
				(point != null && point.getDrawIndex() == path.getSize() - 2) ||
				(path.drawToPlayer == PathPanel.drawFromPlayerMode.START_ONLY.ordinal() && point != null && point.getDrawIndex() == path.getSize() - 1))
			{
				path.getPointAtDrawIndex(path.getSize() -1).drawToPrevious = true;
				path.setNewIndex(path.getPointAtDrawIndex(path.getSize() - 1), 0);
				path.reverseDrawOrder();
			}

			path.loopPath = !path.loopPath;

			plugin.saveProperty("path", pathName);
			plugin.rebuildPanel(false);
		};
	}

	static Consumer<MenuEntry> getRemovePointMenuEntry(PathmakerPlugin plugin, String pathName, PathPoint pathPoint)
	{
		return menuEntry ->
		{
			plugin.removePoint(pathName, pathPoint);
		};
	}
}