/*
b * Copyright 2013 Simon Willeke
 * contact: hamstercount@hotmail.com
 */

/*
    This file is part of Blockinger.

    Blockinger is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Blockinger is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Blockinger.  If not, see <http://www.gnu.org/licenses/>.

    Diese Datei ist Teil von Blockinger.

    Blockinger ist Freie Software: Sie k�nnen es unter den Bedingungen
    der GNU General Public License, wie von der Free Software Foundation,
    Version 3 der Lizenz oder (nach Ihrer Option) jeder sp�teren
    ver�ffentlichten Version, weiterverbreiten und/oder modifizieren.

    Blockinger wird in der Hoffnung, dass es n�tzlich sein wird, aber
    OHNE JEDE GEW�HELEISTUNG, bereitgestellt; sogar ohne die implizite
    Gew�hrleistung der MARKTF�HIGKEIT oder EIGNUNG F�R EINEN BESTIMMTEN ZWECK.
    Siehe die GNU General Public License f�r weitere Details.

    Sie sollten eine Kopie der GNU General Public License zusammen mit diesem
    Programm erhalten haben. Wenn nicht, siehe <http://www.gnu.org/licenses/>.
 */

package com.teguholica.tetris.activities;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;

import com.teguholica.tetris.BlockBoardView;
import com.teguholica.tetris.R;
import com.teguholica.tetris.WorkThread;
import com.teguholica.tetris.components.Controls;
import com.teguholica.tetris.components.Display;
import com.teguholica.tetris.components.GameState;
import com.teguholica.tetris.components.Sound;

public class GameActivity extends FragmentActivity {

    public Sound sound;
    public Controls controls;
    public Display display;
    public GameState game;
    private WorkThread mainThread;
    private DefeatDialogFragment dialog;
    private boolean layoutSwap;

