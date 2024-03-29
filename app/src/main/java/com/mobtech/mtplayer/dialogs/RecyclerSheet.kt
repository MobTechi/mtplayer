package com.mobtech.mtplayer.dialogs


import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.mobtech.mtplayer.AppPreferences
import com.mobtech.mtplayer.R
import com.mobtech.mtplayer.databinding.ModalRvBinding
import com.mobtech.mtplayer.databinding.SleeptimerItemBinding
import com.mobtech.mtplayer.extensions.applyFullHeightDialog
import com.mobtech.mtplayer.extensions.handleViewVisibility
import com.mobtech.mtplayer.models.Music
import com.mobtech.mtplayer.player.MediaPlayerHolder
import com.mobtech.mtplayer.preferences.FiltersAdapter
import com.mobtech.mtplayer.preferences.NotificationActionsAdapter
import com.mobtech.mtplayer.ui.ItemSwipeCallback
import com.mobtech.mtplayer.ui.ItemTouchCallback
import com.mobtech.mtplayer.ui.MediaControlInterface
import com.mobtech.mtplayer.ui.UIControlInterface
import com.mobtech.mtplayer.utils.Theming
import me.zhanghai.android.fastscroll.FastScrollerBuilder


@Suppress("DEPRECATION")
class RecyclerSheet : BottomSheetDialogFragment() {

    private var _modalRvBinding: ModalRvBinding? = null
    private lateinit var mQueueAdapter: QueueAdapter

    // sheet type
    var sheetType = FILTERS_TYPE

    // interfaces
    private lateinit var mUIControlInterface: UIControlInterface
    private lateinit var mMediaControlInterface: MediaControlInterface
    var onQueueCancelled: (() -> Unit)? = null
    var onFavoritesDialogCancelled: (() -> Unit)? = null
    var onSleepTimerDialogCancelled: (() -> Unit)? = null
    var onSleepTimerEnabled: ((Boolean, String) -> Unit)? = null

    private val mMediaPlayerHolder get() = MediaPlayerHolder.getInstance()

