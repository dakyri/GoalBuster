package com.sb.gb;

import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import com.larvalabs.svgandroid.SVG;
import com.larvalabs.svgandroid.SVGParser;
import com.sb.widget.EventViewerList;
import com.sb.FeedEntry;
import com.sb.GameServerConnection;
import com.sb.Goal;
import com.sb.Jackpot;
import com.sb.Match;
import com.sb.MatchState;
import com.sb.MatchStatus;
import com.sb.MatchTime;
import com.sb.MatchTimerState;
import com.sb.Prediction;
import com.sb.SBTime;
import com.sb.widget.TeamInfoView;
import com.sb.gb.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Animation.AnimationListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class GoalBusterActivity extends Activity implements AnimationListener
{
	protected final String versionString="AndroidSportsBaster/GoalBaster 0.1.0";
	
	public int liveBetAdvanceFactor=60;
	public int clockAdvanceFactor=15;
	public Boolean liveBetAdvanceMinimum=true;
	
	public PredictionGoal currentBet=new PredictionGoal();
//	public Bet[] selectedBets=new Array();
	
	protected Boolean waitingForTheHalfHack=false;
	protected Boolean bettingSelectedSlots=false;
	protected Boolean testMode=true;
	public Boolean freePlayMode=false;
	protected int selectedFutureMatchId=-1;
	protected String gameConfig="1";
	public int biaModeMatchId=0;
	
	public float stakeMultiple=2;
	public float stakeMinimum=2;
	public float stakeMaximum=99;

	public int potUpdateRate=300;
	public int clockUpdateRate=60;
	public int statusTimeout=20;
	public int matchStatusUpdateRate=300;

	protected GameServerConnection server = null;
	protected boolean initialConfigRecieved = false;
	protected boolean initialMatchListRecieved = false;
	
	protected View viewStatusBar;
	protected TextView viewStatus;
	protected TextView viewAccount;
	protected ProgressBar viewProgress;
	protected Animation animSlideIn;
	protected Animation animSlideOut;
	protected EventViewerList dataFeedPanel = null;
	protected TimeAndEventSlider eventSlider = null;
	protected TeamInfoView team1Details = null;
	protected TeamInfoView team2Details = null;
	protected DigitalGameClock gameClock = null;
	
	public Match currentMatch = null;
	public MatchTime currentBetTime=new MatchTime();
	public MatchTime time=new MatchTime();
	public Boolean predictionCheckLooper=false;
	
	public boolean gotFinishedStatus=false;
	protected boolean liveViewEnabled=true;
	protected int matchStatusUpdateTimerDelay=0;

	
	public static final int BET_INTERVAL_LEN_MINS=5;
	
	protected int[] sbcIds = {
			R.id.sbc1,
			R.id.sbc2,
			R.id.sbc3,
			R.id.sbc4,
			R.id.sbc5,
			R.id.sbc6,
			R.id.sbc7,
			R.id.sbc8,
			R.id.sbc9,
			R.id.sbc10,
			R.id.sbc11,
			R.id.sbc12,
			R.id.sbc13,
			R.id.sbc14,
			R.id.sbc15,
			R.id.sbc16,
			R.id.sbc17,
			R.id.sbc18,
			R.id.sbc19,
			R.id.sbc20,
			R.id.sbc21,
			R.id.sbc22,
			R.id.sbc23,
			R.id.sbc24,
			R.id.sbc25,
			R.id.sbc26,
			R.id.sbc27,
			R.id.sbc28,
			R.id.sbc29,
			R.id.sbc30,
			R.id.sbc31,
			R.id.sbc32,
			R.id.sbc33,
			R.id.sbc34,
			R.id.sbc35,
			R.id.sbc36,
			R.id.sbc37,
			R.id.sbc38,
			R.id.sbc39,
			R.id.sbc40
	};
	protected SlotBetControl [] slotControls = null;
	protected SlotBetControl [] team1BetControls = null;
	protected SlotBetControl [] team2BetControls = null;
	
	protected Timer lookForLiveTimer = null;
	protected Timer clockUpdateTimer = null;
	protected Timer initialConnectionTimer = null;
	protected Timer statusTimeoutTimer = null;
	protected Timer potUpdateTimer = null;
	protected Timer matchStatusUpdateTimer = null;
	protected Timer postResultMatchListUpdateTimer = null;
	public boolean timerStopped=false;
	
	protected ArrayList<Prediction> selectedBets=null;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		Log.d("log log log", "GoalBuster::onCreate()");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		dataFeedPanel = new EventViewerList(this);
		eventSlider = new TimeAndEventSlider(this);
		gameClock = new DigitalGameClock(this);
		team2Details = new TeamInfoView(this);
		team1Details = new TeamInfoView(this);
		
		animSlideIn = AnimationUtils.loadAnimation(this, R.anim.slide_in);
		animSlideOut = AnimationUtils.loadAnimation(this, R.anim.slide_out);
		viewStatusBar = findViewById(R.id.status_bar);
		viewProgress = (ProgressBar) findViewById(R.id.progress);
		viewStatus = (TextView) findViewById(R.id.statusView);
		viewAccount = (TextView) findViewById(R.id.accountView);
		
		slotControls = new SlotBetControl[sbcIds.length];
		team1BetControls = new SlotBetControl[sbcIds.length/2];
		team2BetControls = new SlotBetControl[sbcIds.length/2];
		
		for (int i=0; i<sbcIds.length; i++) {
			slotControls[i] = (SlotBetControl)findViewById(sbcIds[i]);
		}
		for (int i=0; i<sbcIds.length/2; i++) {
			team1BetControls[i] = slotControls[2*i];
			team2BetControls[i] = slotControls[2*i+1];
		}
		slotControls[0].setCurrentState(SlotBetControl.LOST);
		slotControls[1].setCurrentState(SlotBetControl.WON);
		slotControls[2].setCurrentState(SlotBetControl.PENDING);
		
	   // set up server connection
		server = new GameServerConnection(
			this, gameServerHandler,
			R.string.template_user_agent,
			R.string.url_gameapi,
			-1,
			freePlayMode,
			clockAdvanceFactor
		);
		showConnecting("Initial Connection ...");
		initialConfigRecieved = false;
		server.getGameProperties("1");
	}

	/**
	 * The activity is about to become visible.
	 */
	@Override
	protected void onStart()
	{
		super.onStart();
	}
	
	/** 
	 * The activity has become visible (it is now "resumed").
	 */
   @Override
   protected void onResume()
   {
		super.onResume();
   }
	
   /**
	*  Another activity is taking focus (this activity is about to be "paused").
	*/
	@Override
	protected void onPause()
	{
		super.onPause();
	}
	
	/**
	 * The activity is no longer visible (it is now "stopped")
	 */
	@Override
	protected void onStop()
	{
		super.onStop();
	}
	
	/**
	 * The activity is about to be destroyed.
	 */
	@Override
	protected void onDestroy()
	{
		super.onDestroy();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId()) {
			case R.id.gb_select_match: {
				showSelect();
				return true;
			}
			case R.id.gb_help: {
				showHelp();
				return true;
			}
			case R.id.gb_about: {
				showAbout();
				return true;
			}
			case R.id.gb_preferences: {
				showPreferences();
				return true;
			}
			default: {
				Log.d("option", Integer.toString(item.getItemId()));
			}
		}
		return false;
	}


