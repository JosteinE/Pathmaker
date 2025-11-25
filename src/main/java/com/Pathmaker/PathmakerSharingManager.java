package com.Pathmaker;

import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.client.menus.WidgetMenuOption;
import net.runelite.client.menus.MenuManager;
import net.runelite.api.MenuEntry;
import java.util.List;
import javax.inject.Inject;
import java.util.Arrays;
import net.runelite.api.ChatMessageType;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.game.chatbox.ChatboxPanelManager;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j; // https://projectlombok.org/features/log

@Slf4j
public class PathmakerSharingManager // INSPIRED BY https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/plugins/groundmarkers/GroundMarkerSharingManager.java
{
    private static final WidgetMenuOption EXPORT_PATHS_OPTION = new WidgetMenuOption("Export", "Pathmaker", InterfaceID.Orbs.WORLDMAP, InterfaceID.OrbsNomap.WORLDMAP);
    private static final WidgetMenuOption IMPORT_PATHS_OPTION = new WidgetMenuOption("Import", "Pathmaker", InterfaceID.Orbs.WORLDMAP, InterfaceID.OrbsNomap.WORLDMAP);
    private static final WidgetMenuOption CLEAR_PATHS_OPTION = new WidgetMenuOption("Clear", "Pathmaker", InterfaceID.Orbs.WORLDMAP, InterfaceID.OrbsNomap.WORLDMAP);

    private final PathmakerPlugin plugin;
    private final Client client;
    private final MenuManager menuManager;
    private final ChatMessageManager chatMessageManager;
    private final ChatboxPanelManager chatboxPanelManager;

    @Inject
    private PathmakerSharingManager(PathmakerPlugin plugin, Client client, MenuManager menuManager, ChatMessageManager chatMessageManager, ChatboxPanelManager chatboxPanelManager)
    {
        this.plugin = plugin;
        this.client = client;
        this.menuManager = menuManager;
        this.chatMessageManager = chatMessageManager;
        this.chatboxPanelManager = chatboxPanelManager;
    }

    void addMenuOptions()
    {
        menuManager.addManagedCustomMenu(EXPORT_PATHS_OPTION, this::exportPaths);
        menuManager.addManagedCustomMenu(IMPORT_PATHS_OPTION, this::promptForImport);
        menuManager.addManagedCustomMenu(CLEAR_PATHS_OPTION, this::promptForClear);
    }

    void removeMenuOptions()
    {
        menuManager.removeManagedCustomMenu(EXPORT_PATHS_OPTION);
        menuManager.removeManagedCustomMenu(IMPORT_PATHS_OPTION);
        menuManager.removeManagedCustomMenu(CLEAR_PATHS_OPTION);
    }


    private void sendChatMessage(final String message)
    {
        chatMessageManager.queue(QueuedMessage.builder()
                .type(ChatMessageType.CONSOLE)
                .runeLiteFormattedMessage(message)
                .build());
    }

    private void exportPaths(MenuEntry menuEntry)
    {
        sendChatMessage("exportPaths");
//        // Collect IDs of the currently loaded regions
//        int[] regions = client.getMapRegions();
//        if (regions == null)
//        {
//            return;
//        }
//
//        // Collect IDs of the currently active path-point tiles
//        List<PathPoint> activePoints = Arrays.stream(regions)
//                .mapToObj(regionId -> plugin.getPoints(regionId).stream())
//                .flatMap(Function.identity())
//                .collect(Collectors.toList());
//
//        // Check if empty
//        if (activePoints.isEmpty())
//        {
//            sendChatMessage("You have no ground markers to export.");
//            return;
//        }
//
//        // Convert data table to single string
//        final String exportDump = gson.toJson(activePoints);
//
//        log.debug("Exported ground markers: {}", exportDump);
//
//        // Assign the string to the users clipboard
//        Toolkit.getDefaultToolkit()
//                .getSystemClipboard()
//                .setContents(new StringSelection(exportDump), null);
//        sendChatMessage(activePoints.size() + " ground markers were copied to your clipboard.");
    }

