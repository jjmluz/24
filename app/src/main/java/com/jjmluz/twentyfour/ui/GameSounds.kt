package com.jjmluz.twentyfour.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.jjmluz.twentyfour.R
import com.jjmluz.twentyfour.game.Sound

/** Short sound effects, decoded up front so they play without delay. */
class GameSounds(context: Context) {

    private val pool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private val ids = Sound.entries.associateWith { pool.load(context, it.resource, 1) }

    fun play(sound: Sound) {
        pool.play(ids.getValue(sound), 1f, 1f, 1, 0, 1f)
    }

    fun release() = pool.release()

    private val Sound.resource
        get() = when (this) {
            Sound.DEAL -> R.raw.deal
            Sound.PICK_NUMBER -> R.raw.pick_number
            Sound.PICK_OP -> R.raw.pick_op
            Sound.MERGE -> R.raw.merge
            Sound.UNDO -> R.raw.undo
            Sound.BLOCKED -> R.raw.blocked
            Sound.MISS -> R.raw.miss
            Sound.SOLVED -> R.raw.solved
        }
}
