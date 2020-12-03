package org.andstatus.game2048

/** sortOrder < 0 for left-aligned buttons, >= 0 - for right-aligned */
enum class AppBarButtonsEnum(val sortOrder: Int) {
    APP_LOGO(-5),
    WATCH(-4),
    BOOKMARK(-3),
    BOOKMARKED(-3),
    PAUSE(-2),
    RESTART(-1),
    UNDO(3),
    REDO(5),
    GAME_MENU(10),

    PLAY(-4),
    TO_START(2),
    BACKWARDS(3),
    STOP(4),
    FORWARD(5),
    TO_CURRENT(6),
}