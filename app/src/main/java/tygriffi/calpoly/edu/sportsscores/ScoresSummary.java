package tygriffi.calpoly.edu.sportsscores;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

import java.util.ArrayList;
import java.util.Calendar;

/**
 * Created by Tyler on 11/16/2016.
 */

public class ScoresSummary extends AppCompatActivity {

    private Calendar scoreDate;
    private static ArrayList<Game> entries;
    private MyAdapter adapter = null;
    private boolean teamBool;
    private String teamName = null;
    private boolean isFavorites = false;
    private SwipeRefreshLayout swipeContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_scores);
        entries = new ArrayList<>();

        final String category = getIntent().getStringExtra("CATEGORY");
        final String team = getIntent().getStringExtra("TEAM");
        final String date = getIntent().getStringExtra("DATE");
        String fav = getIntent().getStringExtra("FAV");


        if (team == null) {
            teamBool = false;
            if (fav == null) {
                setTitle(category);
            }
            else {
                setTitle("Favorites");
                isFavorites = true;
            }
        }
        else {
            teamBool = true;
            teamName = team;
            setTitle(team);
        }
        scoreDate = setCalendar(date);
        final APIWork api = new APIWork();
        LinearLayout dateBar = (LinearLayout) findViewById(R.id.dateBar);
        if (teamBool) {
            dateBar.setVisibility(View.GONE);
        }
        TextView dateTV = (TextView) findViewById(R.id.scoresMainDate);
        TextView tomorrow = (TextView) findViewById(R.id.tomorrow);
        TextView yesterday = (TextView) findViewById(R.id.yesterday);
        dateTV.setText(date);

        if (savedInstanceState == null) {
            getAllScores(category, team, date);
        }
        else {
            entries = savedInstanceState.getParcelableArrayList("GAMES");
        }
        //Setup Swipe
        swipeContainer = (SwipeRefreshLayout) findViewById(R.id.allScoresSwipeLayout);
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                SQLiteDatabase myDatabase = openOrCreateDatabase("MobAppDev",MODE_PRIVATE,null);
                RequestQueue queue = Volley.newRequestQueue(getApplicationContext());
                final APIWork api = new APIWork(myDatabase, queue);
                api.apiSetup();
                try {
                    Thread.sleep(250);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                entries.clear();
                adapter.notifyDataSetChanged();

                getAllScores(category, team, date);
                adapter.notifyDataSetChanged();
                swipeContainer.setRefreshing(false);
            }
        });

        //Set up Recycler
        RecyclerView rv = (RecyclerView) findViewById(R.id.rv);
        assert rv != null;
        adapter = new MyAdapter(entries);
        rv.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        rv.setItemAnimator(new DefaultItemAnimator());
        rv.setAdapter(adapter);

        if (!teamBool) {
            tomorrow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(getApplicationContext(), ScoresSummary.class);
                    intent.putExtra("CATEGORY", "NFL");
                    scoreDate.add(Calendar.DAY_OF_MONTH, 1);
                    if (isFavorites) {
                        intent.putExtra("FAV","ALL");
                    }
                    intent.putExtra("DATE", api.getDateAsDBString(scoreDate));
                    startActivity(intent);
                    finish();
                }
            });

            yesterday.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(getApplicationContext(), ScoresSummary.class);
                    intent.putExtra("CATEGORY", "NFL");
                    if (isFavorites) {
                        intent.putExtra("FAV","ALL");
                    }
                    scoreDate.add(Calendar.DAY_OF_MONTH, -1);
                    intent.putExtra("DATE", api.getDateAsDBString(scoreDate));
                    startActivity(intent);
                    finish();
                }
            });

            dateTV.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int mYear = Integer.valueOf(date.split("-")[0]);
                    int mMonth = Integer.valueOf(date.split("-")[1]) - 1;
                    int mDay = Integer.valueOf(date.split("-")[2]);

                    DatePickerDialog dialog = new DatePickerDialog(ScoresSummary.this, R.style.DialogTheme,
                            new mDateSetListener(), mYear, mMonth, mDay);
                    dialog.show();
                }
            });
        }
    }

    private void getAllScores(String category, String team, String date) {
        if (teamBool) {
            getTeamScores(category, team);
        }
        else if (isFavorites) {
            getFavoritesScores(category, date);
        }
        else {
            getLeagueScores(category, date);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        //Save array of entries
        outState.putParcelableArrayList("GAMES", entries);
        super.onSaveInstanceState(outState);
    }

    public void getLeagueScores(String league, String date) {
        SQLiteDatabase myDatabase = openOrCreateDatabase("MobAppDev",MODE_PRIVATE,null);
        if (league.equals("NFL")) {
            Cursor resultSet = myDatabase.rawQuery("Select * from NFLGames WHERE date = '" +
                    date + "'",null);
            getGames(resultSet, league);
        }
        myDatabase.close();
    }

    public void getTeamScores(String league, String team) {
        int games, wins, loses, ties;
        SQLiteDatabase myDatabase = openOrCreateDatabase("MobAppDev",MODE_PRIVATE,null);
        if (league.equals("NFL")) {
            Cursor resultSet = myDatabase.rawQuery("Select * from NFLGames WHERE homeTeam = '" +
                    team + "' OR awayTeam = '" + team + "' ORDER BY date DESC",null);
            getGames(resultSet, league);
            games = resultSet.getCount();
            resultSet = myDatabase.rawQuery("Select * from NFLGames WHERE homeTeam = '" +
                    team + "' AND homeScore > awayScore",null);
            wins = resultSet.getCount();
            resultSet = myDatabase.rawQuery("Select * from NFLGames WHERE awayTeam = '" +
                    team + "' AND homeScore < awayScore",null);
            wins = wins + resultSet.getCount();
            resultSet = myDatabase.rawQuery("Select * from NFLGames WHERE homeTeam = '" +
                    team + "' AND homeScore = awayScore",null);
            ties = resultSet.getCount();
            resultSet = myDatabase.rawQuery("Select * from NFLGames WHERE awayTeam = '" +
                    team + "' AND homeScore = awayScore",null);
            ties = ties + resultSet.getCount();
            loses = games - wins - ties;

            if (ties != 0) {
                setTitle(team + ": " + wins + "-" + loses + "-" + ties);
            }
            else {
                setTitle(team + ": " + wins + "-" + loses);
            }
        }
        myDatabase.close();
    }

    public void getFavoritesScores(String league, String date) {
        SQLiteDatabase myDatabase = openOrCreateDatabase("MobAppDev",MODE_PRIVATE,null);
        if (league.equals("NFL")) {
            Cursor resultSet = myDatabase.rawQuery("Select * from NFLGames WHERE " +
                    "date = '" + date + "' AND (homeTeam IN (" +
                    "SELECT team FROM MyFavorites WHERE fav = 1) OR " +
                    "awayTeam IN (" +
                    "SELECT team FROM MyFavorites WHERE fav = 1))",null);
            getGames(resultSet, league);
        }
        myDatabase.close();
    }

    public void getGames(Cursor resultSet, String league) {
        if (resultSet.getCount() > 0) {
            //Get all entries and add to array list
            while (resultSet.moveToNext()) {
                entries.add(new Game(league, resultSet.getString(0), resultSet.getString(1),
                 resultSet.getInt(2), resultSet.getInt(3), resultSet.getString(4), resultSet.getString(5),
                 resultSet.getInt(7) == 1, resultSet.getInt(6) == 1, resultSet.getInt(8)));
            }
        }
    }

    public Calendar setCalendar(String date) {
        String[] dateSplit = date.split("-");
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, Integer.valueOf(dateSplit[0]));
        cal.set(Calendar.MONTH, Integer.valueOf(dateSplit[1]) - 1);
        cal.set(Calendar.DAY_OF_MONTH, Integer.valueOf(dateSplit[2]));

        return cal;
    }

    public class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyViewHolder> {
        private ArrayList<Game> mEntries;
        public class MyViewHolder extends RecyclerView.ViewHolder {
            public TextView homeTeam;
            public TextView homeScore;
            public TextView awayTeam;
            public TextView awayScore;
            public TextView gameInfo;

            public MyViewHolder(View view) {
                super(view);
                homeTeam = (TextView) view.findViewById(R.id.homeTeamTxt);
                awayTeam = (TextView) view.findViewById(R.id.awayTeamTxt);
                homeScore = (TextView) view.findViewById(R.id.homeTeamScore);
                awayScore = (TextView) view.findViewById(R.id.awayTeamScore);
                gameInfo = (TextView) view.findViewById(R.id.gameInfoTxt);
            }
        }

        public MyAdapter(ArrayList<Game> entries) {
            this.mEntries = entries;
        }

        @Override
        public int getItemViewType(int position) {
            return R.layout.game_score;
        }

        @Override
        public MyAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.game_score, parent, false);
            return new MyViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(final MyViewHolder holder, int position) {
            final Game item = mEntries.get(position);
            holder.homeTeam.setText(item.hometeam);
            holder.awayTeam.setText(item.awayteam);
            holder.homeScore.setText(String.valueOf(item.homeScore));
            holder.awayScore.setText(String.valueOf(item.awayScore));
            String info = teamBool ? "Final\n" + item.date : "Final";
            holder.gameInfo.setText(info);

            //check if game played (yes == 1)
            if (!item.played) {
                //Game is in progress
                info = teamBool ? item.date + "\n" : "";
                if (item.inProgress) {
                    info = info + item.time;
                    info = info + "\n" + "Qtr: " + item.quarter;
                }
                else {
                    info = info + "Kickoff\n" + item.time;
                }
                holder.gameInfo.setText(info);
            }

            if (teamName == null || !teamName.equals(item.hometeam)) {
                holder.homeTeam.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        loadTeam(item.hometeam);
                    }
                });
            }
            if (teamName == null || !teamName.equals(item.awayteam)) {
                holder.awayTeam.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        loadTeam(item.awayteam);
                    }
                });
            }
        }

        @Override
        public int getItemCount() {
            return mEntries.size();
        }
    }

    private void loadTeam(String team) {
        APIWork api = new APIWork();
        Intent intent = new Intent(getApplicationContext(), ScoresSummary.class);
        intent.putExtra("CATEGORY", "NFL");
        intent.putExtra("TEAM", team);
        intent.putExtra("DATE", api.getDateAsDBString(scoreDate));
        startActivity(intent);
    }


    @Override
    public void onStop() {
        super.onStop();

    }

    private class mDateSetListener implements DatePickerDialog.OnDateSetListener {
        @Override
        public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
            String newDate = year + "-";
            month = month + 1;
            newDate = month < 10 ? newDate + "0" + month + "-" : newDate + month + "-";
            newDate = dayOfMonth < 10 ? newDate + "0" + dayOfMonth: newDate + dayOfMonth;

            Log.d("Lisenter", newDate);

            Intent intent = new Intent(getApplicationContext(), ScoresSummary.class);
            intent.putExtra("CATEGORY", "NFL");
            if (isFavorites) {
                intent.putExtra("FAV","ALL");
            }
            scoreDate.add(Calendar.DAY_OF_MONTH, -1);
            intent.putExtra("DATE", newDate);
            startActivity(intent);
            finish();
        }
    }

}
