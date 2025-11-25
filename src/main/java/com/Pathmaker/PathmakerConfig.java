package com.Pathmaker;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("Pathmaker")
public interface PathmakerConfig extends Config
{
	@ConfigSection(
		name = "Welcome Greeting",
		description = "The message to show to the user when they login",
        position = 0
	)
    String welcomeGreeting = "welcomeGreeting";

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
    default double currentTileBorderWidth()
    {
        return 2;
    }

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
    default double hoveredTileBorderWidth()
    {
        return 2;
    }

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

	default String greeting()
	{
		return "Hello";
	}
}
