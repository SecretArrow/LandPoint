package com.landpoint.app.ui

/**
 * What the fold on a window is doing, in that window's own pixels.
 *
 * Plain ints and booleans rather than the window library's types, so the layout
 * decisions that consume it (see [WindowLayout]) stay arithmetic a JVM test can
 * check without a device — the same reasoning that keeps the width decisions on
 * a bare `Int` instead of a size class. The translation from the platform's
 * reading happens exactly once, where the reading is taken
 * ([rememberWindowInfo]); nothing downstream knows the window library exists.
 *
 * [foldTopY] and [foldBottomY] are the hinge band's top and bottom edges, with y
 * measured down from the window's top. That is the space the platform reports
 * the fold's bounds in, and the same one Compose's `positionInWindow` measures
 * in, so a decision can compare an overlay's edge against the hinge as one
 * number against another.
 */
data class FoldState(val isSeparating: Boolean, val hingeRunsHorizontally: Boolean, val foldTopY: Int, val foldBottomY: Int) {
    companion object {
        /**
         * The reading for a window with no fold to mind about: a straight phone,
         * an emulator, a foldable opened flat.
         */
        val FLAT = FoldState(
            isSeparating = false,
            hingeRunsHorizontally = false,
            foldTopY = 0,
            foldBottomY = 0
        )
    }
}
