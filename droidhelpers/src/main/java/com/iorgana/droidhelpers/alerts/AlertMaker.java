package com.iorgana.droidhelpers.alerts;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;

import com.iorgana.droidhelpers.R;

/**
 * ************************************************************************
 * AlertMaker
 * ************************************************************************
 * - Builder class for creating styled alert layouts.
 * - Supports multiple alert types (Primary, Secondary, Info, Success,
 *   Warning, Danger, HTML).
 */
public class AlertMaker {
    /**
     * ************************************************************************
     * OnCloseListener
     * ************************************************************************
     * - Callback interface for alert close events.
     */
    public interface OnCloseListener{
        void onClose();
    }

    /**
     * ************************************************************************
     * AlertType
     * ************************************************************************
     * - Enum defining the available alert style types.
     */
    public enum AlertType {
        Primary, Secondary, Info, Success, Warning, Danger, HTML, DEFAULT
    }

    private final Context context;
    private OnCloseListener onCloseListener;
    private AlertType alertType = AlertType.Primary;
    private String title;
    private String content;

    // deprecated html under maintenance
    private String html;

    // By default, alert is not cancelable
    private boolean isCancelable = false;
    private String lastError;

    // Alert Props
    private int marginTop=0;
    private int marginBottom=0;
    private int marginStart=0;
    private int marginEnd=0;



    /**
     * ************************************************************************
     * AlertMaker (Constructor)
     * ************************************************************************
     * - Create a new AlertMaker instance.
     * ------------------------------------------------------------------------
     * @param context Any valid context.
     */
    public AlertMaker(Context context){
        this.context = context;
    }

    /**
     * ************************************************************************
     * setType()
     * ************************************************************************
     * - Set the alert type style.
     * ------------------------------------------------------------------------
     * @param alertType The AlertType to apply.
     * @return This AlertMaker instance for chaining.
     */
    public AlertMaker setType(AlertType alertType) {
        this.alertType = alertType;
        return this;
    }

    /**
     * ************************************************************************
     * setTitle()
     * ************************************************************************
     * - Set the alert title text.
     * ------------------------------------------------------------------------
     * @param title The title string.
     * @return This AlertMaker instance for chaining.
     */
    public AlertMaker setTitle(String title) {
        this.title = title;
        return this;
    }

    /**
     * ************************************************************************
     * setContent()
     * ************************************************************************
     * - Set the alert content text.
     * ------------------------------------------------------------------------
     * @param content The content string.
     * @return This AlertMaker instance for chaining.
     */
    public AlertMaker setContent(String content) {
        this.content = content;
        return this;
    }

    /**
     * ************************************************************************
     * setHtml()
     * ************************************************************************
     * - Set HTML content for the alert.
     * ------------------------------------------------------------------------
     * @param html The HTML string.
     * @return This AlertMaker instance for chaining.
     * @deprecated HTML alerts are under maintenance and not functional.
     */
    public AlertMaker setHtml(String html) {
        this.html = html;
        return this;
    }

    /**
     * ************************************************************************
     * setCancelable()
     * ************************************************************************
     * - Set whether the alert is cancelable (can be dismissed).
     * ------------------------------------------------------------------------
     * @param cancelable true to allow dismissal, false otherwise.
     * @return This AlertMaker instance for chaining.
     */
    public AlertMaker setCancelable(boolean cancelable) {
        this.isCancelable = cancelable;
        return this;
    }

    /**
     * ************************************************************************
     * setMargins()
     * ************************************************************************
     * - Set all four margins for the alert layout.
     * ------------------------------------------------------------------------
     * @param marginTop    The top margin in pixels.
     * @param marginBottom The bottom margin in pixels.
     * @param marginStart  The start margin in pixels.
     * @param marginEnd    The end margin in pixels.
     * @return This AlertMaker instance for chaining.
     */
    public AlertMaker setMargins(int marginTop, int marginBottom, int marginStart, int marginEnd){
        this.marginTop = marginTop;
        this.marginBottom = marginBottom;
        this.marginStart = marginStart;
        this.marginEnd = marginEnd;
        return this;
    }

