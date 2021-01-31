package org.andstatus.game2048.ai

enum class AiAlgorithm(val id: String) {
    RANDOM("ai_random"),
    MAX_SCORE_OF_ONE_MOVE("ai_max_score_one"),
    MAX_EMPTY_BLOCKS_OF_N_MOVES("ai_max_empty_n"),
    MAX_SCORE_OF_N_MOVES("ai_max_score_n"),
    LONGEST_RANDOM_PLAY("ai_longest_random");

    val labelKey: String get() = id

    companion object {
        fun load(id: String?): AiAlgorithm = values()
            .firstOrNull { it.id == id } ?: LONGEST_RANDOM_PLAY
    }
}