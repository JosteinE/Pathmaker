package com.Pathmaker;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup(PathmakerConfig.GROUND_MARKER_CONFIG_GROUP)
public interface PathmakerConfig extends Config
{
    String GROUND_MARKER_CONFIG_GROUP = "pathmaker";
    String SHOW_MAP_ORB_MENU_OPTIONS = "showMapOrbMenuOptions";

    //------------------------------------------------------------//
    // Current Tile Section
    //------------------------------------------------------------//
    @ConfigSection(
            name = "Current tile",
            description = "Current tile configuration.",
            position = 2
    )
    String currentTile = "currentTile";

    @ConfigItem(
            keyName = "highlightCurrentTile",
            name = "Highlight true tile",
            description = "Highlights true tile player is on as seen by server.",
            position = 1,
            section = currentTile
    )
    default boolean highlightCurrentTile()
    {
        return true;
    }

    @Alpha
    @ConfigItem(
            keyName = "highlightCurrentColor",
            name = "Highlight color",
            description = "Configures the highlight color of current true tile.",
            position = 2,
            section = currentTile
    )
    default Color highlightCurrentColor()
    {
        return Color.CYAN;
    }

    @Alpha
    @ConfigItem(
            keyName = "currentTileFillColor",
            name = "Fill color",
            description = "Configures the fill color of current true tile.",
            position = 3,
            section = currentTile
    )
    default Color currentTileFillColor()
    {
        return new Color(0, 0, 0, 50);
    }

    @ConfigItem(
            keyName = "currentTileBorderWidth",
            name = "Border width",
            description = "Width of the true tile marker border.",
            position = 4,
            section = currentTile
    )
    @Range(max = 10)
    default int currentTileBorderWidth()
    {
        return 2;
    }

    //------------------------------------------------------------//
    // Hovered Tile Section
    //------------------------------------------------------------//
    @ConfigSection(
            name = "Hovered tile",
            description = "Hovered tile configuration.",
            position = 1
    )
    String hoveredTile = "hoveredTile";

    @ConfigItem(
            keyName = "highlightHoveredTile",
            name = "Highlight hovered tile",
            description = "Highlights tile player is hovering with mouse.",
            position = 1,
            section = hoveredTile
    )
    default boolean highlightHoveredTile()
    {
        return true;
    }

    @Alpha
    @ConfigItem(
            keyName = "highlightHoveredColor",
            name = "Highlight color",
            description = "Configures the highlight color of hovered tile.",
            position = 2,
            section = hoveredTile
    )
    default Color highlightHoveredColor()
    {
        return new Color(255, 0, 0, 255);
    }

    @Alpha
    @ConfigItem(
            keyName = "hoveredTileFillColor",
            name = "Fill color",
            description = "Configures the fill color of hovered tile.",
            position = 3,
            section = hoveredTile
    )
    default Color hoveredTileFillColor()
    {
        return new Color(255, 0, 0, 50);
    }

    @ConfigItem(
            keyName = "hoveredTileBorderWidth",
            name = "Border width",
            description = "Width of the hovered tile marker border.",
            position = 4,
            section = hoveredTile
    )
    @Range(max = 10)
    default int hoveredTileBorderWidth()
    {
        return 2;
    }

    enum hoveredTileLabelMode
    {
        NONE,
        TILE_REGION,
        TILE_LOCATION,
        DISTANCE,
        ALL,
    }
    @ConfigItem(
            keyName = "hoveredTileLabelModeSelect",
            name = "Hovered tile label mode",
            description = "Label to be placed on the hovered tile.",
            position = 5,
            section = hoveredTile
    )
    default hoveredTileLabelMode hoveredTileLabelModeSelect()
    {
        return hoveredTileLabelMode.NONE;
    }

    @Alpha
    @ConfigItem(
            keyName = "hoveredTileLabelColor",
            name = "Hovered tile label color",
            description = "Configures the fill color of hovered tile label.",
            position = 6,
            section = hoveredTile
    )
    default Color hoveredTileLabelColor()
    {
        return new Color(255, 255, 0, 255);
    }

    //------------------------------------------------------------//
    // Path Line Section
    //------------------------------------------------------------//
    @ConfigSection(
            name = "Path line",
            description = "Path line configuration.",
            position = 3
    )
    String pathLine = "pathLine";

    @ConfigItem(
            keyName = "drawPathLine",
            name = "Draw",
            description = "Render path lines",
            position = 1,
            section = pathLine
    )
    default boolean drawPathLine()
    {
        return true;
    }

    @ConfigItem(
            keyName = "pathWidth",
            name = "Path width",
            description = "Width of the path line.",
            position = 2,
            section = pathLine
    )
    @Range(max = 10)
    default int pathLineWidth()
    {
        return 2;
    }

    @Alpha
    @ConfigItem(
            keyName = "pathLineColor",
            name = "Line color",
            description = "Configures the path line color.",
            position = 3,
            section = pathLine
    )
    default Color pathLineColor()
    {
        return new Color(0, 255, 0, 255);
    }

    //------------------------------------------------------------//
    // Path Container Section
    //------------------------------------------------------------//
    @ConfigSection(
            name = "Path Container",
            description = "Contains all paths.",
            position = 4
    )
    String pathContainer = "pathContainer";

    @ConfigItem(
            keyName = SHOW_MAP_ORB_MENU_OPTIONS,
            name = "Show map orb menu options",
            description = "Adds import/export/clear options to the world map orb.",
            position = 1,
            section = pathContainer
    )
    default boolean showMapOrbMenuOptions()
    {
        return true;
    }

    @ConfigItem(
            keyName = "activePath",
            name = "Active path",
            description = "The currently selected path to add points to.",
            position = 2,
            section = pathContainer
    )
    default String activePath()
    {
        return "Unnamed";
    }

    @ConfigItem(
            keyName = "storedPaths",
            name = "Stored paths",
            description = "A list of all of the stored paths.",
            position = 3,
            section = pathContainer
    )
    default String storedPaths()
    {
        return "";
    }

    //------------------------------------------------------------//
    // Info Box Section
    //------------------------------------------------------------//
    @ConfigSection(
            name = "Info Box",
            description = "Info Box configuration.",
            position = 5
    )
    String infoBox = "infoBox";

    @ConfigItem(
            keyName = "infoBoxEnabled",
            name = "Enabled",
            description = "Render info box",
            position = 1,
            section = infoBox
    )
    default boolean infoBoxEnabled()
    {
        return true;
    }

    @ConfigItem(
            keyName = "infoBoxSpeed",
            name = "Show Speed",
            description = "Print how many tiles the player moved since last tick.",
            position = 2,
            section = infoBox
    )
    default boolean infoBoxSpeed()
    {
        return false;
    }
}