    /**
     * ************************************************************************
     * setMarginTop()
     * ************************************************************************
     * - Set the top margin for the alert layout.
     * ------------------------------------------------------------------------
     * @param marginTop The top margin in pixels.
     * @return This AlertMaker instance for chaining.
     */
    public AlertMaker setMarginTop(int marginTop){
        this.marginTop = marginTop;
        return this;
    }
    /**
     * ************************************************************************
     * setMarginBottom()
     * ************************************************************************
     * - Set the bottom margin for the alert layout.
     * ------------------------------------------------------------------------
     * @param marginBottom The bottom margin in pixels.
     * @return This AlertMaker instance for chaining.
     */
    public AlertMaker setMarginBottom(int marginBottom){
        this.marginBottom = marginBottom;
        return this;
    }

    /**
     * ************************************************************************
     * setListener()
     * ************************************************************************
     * - Set the close listener for the alert.
     * ------------------------------------------------------------------------
     * @param onCloseListener The OnCloseListener callback.
     * @return This AlertMaker instance for chaining.
     */
    public AlertMaker setListener(OnCloseListener onCloseListener){
        this.onCloseListener = onCloseListener;
        return this;
    }

    /**
     * ************************************************************************
     * getLastError()
     * ************************************************************************
     * - Get the last error message that occurred.
     * ------------------------------------------------------------------------
     * @return The last error string, or null.
     */
    public String getLastError(){
        return this.lastError;
    }

    /**
     * ************************************************************************
     * build()
     * ************************************************************************
     * - Build and return the alert layout.
     * - If AlertType is HTML and HTML content is set, tries HTML layout.
     * - Otherwise, builds a normal styled layout.
     * ------------------------------------------------------------------------
     * @return The constructed LinearLayout for the alert.
     */
    public LinearLayout build(){

        // [-] Check if we should build HTML
        // @// TODO: 7/25/2026 html under maintenance
//        if(alertType== AlertType.HTML){
//           LinearLayout resultLayout = buildHtmlLayout();
//           if(resultLayout!=null){
//               return resultLayout;
//           }
//        }

        // [-] Else, build normal layout
        return buildNormalLayout();
    }


    /**
     * ************************************************************************
     * buildHtmlLayout()
     * ************************************************************************
     * - Build an HTML-based alert layout.
     * ------------------------------------------------------------------------
     * @return The HTML LinearLayout, or null if HTML content is not set.
     * @deprecated This method is not working in this release. HtmlFormatter
     *             was removed because it had bugs. Will be replaced in the future.
     */
    private LinearLayout buildHtmlLayout(){
        if(html!=null && !html.isEmpty() && alertType== AlertType.HTML){
            LinearLayout htmlLayout = (LinearLayout) LayoutInflater.from(context).inflate(R.layout.layout_alert_html, null);
            TextView textView = htmlLayout.findViewById(R.id.txtViewHtml);
            ImageButton closeBtn = htmlLayout.findViewById(R.id.closeBtn);
            LinearLayout closeBtnContainer = htmlLayout.findViewById(R.id.closeBtnContainer);


            // Set layout margins
            LinearLayout.LayoutParams layoutParamsNew = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
            layoutParamsNew.setMarginEnd(marginEnd);
            layoutParamsNew.setMarginStart(marginStart);
            layoutParamsNew.topMargin = marginTop;
            layoutParamsNew.bottomMargin = marginBottom;
            htmlLayout.setLayoutParams(layoutParamsNew);

            // Layout Background
            Drawable alertBackground = ResourcesCompat.getDrawable(context.getResources(), R.drawable.shape_alert_default, null);
            int textColor = context.getResources().getColor(R.color.color_default);
            htmlLayout.setBackground(alertBackground);
            textView.setTextColor(textColor);

            // Handle Close
            if(this.isCancelable){
                closeBtnContainer.setVisibility(View.VISIBLE);
                closeBtn.setOnClickListener(view-> htmlLayout.setVisibility(View.GONE));
            }else{
                LinearLayout closeParent = (LinearLayout) closeBtn.getParent();
                closeParent.setVisibility(View.GONE);
            }

            closeBtn.setOnClickListener(v->{
                htmlLayout.removeAllViews();
                htmlLayout.setVisibility(View.GONE);
                if(onCloseListener!=null) onCloseListener.onClose();
            });

            // Insert html in layout
            // @ TODO: 7/25/2026 HtmlFormatter removed from the lib because it had some bugs. we'll see if we can replace it with something else or new HtmlFormatter
            //   Spanned formattedHtml = HtmlFormatter.formatHtml(new HtmlFormatterBuilder().setHtml(html).setImageGetter(new HtmlResImageGetter(context)));
            //   textView.setText(formattedHtml);


            // Return layout
            return htmlLayout;
        }
        return null;
    }


