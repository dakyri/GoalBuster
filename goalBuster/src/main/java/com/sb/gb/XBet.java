package com.sb.gb;

public class XBet
{
	public int matchId = 0;
	public int period = 0;
	public String teamName = "";
	public float wager = 0;
	public String currency = "GBP";
	public int minute = 0;
	public int teamId = 0;
	public int betSlotId = -1;
	public int timeSlotIdx = -1;

	public XBet()
	{
	}

	public String toString()
	{
		String minstr = Integer.toString(minute);
		if (minute < 10){
			minstr = "0" + minstr;
		}
		return /* formatCurrency(wager, currency, true, 2) */" on " + teamName + " at " +
			Integer.toString(period) + ":" + minstr;
	}

	public void setTo(int _matchId, int _teamId, String _teamName,
		int _period, int _minute, float _amount, String _currency)
	{
		matchId = _matchId;
		teamId = _teamId;
		teamName = _teamName;
		period = _period;
		minute = _minute;
		wager = _amount;
		currency = _currency;
	}
}
