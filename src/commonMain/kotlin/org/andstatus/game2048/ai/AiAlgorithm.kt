package org.andstatus.game2048.ai

enum class AiAlgorithm(val id: String) {
    RANDOM("random"),
    MAX_SCORE_OF_NEXT_MOVE("max_next_one"),
    MAX_SCORE_OF_N_MOVES("max_next_n"),
    MAX_FREE_OF_N_MOVES("max_free_n"),
    LONGEST_RANDOM_PLAY("longest_random");

    companion object {
        fun load(value: String?): AiAlgorithm = AiAlgorithm.values()
            .firstOrNull { it.toString() == value } ?: LONGEST_RANDOM_PLAY
    }
}