    /**
     * ************************************************************************
     * buildNormalLayout()
     * ************************************************************************
     * - Build a styled alert layout with the selected AlertType.
     * ------------------------------------------------------------------------
     * @return The constructed LinearLayout.
     */
    private LinearLayout buildNormalLayout(){
        // Get Layout Views
        LinearLayout alertLayout = (LinearLayout) LayoutInflater.from(context).inflate(R.layout.layout_alert, null);
        TextView alertTitle = alertLayout.findViewById(R.id.alertTitle);
        TextView alertContent = alertLayout.findViewById(R.id.alertContent);
        ImageButton closeBtn = alertLayout.findViewById(R.id.closeBtn);
        LinearLayout closeBtnContainer = alertLayout.findViewById(R.id.closeBtnContainer);

        if(title==null){
            alertTitle.setVisibility(View.GONE);
        }else{
            alertTitle.setVisibility(View.VISIBLE);
        }

        if(content==null){
            alertContent.setVisibility(View.GONE);
        }else{
            alertContent.setVisibility(View.VISIBLE);
        }

        // Get Alert Styles
        Drawable alertBackground;
        int textColor;

        // Set layout style
        switch (this.alertType){
            case Secondary:
                alertBackground = ResourcesCompat.getDrawable(context.getResources(), R.drawable.shape_alert_secondary, null);
                textColor = context.getResources().getColor(R.color.color_secondary);
                break;
            case Info:
                alertBackground = ResourcesCompat.getDrawable(context.getResources(), R.drawable.shape_alert_info, null);
                textColor = context.getResources().getColor(R.color.color_info);
                break;
            case Success:
                alertBackground = ResourcesCompat.getDrawable(context.getResources(), R.drawable.shape_alert_success, null);
                textColor = context.getResources().getColor(R.color.color_success);
                break;
            case Warning:
                alertBackground = ResourcesCompat.getDrawable(context.getResources(), R.drawable.shape_alert_warning, null);
                textColor = context.getResources().getColor(R.color.color_warning);
                break;
            case Danger:
                alertBackground = ResourcesCompat.getDrawable(context.getResources(), R.drawable.shape_alert_danger, null);
                textColor = context.getResources().getColor(R.color.color_danger);
                break;
            case Primary:
                alertBackground = ResourcesCompat.getDrawable(context.getResources(), R.drawable.shape_alert_primary, null);
                textColor = context.getResources().getColor(R.color.color_primary);
                break;
            default:
                alertBackground = ResourcesCompat.getDrawable(context.getResources(), R.drawable.shape_alert_default, null);
                textColor = context.getResources().getColor(R.color.color_default);
                break;

        }

        // Set Alert Style
        alertLayout.setBackground(alertBackground);
        alertTitle.setTextColor(textColor);
        alertContent.setTextColor(textColor);

        // Set layout margins
        LinearLayout.LayoutParams layoutParamsNew = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        layoutParamsNew.setMarginEnd(marginEnd);
        layoutParamsNew.setMarginStart(marginStart);
        layoutParamsNew.topMargin = marginTop;
        layoutParamsNew.bottomMargin = marginBottom;

        alertLayout.setLayoutParams(layoutParamsNew);



        // Set Alert Data
        if(this.title!=null && !this.title.isEmpty()){
            alertTitle.setText(this.title);
        }else{
            alertTitle.setVisibility(View.GONE);
        }
        if(this.content!=null && !this.content.isEmpty()){
            alertContent.setText(this.content);
        }else{
            alertContent.setVisibility(View.GONE);
        }

        if(this.isCancelable){
            closeBtnContainer.setVisibility(View.VISIBLE);
            closeBtn.setOnClickListener(view-> alertLayout.setVisibility(View.GONE));
        }else{
            LinearLayout closeParent = (LinearLayout) closeBtn.getParent();
            closeParent.setVisibility(View.GONE);
        }

        closeBtn.setOnClickListener(v->{
            alertLayout.removeAllViews();
            alertLayout.setVisibility(View.GONE);
            if(onCloseListener!=null) onCloseListener.onClose();
        });

        return alertLayout;
    }
}