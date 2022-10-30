package com.mobtech.mtplayer.dialogs

import android.app.Activity
import android.content.Context
import android.text.Spanned
import androidx.core.text.parseAsHtml
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mobtech.mtplayer.AppPreferences
import com.mobtech.mtplayer.R
import com.mobtech.mtplayer.extensions.toFormattedDuration
import com.mobtech.mtplayer.models.Music
import com.mobtech.mtplayer.player.MediaPlayerHolder
import com.mobtech.mtplayer.ui.UIControlInterface


object Dialogs {

    @JvmStatic
    fun showClearFiltersDialog(activity: Activity) {
        val uiControlInterface = (activity as UIControlInterface)
        if (AppPreferences.getPrefsInstance().isAskForRemoval) {
            MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.filter_pref_title)
                .setMessage(R.string.filters_clear)
                .setPositiveButton(R.string.yes) { _, _ ->
                    uiControlInterface.onFiltersCleared()
                }
                .setNegativeButton(R.string.no, null)
                .show()
            return
        }
        uiControlInterface.onFiltersCleared()
    }

    @JvmStatic
    fun showClearQueueDialog(context: Context, mediaPlayerHolder: MediaPlayerHolder) {

        fun clearQueue() {
            AppPreferences.getPrefsInstance().isQueue = null
            AppPreferences.getPrefsInstance().queue = null
            with(mediaPlayerHolder) {
                queueSongs.clear()
                setQueueEnabled(enabled = false, canSkip = isQueueStarted)
            }
        }

        if (AppPreferences.getPrefsInstance().isAskForRemoval) {
            MaterialAlertDialogBuilder(context)
                .setTitle(R.string.queue)
                .setMessage(R.string.queue_songs_clear)
                .setPositiveButton(R.string.yes) { _, _ ->
                    clearQueue()
                }
                .setNegativeButton(R.string.no, null)
                .show()
            return
        }
        clearQueue()
    }

    @JvmStatic
    fun showClearFavoritesDialog(activity: Activity) {
        val uiControlInterface = activity as UIControlInterface
        if (AppPreferences.getPrefsInstance().isAskForRemoval) {
            MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.favorites)
                .setMessage(R.string.favorites_clear)
                .setPositiveButton(R.string.yes) { _, _ ->
                    uiControlInterface.onFavoritesUpdated(clear = true)
                }
                .setNegativeButton(R.string.no, null)
                .show()
            return
        }
        uiControlInterface.onFavoritesUpdated(clear = true)
    }

    @JvmStatic
    fun stopPlaybackDialog(context: Context, mediaPlayerHolder: MediaPlayerHolder) {
        if (AppPreferences.getPrefsInstance().isAskForRemoval) {
            MaterialAlertDialogBuilder(context)
                .setCancelable(false)
                .setTitle(R.string.app_name)
                .setMessage(R.string.on_close_activity)
                .setPositiveButton(R.string.yes) { _, _ ->
                    mediaPlayerHolder.stopPlaybackService(stopPlayback = true)
                }
                .setNegativeButton(R.string.no) { _, _ ->
                    mediaPlayerHolder.stopPlaybackService(stopPlayback = false)
                }
                .setNeutralButton(R.string.cancel, null)
                .show()
            return
        }
        mediaPlayerHolder.stopPlaybackService(stopPlayback = false)
    }

    @JvmStatic
    fun computeDurationText(ctx: Context, favorite: Music?): Spanned? {
        favorite?.startFrom?.let { start ->
            if (start > 0L) {
                return ctx.getString(
                    R.string.favorite_subtitle,
                    start.toLong().toFormattedDuration(
                        isAlbum = false,
                        isSeekBar = false
                    ),
                    favorite.duration.toFormattedDuration(isAlbum = false, isSeekBar = false)
                ).parseAsHtml()
            }
        }
        return favorite?.duration?.toFormattedDuration(isAlbum = false, isSeekBar = false)
            ?.parseAsHtml()
    }
}
