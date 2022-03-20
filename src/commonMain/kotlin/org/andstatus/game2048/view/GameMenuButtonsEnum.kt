package org.andstatus.game2048.view

enum class GameMenuButtonsEnum(val icon: String, val labelKey: String) {
    BOOKMARKS("bookmarks", "goto_bookmark"),
    RECENT("recent", "recent_games"),
    RESTART("restart", "try_again"),
    SHARE("share", "share"),
    LOAD("load", "load_game"),
    HELP("help", "help_title"),
    DELETE("delete", "delete_game"),
    SELECT_THEME("palette", "select_theme"),
    SELECT_AI_ALGORITHM("magic", "select_ai_algorithm"),
    EXIT("exit", "exit"),
}
