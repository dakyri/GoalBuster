package com.sb.gb;

import com.sb.MatchTime;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class TimeAndEventSlider extends View 
{

	private MatchTime currentGameT = new MatchTime();
	private MatchTime currentBetT = new MatchTime();
	
	public TimeAndEventSlider(Context context)
	{
		super(context);
		// TODO Auto-generated constructor stub
	}

	public TimeAndEventSlider(Context context, AttributeSet attrs) 
	{
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}

	public TimeAndEventSlider(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
		// TODO Auto-generated constructor stub
	}

	public void setGameTime(MatchTime time)
	{
		// TODO Auto-generated method stub
		
	}

	public void setBetTime(MatchTime currentBetTime)
	{
		// TODO Auto-generated method stub
		
	}

	public void addItem(int goalEvent, MatchTime matchTime, int teamId,
			String string, String string2)
	{
		// TODO Auto-generated method stub
		
	}
	
	public void clearFeed()
	{
		// TODO Auto-generated method stub
	}

	@Override
	protected void onDraw(Canvas c)
	{
		super.onDraw(c);
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent m)
	{
		return super.onTouchEvent(m);
	}
	

}
