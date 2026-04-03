package k4k.travelcorequesting.client.utils;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;

public class DrawContexts {
    public static int drawTextWrapped(DrawContext context, TextRenderer textRenderer, StringVisitable text, int x, int y, int width, int color, boolean shadow) {
        for (OrderedText orderedText : textRenderer.wrapLines(text, width)) {
            context.drawText(textRenderer, orderedText, x, y, color, shadow);
            y += 9;
        }
        return textRenderer.getWrappedLinesHeight(text, width);
    }
}
