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

import com.iorgana.droidhelpers.htmltextview.HtmlFormatter;
import com.iorgana.droidhelpers.htmltextview.HtmlFormatterBuilder;
import com.iorgana.droidhelpers.htmltextview.HtmlResImageGetter;

public class AlertMaker {
    public interface OnCloseListener{
        void onClose();
    }
    public enum AlertType {
        Primary, Secondary, Info, Success, Warning, Danger, DEFAULT, HTML
    }

    private final Context context;
    private OnCloseListener onCloseListener;
    private AlertType alertType = AlertType.Primary;
    private String title;
    private String content;
    private String html;

    // By default, alert is not cancelable
    private boolean isCancelable = false;
    private String lastError;

    // Alert Props
    private int marginTop=0;
    private int marginBottom=0;
    private int marginStart=0;
    private int marginEnd=0;



    public AlertMaker(Context context){
        this.context = context;
    }

    /**
     * Set Alert AlertType
     */
    public AlertMaker setType(AlertType alertType) {
        this.alertType = alertType;
        return this;
    }

    /**
     * Set Label
     */
    public AlertMaker setTitle(String title) {
        this.title = title;
        return this;
    }

    /**
     * Set Content
     */
    public AlertMaker setContent(String content) {
        this.content = content;
        return this;
    }

    /**
     * Set Html
     */
    public AlertMaker setHtml(String html) {
        this.html = html;
        return this;
    }

    /**
     * Set Is Cancelable
     */
    public AlertMaker setCancelable(boolean cancelable) {
        this.isCancelable = cancelable;
        return this;
    }

    /**
     * Set Alert Margins
     */
    public AlertMaker setMargins(int marginTop, int marginBottom, int marginStart, int marginEnd){
        this.marginTop = marginTop;
        this.marginBottom = marginBottom;
        this.marginStart = marginStart;
        this.marginEnd = marginEnd;
        return this;
    }

    /**
     * Set Margin Top
     */
    public AlertMaker setMarginTop(int marginTop){
        this.marginTop = marginTop;
        return this;
    }
    /**
     * Set Margin Bottom
     */
    public AlertMaker setMarginBottom(int marginBottom){
        this.marginBottom = marginBottom;
        return this;
    }

    /**
     * Set Close Listener
     */
    public AlertMaker setListener(OnCloseListener onCloseListener){
        this.onCloseListener = onCloseListener;
        return this;
    }

    /**
     * Get Last Error
     */
    public String getLastError(){
        return this.lastError;
    }

    /**
     * Show Alert
     */
    public LinearLayout build(){

        // [-] Check if we should build HTML
        if(alertType== AlertType.HTML){
           LinearLayout resultLayout = buildHtmlLayout();
           if(resultLayout!=null){
               return resultLayout;
           }
        }

        // [-] Else, build normal layout
        return buildNormalLayout();
    }


    /**
     * Build Html Layout
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
            Spanned formattedHtml = HtmlFormatter.formatHtml(new HtmlFormatterBuilder().setHtml(html).setImageGetter(new HtmlResImageGetter(context)));
            textView.setText(formattedHtml);


            // Return layout
            return htmlLayout;
        }
        return null;
    }


    /**
     * Build Normal Layout
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