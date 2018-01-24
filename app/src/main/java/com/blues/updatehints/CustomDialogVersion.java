package com.blues.updatehints;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;

/**
 * 版本更新提示
 */
public class CustomDialogVersion extends Dialog {
    private Context mContext;
    private View contentView;
    private View closeDialog;

    public CustomDialogVersion(Context context, View contentView) {
        super(context, R.style.dialog);
        mContext = context;
        this.contentView = contentView;
    }

    public CustomDialogVersion(Context context, int themeResId) {
        super(context, R.style.dialog);
        mContext = context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(contentView);
        closeDialog = contentView.findViewById(R.id.close_dialog);

        if (closeDialog != null)
            closeDialog.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isShowing()) dismiss();
                }
            });
    }
}
