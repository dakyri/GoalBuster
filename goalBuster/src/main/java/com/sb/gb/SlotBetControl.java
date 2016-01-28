package com.sb.gb;

import com.larvalabs.svgandroid.SVG;
import com.larvalabs.svgandroid.SVGParser;
import com.sb.MatchTime;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Picture;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PictureDrawable;
import android.graphics.drawable.ScaleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.Button;

public class SlotBetControl extends Button
{
	public static final int NORMAL=1;
	public static final int LOST=2;
	public static final int WON=3;
	public static final int PENDING=4;
	public static final int FINISHED=5;
	public static final int SELECTED=6;
	
	private static final int[] WON_STATE_SET = {
        R.attr.state_won
	};
	private static final int[] LOST_STATE_SET = {
        R.attr.state_lost
	};
	private static final int[] PENDING_STATE_SET = {
        R.attr.state_pending
	};
	
	private static final int[] FINISHED_STATE_SET = {
        R.attr.state_finished
	};
	
	private static final int[] SELECTED_STATE_SET = {
        R.attr.state_selected
	};
	
	private int currentState;
	
	public String labelNumber;
	public String teamName = "";
	public int teamId = 0;
	public int matchId = 0;
	public int timeMin=0;
	public int timeHalf=1;
	
	protected StateListDrawable states = null;
	PictureDrawable svgCup = null;
	PictureDrawable svgOffside = null;
	PictureDrawable svgQuestion = null;
	Bitmap bmpCup = null;
	Bitmap bmpOffside = null;
	Bitmap bmpQuestion = null;
	
	public SlotBetControl(Context context)
	{
		super(context);
		setCurrentState(NORMAL);
		setSelector();
	}

	public SlotBetControl(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		processAttributes(attrs);
		setCurrentState(NORMAL);
		setSelector();
	}

	public SlotBetControl(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
		processAttributes(attrs);
		setCurrentState(NORMAL);
		setSelector();
	}
	
	public MatchTime time()
	{
		return new MatchTime(timeHalf, timeMin, 0);
		
	}

	public void setSelector()
	{
		/*
		SVG svg = SVGParser.getSVGFromResource(getResources(), R.raw.cup);
		svgCup = svg.createPictureDrawable();
		svg = SVGParser.getSVGFromResource(getResources(), R.raw.offside);
		svgOffside = svg.createPictureDrawable();
		svg = SVGParser.getSVGFromResource(getResources(), R.raw.question);
		svgQuestion = svg.createPictureDrawable();
		*/
		
		bmpCup = BitmapFactory.decodeResource(getResources(), R.drawable.cup);
		bmpOffside = BitmapFactory.decodeResource(getResources(), R.drawable.offside);
		bmpQuestion = BitmapFactory.decodeResource(getResources(), R.drawable.question);
		
		states = new StateListDrawable();
		states.addState(LOST_STATE_SET, getResources().getDrawable(R.drawable.grey_button_background_pressed_blue));
		states.addState(WON_STATE_SET, getResources().getDrawable(R.drawable.grey_button_background_pressed_blue));
		states.addState(PENDING_STATE_SET, getResources().getDrawable(R.drawable.grey_button_background_pressed_blue));
		states.addState(FINISHED_STATE_SET, getResources().getDrawable(R.drawable.grey_button_background_pressed_blue));
		states.addState(SELECTED_STATE_SET, getResources().getDrawable(R.drawable.grey_button_background_pressed_blue));
		states.addState(new int[] {android.R.attr.state_focused, -android.R.attr.state_pressed}, getResources().getDrawable(R.drawable.grey_button_background_focus_blue));
		states.addState(new int[] {android.R.attr.state_pressed}, getResources().getDrawable(R.drawable.grey_button_background_pressed_blue));
		states.addState(EMPTY_STATE_SET, getResources().getDrawable(R.drawable.grey_button_background_normal));
		setBackgroundDrawable(states);
//		setBackgroundResource(R.drawable.sbc_selector);
	}
	
	public void processAttributes(AttributeSet attrs)
	{
		if (attrs != null) {
			int ats = attrs.getAttributeIntValue("http://schemas.android.com/apk/res/com.sb.gb", "slotNumber", 0);
			if (ats == 0) {
			} else {
				setText(Integer.toString(ats));
			}
		}
	}
	
	public void setFinished(boolean en)
	{
		if (en) {
			if (currentState == WON || currentState == LOST) {
			} else {
				currentState = FINISHED;
			}
		} else {
			if (currentState == PENDING) {
			} else {
				currentState = NORMAL;
			}
		}
		setupStateGraphics();
	}

	public int getCurrentState()
	{
		return currentState;
	}
	
