package com.fabriciofkt157.encoder;

import android.annotation.SuppressLint;
import android.view.MotionEvent;
import android.widget.ImageButton;
import android.widget.TextView;

public class Ferramentas {
    public static void limparBg(ImageButton[] imgBtns, TextView[] txtBtns, int color){
        if(txtBtns != null){
            for (TextView btn : txtBtns) {
                btn.setBackgroundResource(color);
            }
        }
        if(imgBtns != null){
            for (ImageButton btn : imgBtns) {
                btn.setBackgroundResource(color);
            }
        }
    }
    @SuppressLint("ClickableViewAccessibility")
    public static void botaoPressionado(ImageButton btn){
        btn.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    btn.setAlpha(0.5f);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    btn.setAlpha(1.0f);
                    break;
                default:
                    break;
            }
            return false;
        });
    }

}