/***************************************
 * ANIMATION LISTENER IMPLEMENTATIONS
 ***************************************/
	public void onAnimationEnd(Animation animation)
	{
 //   	if (animation == animSlideIn) {
			viewProgress.setVisibility(View.VISIBLE);
 //   	}
	}

	public void onAnimationRepeat(Animation animation)
	{
	}

	public void onAnimationStart(Animation animation)
	{
	}
	
/***************************************
 * SERVER RESULT HANDLERS
 ***************************************/
	protected Handler gameServerHandler = new Handler()
	{
		public void handleMessage(Message msg)
		{
			switch (msg.what) {
			case GameServerConnection.SERVER_ACCOUNT_EVENT: {
				showAccountBalance();
				break;
			}
			case GameServerConnection.SERVER_CONFIG_EVENT: {
				if (!initialConfigRecieved) {
					showStatus("Recieved initial configuration");
					onInitialConfig();
				}
				hideConnecting();
				break;
			}
			case GameServerConnection.SERVER_UPDATE_JACKPOT_EVENT: {
				//trace("got jackpot update");
				//gameResultsPanel.updateJackpots(server.jackpotList());
				break;
			}
			case GameServerConnection.SERVER_5R_JACKPOT_EVENT: {
				//trace("got 5r jackpot update");
				//gameResultsPanel.updateJackpots(server.jackpotList());
				break;
			}
			case GameServerConnection.SERVER_PLAY_TICKET_EVENT: {
				onPlayTicketResponse();
				break;
			}
			case GameServerConnection.SERVER_LIST_PREDICTIONS_EVENT: {
				onListPredictionsReceived();
				break;
			}
			case GameServerConnection.SERVER_LAST_PAYOUT_EVENT: {
				//trace("got last payout");
				//gameResultsPanel.updateJackpots(server.jackpotList());
				break;
			}
			case GameServerConnection.SERVER_LIST_MATCHES_EVENT: {
				showStatus("Matches Recieved");
				if (!initialMatchListRecieved) {
					onInitialMatchListRecieved();
				} else {
					onMatchListRecieved();
				}
				hideConnecting();
				break;
			}
			case GameServerConnection.SERVER_MATCH_RESULT_EVENT: {
				onMatchResultRecieved();
				break;
			}
			case GameServerConnection.SERVER_MATCH_STATUS_EVENT: {
				onMatchStatusRecieved();
				break;
			}
			case GameServerConnection.SERVER_MATCH_POT_EVENT: {
				/*
				if (server.matchPotId == gameResultsPanel.matchID) {
					gameResultsPanel.updatePot(server.matchPot, null);
				*/
				break;
			}
			case GameServerConnection.SERVER_ERROR_EVENT: {
				onServerError();
				break;
			}
			default: {
				showError(668, "Unknown message from server!");
			}
			}
		}

	};

	protected void onServerError()
	{
		MatchTime bett=null;
		int slno=0;
		if (initialConnectionTimer != null) {
			initialConnectionTimer.cancel();
			initialConnectionTimer = null;
		}
		if (server.lastErrorCode == GameServerConnection.BET_IN_THE_PAST &&
				(currentBet != null) && !bettingSelectedSlots) {
			bett = new MatchTime(currentBetTime);
			if (bett.equals(currentBet.time())) {
				bett.min++;
			}
			slno = time2SlotIdx(bett);
			if (slno == currentBet.timeSlotIdx) {
				++slno;
				if (slno >= 10) {
					slno = -1;
					bett = null;
				} else {
					bett = slotIdx2Time(bett.half, slno);
					currentBet.timeSlotIdx = slno;
					currentBet.minute = bett.min;
				}
			}
			if (slno >= 0 && !(bett == null)) {
				reBet(currentBet.betSlotId + 2, bett, currentBet.time());
			}else {
				showError(server.lastErrorCode, server.lastErrorMessage);
			}
		}else {
			bettingSelectedSlots = false;
			/*
			if (server.matchesXML == null || server.propertiesXML == null) {
				liveBetEnable(false);
//				futureBetEnable(false);
//				showFutureBetPanel();
				showError(server.lastErrorCode, server.lastErrorMessage, 1);
			}else {
				showError(server.lastErrorCode, server.lastErrorMessage, 0);
			}*/
		}
		return;
	}
	
	protected void onPlayTicketResponse()
	{
//		trace("processed ticket, updating");
		if (bettingSelectedSlots) {
			if (playNextSelected()) {
				return;
			}
		}
		hideConnecting();
		if ((selectedBets != null) && selectedBets.size() > 0) {
			clearSelection();
		}
		if (server.currentPrediction == null) {
//			trace("null prediction processing ticket");
			return;
		}
//		trace("got prediction, ticket response event for ticket ", server.currentPrediction.ticket, "at", m.name, m.location);
		server.getPlayerAccount(freePlayMode);
		livePredictionListUpdated(server.currentPredictionList);
		currentBet = null;
		return;
	}

	protected void onMatchListRecieved()
	{

//		trace("got a new match list");
		if (server.liveMatchList() == null || server.liveMatchList().size() == 0) {
			if (!gotFinishedStatus) {
				liveBetEnable(false);
				showFutureBetPanel();
				setNextMatchStartCheck();
			}
		}else {
			liveBetEnable(true);
			/*
			if (currentMatchId() < 0 || server.findMatch(currentMatchId()) == null) {
				if (!gotFinishedStatus) {
					setMatch(server.liveMatch(0));
				}
			}*/
		}
		if (server.nextMatchList() == null || server.nextMatchList().size() == 0) {
			futureBetEnable(false);
			selectedFutureMatchId = -1;
			showLiveBetPanel();
		}
	}

	private void futureBetEnable(boolean b) {
		// TODO Auto-generated method stub
		
	}

	private void showLiveBetPanel()
	{
		if (liveViewEnabled) {
		// TODO Auto-generated method stub
		}
		
	}

	private void showFutureBetPanel() {
		// TODO Auto-generated method stub
		
	}

	protected void onMatchResultRecieved()
	{
		Jackpot j=null;
		Match m=getMatch(currentMatchId());
		if (m == null) {
			return;
		}
		m.setLiveStatus(-1);
		ArrayList<Jackpot> ja=new ArrayList<Jackpot>();
		for (Jackpot jo: server.matchResultJackpots) {
			if (jo.processed) {
				j = server.jackpot4Id(jo.id);
				if (j != null) {
					jo.name = j.name;
					jo.currency = j.currency;
					ja.add(jo);
				}
			}
		}
/*
		fireworks(
				m, server.matchResultStartTime, server.matchResultWinnings,
				server.matchResultEstimate,
				freePlayMode ? m.status.freePot : m.status.pot,
				server.playerCurrency, server.finalResults(), ja);
*/
		return;
	}

	protected void onInitialConfig()
	{
		showConnecting("Fetching Matches and Account ...");
		server.getPlayerAccount(freePlayMode);
		server.getMatches();
		stakeMinimum = server.minimumBet;
		stakeMaximum = server.maximumBet;
		stakeMultiple = server.minimumBet;
		initialConfigRecieved = true;
	}
	
	protected void onInitialMatchListRecieved()
	{
		if (initialConnectionTimer != null) {
			initialConnectionTimer.cancel();
			initialConnectionTimer = null;
		}
		/*
		if (biaModeMatchId > 0) {
			trace("in biaMode", biaModeMatchId);
			if (server.reqMatchList() == null || server.reqMatchList().length == 0) {
				futureBetEnable(false);
				trace("no next matches disabling tab");
				if (server.liveMatchList() == null || server.liveMatchList().length == 0) {
					liveBetEnable(false);
					trace("no live matches disabling tab");
				}
			} else {
				selectedFutureMatchId = server.reqMatch(0).matchId;
			}
			liveBetEnable(false);
			hideConnectingView();
			showFutureBetPanel();
		}else {
		}*/
		if (server.nextMatchList() == null || server.nextMatchList().size() == 0) {
			//futureBetEnable(false);
		}else {
			selectedFutureMatchId = server.nextMatch(0).matchId;
		}
		if (server.liveMatchList() == null || server.liveMatchList().size() == 0) {
			liveBetEnable(false);
			setNextMatchStartCheck();
			hideConnecting();
//			showFutureBetPanel();
		} else {
			setMatch(server.liveMatch(0));
			liveBetEnable(true);
			hideConnecting();
//			showLiveBetPanel();
		}
		initialMatchListRecieved = true;
	}

	private void onListPredictionsReceived()
	{
//		trace("got a new prediction list ... checking");
		if (server.listPredictionResultsMatch == null) {
			return;
		}
		Match m=server.listPredictionResultsMatch;
		checkPredictionsFor(m);
		if (m.matchId == currentMatchId()) {
			livePredictionListUpdated(m.predictions);
		}
		return;
	}

	private void onMatchStatusRecieved()
	{
		Match m=null;
		int fuseT=0;
		stopMatchStatusTimeoutTimer();
		MatchStatus ms=server.matchStatus;
		if (ms == null) {
//			trace("match status event is null");
			return;
		}
//		trace("match status time", ms.time.toShortString(), "score", ms.t1Score, ms.t2Score, "timestamp", ms.timestamp, "current time", ServerConnection.getTimeStamp());
		if (ms.status.equals(MatchState.ABANDONED) || ms.status.equals(MatchState.CANCELLED) || ms.status.equals(MatchState.DELETED)) {
			ms.time.setTo(1, 0, 0);
		} else if (ms.status.equals(MatchState.OPEN)) {
			ms.time.setTo(1, 0, 0);
		} else if (ms.status.equals(MatchState.FINISHED)) {
			m = getMatch(ms.id);
			if (m != null) {
				checkPredictionsFor(m);
				if (m.matchId == currentMatchId()) {
					livePredictionListUpdated(m.predictions);
				}
			}
		} else if (ms.status.equals(MatchState.INTERVAL)) {
				if (ms.time.inAddedTime()) {
					ms.time.min = 0;
					ms.time.half++;
				}
		} else if (ms.status.equals(MatchState.PAUSED)) {
		} else if (ms.status.equals(MatchState.PLAYING)) {
		} else {
//				trace("unhandled match state", ms.status);
		}
//		trace("match status adjusted time to", ms.time.toShortString());
		int mtState=updateLiveMatchStatus(
				ms.id, ms.time,
				ms.t1Score, ms.t2Score, ms.status,
				ms.estimate, ms.goalList());
//		trace("prediction check in match status", predictionCheckLooper);
		if (!predictionCheckLooper) {
			livePanelPredictionCheck();
		}
		if (mtState != MatchTimerState.RUNNING) {
			if (mtState != MatchTimerState.PAUSED) {
				if (mtState != MatchTimerState.FINISHED) {
//					trace("stopping all timers");
					if (clockUpdateTimer != null) {
						clockUpdateTimer.cancel();
						clockUpdateTimer = null;
					}
				} else {
//					trace("stopping timer, and requesting new match list");
					if (clockUpdateTimer != null) {
						clockUpdateTimer.cancel();
						clockUpdateTimer = null;
					}
					
				//	server.getMatchResults(ms.id, new PredictionGoal(), server.playerID);
					startPostResultsMatchListUpdateTimer();
					server.getPlayerAccount(freePlayMode);
				}
			} else {
//				trace("starting match status update timer at 60s");
				startMatchStatusUpdateTimer(60);
			}
		} else {
//			trace("starting timer update at 60s and match status at 300s");
			startClockUpdateTimer(60 - ms.time.sec);
			if (!waitingForTheHalfHack) {
				fuseT = ms.time.min * 60 + ms.time.sec;
				fuseT = fuseT % matchStatusUpdateRate;
				fuseT = matchStatusUpdateRate - fuseT;
				if (fuseT <= 0) {
					fuseT = matchStatusUpdateRate;
				}
//				trace("timer update in", 60 - ms.time.sec, "secs", "match in", fuseT, "secs");
				startMatchStatusUpdateTimer(fuseT);
			}
		}

	}
	
