/*
 * Copyright (C) 2010 Cyril Mottier (http://www.cyrilmottier.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package greendroid.widget;

import java.util.ArrayList;
import java.util.List;

import net.oschina.app.R;
import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.PopupWindow;

/**
 * Abstraction of a {@link QuickAction} wrapper. A QuickActionWidget is
 * displayed on top of the user interface (it overlaps all UI elements but the
 * notification bar). Clients may listen to user actions using a
 * {@link OnQuickActionClickListener} .
 * 
 * @author Benjamin Fellous
 * @author Cyril Mottier
 */
public abstract class QuickActionWidget extends PopupWindow {

	private static final int MEASURE_AND_LAYOUT_DONE = 1 << 1;

	private final int[] mLocation = new int[2];
	private final Rect mRect = new Rect();

	private int mPrivateFlags;

	private Context mContext;

	private View mAnchor;
	private boolean mIsMenuClick;

	private boolean mDismissOnClick;
	private int mArrowOffsetY;

	private int mPopupY;// popupwindow离屏幕顶部的距离
	private boolean mIsOnTop;// popupwindow显示在屏幕的上半部分还是下半部分

	private int mScreenHeight;
	private int mScreenWidth;
	private boolean mIsDirty;

	private OnQuickActionClickListener mOnQuickActionClickListener;
	private ArrayList<QuickAction> mQuickActions = new ArrayList<QuickAction>();

	public static interface OnQuickActionClickListener {

		void onQuickActionClicked(QuickActionWidget widget, int position);
	}

