package com.sb.gb;

import java.util.Date;

import com.sb.MatchTime;
import com.sb.Prediction;

public class PredictionGoal extends Prediction
{
    public int matchId;
	public int teamId;
	public int period;
	public int minute;
	
	public int timeSlotIdx;
	
	public int betSlotId;
	
	public float jackpot;
	
	public PredictionGoal()
	{
		this(-1,-1,0, 0, 0, "FRE");
	}
	
	public PredictionGoal(int _matchId, int _teamId, int _period, int _minute,
			float _bet, String _currency)
	{
		this(_matchId, _teamId, _period, _minute, _bet, _currency, "", "U", 0);
	}

	public PredictionGoal(int _matchId, int _teamId, int _period, int _minute,
			float _bet, String _currency,
			String _timestamp, String _result, float _jackp)
	{
		super(0, _bet, _currency);
//		super("G", _bet, _currency, _timestamp, _result, _jackp);
		matchId = _matchId;
		teamId = _teamId;
		period = _period;
		minute = _minute;
	}
	
	public MatchTime time()
	{
		return new MatchTime(period, minute, 0);
	}
	
	@Override
	public String toString() 
	{
	    return "[" + "Match " + Integer.toString(matchId) 
	    		+ ", " + "Team " + Integer.toString(teamId) + ", " 
	    		+ "Time " + Integer.toString(period) + ":" 
	    		+ Integer.toString(minute) + ", " + "]=>" + result;
	}

	@Override
	public String ticketInputString() {
		// TODO Auto-generated method stub
		return Integer.toString(teamId) + ","
			+ Integer.toString(matchId) + ","
			+ Integer.toString(minute) + ","
			+ Integer.toString(period) /*+ ","*/
			/*+ type*/;
	}
	
	@Override
	public Prediction clone()
	{
/*		PredictionGoal p = new PredictionGoal(
				matchId, teamId, period, minute,
				bet, currency,
				timestamp, result, jackpot);*/
		PredictionGoal p = new PredictionGoal(
				matchId, teamId, period, minute,
				bet, currency);
//		p.ticket = ticket;
		return p;
	}

	
	@Override
	public Prediction fromTicketInput(int _pmId, String _ticketId, float _bet,
			String _currency, Date _timestamp, String _result, String selStr,
			Object... extras) {
		// TODO Auto-generated method stub
		String[] selArr = selStr.split(",");
		if (selArr.length != 5) {
			return null;
		}
		PredictionGoal pg = new PredictionGoal(
				Integer.parseInt(selArr[1]),
				Integer.parseInt(selArr[0]),
				Integer.parseInt(selArr[2]),
				Integer.parseInt(selArr[3]),
//				selArr[4], // this is now hard wired for this prediction subclass
				_bet,
				_ticketId);
		return pg;
	}
}