/*********************************************
 * UTILITIES AND WRAPPERS
 *********************************************/
	protected void liveBetEnable(boolean b)
	{
		// TODO Auto-generated method stub
		
	}

	public void setTime(MatchTime gT, MatchTime bT)
	{
		if (gT == null) {
			time = new MatchTime(0, 0, 0);
		}else {
			time = gT;
		}
		if (bT == null) {
			currentBetTime = new MatchTime(0, 0, 0);
		}else {
			currentBetTime = bT;
		}
		showCurrentTime();
		return;
	}

	protected int currentMatchId()
	{
		if (currentMatch != null) return currentMatch.matchId;
		return -1;
	}

	public Match getMatch(int id)
	{
		if (server == null) {
			return null;
		}
		return null; //server.findMatch(id);
	}
	
	public static MatchTime slotIdx2Time(int hf, int slno)
	{
		if (slno > 9) {
			slno = 9;
		}
		return new MatchTime(hf, slno * 5, 0);
	}

	public static int time2SlotIdx(MatchTime t)
	{
		int slno=(int) Math.floor(t.min / 5);
		if (slno > 9) {
			slno = 9;
		}
		return slno;
	}

	protected SlotBetControl betControlFor(PredictionGoal p)
	{
		if (currentMatch == null) {
			return null;
		}
		if (p.matchId != currentMatch.matchId) {
			return null;
		}
		SlotBetControl[] sbca=null;
		if (p.teamId != currentMatch.homeTeam.teamId) {
			if (p.teamId == currentMatch.awayTeam.teamId) {
				sbca = team2BetControls;
			}
		}else {
			sbca = team1BetControls;
		}
		for (SlotBetControl sbc: sbca) {
			if (sbc.timeHalf == p.period) {
				if ((sbc.timeMin <= p.minute) && 
					  (p.minute < sbc.timeMin + BET_INTERVAL_LEN_MINS || 
						 sbc.timeMin == 45 && p.minute >= 45)) {
					return sbc;
				}
			}
		}
		return null;
	}

	
	public String formatCurrency(float val, String currency, boolean useSymbol, int decimals)
	{
		String valStr;
		if (decimals > 0) {
			valStr = String.format("%.2f", val);
		} else {
			valStr = Integer.toString((int) Math.floor(val));
		}		
		String showCurrency = "";
		
		if (currency != null) {
			showCurrency = currency;
		}
		if (showCurrency.equals("")) {
			return valStr;
		}
		if (useSymbol) {
			if (showCurrency.equals("GBP")) {
				return '£'+valStr;
			}
			if (showCurrency.equals("EUR")) {
				return '€'+valStr;
			}
			if (showCurrency.equals("USD")) {
				return "US$"+valStr;
			}
		}
		return valStr+' '+showCurrency;
	}
	
