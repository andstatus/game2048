package org.andstatus.game2048

// TODO: Game options / tweaks. Default values are for original game,
// see https://en.wikipedia.org/wiki/2048_(video_game)
// and the game in browser: https://play2048.co/
const val allowResultingTileToMerge = false  // The resulting tile cannot merge with another tile again in the same move
const val allowUsersMoveWithoutBlockMoves = false
const val allowUndo = true  // Default = false