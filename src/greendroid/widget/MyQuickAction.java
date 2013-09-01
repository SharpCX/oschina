package greendroid.widget;

import android.content.Context;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.drawable.Drawable;

public class MyQuickAction extends QuickAction {
    private static final ColorFilter BLACK_CF = new LightingColorFilter(Color.BLACK, Color.BLACK);

    public MyQuickAction(Context context, int drawableId, int titleId) {
        super(context, buildDrawable(context, drawableId), titleId);
    }
    
    public static Drawable buildDrawable(Context context, int drawableId) {
        Drawable drawable  = context.getResources().getDrawable(drawableId);
        drawable.setColorFilter(BLACK_CF);
        return drawable;
    }
}