/********************************
 * TIMERS 
 ********************************/
	protected void startPotUpdateTimer()
	{
		stopPotUpdateTimer();
		potUpdateTimer = new Timer();
		potUpdateTimer.schedule(
			new TimerTask() {
				public void run()
				{
					if (server != null && currentMatchId() != -1) {
						server.getMatchPotUpdate(currentMatchId());
					}
				}
			}, potUpdateRate * 1000);
		return;
	}

	protected void stopPotUpdateTimer()
	{
		if (this.potUpdateTimer != null) {
			potUpdateTimer.cancel();
			potUpdateTimer = null;
		}
		return;
	}

	public void startClockUpdateTimer(final int udsecs)
	{
		stopClockUpdateTimer();
		clockUpdateTimer = new Timer();
		clockUpdateTimer.schedule(
			new TimerTask() {
				protected int tickCount=0;
				public void run()
				{
					tickCount++;
					if (tickCount > ((udsecs > clockUpdateRate)?clockUpdateRate:udsecs)) {
						stopClockUpdateTimer();
						clockUpdateTimerComplete();
					} else {
						updateTimerTick(false);
					}
				}
			}, 1000, 1000);
		return;
	}

	public void updateTimerTick(boolean isMinTick)
	{
//		trace("LiveBetPanel::updateTimerTick()", isMinTick, timerStopped);
		if (!timerStopped) {
			if (isMinTick) {
				time.min++;
				time.sec = 0;
			}else {
				time.incS(1);
			}
			if (time.sec == 0) {
				currentBetTime = adjustedBetTime(time, MatchState.PLAYING);
			}
			showCurrentTime();
		}
		return;
	}
	
	protected void clockUpdateTimerComplete()
	{
		if (!predictionCheckLooper) {
			livePanelPredictionCheck();
		}
		Match m=getMatch(currentMatchId());
		if (m == null) {
			return;
		}
		MatchStatus ms= (m != null? m.status : null);
		if (!(ms == null) && ms.time.plusS(60).inAddedTime()) {
			startMatchStatusUpdateTimer(60);
			waitingForTheHalfHack = true;
		}else {
			waitingForTheHalfHack = false;
		}
		startClockUpdateTimer(clockUpdateRate);
		return;
	}
	
	public void stopClockUpdateTimer()
	{
		if (clockUpdateTimer != null) {
			clockUpdateTimer.cancel();
			clockUpdateTimer = null;
		}
		return;
	}

	protected void startMatchStatusTimeoutTimer()
	{
		stopMatchStatusTimeoutTimer();
		statusTimeoutTimer = new Timer();
		statusTimeoutTimer.schedule(
			new TimerTask() {
				public void run()
				{
					if (currentMatchId() > 0) {
						server.getMatchStatus(currentMatchId());
						startMatchStatusTimeoutTimer();
					}
				}

			}, statusTimeout * 1000);
	}

	protected void startPostResultsMatchListUpdateTimer()
	{
		if (postResultMatchListUpdateTimer == null) {
			postResultMatchListUpdateTimer = new Timer();
			postResultMatchListUpdateTimer.schedule(
				new TimerTask() {
					public void run()
					{
						gotFinishedStatus = false;
						postResultMatchListUpdateTimer.cancel();
						postResultMatchListUpdateTimer = null;
						server.getMatches(biaModeMatchId);
						server.updateJackpots();
					}

				}, 1000 * 60 * 2);
		}
		return;
	}
	
	protected void stopMatchStatusTimeoutTimer()
	{
		if (statusTimeoutTimer != null) {
			statusTimeoutTimer.cancel();
			statusTimeoutTimer = null;
		}
		return;
	}
	
	public void startMatchStatusUpdateTimer(int tupds)
	{
		if (tupds == 0) {
			if (matchStatusUpdateTimer != null) {
				matchStatusUpdateTimer.cancel();
				matchStatusUpdateTimer = null;
			}
			return;
		}
		if (matchStatusUpdateTimer != null) {
			if (matchStatusUpdateTimerDelay == tupds * 1000) {
				return;
			}
			matchStatusUpdateTimer.cancel();
			matchStatusUpdateTimer = null;
		}
		matchStatusUpdateTimer = new Timer();
		matchStatusUpdateTimerDelay = tupds * 1000;
		matchStatusUpdateTimer.schedule(
			new TimerTask() {
				public void run()
				{
					if (currentMatchId() > 0) {
						startMatchStatusUpdateTimer(matchStatusUpdateRate);
						server.getMatchStatus(currentMatchId());
						startMatchStatusTimeoutTimer();
					}
				}
			},
			matchStatusUpdateTimerDelay, matchStatusUpdateTimerDelay);
		return;
	}

	protected void setNextMatchStartCheck()
	{
		ArrayList<Match> lma=server.liveMatchList();
		ArrayList<Match> nma=server.nextMatchList();
		if ((lma == null) || lma.size() == 0) {
			liveBetEnable(false);
		}
		if (nma == null || nma.size() == 0) {
		}
		Match m=nma.get(0);
		long nmms=0; //SBTime.dateTimeMillis(m.start);
		long nowms=new Date().getTime();
		long wms=nmms - nowms;
		wms = wms / 1000;
		if (wms < 60) {
			wms = 60;
		}
		if (wms > 24 * 60 * 60) {
			wms = 24 * 60 * 60;
		}
		
		if (lookForLiveTimer != null) {
			lookForLiveTimer.cancel();
			lookForLiveTimer = null;
		}
		lookForLiveTimer = new Timer();
		lookForLiveTimer.schedule(
			new TimerTask() {
				public void run()
				{
					if (server != null) {
						server.getMatches();
					}
				}
			}, wms * 1000);
		
		return;
	}

