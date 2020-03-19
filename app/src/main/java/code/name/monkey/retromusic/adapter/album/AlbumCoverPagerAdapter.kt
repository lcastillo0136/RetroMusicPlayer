package code.name.monkey.retromusic.adapter.album

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.NonNull
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.dialogs.SongDetailDialog
import code.name.monkey.retromusic.fragments.AlbumCoverStyle
import code.name.monkey.retromusic.glide.RetroMusicColoredTarget
import code.name.monkey.retromusic.glide.SongGlideRequest
import code.name.monkey.retromusic.misc.CustomFragmentStatePagerAdapter
import code.name.monkey.retromusic.model.Song
import code.name.monkey.retromusic.util.MusicUtil
import code.name.monkey.retromusic.util.NavigationUtil
import code.name.monkey.retromusic.util.PreferenceUtil
import com.bumptech.glide.Glide
import org.jaudiotagger.audio.AudioFile
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.audio.exceptions.CannotReadException
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException
import org.jaudiotagger.tag.TagException
import java.io.File
import java.io.IOException
import java.util.*

class AlbumCoverPagerAdapter(
    fragmentManager: FragmentManager,
    private val dataSet: ArrayList<Song>
) : CustomFragmentStatePagerAdapter(fragmentManager) {

    private var currentColorReceiver: AlbumCoverFragment.ColorReceiver? = null
    private var currentColorReceiverPosition = -1

    override fun getItem(position: Int): Fragment {
        return AlbumCoverFragment.newInstance(dataSet[position])
    }

    override fun getCount(): Int {
        return dataSet.size
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val o = super.instantiateItem(container, position)
        if (currentColorReceiver != null && currentColorReceiverPosition == position) {
            receiveColor(currentColorReceiver!!, currentColorReceiverPosition)
        }
        return o
    }

    /**
     * Only the latest passed [AlbumCoverFragment.ColorReceiver] is guaranteed to receive a
     * response
     */
    fun receiveColor(colorReceiver: AlbumCoverFragment.ColorReceiver, position: Int) {

        if (getFragment(position) is AlbumCoverFragment) {
            val fragment = getFragment(position) as AlbumCoverFragment
            currentColorReceiver = null
            currentColorReceiverPosition = -1
            fragment.receiveColor(colorReceiver, position)
        } else {
            currentColorReceiver = colorReceiver
            currentColorReceiverPosition = position
        }
    }

    class AlbumCoverFragment : Fragment() {

        lateinit var albumCover: ImageView
        private var isColorReady: Boolean = false
        private var color: Int = 0
        private lateinit var song: Song
        private var colorReceiver: ColorReceiver? = null
        private var request: Int = 0

        private val layout: Int
            get() {
                return when (PreferenceUtil.getInstance(requireContext()).albumCoverStyle) {
                    AlbumCoverStyle.NORMAL -> R.layout.fragment_album_cover
                    AlbumCoverStyle.FLAT -> R.layout.fragment_album_flat_cover
                    AlbumCoverStyle.CIRCLE -> R.layout.fragment_album_circle_cover
                    AlbumCoverStyle.CARD -> R.layout.fragment_album_card_cover
                    AlbumCoverStyle.MATERIAL -> R.layout.fragment_album_material_cover
                    AlbumCoverStyle.FULL -> R.layout.fragment_album_full_cover
                    AlbumCoverStyle.FULL_CARD -> R.layout.fragment_album_full_card_cover
                    else -> R.layout.fragment_album_cover
                }
            }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            if (arguments != null) {
                song = requireArguments().getParcelable(SONG_ARG)!!
            }
        }

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            val finalLayout = when {
                PreferenceUtil.getInstance(requireContext()).carouselEffect() -> R.layout.fragment_album_carousel_cover
                else -> layout
            }
            val view = inflater.inflate(finalLayout, container, false)
            albumCover = view.findViewById(R.id.player_image)
            albumCover.setOnClickListener {
                NavigationUtil.goToLyrics(requireActivity())
            }
            return view
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            loadAlbumCover()
        }

        override fun onDestroyView() {
            super.onDestroyView()
            colorReceiver = null
        }

        private fun loadAlbumCover() {
            if (song != null) {
                val songFile = File(song.data)
                if (songFile.exists()) {
                    var hasCover: Boolean = false
                    try {
                        val audioFile = AudioFileIO.read(songFile) as AudioFile


                    } catch (@NonNull e: CannotReadException) {
                        Log.e(SongDetailDialog.TAG, "error while reading the song file", e)
                    } catch (@NonNull e: IOException) {
                        Log.e(SongDetailDialog.TAG, "error while reading the song file", e)
                    } catch (@NonNull e: TagException) {
                        Log.e(SongDetailDialog.TAG, "error while reading the song file", e)
                    } catch (@NonNull e: ReadOnlyFileException) {
                        Log.e(SongDetailDialog.TAG, "error while reading the song file", e)
                    } catch (@NonNull e: InvalidAudioFrameException) {
                        Log.e(SongDetailDialog.TAG, "error while reading the song file", e)
                    } finally {
                        if (!hasCover) {
                            SongGlideRequest.Builder.from(Glide.with(requireContext()), song)
                                .checkIgnoreMediaStore(requireContext())
                                .generatePalette(requireContext()).build()
                                .into(object : RetroMusicColoredTarget(albumCover) {
                                    override fun onColorReady(color: Int) {
                                        setColor(color)
                                    }
                                })
                        }
                    }
                }
            }

        }

        private fun setColor(color: Int) {
            this.color = color
            isColorReady = true
            if (colorReceiver != null) {
                colorReceiver!!.onColorReady(color, request)
                colorReceiver = null
            }
        }

        internal fun receiveColor(colorReceiver: ColorReceiver, request: Int) {
            if (isColorReady) {
                colorReceiver.onColorReady(color, request)
            } else {
                this.colorReceiver = colorReceiver
                this.request = request
            }
        }

        interface ColorReceiver {
            fun onColorReady(color: Int, request: Int)
        }

        companion object {

            private const val SONG_ARG = "song"

            fun newInstance(song: Song): AlbumCoverFragment {
                val frag = AlbumCoverFragment()
                val args = Bundle()
                args.putParcelable(SONG_ARG, song)
                frag.arguments = args
                return frag
            }
        }
    }

    companion object {
        val TAG: String = AlbumCoverPagerAdapter::class.java.simpleName
    }
}

