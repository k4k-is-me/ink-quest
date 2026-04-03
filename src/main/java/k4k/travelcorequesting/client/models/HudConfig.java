package k4k.travelcorequesting.client.models;

public record HudConfig (
        int hudW,
        int questPaddingX,
        int questPaddingY,
        int questTitleGap,
        int requiredTaskX,
        int requiredTaskIconGap,
        int requiredTaskGap,
        int optionalTaskX,
        int optionalTaskGap,
        int optionalTaskIconGap,
        int barTaskGap,
        int successBarW,
        int successBarH,
        int barGap,
        int failureBarW,
        int failureBarH,
        int questGap
) {}