/*******************************************
 * WRAPPERS AND MAIN PROCESSING ROUTINES
 *******************************************/
	public void setMatchDetails(Match m)
	{
		currentMatch = m;
		if (currentMatch == null) {
			team1Details.setTo("", 0, "");
			team2Details.setTo("", 0, "");
//			eventSlider.setTeamIDs(-1, -1);
//			eventSlider.clearFeed();
			setTime(new MatchTime(0, 0, 0), new MatchTime(0, 0, 0));
			for (SlotBetControl sbc : team1BetControls) {
				sbc.teamName = "";
				sbc.teamId = -1;
				sbc.matchId = -1;
			}
			for (SlotBetControl sbc : team2BetControls) {
				sbc.teamName = "";
				sbc.teamId = -1;
				sbc.matchId = -1;
			}
		}else {
			team1Details.setTo(currentMatch.homeTeam.name, currentMatch.homeTeam.homeColor, currentMatch.homeTeam.icon);
			team2Details.setTo(currentMatch.awayTeam.name, currentMatch.awayTeam.awayColor, currentMatch.awayTeam.icon);
//			eventSlider.setTeamIDs(currentMatch.homeTeam.teamId, currentMatch.awayTeam.teamId);
//			eventSlider.clearFeed();
//			eventSlider.addFeed(currentMatch.feed);
			if (currentMatch.status == null) {
				setTime(new MatchTime(0, 0, 0), new MatchTime(0, 0, 0));
			}else {
				setTime(currentMatch.status.time, adjustedBetTime(currentMatch.status.time, currentMatch.status.status));
			}
			if (currentMatch.homeTeam == null) {
				for (SlotBetControl sbc : team1BetControls) {
					sbc.teamName = "";
					sbc.teamId = -1;
					sbc.matchId = -1;
				}
			} else {
				for (SlotBetControl sbc : team1BetControls) {
					sbc.teamName = currentMatch.homeTeam.name;
					sbc.teamId = currentMatch.homeTeam.teamId;
					sbc.matchId = currentMatch.matchId;
				}
			}
			if (currentMatch.awayTeam == null) {
				for (SlotBetControl sbc : team2BetControls) {
					sbc.teamName = "";
					sbc.teamId = -1;
					sbc.matchId = -1;
				}
			}else {
				for (SlotBetControl sbc : team2BetControls) {
					sbc.teamName = currentMatch.awayTeam.name;
					sbc.teamId = currentMatch.awayTeam.teamId;
					sbc.matchId = currentMatch.matchId;
				}
			}
			livePredictionListUpdated(currentMatch.predictions);
		}
		return;
	}

	public MatchTime adjustedBetTime(MatchTime gT, String state)
	{
		MatchTime abT=null;
		if (state.equals(MatchState.INTERVAL) || state.equals(MatchState.OPEN)) {
				abT = gT;
//				trace("adjusted ", state, " bet time is unadjusted", abT.toString());
		} else if (state.equals(MatchState.ABANDONED) || state.equals(MatchState.CANCELLED)
					|| state.equals(MatchState.DELETED) || state.equals(MatchState.FINISHED)) {
				abT = new MatchTime(4, 45, 0);
//				trace("adjusted ", state, " bet time is set to wrongness", abT.toString());
		} else {
			abT = new MatchTime(gT);
			abT.incS(liveBetAdvanceFactor);
			if (liveBetAdvanceMinimum) {
				if (abT.sec > 0) {
					abT.sec = 0;
					abT.min++;
				}
			} else {
				abT.sec = 0;
			}
			if (abT.min > 45 || abT.half > 2 && abT.min > 15) {
				abT.min = 0;
				abT.sec = 0;
				abT.half++;
			}else {
				abT.min = (int) (5 * Math.floor((abT.min + 5) / 5));
				abT.sec = 0;
//				trace("setting to normal advance playing", (abT.min + 5) / 5, Math.floor((abT.min + 5) / 5));
			}
//			trace("adjusted ", state, " bet time for", gT.toString(), "is set to bet t", abT.toString());
		}
		return abT;
	}

	public boolean clearSelection()
	{
		if (selectedBets == null) {
			selectedBets = new ArrayList<Prediction>();
		}
		while (selectedBets.size() > 0) {
			selectedBets.remove(0);
		}
		livePanelDisableBetSelection();
		livePanelClearSlotSelections();
		return false;
	}

	private void livePanelClearSlotSelections()
	{
		// TODO Auto-generated method stub
		
	}

	private void livePanelDisableBetSelection()
	{
		// TODO Auto-generated method stub
		
	}

	protected boolean playNextSelected()
	{
		if (selectedBets.size() <= 0) {
			livePanelDisableBetSelection();
			return false;
		}
		currentBet = (PredictionGoal) selectedBets.remove(0);
		if (currentBet == null) {
			return false;
		}
		Match m = getMatch(currentBet.matchId);
		if (m != null) {
			server.playParimutuelTicket(currentBet.matchId, currentBet, m.predictions);
		}
		return true;
	}

	protected void setMatch(Match m)
	{
		setMatchDetails(m);
		//clearSelection();
		if (m != null) {
			server.getMatchStatus(m.matchId);
			startMatchStatusTimeoutTimer();
//			server.listPredictionResults(m, new PredictionGoal());
			if (m.status.status != MatchState.PLAYING) {
				stopClockUpdateTimer();
			} else {
				startClockUpdateTimer(clockUpdateRate);
			}
		}
//		liveBetPanel.teamStatsPanel.setMatch(currentMatch);
//		dataFeedPanel.addFeed(currentFeed);
	}

	public void livePredictionListUpdated(ArrayList<Prediction> pa)
	{
		SlotBetControl sbc2=null;
		for (SlotBetControl sbc: team1BetControls) {
			sbc.setCurrentState(SlotBetControl.NORMAL);
		}
		for (SlotBetControl sbc: team2BetControls){
			sbc.setCurrentState(SlotBetControl.NORMAL);
		}
		if ((pa != null) && (this.currentMatch != null)) {
			sbc2 = null;
//			trace("prediction list, size", pa.length);
			for (Prediction p: pa) {
				if (p != null) {
//					trace("checking prediction ticket", p);
					sbc2 = betControlFor((PredictionGoal)p);
					if (sbc2 != null) {
						if (!p.result.equals("W")) {
							if (!p.result.equals("L")) {
								sbc2.setCurrentState(SlotBetControl.PENDING);
							}else {
								sbc2.setCurrentState(SlotBetControl.LOST);
							}
						}else {
							sbc2.setCurrentState(SlotBetControl.WON);
						}
					} else {
//						trace("no corresponding bet control for prediction", p);
					}
				}
			}
		}
		return;
	}

	public int updateLiveMatchStatus(
			int _matchId, MatchTime t, int t1Score, int t2Score,
			String _matchState, Number _estimate, ArrayList<Goal> goals)
	{
		int timerState=0;
		if (currentMatchId() < 0) {
//			trace("update match status negative match id in live tab");
			return MatchTimerState.STOPPED;
		}
		if (currentMatchId() != _matchId) {
//			trace("update match status wrong match");
			return MatchTimerState.STOPPED;
		}
		if (_matchState.equals(MatchState.FINISHED)) {
			timerStopped = true;
			timerState = MatchTimerState.FINISHED;
			gotFinishedStatus = true;
			showStatus("FINAL WHISTLE");
		} else {
			hideFireworks();
			if (_matchState.equals(MatchState.INTERVAL)) {
				if (t.half != 1) {
					if (t.half != 2) {
						if (t.half != 3) {
							if (t.half != 4) {
								showStatus("INTERVAL ");
							} else {
								showStatus("EXTRA TIME INTERVAL");
							}
						}else {
							showStatus("BEFORE EXTRA TIME");
						}
					}else {
						showStatus("HALF TIME");
					}
				} else if ((currentMatch != null) /*&&
						   (currentMatch.start != null) &&
						   (currentMatch.start != "")*/) {
//					showStatus("KICKOFF AT " + MatchTime.timeFormat(currentMatch.start));
				} else {
					showStatus("WAITING TO START");
				}
				time = t;
				currentBetTime = adjustedBetTime(t, _matchState);
				timerStopped = true;
				timerState = MatchTimerState.PAUSED;
			} else if (_matchState.equals(MatchState.OPEN)) {
				showStatus("GAME OPEN");
				time = t;
				currentBetTime = adjustedBetTime(t, _matchState);
				timerStopped = true;
				timerState = MatchTimerState.PAUSED;
			} else if (_matchState.equals(MatchState.PAUSED)) {
				time = t;
				currentBetTime = adjustedBetTime(t, _matchState);
				if (time.half != 1){
					showStatus("GAME PAUSED - SECOND HALF");
				}else {
					showStatus("GAME PAUSED - FIRST HALF");
				}
				timerStopped = true;
				timerState = MatchTimerState.PAUSED;
			} else {
				time = t;
				currentBetTime = adjustedBetTime(t, _matchState);
				if (time.half != 1) {
					showStatus("LIVE BET - SECOND HALF");
				}else {
					showStatus("LIVE BET - FIRST HALF");
				}
				timerState = MatchTimerState.RUNNING;
				timerStopped = false;
			}
		}
		if (team1Details != null) {
			team1Details.setScore(t1Score);
		}
		if (team2Details != null) {
			team2Details.setScore(t2Score);
		}
		eventSlider.clearFeed();
		for (Goal g: goals) {
			eventSlider.addItem(FeedEntry.GOAL_EVENT,
				new MatchTime(g.half, g.min, g.sec), g.teamId,
				"Goal!!!!",
				"Goal to " + (g.teamId != currentMatch.homeTeam.teamId ? currentMatch.awayTeam.name : currentMatch.homeTeam.name) + "!");
		}
		showCurrentTime();
		return timerState;
	}


	protected void livePanelPredictionCheck()
	{
		if (server == null) {
			return;
		}
		Match m=getMatch(currentMatchId());
		if (m == null) {
			return;
		}
		if (m.predictions == null || m.predictions.size() == 0) {
			return;
		}
		if (hasLivePredictionsToCheck(m)) {
//			trace("match has live predictions!");
//			server.listPredictionResults(m, new PredictionGoal());
		}else {
//			trace("no live predictions in current live match");
		}
		return;
	}

	protected void checkPredictionsFor(Match m)
	{
		MatchTime t=null;
		boolean chk=false;
		if (m == null || m.predictions == null) {
			return;
		}
//		trace("checking prediction results for match", m.name, m.liveStatus());
		if (m.liveStatus() != 0) {
			if (m.liveStatus() > 0) {
				for (Prediction p: m.predictions) {
					if (p.result.equals("W")) {
//						trace("winning result in future match, wtf!");
						continue;
					}
					p.result = "U";
				}
				return;
			}
			for (Prediction p: m.predictions) {
				if (p.result.equals("W")) {
					continue;
				}
				p.result = "L";
			}
			return;
		}else {
			t = new MatchTime(m.status.time);
//			trace("match is live, checking, comparison time is", t, time);
			if (m.matchId == currentMatchId()) {
				if (time.gt(t)) {
					t.setTo(time);
				}
			}
//			trace("and now comparison time is", t.half, t.min);
			for (Prediction p: m.predictions) {
				PredictionGoal pg = (PredictionGoal)p;
				if (!p.result.equals("W")) {
					chk = false;
					if (pg.period < t.half) {
						chk = true;
					} else if (pg.period != t.half) {
						chk = false;
					} else if (t.min >= pg.minute + BET_INTERVAL_LEN_MINS) {
						chk = true;
					}
					if (chk) {
						p.result = "L";
					} else {
						p.result = "U";
					}
				}
			}
		}
		return;
	}

	public Boolean hasLivePredictionsToCheck(Match m)
	{
		if (m == null || currentMatch == null ||
			 (m.matchId != currentMatch.matchId)) {
			return false;
		}
		if (m.predictions == null || m.predictions.size() == 0) {
			return false;
		}
		for (Prediction p: m.predictions) {
			PredictionGoal pg = (PredictionGoal)p;
			if (p.result.equals("U")) {
				if (pg.period == time.half && time.min >= pg.minute) {
					return true;
				}
				if ((pg.period < time.half)) {
					return true;
				}
			}
		}
		return false;
	}
	