    override fun onAttach(context: Context) {
        super.onAttach(context)

        arguments?.getString(TAG_TYPE)?.let { which ->
            sheetType = which
        }

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mUIControlInterface = activity as UIControlInterface
            mMediaControlInterface = activity as MediaControlInterface
        } catch (e: ClassCastException) {
            e.printStackTrace()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _modalRvBinding = ModalRvBinding.inflate(inflater, container, false)
        return _modalRvBinding?.root
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onSleepTimerDialogCancelled?.invoke()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        onQueueCancelled?.invoke()
        onFavoritesDialogCancelled?.invoke()
        onSleepTimerDialogCancelled?.invoke()
        _modalRvBinding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dialog.applyFullHeightDialog(requireActivity())

        var dialogTitle: String

        _modalRvBinding?.run {

            when (sheetType) {

                FILTERS_TYPE -> {

                    sleepTimerElapsed.handleViewVisibility(show = false)

                    dialogTitle = getString(R.string.filter_pref_title)

                    modalRv.setHasFixedSize(true)
                    val filtersAdapter = FiltersAdapter(requireActivity())
                    modalRv.adapter = filtersAdapter

                    bottomDivider.handleViewVisibility(show = true)
                    btnDelete.handleViewVisibility(show = true)
                    btnDelete.setOnClickListener {
                        Dialogs.showClearFiltersDialog(requireActivity())
                    }

                    btnNegative.setOnClickListener {
                        dismiss()
                    }
                    btnPositive.setOnClickListener {
                        val updatedItems = filtersAdapter.getUpdatedItems()
                        if (AppPreferences.getPrefsInstance().filters != updatedItems) {
                            AppPreferences.getPrefsInstance().filters = updatedItems
                            mUIControlInterface.onAppearanceChanged()
                            return@setOnClickListener
                        }
                        dismiss()
                    }
                }

                QUEUE_TYPE -> {

                    dialogTitle = getString(R.string.queue)

                    _modalRvBinding?.btnContainer?.handleViewVisibility(show = false)
                    sleepTimerElapsed.handleViewVisibility(show = false)

                    bottomDivider.handleViewVisibility(show = true)
                    btnDelete.handleViewVisibility(show = true)
                    btnDelete.setOnClickListener {
                        mMediaPlayerHolder.let { mph ->
                            Dialogs.showClearQueueDialog(requireContext(), mph)
                        }
                    }

                    setRecyclerViewProps(modalRv)

                    FastScrollerBuilder(modalRv).useMd2Style().build()

                    mMediaPlayerHolder.run {

                        mQueueAdapter = QueueAdapter(requireActivity(), this)
                        modalRv.adapter = mQueueAdapter

                        mQueueAdapter.onQueueCleared = {
                            dismiss()
                        }

                        ItemTouchHelper(
                            ItemTouchCallback(
                                mQueueAdapter.queueSongs,
                                isActiveTabs = false
                            )
                        )
                            .attachToRecyclerView(modalRv)
                        ItemTouchHelper(
                            ItemSwipeCallback(
                                requireActivity(),
                                isQueueDialog = true,
                                isFavoritesDialog = false
                            ) { viewHolder: RecyclerView.ViewHolder, _: Int ->
                                if (!mQueueAdapter.performQueueSongDeletion(viewHolder.absoluteAdapterPosition)) {
                                    mQueueAdapter.notifyItemChanged(viewHolder.absoluteAdapterPosition)
                                }
                            }).attachToRecyclerView(modalRv)

                        modalRv.post {
                            if (isQueueStarted) {
                                val indexOfCurrentSong = queueSongs.indexOf(currentSong)
                                val layoutManager = modalRv.layoutManager as LinearLayoutManager
                                layoutManager.scrollToPositionWithOffset(indexOfCurrentSong, 0)
                            }
                        }
                    }
                }

                SLEEPTIMER_TYPE -> {

                    dialogTitle = getString(R.string.sleeptimer)

                    val sleepTimerAdapter = SleepTimerAdapter()
                    modalRv.setHasFixedSize(true)
                    modalRv.layoutManager = LinearLayoutManager(
                        requireActivity(),
                        LinearLayoutManager.HORIZONTAL,
                        false
                    )
                    modalRv.adapter = sleepTimerAdapter
                    sleepTimerElapsed.handleViewVisibility(show = false)

                    btnNegative.setOnClickListener {
                        dismiss()
                    }
                    btnPositive.setOnClickListener {
                        mMediaPlayerHolder.run {
                            val isEnabled =
                                pauseBySleepTimer(sleepTimerAdapter.getSelectedSleepTimerValue())
                            onSleepTimerEnabled?.invoke(
                                isEnabled,
                                sleepTimerAdapter.getSelectedSleepTimer()
                            )
                        }
                        dismiss()
                    }
                }

                SLEEPTIMER_ELAPSED_TYPE -> {

                    dialogTitle = getString(R.string.sleeptimer)

                    modalRv.handleViewVisibility(show = false)

                    btnNegative.setOnClickListener {
                        dismiss()
                    }

                    with(btnPositive) {
                        text = getString(R.string.sleeptimer_stop)
                        contentDescription = getString(R.string.sleeptimer_cancel_desc)
                        setOnClickListener {
                            mMediaPlayerHolder.cancelSleepTimer()
                            onSleepTimerEnabled?.invoke(false, "")
                            dismiss()
                        }
                    }
                }

                NOTIFICATION_ACTIONS_TYPE -> {

                    dialogTitle = getString(R.string.notification_actions_pref_title)

                    sleepTimerElapsed.handleViewVisibility(show = false)

                    modalRv.setHasFixedSize(true)
                    val notificationActionsAdapter = NotificationActionsAdapter(requireContext())
                    val layoutManager = LinearLayoutManager(requireActivity())
                    modalRv.layoutManager = layoutManager
                    modalRv.adapter = notificationActionsAdapter

                    // set listeners for buttons
                    btnNegative.setOnClickListener {
                        dismiss()
                    }
                    btnPositive.setOnClickListener {
                        mMediaPlayerHolder.onHandleNotificationUpdate(
                            isAdditionalActionsChanged = true
                        )
                        dismiss()
                    }
                }

                else -> {

                    dialogTitle = getString(R.string.favorites)

                    sleepTimerElapsed.handleViewVisibility(show = false)

                    _modalRvBinding?.btnContainer?.handleViewVisibility(show = false)

                    bottomDivider.handleViewVisibility(show = true)
                    btnDelete.handleViewVisibility(show = true)
                    btnDelete.setOnClickListener {
                        Dialogs.showClearFavoritesDialog(requireActivity())
                    }

                    setRecyclerViewProps(modalRv)
                    val favoritesAdapter = FavoritesAdapter(requireActivity())
                    modalRv.adapter = favoritesAdapter
                    FastScrollerBuilder(modalRv).useMd2Style().build()
                    favoritesAdapter.onFavoritesCleared = {
                        dismiss()
                    }

                    ItemTouchHelper(ItemSwipeCallback(
                        requireActivity(),
                        isQueueDialog = false, isFavoritesDialog = true
                    ) { viewHolder: RecyclerView.ViewHolder,
                        direction: Int ->
                        val index = viewHolder.absoluteAdapterPosition
                        favoritesAdapter.notifyItemChanged(index)
                        if (direction == ItemTouchHelper.RIGHT) {
                            mMediaControlInterface.onAddToQueue(
                                AppPreferences.getPrefsInstance().favorites?.get(
                                    index
                                )
                            )
                            return@ItemSwipeCallback
                        }
                        favoritesAdapter.performFavoriteDeletion(index)
                    }).attachToRecyclerView(modalRv)
                }
            }
            // finally, set the sheet's title
            title.text = dialogTitle
        }
    }

