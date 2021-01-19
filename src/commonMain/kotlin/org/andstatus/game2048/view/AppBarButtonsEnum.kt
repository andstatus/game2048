package org.andstatus.game2048.view

/** sortOrder < 0 for left-aligned buttons, >= 0 - for right-aligned */
enum class AppBarButtonsEnum(val row: Int, val sortOrder: Int) {
    APP_LOGO(0, -5),

    GAME_MENU(0, 10),

    WATCH(1, -4),
    BOOKMARK(1, -3),
    BOOKMARKED(1, -3),
    BOOKMARK_PLACEHOLDER(1, -3),
    PAUSE(1, -2),
    RESTART(1, -1),
    UNDO(1, 3),
    REDO(1, 5),
    REDO_PLACEHOLDER(1, 5),

    PLAY(1, -4),
    TO_START(1, 2),
    BACKWARDS(1, 3),
    STOP(1, 4),
    STOP_PLACEHOLDER(1, 4),
    FORWARD(1, 5),
    FORWARD_PLACEHOLDER(1, 5),
    TO_CURRENT(1, 6),
}