/*******************************************
 * DISPLAY HOOKS
 *******************************************/
		public void showCurrentTime()
		{
			eventSlider.setGameTime(time);
			eventSlider.setBetTime(currentBetTime);
			gameClock.setGameTime(time);
			for (SlotBetControl sbc: team1BetControls) {
				if (sbc.time().ge(currentBetTime)) {
					sbc.setFinished(false);
				} else {
					if (sbc.getCurrentState() == SlotBetControl.SELECTED) {
						sbc.makeSelection(false);
					}
					sbc.setFinished(true);
				}
			}
			for (SlotBetControl sbc: team2BetControls) {
				if (sbc.time().ge(currentBetTime)) {
					sbc.setFinished(false);
				} else {
					if (sbc.getCurrentState() == SlotBetControl.SELECTED) {
						sbc.makeSelection(false);
					}
					sbc.setFinished(true);
				}
			}
			return;
		}

	public void showError(int code, String msg)
	{
		showError(code, msg, 0);
	}
	
	public void showError(int code, String msg, int lvl)
	{
		showStatus(msg);
	}
	
	AlertDialog selectMatchDialog = null;
	
	protected void showPreferences()
	{
		// TODO Auto-generated method stub
		
	}

	protected void showSelect()
	{
		if (server == null) return;
		ArrayList<Match> lml = server.liveMatchList();
		if (lml == null || lml.size() == 0) return;
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setIcon(R.drawable.app_icon);
		builder.setTitle("Select Live Match");
		ListView modeList = new ListView(this);
		final String[] matchNames = new String[lml.size()];
		final Match[] menuMatches = new Match[lml.size()];
		int i = 0;
		for (Match m:lml) {
			menuMatches[i] = m;
			matchNames[i++] = m.name;
		}
		ArrayAdapter<String> modeAdapter = new ArrayAdapter<String>(
				this, android.R.layout.simple_list_item_1, android.R.id.text1, matchNames);
		modeList.setAdapter(modeAdapter);

		selectMatchDialog = builder.create();
		selectMatchDialog = builder.setView(modeList).setCancelable(true).show();

		modeList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id)
			{
				Log.d("menu item", "select match "+menuMatches[position].name);
				setMatch(menuMatches[position]);
				selectMatchDialog.dismiss();
//		      Log.d("item click","got"+view.toString()+" "+view.getId()+" "+Integer.toString(position));
			}
		});
		
	}
	
	protected void showHelp()
	{
		View messageView = getLayoutInflater().inflate(R.layout.help, null, false);
		// When linking text, force to always use default color. This works
		// around a pressed color state bug.
		TextView textView = (TextView) messageView.findViewById(R.id.help_more);
		int defaultColor = textView.getTextColors().getDefaultColor();
		textView.setTextColor(defaultColor);

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setIcon(R.drawable.app_icon);
		builder.setTitle(getString(R.string.app_name)+" Help");
		builder.setView(messageView);
		builder.create();
		builder.show();
	}
	
	protected void showAbout()
	{
		// Inflate the about message contents
		View messageView = getLayoutInflater().inflate(R.layout.about, null, false);

		// When linking text, force to always use default color. This works
		// around a pressed color state bug.
		TextView textView = (TextView) messageView.findViewById(R.id.about_credits);
		int defaultColor = textView.getTextColors().getDefaultColor();
		textView.setTextColor(defaultColor);

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setIcon(R.drawable.app_icon);
		builder.setTitle("About "+getString(R.string.app_name));
		builder.setView(messageView);
		builder.create();
		builder.show();
	}
	
	protected void showAccountBalance()
	{
		if (server != null) {
			viewAccount.setText(formatCurrency(server.accountBalance, server.accountCurrency, true, 2));
		}
	}


	protected void showStatus(String string)
	{
		viewStatus.setText(string);

		
	}
	
	/**
	 * show the view bits indicating an internet connection in progress
	 */
	protected void showConnecting()
	{
		showConnecting(null);
	}
	
	protected void showConnecting(String title)
	{
		if (title != null) {
			viewStatus.setText(title);
		}
		viewProgress.setVisibility(View.VISIBLE);
		viewStatusBar.startAnimation(animSlideIn);
	}
   	
	/**
	 * hide the view bits indicating an internet connection in progress
	 */
	protected void hideConnecting()
	{
		viewStatusBar.startAnimation(animSlideOut);
		viewProgress.setVisibility(View.INVISIBLE);
	}

	public void reBet(int bsid, MatchTime bt, MatchTime slt)
	{
		currentBet.betSlotId = bsid;
		currentBetTime = new MatchTime(bt);
		showCurrentTime();
		confirmBet(bsid, currentBet, "SLOT AT " + slt.toString() + "MIN IS OVER. CONFIRM NEW BET");
	}

	public void confirmBet(int slid, PredictionGoal bet)
	{
		confirmBet(slid, bet, null);
	}
	
	public void confirmBet(int slid, PredictionGoal bet, String title)
	{
		showConfirmScreenSingleBet(slid, title, 
					bet.period <= 1 ? "First half" : "Second half", bet);
	}

	public boolean confirmSelection()
	{
		if (!(selectedBets == null) && selectedBets.size() > 0) {
			showConfirmScreenMultipleBets(selectedBets, null);
			return true;
		}
		return false;
	}

	protected void showConfirmScreenSingleBet(int slid, String title, String halfName, PredictionGoal bet)
	{
		// TODO Auto-generated method stub
		
	}

	public void showConfirmScreenMultipleBets(ArrayList<Prediction>pra, String title)
	{
		// TODO Auto-generated method stub
		
	}

	public void hideFireworks()
	{
		// TODO Auto-generated method stub
		
	}
	
	public void fireworks(
			Match m,
			String start,
			float winnings,
			float estimate,
			float pot,
			String currency,
			ArrayList<Prediction> pred,
			ArrayList<Jackpot> jackpots)
	{
		if (m == null) {
//			trace("game panel:: fireworks() null match");
			return;
		}
		if (m.matchId != currentMatchId()) {
//			trace("game panel:: fireworks() bad match id");
			return;
		}
		livePredictionListUpdated(pred);
//		fireworksPanel.displayFinalResults(m, start, winnings, estimate, pot, currency, pred, jackpots);
//		betViews.selectedChild = fireworksPanel;
		return;
	}
}