    private void promptForImport(MenuEntry menuEntry)
    {
        sendChatMessage("promptForImport");
//        final String clipboardText;
//        try
//        {
//            clipboardText = Toolkit.getDefaultToolkit()
//                    .getSystemClipboard()
//                    .getData(DataFlavor.stringFlavor)
//                    .toString();
//        }
//        catch (IOException | UnsupportedFlavorException ex)
//        {
//            sendChatMessage("Unable to read system clipboard.");
//            log.warn("error reading clipboard", ex);
//            return;
//        }
//
//        log.debug("Clipboard contents: {}", clipboardText);
//        if (Strings.isNullOrEmpty(clipboardText))
//        {
//            sendChatMessage("You do not have any ground markers copied in your clipboard.");
//            return;
//        }
//
//        List<PathPoint> importPoints;
//        try
//        {
//            // CHECKSTYLE:OFF
//            importPoints = gson.fromJson(clipboardText, new TypeToken<List<PathPoint>>(){}.getType());
//            // CHECKSTYLE:ON
//        }
//        catch (JsonSyntaxException e)
//        {
//            log.debug("Malformed JSON for clipboard import", e);
//            sendChatMessage("You do not have any ground markers copied in your clipboard.");
//            return;
//        }
//
//        if (importPoints.isEmpty())
//        {
//            sendChatMessage("You do not have any ground markers copied in your clipboard.");
//            return;
//        }
//
//        chatboxPanelManager.openTextMenuInput("Are you sure you want to import " + importPoints.size() + " ground markers?")
//                .option("Yes", () -> importGroundMarkers(importPoints))
//                .option("No", Runnables.doNothing())
//                .build();
    }

    private void importPaths()//Collection<PathPoint> importPoints)
    {
        sendChatMessage("importPaths");
//        // regions being imported may not be loaded on client,
//        // so need to import each bunch directly into the config
//        // first, collate the list of unique region ids in the import
//        Map<Integer, List<PathPoint>> regionGroupedPoints = importPoints.stream()
//                .collect(Collectors.groupingBy(PathPoint::getRegionId));
//
//        // now import each region into the config
//        regionGroupedPoints.forEach((regionId, groupedPoints) ->
//        {
//            // combine imported points with existing region points
//            log.debug("Importing {} points to region {}", groupedPoints.size(), regionId);
//            Collection<PathPoint> regionPoints = plugin.getPoints(regionId);
//
//            List<PathPoint> mergedList = new ArrayList<>(regionPoints.size() + groupedPoints.size());
//            // add existing points
//            mergedList.addAll(regionPoints);
//
//            // add new points
//            for (PathPoint point : groupedPoints)
//            {
//                // filter out duplicates
//                if (!mergedList.contains(point))
//                {
//                    mergedList.add(point);
//                }
//            }
//
//            plugin.savePoints(regionId, mergedList);
//        });
//
//        // reload points from config
//        log.debug("Reloading points after import");
//        plugin.loadPoints();
//        sendChatMessage(importPoints.size() + " ground markers were imported from the clipboard.");
//    }
    }

    private void promptForClear(MenuEntry entry)
    {
        sendChatMessage("promptForClear");
//        int[] regions = client.getMapRegions();
//        if (regions == null)
//        {
//            return;
//        }
//
//        long numActivePoints = Arrays.stream(regions)
//                .mapToLong(regionId -> plugin.getPoints(regionId).size())
//                .sum();
//
//        if (numActivePoints == 0)
//        {
//            sendChatMessage("You have no ground markers to clear.");
//            return;
//        }
//
//        chatboxPanelManager.openTextMenuInput("Are you sure you want to clear the<br>" + numActivePoints + " currently loaded ground markers?")
//                .option("Yes", () ->
//                {
//                    for (int regionId : regions)
//                    {
//                        plugin.savePoints(regionId, null);
//                    }
//
//                    plugin.loadPoints();
//                    sendChatMessage(numActivePoints + " ground marker"
//                            + (numActivePoints == 1 ? " was cleared." : "s were cleared."));
//
//                })
//                .option("No", Runnables.doNothing())
//                .build();
    }
}