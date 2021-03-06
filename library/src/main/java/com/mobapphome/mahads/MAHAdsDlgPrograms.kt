package com.mobapphome.mahads

/**
 * Created by settar on 7/12/16.
 */


import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.content.ContextCompat
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.PopupMenu
import android.util.Log
import android.view.*
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import android.widget.Toast
import com.google.gson.Gson
import com.mobapphome.mahads.commons.MAHDialogFragment
import com.mobapphome.mahads.commons.MAHFragmentExeption
import com.mobapphome.mahads.commons.*
import com.mobapphome.mahads.tools.*
import kotlinx.android.synthetic.main.mah_ads_dialog_programs.*
import java.util.*


class MAHAdsDlgPrograms(val items: MutableList<Any> = LinkedList<Any>(),
                        var mahRequestResult: MAHRequestResult? = null,
                        var urls: Urls? = null,
                        var fontName: String? = null,
                        var btnInfoVisibility: Boolean = false,
                        var btnInfoWithMenu: Boolean = false,
                        var btnInfoMenuItemTitle: String? = null,
                        var btnInfoActionURL: String? = null,
                        var dataHasAlreadySet: Boolean = false)
    : MAHDialogFragment() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.MAHAdsDlgPrograms)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        try {
            Log.i(Constants.LOG_TAG_MAH_ADS, "MAH Ads Programs Dlg Created ")

            val args = arguments
            val gson = Gson()
            mahRequestResult = gson.fromJson(args.getString("mahRequestResult"), MAHRequestResult::class.java)
            urls = gson.fromJson(args.getString("urls"), Urls::class.java)
            fontName = args.getString("fontName")
            btnInfoVisibility = args.getBoolean("btnInfoVisibility")
            btnInfoWithMenu = args.getBoolean("btnInfoWithMenu")
            btnInfoMenuItemTitle = args.getString("btnInfoMenuItemTitle")
            btnInfoActionURL = args.getString("btnInfoActionURL")

            dialog.window!!.attributes.windowAnimations = R.style.MAHAdsDialogAnimation
            dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            //getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
            dialog.setCanceledOnTouchOutside(false)
            dialog.setOnKeyListener { dialog, keyCode, event ->
                if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_BACK) {
                    onClose()
                }
                false
            }

            return inflater!!.inflate(R.layout.mah_ads_dialog_programs, container)
        } catch (e: MAHFragmentExeption) {
            Log.d(Constants.LOG_TAG_MAH_ADS, e.message, e)
            return null
        }
    }


    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnClose.setOnClickListener { sorrWithMAHExeption { onClose() } }
        btnErrorRefreshMAHAds.setOnClickListener { sorrWithMAHExeption { Updater.updateProgramList(activityMAH, urls!!) } }
        ivBtnCancel.setOnClickListener { sorrWithMAHExeption { onClose() } }
        ivBtnInfo.setOnClickListener { v ->
            sorrWithMAHExeption {
                if (btnInfoWithMenu) {
                    val itemIdForInfo = 1
                    val popup = PopupMenu(context, v)
                    popup.menu.add(Menu.NONE, itemIdForInfo, 1, btnInfoMenuItemTitle)

                    // registering popup with OnMenuItemClickListener
                    popup.setOnMenuItemClickListener { item ->
                        if (item.itemId == itemIdForInfo) {
                            showMAHlib()
                        }
                        true
                    }

                    popup.show()// showing popup menu
                } else {
                    showMAHlib()
                }
            }
        }
        ivBtnCancel.setColorFilter(ContextCompat.getColor(context, R.color.mah_ads_title_bar_text_color))
        ivBtnInfo.setColorFilter(ContextCompat.getColor(context, R.color.mah_ads_title_bar_text_color))

        if (btnInfoVisibility) ivBtnInfo.makeVisible() else ivBtnInfo.makeInvisible()

        ivLoading.setImageResource(R.drawable.ic_loading_mah)
        ivLoading.getDrawable()?.setColorFilter(ContextCompat.getColor(context, R.color.mah_ads_all_and_btn_text_color), PorterDuff.Mode.MULTIPLY);

        rvProgram.makeGone()
        lytErrorF1.makeGone()
        ivLoading.makeGone()


        startLoading()
        setUI(mahRequestResult, true)

        if (savedInstanceState == null) {
            //Call to update data from service or local
            Updater.updateProgramList(activityMAH, urls!!)
        }

        tvTitle.setFontTextView(fontName)
        tvErrorResultF1.setFontTextView(fontName)
        //btnErrorRefreshMAHAds.setFontTextView(fontName)
    }

    fun setUI(result: MAHRequestResult?, firstTime: Boolean) {
        Log.i(Constants.LOG_TAG_MAH_ADS, "------Result State is " + result?.resultState)

        if (result != null && (result.resultState === MAHRequestResult.ResultState.SUCCESS || result.resultState === MAHRequestResult.ResultState.ERR_SOME_ITEMS_HAS_JSON_SYNTAX_ERROR)) {
            dataHasAlreadySet = true


            items.clear()

            result.programsFiltered!!.forEach {
                items.add(it)
            }

            Log.i(Constants.LOG_TAG_MAH_ADS, "items count = ${items.size}")
            val adapterInit = ProgramItmAdpt(items, urls?.urlRootOnServer, fontName,
                    listenerOnClick = {
                        if (it is Program) {
                            val pckgName = it.uri.trim { it <= ' ' }
                            if (checkPackageIfExists(context, pckgName)) {
                                val pack = context.packageManager
                                val app = pack.getLaunchIntentForPackage(pckgName)
                                app.putExtra(Constants.MAH_ADS_INTERNAL_CALLED, true)
                                context.startActivity(app)
                            } else {
                                if (!pckgName.isEmpty()) {
                                    showMarket(context, pckgName)
                                }
                            }
                        }
                    },
                    listenerOnMoreClick = { item, v ->
                        if (item is Program) {
                            val pckgName = item.uri.trim { it <= ' ' }
                            val popup = PopupMenu(context, v)
                            // Inflating the Popup using xml file
                            popup.menuInflater.inflate(R.menu.program_popup_menu, popup.menu)
                            // registering popup with OnMenuItemClickListener
                            popup.setOnMenuItemClickListener {
                                if (it.itemId == R.id.popupMenuOpenOnGoogleP) {
                                    if (!pckgName.isEmpty()) {
                                        showMarket(context, pckgName)
                                    }
                                }
                                true
                            }
                            popup.show()// showing popup menu
                        }
                    })


            rvProgram.post {
                Log.i(Constants.LOG_TAG_MAH_ADS, "lstProgram post called")
                //lstProgram.layoutManager = GridLayoutManager(context, 1)
                rvProgram.layoutManager = LinearLayoutManager(context)
                rvProgram.itemAnimator = DefaultItemAnimator()


                rvProgram.adapter = adapterInit
                lytErrorF1.makeGone()
                rvProgram.makeVisible()
            }
        } else {
            if (result == null || result.isReadFromWeb) {
                rvProgram.post {
                    lytErrorF1.makeVisible()
                    rvProgram.makeGone()
                    tvErrorResultF1.text = resources.getString(
                            R.string.mah_ads_internet_update_error)
                }
            } else {
                if (!firstTime) {
                    rvProgram.post {
                        lytErrorF1.makeVisible()
                        rvProgram.makeGone()
                        tvErrorResultF1.text = resources.getString(
                                R.string.mah_ads_internet_update_error)
                    }
                }
            }
        }

        stopLoading()
    }

    fun startLoading() {
        if (dataHasAlreadySet) {
            return
        }
        val animationLoading = RotateAnimation(0.0f, 360.0f,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
                0.5f)

        animationLoading.duration = 350
        animationLoading.interpolator = LinearInterpolator()
        animationLoading.repeatCount = Animation.INFINITE

        ivLoading.startAnimation(animationLoading)
        ivLoading.makeVisible()
        rvProgram.makeGone()
        lytErrorF1.makeGone()

        Log.i(Constants.LOG_TAG_MAH_ADS, "Animation started")
    }

    fun stopLoading() {
        ivLoading.makeGone()
        rvProgram.makeGone()
        lytErrorF1.makeGone()

        ivLoading.clearAnimation()
        Log.i(Constants.LOG_TAG_MAH_ADS, "Animation stopped")
    }

    fun onClose() = dismissAllowingStateLoss()


    private fun showMAHlib() =
            try {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(btnInfoActionURL))
                context.startActivity(browserIntent)
            } catch (nfe: ActivityNotFoundException) {
                val str = "You haven't set correct url to btnInfoActionURL, your url = " + btnInfoActionURL
                Toast.makeText(context, str, Toast.LENGTH_LONG).show()
                Log.d(Constants.LOG_TAG_MAH_ADS, str, nfe)
            }


    companion object {

        fun newInstance(
                mahRequestResult: MAHRequestResult?,
                urls: Urls?,
                fontName: String?,
                btnInfoVisibility: Boolean,
                btnInfoWithMenu: Boolean,
                btnInfoMenuItemTitle: String,
                btnInfoActionURL: String): MAHAdsDlgPrograms {
            val dialog = MAHAdsDlgPrograms()
            val args = Bundle()
            val gson = Gson()
            args.putString("mahRequestResult", gson.toJson(mahRequestResult))
            args.putString("urls", gson.toJson(urls))
            args.putString("fontName", fontName)
            args.putBoolean("btnInfoVisibility", btnInfoVisibility)
            args.putBoolean("btnInfoWithMenu", btnInfoWithMenu)
            args.putString("btnInfoMenuItemTitle", btnInfoMenuItemTitle)
            args.putString("btnInfoActionURL", btnInfoActionURL)
            dialog.arguments = args
            return dialog
        }
    }
}// Empty constructor required for DialogFragment