    private fun setRecyclerViewProps(rv: RecyclerView) {
        rv.isVerticalScrollBarEnabled = false
        rv.isHorizontalScrollBarEnabled = false
        rv.setHasFixedSize(true)
    }

    fun swapQueueSong(song: Music?) {
        if (::mQueueAdapter.isInitialized) {
            mQueueAdapter.swapSelectedSong(song)
        }
    }

    fun updateCountdown(value: String) {
        _modalRvBinding?.sleepTimerElapsed?.text = value
    }

    private inner class SleepTimerAdapter :
        RecyclerView.Adapter<SleepTimerAdapter.SleepTimerHolder>() {

        private val sleepOptions = resources.getStringArray(R.array.sleepOptions)
        private val sleepOptionValues = resources.getIntArray(R.array.sleepOptionsValues)

        private var mSelectedPosition = 0

        private val mDefaultTextColor =
            Theming.resolveColorAttr(requireActivity(), android.R.attr.textColorPrimary)

        fun getSelectedSleepTimer(): String = sleepOptions[mSelectedPosition]
        fun getSelectedSleepTimerValue() = sleepOptionValues[mSelectedPosition].toLong()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SleepTimerHolder {
            val binding =
                SleeptimerItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return SleepTimerHolder(binding)
        }

        override fun getItemCount(): Int {
            return sleepOptions.size
        }

        override fun onBindViewHolder(holder: SleepTimerHolder, position: Int) {
            holder.bindItems(sleepOptions[holder.absoluteAdapterPosition])
        }

        inner class SleepTimerHolder(private val binding: SleeptimerItemBinding) :
            RecyclerView.ViewHolder(binding.root) {

            fun bindItems(itemSleepOption: String) {

                with(binding.root) {
                    text = itemSleepOption
                    contentDescription = itemSleepOption
                    setTextColor(
                        if (mSelectedPosition == absoluteAdapterPosition) {
                            resources.getColor(R.color.silver_gray)
                        } else {
                            mDefaultTextColor
                        }
                    )
                    setOnClickListener {
                        notifyItemChanged(mSelectedPosition)
                        mSelectedPosition = absoluteAdapterPosition
                        notifyItemChanged(mSelectedPosition)
                    }
                }
            }
        }
    }

    companion object {

        const val TAG_MODAL_RV = "MODAL_RV"
        private const val TAG_TYPE = "MODAL_RV_TYPE"

        // Modal rv type
        const val FILTERS_TYPE = "MODAL_FILTERS"
        const val QUEUE_TYPE = "MODAL_QUEUE"
        const val FAV_TYPE = "MODAL_FAVORITES"
        const val SLEEPTIMER_TYPE = "MODAL_SLEEPTIMER"
        const val SLEEPTIMER_ELAPSED_TYPE = "MODAL_SLEEPTIMER_ELAPSED"
        const val NOTIFICATION_ACTIONS_TYPE = "MODAL_NOTIFICATION_ACTIONS"

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment [RecyclerSheet].
         */
        @JvmStatic
        fun newInstance(which: String) = RecyclerSheet().apply {
            arguments = bundleOf(TAG_TYPE to which)
        }
    }
}
