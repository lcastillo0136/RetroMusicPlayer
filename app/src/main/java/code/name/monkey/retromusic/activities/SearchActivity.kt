package code.name.monkey.retromusic.activities

import android.app.Activity
import android.app.Service
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.speech.RecognizerIntent
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.TextView.BufferType
import android.widget.Toast
import androidx.appcompat.widget.SearchView.OnQueryTextListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.TransitionManager
import code.name.monkey.appthemehelper.ThemeStore
import code.name.monkey.appthemehelper.util.ATHUtil
import code.name.monkey.appthemehelper.util.ColorUtil
import code.name.monkey.appthemehelper.util.MaterialValueHelper
import code.name.monkey.retromusic.App
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.activities.base.AbsMusicServiceActivity
import code.name.monkey.retromusic.adapter.SearchAdapter
import code.name.monkey.retromusic.model.Song
import code.name.monkey.retromusic.mvp.presenter.SearchPresenter
import code.name.monkey.retromusic.mvp.presenter.SearchView
import code.name.monkey.retromusic.util.RetroUtil
import com.google.android.material.textfield.TextInputEditText
import kotlinx.android.synthetic.main.activity_search.*
import java.util.*
import org.jaudiotagger.audio.AudioFile
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.id3.ID3v23Frame
import java.io.File
import javax.inject.Inject
import kotlin.reflect.typeOf
import kotlin.collections.ArrayList
import kotlin.collections.MutableList
import kotlin.collections.emptyList

class SearchActivity : AbsMusicServiceActivity(), OnQueryTextListener, TextWatcher, SearchView {
    @Inject
    lateinit var searchPresenter: SearchPresenter

    private var searchAdapter: SearchAdapter? = null
    private var query: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        setDrawUnderStatusBar()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)
        setStatusbarColorAuto()
        setNavigationbarColorAuto()
        setTaskDescriptionColorAuto()
        setLightNavigationBar(true)

        App.musicComponent.inject(this)
        searchPresenter.attachView(this)

        setupRecyclerView()
        setUpToolBar()
        setupSearchView()

        if (intent.getBooleanExtra(EXTRA_SHOW_MIC, false)) {
            startMicSearch()
        }

        back.setOnClickListener { onBackPressed() }
        voiceSearch.setOnClickListener { startMicSearch() }
        clearText.setOnClickListener { searchView.clearText() }
        searchContainer.backgroundTintList =
            ColorStateList.valueOf(ATHUtil.resolveColor(this, R.attr.colorSurface))

        keyboardPopup.setOnClickListener {
            val inputManager = getSystemService(Service.INPUT_METHOD_SERVICE) as InputMethodManager
            inputManager.showSoftInput(searchView, InputMethodManager.SHOW_IMPLICIT)
        }

        keyboardPopup.backgroundTintList = ColorStateList.valueOf(ThemeStore.accentColor(this))
        ColorStateList.valueOf(
            MaterialValueHelper.getPrimaryTextColor(
                this,
                ColorUtil.isColorLight(ThemeStore.accentColor(this))
            )
        ).apply {
            keyboardPopup.setTextColor(this)
            keyboardPopup.iconTint = this
        }
        if (savedInstanceState != null) {
            query = savedInstanceState.getString(QUERY)
        }
    }

    private fun setupRecyclerView() {
        searchAdapter = SearchAdapter(this, emptyList())
        searchAdapter?.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                super.onChanged()
                empty.visibility = if (searchAdapter!!.itemCount < 1) View.VISIBLE else View.GONE
            }
        })
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@SearchActivity)
            adapter = searchAdapter
        }
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy > 0) {
                    keyboardPopup.shrink()
                } else if (dy < 0) {
                    keyboardPopup.extend()
                }
            }
        })
    }

    private fun setupSearchView() {
        searchView.addTextChangedListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        searchPresenter.detachView()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(QUERY, query)
    }

    private fun setUpToolBar() {
        title = null
    }

    private fun search(query: String) {
        this.query = query
        TransitionManager.beginDelayedTransition(appBarLayout)
        voiceSearch.visibility = if (query.isNotEmpty()) View.GONE else View.VISIBLE
        clearText.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE
        searchPresenter.search(query)
    }

    override fun onMediaStoreChanged() {
        super.onMediaStoreChanged()
        query?.let { search(it) }
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        hideSoftKeyboard()
        return false
    }

    override fun onQueryTextChange(newText: String): Boolean {
        search(newText)
        return false
    }

    private fun hideSoftKeyboard() {
        RetroUtil.hideSoftKeyboard(this@SearchActivity)
        if (searchView != null) {
            searchView.clearFocus()
        }
    }

    override fun showEmptyView() {
        searchAdapter?.swapDataSet(ArrayList())
    }

    override fun showData(data: MutableList<Any>) {
//        val dataF = data.filter {
//            query?.let { it1 ->
//                if (it !is String && it is Song) {
//                    val audioFileqry = getAudioFile((it as Song).data)
//                    if(audioFileqry.tag != null) {
//                        audioFileqry.tag.getFields("TXXX").map { t ->
//                            var body: String = (t as ID3v23Frame).body.briefDescription
//                            body.substring(body.indexOf("Text=") + 5).replace("\"", "")
//                                .replace(";", "")
//                        }.joinToString().indexOf(it1)
//                    } else {
//                        -1
//                    }
//                } else {
//                    1
//                }
//            }!! > -1
//        }
        searchAdapter?.swapDataSet(data)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQ_CODE_SPEECH_INPUT -> {
                if (resultCode == Activity.RESULT_OK && null != data) {
                    val result: ArrayList<String>? =
                        data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                    query = result?.get(0)
                    searchView.setText(query, BufferType.EDITABLE)
                    searchPresenter.search(query!!)
                }
            }
        }
    }

    private fun startMicSearch() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.speech_prompt))
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT)
        } catch (e: ActivityNotFoundException) {
            e.printStackTrace()
            Toast.makeText(this, getString(R.string.speech_not_supported), Toast.LENGTH_SHORT)
                .show()
        }
    }

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
    }

    override fun onTextChanged(newText: CharSequence, start: Int, before: Int, count: Int) {
        search(newText.toString())
    }

    override fun afterTextChanged(s: Editable) {
    }

    companion object {
        val TAG: String = SearchActivity::class.java.simpleName

        const val EXTRA_SHOW_MIC = "extra_show_mic"
        const val QUERY: String = "query"

        private const val REQ_CODE_SPEECH_INPUT = 9002
    }

    private fun getAudioFile(path: String): AudioFile {
        return try {
            AudioFileIO.read(File(path))
        } catch (e: Exception) {
            AudioFile()
        }
    }
}

fun TextInputEditText.clearText() {
    text = null
}