package org.andstatus.game2048.view

enum class GameMenuButtonsEnum(val icon: String, val labelKey: String) {
    BOOKMARKS("bookmarks", "goto_bookmark"),
    RESTORE("restore", "restore_game"),
    RESTART("restart", "try_again"),
    SHARE("share", "share"),
    LOAD("load", "load_game"),
    HELP("help", "help_title"),
    DELETE("delete", "delete_game"),
    SELECT_THEME("palette", "select_theme"),
}