    public static final int NEW_GAME = 0;
    public static final int RESUME_GAME = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_game);
        layoutSwap = false;

		/* Read Starting Arguments */
        Bundle b = getIntent().getExtras();
        int value = NEW_GAME;

		/* Create Components */
        game = (GameState) getLastCustomNonConfigurationInstance();
        if (game == null) {
			/* Check for Resuming (or Resumption?) */
            if (b != null)
                value = b.getInt("mode");

            if ((value == NEW_GAME)) {
                game = GameState.getNewInstance(this);
                game.setLevel(b.getInt("level"));
            } else
                game = GameState.getInstance(this);
        }
        game.reconnect(this);
        dialog = new DefeatDialogFragment();
        controls = new Controls(this);
        display = new Display(this);
        sound = new Sound(this);
		
		/* Init Components */
        if (game.isResumable())
            sound.startMusic(Sound.GAME_MUSIC, game.getSongtime());
        sound.loadEffects();
        if (b != null) {
            value = b.getInt("mode");
            if (b.getString("playername") != null)
                game.setPlayerName(b.getString("playername"));
        } else
            game.setPlayerName(getResources().getString(R.string.anonymous));
        dialog.setCancelable(false);
        if (!game.isResumable())
            gameOver(game.getScore(), game.getTimeString(), game.getAPM());
		
		/* Register Button callback Methods */
        ((Button) findViewById(R.id.pausebutton_1)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                GameActivity.this.finish();
            }
        });
        ((BlockBoardView) findViewById(R.id.boardView)).setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    controls.boardPressed(event.getX(), event.getY());
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    controls.boardReleased();
                }
                return true;
            }
        });
        ((BlockBoardView) findViewById(R.id.boardView2)).setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    controls.boardPressed(event.getX(), event.getY());
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    controls.boardReleased();
                }
                return true;
            }
        });
        ((ImageButton) findViewById(R.id.rightButton)).setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return handleRight(event);
            }
        });
        ((ImageButton) findViewById(R.id.leftButton)).setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    controls.leftButtonPressed();
                    ((ImageButton) findViewById(R.id.leftButton)).setPressed(true);
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    controls.leftButtonReleased();
                    ((ImageButton) findViewById(R.id.leftButton)).setPressed(false);
                }
                return true;
            }
        });
        ((ImageButton) findViewById(R.id.softDropButton)).setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    controls.downButtonPressed();
                    ((ImageButton) findViewById(R.id.softDropButton)).setPressed(true);
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    controls.downButtonReleased();
                    ((ImageButton) findViewById(R.id.softDropButton)).setPressed(false);
                }
                return true;
            }
        });
        ((ImageButton) findViewById(R.id.hardDropButton)).setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    controls.dropButtonPressed();
                    ((ImageButton) findViewById(R.id.hardDropButton)).setPressed(true);
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    controls.dropButtonReleased();
                    ((ImageButton) findViewById(R.id.hardDropButton)).setPressed(false);
                }
                return true;
            }
        });
        ((ImageButton) findViewById(R.id.rotateRightButton)).setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    controls.rotateRightPressed();
                    ((ImageButton) findViewById(R.id.rotateRightButton)).setPressed(true);
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    controls.rotateRightReleased();
                    ((ImageButton) findViewById(R.id.rotateRightButton)).setPressed(false);
                }
                return true;
            }
        });
        ((ImageButton) findViewById(R.id.rotateLeftButton)).setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    controls.rotateLeftPressed();
                    ((ImageButton) findViewById(R.id.rotateLeftButton)).setPressed(true);
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    controls.rotateLeftReleased();
                    ((ImageButton) findViewById(R.id.rotateLeftButton)).setPressed(false);
                }
                return true;
            }
        });

        ((BlockBoardView) findViewById(R.id.boardView)).init();
        ((BlockBoardView) findViewById(R.id.boardView)).setHost(this);

        ((BlockBoardView) findViewById(R.id.boardView2)).init();
        ((BlockBoardView) findViewById(R.id.boardView2)).setHost(this);
    }

    private boolean handleRight(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            controls.rightButtonPressed();
            ((ImageButton) findViewById(R.id.rightButton)).setPressed(true);
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            controls.rightButtonReleased();
            ((ImageButton) findViewById(R.id.rightButton)).setPressed(false);
        }
        return true;
    }

    /**
     * Called by BlockBoardView upon completed creation
     *
     * @param caller
     */
    public void startGame(BlockBoardView caller) {
        if (mainThread == null) {
            mainThread = new WorkThread(this, caller.getHolder());
            mainThread.setFirstTime(false);
            game.setRunning(true);
            mainThread.setRunning(true);
            mainThread.start();
        } else {
            mainThread.addHolder(caller.getHolder());
        }
    }

    /**
     * Called by BlockBoardView upon destruction
     */
    public void destroyWorkThread() {
        if (!mainThread.getRunning()) {
            return;
        }
        boolean retry = true;
        mainThread.setRunning(false);
        while (retry) {
            try {
                mainThread.join();
                retry = false;
            } catch (InterruptedException e) {

            }
        }
    }

    /**
     * Called by GameState upon Defeat
     *
     * @param score
     */
    public void putScore(long score) {
        String playerName = game.getPlayerName();
        if (playerName == null || playerName.equals(""))
            playerName = getResources().getString(R.string.anonymous);//"Anonymous";

        Intent data = new Intent();
        data.putExtra(MainActivity.PLAYERNAME_KEY, playerName);
        data.putExtra(MainActivity.SCORE_KEY, score);
        setResult(MainActivity.RESULT_OK, data);

        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();
        sound.pause();
        sound.setInactive(true);
        game.setRunning(false);
    }

    ;

    @Override
    protected void onStop() {
        super.onStop();
        sound.pause();
        sound.setInactive(true);
    }

    ;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        game.setSongtime(sound.getSongtime());
        sound.release();
        sound = null;
        game.disconnect();
    }

    ;

    @Override
    protected void onResume() {
        super.onResume();
        sound.resume();
        sound.setInactive(false);
    	
    	/* Check for changed Layout */
        boolean tempswap = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("pref_layoutswap", false);
        if (layoutSwap != tempswap) {
            layoutSwap = tempswap;
            if (layoutSwap) {
                setContentView(R.layout.activity_game_alt);
            } else {
                setContentView(R.layout.activity_game);
            }
        }
        game.setRunning(true);
    }

    ;


    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        Log.e("wtf", keyCode + "" + "  event:" + event.getAction());
        if (keyCode == 20) {
            //往下一格
            controls.dropButtonReleased();
        } else if (keyCode == 22) {
            //往右
            controls.rightButtonReleased();
        } else if (keyCode == 21) {
            //左边
            controls.leftButtonReleased();
        } else if (keyCode == 103 || keyCode == 102 || keyCode == 19) {
            //变化
            controls.rotateRightReleased();
        } else {
            return super.onKeyUp(keyCode, event);
        }
        return true;
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.e("wtf", keyCode + "" + "  event:" + event.getAction());

        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (keyCode == 20) {
                //往下一格
                controls.dropButtonPressed();
            } else if (keyCode == 22) {
                //往右
                controls.rightButtonPressed();
            } else if (keyCode == 21) {
                //左边
                controls.leftButtonPressed();
            } else if (keyCode == 103 || keyCode == 102 || keyCode == 19) {
                //变化
                controls.rotateRightPressed();
            } else {
                return super.onKeyDown(keyCode, event);
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        return game;
    }

    public void gameOver(long score, String gameTime, int apm) {
        dialog.setData(score, gameTime, apm);
        dialog.show(getSupportFragmentManager(), "hamster");
    }

}
