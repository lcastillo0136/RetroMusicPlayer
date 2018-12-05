package code.name.monkey.retromusic.dialogs

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import code.name.monkey.appthemehelper.ThemeStore
import code.name.monkey.retromusic.Constants.USER_PROFILE
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.ui.activities.MainActivity
import code.name.monkey.retromusic.util.Compressor
import code.name.monkey.retromusic.util.NavigationUtil
import code.name.monkey.retromusic.util.PreferenceUtil
import code.name.monkey.retromusic.views.RoundedBottomSheetDialogFragment
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_main_options.*
import java.io.File
import java.util.*

class MainOptionsBottomSheetDialogFragment : RoundedBottomSheetDialogFragment(), View.OnClickListener {

    private val disposable = CompositeDisposable()

    private val timeOfTheDay: String
        get() {
            var message = getString(R.string.title_good_day)
            val c = Calendar.getInstance()
            val timeOfDay = c.get(Calendar.HOUR_OF_DAY)

            when (timeOfDay) {
                in 0..5 -> message = getString(R.string.title_good_night)
                in 6..11 -> message = getString(R.string.title_good_morning)
                in 12..15 -> message = getString(R.string.title_good_afternoon)
                in 16..19 -> message = getString(R.string.title_good_evening)
                in 20..23 -> message = getString(R.string.title_good_night)
            }
            return message
        }

    override fun onDestroyView() {
        super.onDestroyView()
        disposable.clear()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_main_options, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        text!!.setTextColor(ThemeStore.textColorSecondary(context!!))
        titleWelcome!!.setTextColor(ThemeStore.textColorPrimary(context!!))
        titleWelcome!!.text = String.format("%s %s!", timeOfTheDay, PreferenceUtil.getInstance().userName)
        loadImageFromStorage()

        actionSettings.setOnClickListener(this)
        actionAbout.setOnClickListener(this)
        actionSleepTimer.setOnClickListener(this)
        userInfoContainer.setOnClickListener(this)
        actionRate.setOnClickListener(this)
    }


    override fun onClick(view: View) {
        val mainActivity = activity as MainActivity? ?: return
        when (view.id) {
            R.id.actionSettings -> NavigationUtil.goToSettings(mainActivity)
            R.id.actionAbout -> NavigationUtil.goToAbout(mainActivity)
            R.id.actionSleepTimer -> if (fragmentManager != null) {
                SleepTimerDialog().show(fragmentManager!!, TAG)
            }
            R.id.userInfoContainer -> NavigationUtil.goToUserInfo(activity!!)
            R.id.actionRate -> NavigationUtil.goToPlayStore(mainActivity)
        }
        dismiss()
    }

    private fun loadImageFromStorage() {

        disposable.add(Compressor(context!!)
                .setMaxHeight(300)
                .setMaxWidth(300)
                .setQuality(75)
                .setCompressFormat(Bitmap.CompressFormat.WEBP)
                .compressToBitmapAsFlowable(
                        File(PreferenceUtil.getInstance().profileImage, USER_PROFILE))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ userImage!!.setImageBitmap(it) }, {
                    userImage!!.setImageDrawable(ContextCompat
                            .getDrawable(context!!, R.drawable.ic_person_flat))
                }, {

                }))
    }

    companion object {

        private const val TAG: String = "MainOptionsBottomSheetD"

        fun newInstance(selected_id: Int): MainOptionsBottomSheetDialogFragment {
            val bundle = Bundle()
            bundle.putInt("selected_id", selected_id)
            val fragment = MainOptionsBottomSheetDialogFragment()
            fragment.arguments = bundle
            return fragment
        }

        fun newInstance(): MainOptionsBottomSheetDialogFragment {
            return MainOptionsBottomSheetDialogFragment()
        }
    }
}