	/**
	 * Creates a new QuickActionWidget for the given context.
	 * 
	 * @param context
	 *            The context in which the QuickActionWidget is running in
	 */
	public QuickActionWidget(Context context) {
		super(context);

		mContext = context;

		initializeDefault();

		setFocusable(true);
		setTouchable(true);
		setOutsideTouchable(true);
		setWidth(LayoutParams.WRAP_CONTENT);
		setHeight(LayoutParams.WRAP_CONTENT);

		final WindowManager windowManager = (WindowManager) mContext
				.getSystemService(Context.WINDOW_SERVICE);
		//根据版本不同，使用不同版本的api
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
			Point point = new Point();
			windowManager.getDefaultDisplay().getSize(point);
			mScreenWidth = point.x;
			mScreenHeight = point.y;
		} else {
			mScreenWidth = windowManager.getDefaultDisplay().getWidth();
			mScreenHeight = windowManager.getDefaultDisplay().getHeight();
		}

	}

	/**
	 * Equivalent to {@link PopupWindow#setContentView(View)} but with a layout
	 * identifier.
	 * 
	 * @param layoutId
	 *            The layout identifier of the view to use.
	 */
	public void setContentView(int layoutId) {
		setContentView(LayoutInflater.from(mContext).inflate(layoutId, null));
	}

	private void initializeDefault() {
		mDismissOnClick = true;
		mArrowOffsetY = mContext.getResources().getDimensionPixelSize(
				R.dimen.gd_arrow_offset);
	}

	/**
	 * get QuickAction by position of collections
	 * 
	 * @param position
	 * @return
	 */
	public QuickAction getQuickAction(int position) {
		if (position < 0 || position >= mQuickActions.size())
			return null;
		return mQuickActions.get(position);
	}

	public int getArrowOffsetY() {
		return mArrowOffsetY;
	}

	public void setArrowOffsetY(int offsetY) {
		mArrowOffsetY = offsetY;
	}

	protected int getScreenWidth() {
		return mScreenWidth;
	}

	protected int getScreenHeight() {
		return mScreenHeight;
	}

	public void setDismissOnClick(boolean dismissOnClick) {
		mDismissOnClick = dismissOnClick;
	}

	public boolean getDismissOnClick() {
		return mDismissOnClick;
	}

	/**
	 * @param listener
	 */
	public void setOnQuickActionClickListener(
			OnQuickActionClickListener listener) {
		mOnQuickActionClickListener = listener;
	}

	public void addQuickAction(QuickAction action) {
		if (action != null) {
			mQuickActions.add(action);
			mIsDirty = true;
		}
	}

	/**
	 * Removes all {@link QuickAction} from this {@link QuickActionWidget}.
	 */
	public void clearAllQuickActions() {
		if (!mQuickActions.isEmpty()) {
			mQuickActions.clear();
			mIsDirty = true;
		}
	}

	/**
	 * Call that method to display the {@link QuickActionWidget} anchored to the
	 * given view.
	 * 
	 * @param anchor
	 *            The view the {@link QuickActionWidget} will be anchored to.
	 */
	public void show(View anchor) {

		final View contentView = getContentView();

		if (contentView == null) {
			throw new IllegalStateException(
					"You need to set the content view using the setContentView method");

		}

		// 设置触摸事件 - 修复触摸弹窗以外的地方无法隐藏弹窗
		contentView.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				final int x = (int) event.getX();
				final int y = (int) event.getY();

				if ((event.getAction() == MotionEvent.ACTION_DOWN)
						&& ((x < 0) || (x >= getWidth()) || (y < 0) || (y >= getHeight()))) {
					dismiss();
					return true;
				} else if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {

					dismiss();
					return true;
				} else {
					return contentView.onTouchEvent(event);
				}
			}
		});

		// Replaces the background of the popup with a cleared background
		// setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
		// 修复点击背景空白
		setBackgroundDrawable(null);

		final int[] loc = mLocation;
		anchor.getLocationOnScreen(loc);
		mRect.set(loc[0], loc[1], loc[0] + anchor.getWidth(),
				loc[1] + anchor.getHeight());

		if (mIsDirty) {
			clearQuickActions();
			populateQuickActions(mQuickActions);
		}

		onMeasureAndLayout(mRect, contentView);

		if ((mPrivateFlags & MEASURE_AND_LAYOUT_DONE) != MEASURE_AND_LAYOUT_DONE) {
			throw new IllegalStateException(
					"onMeasureAndLayout() did not set the widget specification by calling"
							+ " setWidgetSpecs()");
		}

		showArrow();
		prepareAnimationStyle();
		showAtLocation(anchor, Gravity.NO_GRAVITY, 0, mPopupY);
	}

	public void show(View anchor, boolean isMenuClick) {
		this.mIsMenuClick = isMenuClick;
		show(anchor);
	}

	protected void show() {
		if (mAnchor != null)
			show(mAnchor);
	}

	protected boolean isMenuClick() {
		return mIsMenuClick;
	}

	protected void setMenuClick(boolean isMenuClick) {
		this.mIsMenuClick = isMenuClick;
	}

	protected void clearQuickActions() {
		if (!mQuickActions.isEmpty()) {
			onClearQuickActions();
		}
	}

	protected void onClearQuickActions() {
	}

	protected abstract void populateQuickActions(List<QuickAction> quickActions);

	protected abstract void onMeasureAndLayout(Rect anchorRect, View contentView);

	protected void setWidgetSpecs(int popupY, boolean isOnTop) {
		mPopupY = popupY;
		mIsOnTop = isOnTop;

		mPrivateFlags |= MEASURE_AND_LAYOUT_DONE;
	}

	private void showArrow() {

		final View contentView = getContentView();
		final int arrowId = mIsOnTop ? R.id.gdi_arrow_down : R.id.gdi_arrow_up;
		final View arrow = contentView.findViewById(arrowId);
		final View arrowUp = contentView.findViewById(R.id.gdi_arrow_up);
		final View arrowDown = contentView.findViewById(R.id.gdi_arrow_down);

		if (arrowId == R.id.gdi_arrow_up) {
			arrowUp.setVisibility(View.VISIBLE);
			arrowDown.setVisibility(View.INVISIBLE);
		} else if (arrowId == R.id.gdi_arrow_down) {
			arrowUp.setVisibility(View.INVISIBLE);
			arrowDown.setVisibility(View.VISIBLE);
		}

		ViewGroup.MarginLayoutParams param = (ViewGroup.MarginLayoutParams) arrow
				.getLayoutParams();
		param.leftMargin = mRect.centerX() - (arrow.getMeasuredWidth()) / 2;
	}

	private void prepareAnimationStyle() {

		final int screenWidth = mScreenWidth;
		final boolean onTop = mIsOnTop;
		final int arrowPointX = mRect.centerX();

		if (arrowPointX <= screenWidth / 4) {
			setAnimationStyle(onTop ? R.style.GreenDroid_Animation_PopUp_Left
					: R.style.GreenDroid_Animation_PopDown_Left);
		} else if (arrowPointX >= 3 * screenWidth / 4) {
			setAnimationStyle(onTop ? R.style.GreenDroid_Animation_PopUp_Right
					: R.style.GreenDroid_Animation_PopDown_Right);
		} else {
			setAnimationStyle(onTop ? R.style.GreenDroid_Animation_PopUp_Center
					: R.style.GreenDroid_Animation_PopDown_Center);
		}
	}

	protected Context getContext() {
		return mContext;
	}

	protected OnQuickActionClickListener getOnQuickActionClickListener() {
		return mOnQuickActionClickListener;
	}
}
