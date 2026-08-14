package me.clientsidecrystals.util;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.Locale;
// some crystal servers (id consider it related :D)
public final class WestShopNameStyle {
    public static final String ADDRESS = "westshop.org";
    public static final String NAPVP_ADDRESS = "napvp.us";

    private static final String WESTSHOP_NAME = "WestShop";
    private static final long LOOP_MILLIS = 4_800L;
    private static final int[] GOLD_STOPS = {
            0xFFC98200,
            0xFFE0A20B,
            0xFFFFC83D,
            0xFFFFE18A,
            0xFFFFCF55,
            0xFFE0A20B
    };
    private static final int[] BLUE_STOPS = {
            0xFF1747B8,
            0xFF1D78E6,
            0xFF5FEAFF,
            0xFFC7FBFF,
            0xFF5FEAFF,
            0xFF1D78E6
    }; // this took too long to look right

    private WestShopNameStyle() {
    }

    public static boolean isWestShop(String address) {
        return matchesAddress(address, ADDRESS);
    }

    public static boolean isNaPvP(String address) {
        return matchesAddress(address, NAPVP_ADDRESS);
    }

    public static void drawAnimatedName(DrawContext context, TextRenderer renderer, int x, int y) {
        drawAnimatedText(context, renderer, WESTSHOP_NAME, x, y, GOLD_STOPS);
    }

    public static void drawAnimatedNaPvPName(
            DrawContext context,
            TextRenderer renderer,
            String name,
            int x,
            int y
    ) {
        drawAnimatedText(context, renderer, stripFormatting(name), x, y, BLUE_STOPS);
    }

    private static boolean matchesAddress(String address, String expected) {
        return expected.equals(address == null ? "" : address.trim().toLowerCase(Locale.ROOT));
    }

    private static void drawAnimatedText(
            DrawContext context,
            TextRenderer renderer,
            String text,
            int x,
            int y,
            int[] colors
    ) {
        int drawX = x;
        float phase = (System.currentTimeMillis() % LOOP_MILLIS) / (float) LOOP_MILLIS;
        int denominator = Math.max(1, text.length() - 1);

        for (int i = 0; i < text.length(); i++) {
            String character = String.valueOf(text.charAt(i));
            float position = (i / (float) denominator - phase + 1.0F) % 1.0F;
            context.drawTextWithShadow(renderer, character, drawX, y, interpolate(position, colors));
            drawX += renderer.getWidth(character);
        }
    }

    private static int interpolate(float position, int[] colors) {
        float scaled = position * colors.length;
        float floor = (float) Math.floor(scaled);
        int leftIndex = (int) floor % colors.length;
        int rightIndex = (leftIndex + 1) % colors.length;
        float amount = smoothStep(scaled - floor);

        int left = colors[leftIndex];
        int right = colors[rightIndex];
        int alpha = lerp((left >>> 24) & 0xFF, (right >>> 24) & 0xFF, amount);
        int red = lerp((left >> 16) & 0xFF, (right >> 16) & 0xFF, amount);
        int green = lerp((left >> 8) & 0xFF, (right >> 8) & 0xFF, amount);
        int blue = lerp(left & 0xFF, right & 0xFF, amount);
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    private static float smoothStep(float value) {
        return value * value * (3.0F - 2.0F * value);
    }

    private static int lerp(int left, int right, float amount) {
        return Math.round(left + (right - left) * amount);
    }

    private static String stripFormatting(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        StringBuilder plain = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char current = text.charAt(i);
            if (current == '\u00a7' && i + 1 < text.length()) {
                i++;
                continue;
            }
            plain.append(current);
        }
        return plain.toString();
    }
} // this file only 4 1.21.11 + 
