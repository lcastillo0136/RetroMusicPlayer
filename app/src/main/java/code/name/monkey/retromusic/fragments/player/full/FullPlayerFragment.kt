package code.name.monkey.retromusic.fragments.player.full

import android.app.ActivityOptions
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import code.name.monkey.appthemehelper.util.ToolbarContentTintHelper
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.extensions.hide
import code.name.monkey.retromusic.extensions.show
import code.name.monkey.retromusic.fragments.base.AbsPlayerFragment
import code.name.monkey.retromusic.fragments.player.PlayerAlbumCoverFragment
import code.name.monkey.retromusic.glide.ArtistGlideRequest
import code.name.monkey.retromusic.glide.RetroMusicColoredTarget
import code.name.monkey.retromusic.helper.MusicPlayerRemote
import code.name.monkey.retromusic.helper.MusicProgressViewUpdateHelper
import code.name.monkey.retromusic.loaders.ArtistLoader
import code.name.monkey.retromusic.model.Song
import code.name.monkey.retromusic.model.lyrics.AbsSynchronizedLyrics
import code.name.monkey.retromusic.model.lyrics.Lyrics
import code.name.monkey.retromusic.util.NavigationUtil
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.fragment_full.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FullPlayerFragment : AbsPlayerFragment(), MusicProgressViewUpdateHelper.Callback {
    private lateinit var lyricsLayout: FrameLayout
    private lateinit var lyricsLine1: TextView
    private lateinit var lyricsLine2: TextView

    private var lyrics: Lyrics? = null
    private lateinit var progressViewUpdateHelper: MusicProgressViewUpdateHelper

    override fun onUpdateProgressViews(progress: Int, total: Int) {
        if (!isLyricsLayoutBound()) return

//        if (!isLyricsLayoutVisible()) {
//            hideLyricsLayout()
//            return
//        }

        if (lyrics !is AbsSynchronizedLyrics) {

            lyricsLayout.visibility = View.VISIBLE
            lyricsLayout.alpha = 1f
            lyricsLine2.visibility = View.VISIBLE

            lyricsLine2.text = lyrics?.text;
            lyricsLine2.measure(
                View.MeasureSpec.makeMeasureSpec(lyricsLine2.measuredWidth, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.UNSPECIFIED
            )
            // lyricsLine2.textSize = 12f
            val h: Float = lyricsLine2.measuredHeight.toFloat()

            // lyricsLine2.alpha = 0f
            // lyricsLine2.translationY = h
            // lyricsLine2.animate().alpha(1f).translationY(0f).duration = VISIBILITY_ANIM_DURATION
            return
        }
        val synchronizedLyrics = lyrics as AbsSynchronizedLyrics

        lyricsLayout.visibility = View.VISIBLE
        lyricsLayout.alpha = 1f

        val oldLine = lyricsLine2.text.toString()
        val line = synchronizedLyrics.getLine(progress)

        if (oldLine != line || oldLine.isEmpty()) {
            lyricsLine1.text = oldLine
            lyricsLine2.text = line

            lyricsLine1.visibility = View.VISIBLE
            lyricsLine2.visibility = View.VISIBLE

            lyricsLine2.measure(
                View.MeasureSpec.makeMeasureSpec(
                    lyricsLine2.measuredWidth,
                    View.MeasureSpec.EXACTLY
                ),
                View.MeasureSpec.UNSPECIFIED
            )
            val h: Float = lyricsLine2.measuredHeight.toFloat()

            lyricsLine1.alpha = 1f
            lyricsLine1.translationY = 0f
            lyricsLine1.animate().alpha(0f).translationY(-h).duration = VISIBILITY_ANIM_DURATION

            lyricsLine2.alpha = 0f
            lyricsLine2.translationY = h
            lyricsLine2.animate().alpha(1f).translationY(0f).duration = VISIBILITY_ANIM_DURATION
        }
    }

    private fun isLyricsLayoutVisible(): Boolean {
        return lyrics != null && lyrics!!.isSynchronized && lyrics!!.isValid
    }

    private fun isLyricsLayoutBound(): Boolean {
        return lyricsLayout != null && lyricsLine1 != null && lyricsLine2 != null
    }

    private fun hideLyricsLayout() {
        lyricsLayout.animate().alpha(0f).setDuration(VISIBILITY_ANIM_DURATION)
            .withEndAction(Runnable {
                if (!isLyricsLayoutBound()) return@Runnable
                lyricsLayout.visibility = View.GONE
                lyricsLine1.text = null
                lyricsLine2.text = null
            })
    }

    override fun setLyrics(l: Lyrics?) {
        lyrics = l

        if (!isLyricsLayoutBound()) return

//        if (!isLyricsLayoutVisible()) {
//            hideLyricsLayout()
//            return
//        }

        lyricsLine1.text = null
        lyricsLine2.text = null

        lyricsLayout.visibility = View.VISIBLE
        lyricsLayout.animate().alpha(1f).duration = VISIBILITY_ANIM_DURATION
    }

    override fun playerToolbar(): Toolbar {
        return playerToolbar
    }

    private var lastColor: Int = 0
    override val paletteColor: Int
        get() = lastColor
    private lateinit var fullPlaybackControlsFragment: FullPlaybackControlsFragment

    private fun setUpPlayerToolbar() {
        playerToolbar.apply {
            setNavigationOnClickListener { requireActivity().onBackPressed() }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_full, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lyricsLayout = view.findViewById(R.id.player_lyrics)
        lyricsLine1 = view.findViewById(R.id.player_lyrics_line1)
        lyricsLine2 = view.findViewById(R.id.player_lyrics_line2)

        setUpSubFragments()
        setUpPlayerToolbar()
        setupArtist()
        nextSong.isSelected = true
        nextSongLabel.setOnClickListener {
            NavigationUtil.goToPlayingQueue(requireActivity())
        }
        nextSong.setOnClickListener {
            NavigationUtil.goToPlayingQueue(requireActivity())
        }
        progressViewUpdateHelper = MusicProgressViewUpdateHelper(this, 500, 1000)
        progressViewUpdateHelper.start()
        lyricsLine2.setOnClickListener {
            NavigationUtil.goToLyrics(requireActivity())
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 1) {
            this.refreshLyrics()
        }
    }

    private fun setupArtist() {
        artistImage.setOnClickListener {
            val transitionName =
                "${getString(R.string.transition_artist_image)}_${MusicPlayerRemote.currentSong.artistId}"
            val activityOptions =
                ActivityOptions.makeSceneTransitionAnimation(
                    requireActivity(),
                    artistImage,
                    transitionName
                )
            NavigationUtil.goToArtistOptions(
                requireActivity(),
                MusicPlayerRemote.currentSong.artistId,
                activityOptions
            )
        }
    }

    private fun setUpSubFragments() {
        fullPlaybackControlsFragment =
            childFragmentManager.findFragmentById(R.id.playbackControlsFragment) as FullPlaybackControlsFragment

        val playerAlbumCoverFragment =
            childFragmentManager.findFragmentById(R.id.playerAlbumCoverFragment) as PlayerAlbumCoverFragment
        playerAlbumCoverFragment.setCallbacks(this)
        playerAlbumCoverFragment.removeSlideEffect()
    }

    override fun onShow() {
    }

    override fun onHide() {
    }

    override fun onBackPressed(): Boolean {
        return false
    }

    override fun toolbarIconColor(): Int {
        return Color.WHITE
    }

    override fun onColorChanged(color: Int) {
        lastColor = color
        fullPlaybackControlsFragment.setDark(color)
        callbacks?.onPaletteColorChanged()
        ToolbarContentTintHelper.colorizeToolbar(playerToolbar, Color.WHITE, activity)
    }

    override fun onFavoriteToggled() {
        toggleFavorite(MusicPlayerRemote.currentSong)
        fullPlaybackControlsFragment.onFavoriteToggled()
    }

    override fun toggleFavorite(song: Song) {
        super.toggleFavorite(song)
        if (song.id == MusicPlayerRemote.currentSong.id) {
            updateIsFavorite()
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        updateArtistImage()
        updateLabel()
    }

    override fun onPlayingMetaChanged() {
        super.onPlayingMetaChanged()
        updateArtistImage()
        updateLabel()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        progressViewUpdateHelper.stop()
    }

    private fun updateArtistImage() {
        if (context != null) {
            CoroutineScope(Dispatchers.IO).launch {
                val artist = ArtistLoader.getArtist(requireContext(), MusicPlayerRemote.currentSong.artistId)
                withContext(Dispatchers.Main) {
                    if (context != null ) {
                        ArtistGlideRequest.Builder.from(Glide.with(requireContext()), artist)
                            .generatePalette(requireContext())
                            .build()
                            .into(object : RetroMusicColoredTarget(artistImage) {
                                override fun onColorReady(color: Int) {
                                }
                            })
                    }
                }
            }
        }
    }

    override fun onQueueChanged() {
        super.onQueueChanged()
        if (MusicPlayerRemote.playingQueue.isNotEmpty()) updateLabel()
    }

    private fun updateLabel() {
        (MusicPlayerRemote.playingQueue.size - 1).apply {
            if (this == (MusicPlayerRemote.position)) {
                nextSongLabel.setText(R.string.last_song)
                nextSong.hide()
            } else {
                val title = MusicPlayerRemote.playingQueue[MusicPlayerRemote.position + 1].title
                nextSongLabel.setText(R.string.next_song)
                nextSong.apply {
                    text = title
                    show()
                }
            }
        }
    }
}