	public void setCurrentState(int newState)
	{
		if (newState < 1 || newState > 6) {
			newState = NORMAL;
		}
//		Log.d("set state button", "was "+Integer.toString(currentState)+" to "+Integer.toString(newState));
		
		if (currentState != newState) {
			if (currentState == WON || currentState == LOST) {
				currentState = newState;
			} else if (currentState != PENDING) {
				if (currentState != SELECTED) {
//					if (enabled) {
						currentState = newState;
//					}else {
//						currentState = newState;
//					}
				} else {
					currentState = newState;
				}
			} else if (!(newState == WON) && !(newState == LOST)) {
				currentState = newState;
			} else {
				currentState = newState;
			}
		} else {
			currentState = newState;
		}
		Log.d("set state button", "now "+Integer.toString(currentState)+" on "+(getText()!=null?getText():""));
		setupStateGraphics();
		return;
	}

	private void setupStateGraphics()
	{
		drawableStateChanged();
	}
	
	@Override
	protected int[] onCreateDrawableState(int extraSpace)
	{
	    final int[] drawableState = super.onCreateDrawableState(extraSpace + 5);
	    if (currentState == WON){
	        mergeDrawableStates(drawableState, WON_STATE_SET);
	    }
	    if (currentState == LOST){
	        mergeDrawableStates(drawableState, LOST_STATE_SET);
	    }
	    if (currentState == PENDING){
	        mergeDrawableStates(drawableState, PENDING_STATE_SET);
	    }
	    if (currentState == SELECTED){
	        mergeDrawableStates(drawableState, SELECTED_STATE_SET);
	    }
	    if (currentState == FINISHED){
	        mergeDrawableStates(drawableState, FINISHED_STATE_SET);
	    }
	    return drawableState;
	}
	
	protected Rect bmpRect(Bitmap p)
	{
		Rect r = new Rect(0, 0, getWidth(), getHeight());
		float h = getHeight();
		float w = getWidth();
		if (p == null) {
			return r;
		}
		float pw = p.getWidth();
		float ph = p.getHeight();
		if (pw == 0 || ph == 0) {
			return r;
		}
		if ((w/pw) > (h/ph)) {
			float rw = pw *(h/ph);
			r.left = (int) ((w-rw)/2);
			r.right = r.left + (int)rw;
		} else {
			float rh = ph*(w/pw);
			r.top = (int) ((h-rh)/2);
			r.bottom = r.top + (int)rh;
		}
//		Log.d("rect", r.left+","+r.top+","+r.right+","+r.bottom);
		return r;
	}
	
	protected Rect pictRect(Picture p)
	{
		Rect r = new Rect(0, 0, getWidth(), getHeight());
		float h = getHeight();
		float w = getWidth();
		if (p == null) {
			return r;
		}
		float pw = p.getWidth();
		float ph = p.getHeight();
		if (pw == 0 || ph == 0) {
			return r;
		}
		if ((w/pw) > (h/ph)) {
			float rw = pw *(h/ph);
			r.left = (int) ((w-rw)/2);
			r.right = r.left + (int)rw;
		} else {
			float rh = ph*(w/pw);
			r.top = (int) ((h-rh)/2);
			r.bottom = r.top + (int)rh;
		}
//		Log.d("rect", r.left+","+r.top+","+r.right+","+r.bottom);
		return r;
	}
	
	@Override
	public void draw(Canvas canvas)
	{
//		Log.d("drawing "+(getText()!=null?getText():""), "state"+Integer.toString(currentState));
		switch (currentState) {
		case PENDING: {
			/*
			if (svgQuestion != null) {
				Picture p = svgQuestion.getPicture();
				canvas.drawPicture(p, pictRect(p));
				return;
			}
			*/
			if (bmpQuestion != null) {
				canvas.drawBitmap(bmpQuestion, null, bmpRect(bmpQuestion), null);
				return;
			}
			break;
		}

		case LOST:  {
			/*
			if (svgOffside != null) {
				Picture p = svgOffside.getPicture();
				canvas.drawPicture(p, pictRect(p));
				return;
			}*/
			if (bmpOffside != null) {
				canvas.drawBitmap(bmpOffside, null, bmpRect(bmpOffside), null);
				return;
			}
			break;
		}
		case WON: {
			/*
			if (svgCup != null) {
				Picture p = svgCup.getPicture();
				canvas.drawPicture(p, pictRect(p));
				return;
			}*/
			if (bmpCup != null) {
				canvas.drawBitmap(bmpCup, null, bmpRect(bmpCup), null);
				return;
			}
			break;
		}
		default:
		case SELECTED: {
			super.draw(canvas);
			break;
		}
		}
		
	}

	public void makeSelection(Boolean sel)
	{
		switch (currentState) {
			case FINISHED:
			case WON:
			case LOST:
			case PENDING: {
				break;
			}
			default: {
				if (sel) {
					currentState = SELECTED;
//					gb.addBetToSelection(_wagerAmount, _wagerCurrency, _matchId, _teamId, _teamName, _betSlotId, _timeSlot, _timeHalf, _timeMin);
				} else {
					currentState = NORMAL;
//					gb.removeBetFromSelection(_matchId, _teamId, _teamName, _betSlotId, _timeSlot, _timeHalf, _timeMin);
				}
				break;
			}
		}
		return;
	}

	
}
