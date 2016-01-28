/**
 * 
 */
package com.sb.gb;

import com.sb.widget.GameClock;
import com.sb.MatchTime;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

/**
 * @author dak
 *
 */
public class DigitalGameClock extends View implements GameClock {

	/**
	 * @param context
	 */
	public DigitalGameClock(Context context)
	{
		super(context);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param context
	 * @param attrs
	 */
	public DigitalGameClock(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param context
	 * @param attrs
	 * @param defStyle
	 */
	public DigitalGameClock(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see com.sb.GameClock#setGameTime(com.sb.MatchTime)
	 */
	public void setGameTime(MatchTime t)
	{
		// TODO Auto-generated method stub

	}

}
