package k4k.travelcorequesting.client.screens;

import k4k.travelcorequesting.TravelcoreQuesting;
import k4k.travelcorequesting.client.models.QuestBriefDto;
import k4k.travelcorequesting.common.datastructures.Rect;
import k4k.travelcorequesting.client.interfaces.ClientQuestManagerContainer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class QuestBookQuestsScreen extends Screen {
    private static final Identifier BACKGROUND_TEXTURE = new Identifier(TravelcoreQuesting.MOD_ID, "textures/gui/quest_book.png");
    private static final Rect BACKGROUND_RECT = new Rect(0, 0, 256, 180);
    private static final int QUEST_BRIEF_HEIGHT = 20;
    private static final int QUEST_LIST_X = 16;
    private static final int QUEST_LIST_Y = 16;

    public QuestBookQuestsScreen() {
        super(Text.translatable("gui.quest_book"));
    }

    public void onDisplayed() {
        if (client == null) return;
        ClientQuestManagerContainer.getQuestManager(client)
                .syncWithServer();
    }

    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (client == null) return;

        var questManager = ClientQuestManagerContainer.getQuestManager(client);

        this.renderBackground(context);

        var onScreenX = (this.width - BACKGROUND_RECT.width()) / 2;
        var onScreenY = (this.height - BACKGROUND_RECT.height()) / 2;

        context.drawTexture(
                BACKGROUND_TEXTURE, onScreenX, onScreenY,
                BACKGROUND_RECT.x(), BACKGROUND_RECT.y(),
                BACKGROUND_RECT.width(), BACKGROUND_RECT.height()
        );

        super.render(context, mouseX, mouseY, delta);

        var i = 0;
        for (var quest : questManager.getPlayerQuests()) {
            renderQuestBrief(
                    context,
                    quest,
                    onScreenX + QUEST_LIST_X,
                    onScreenY + QUEST_LIST_Y + i * QUEST_BRIEF_HEIGHT
            );

            i++;
        }
    }

    public void renderQuestBrief(DrawContext context, QuestBriefDto quest, int x, int y) {
        if (client == null) return;

        var icon = quest.icon().withPath(path -> "textures/icons/" + path + ".png");

        context.drawTexture(icon, x, y, 0, 0, 8, 8, 8, 8);
        context.drawText(
                client.textRenderer,
                Text.literal(quest.title().getString()).styled(style -> style.withBold(true)),
                x + 9,
                y,
                0x2B2B2B,
                true
        );
        context.drawText(
                client.textRenderer,
                quest.description(),
                x + 9,
                y + client.textRenderer.fontHeight + 1,
                0xAAAAAA,
                false
        );
    }
}
