package tygriffi.calpoly.edu.sportsscores;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

import java.util.Arrays;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    private SQLiteDatabase myDatabase;
    private SwipeRefreshLayout swipeContainer;
    private Menu searchMenu;
    private String[] nflTeams = new String[32];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Home");
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        swipeContainer = (SwipeRefreshLayout) findViewById(R.id.content_main);
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                LinearLayout nfl = (LinearLayout) findViewById(R.id.nflLL);
                LinearLayout fav = (LinearLayout) findViewById(R.id.favTeams);
                nfl.removeAllViews();
                fav.removeAllViews();
                getAndUpdateDate();
                swipeContainer.setRefreshing(false);
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        getAndUpdateDate();

        TextView nflAll = (TextView) findViewById(R.id.nfl_see_all_link);
        nflAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToNFLScores(false);
            }
        });

        TextView favAll = (TextView) findViewById(R.id.fav_see_all_link);
        favAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToNFLScores(true);
            }
        });
    }

    private void initializeNFLTeams() {
        Cursor resultSet = myDatabase.rawQuery("Select DISTINCT * from (" +
                "Select homeTeam From NFLGames " +
                "UNION " +
                "Select awayTeam From NFLGames)T ",null);
        int i = 0;
        while (resultSet.moveToNext()) {
            nflTeams[i++] = resultSet.getString(0);
        }
    }

    private void getAndUpdateDate() {
        //Check for saved state or database
        myDatabase = openOrCreateDatabase("MobAppDev",MODE_PRIVATE,null);
        RequestQueue queue = Volley.newRequestQueue(this);
        final APIWork api = new APIWork(myDatabase, queue);
        new Thread(new Runnable() {
            @Override
            public void run() {
                api.apiSetup();
            }
        }).start();

        new checkForLoad().execute("");
    }

    private void setUpFavoritesMenu(SQLiteDatabase myDatabase) {
        NavigationView navView = (NavigationView) findViewById(R.id.nav_view);
        Menu m = navView.getMenu();
        SubMenu favMenu = m.getItem(1).getSubMenu();
        favMenu.clear();
        favMenu.add("Edit Favorites").setIcon(R.drawable.ic_action_name);
        myDatabase.execSQL("CREATE TABLE IF NOT EXISTS MyFavorites(team VARCHAR UNIQUE, " +
                "fav BOOL);");
        Cursor resultSet = myDatabase.rawQuery("Select * from MyFavorites",null);
        if (resultSet.getCount() != 0 ){
            while (resultSet.moveToNext()) {
                if (resultSet.getInt(1) == 1) {
                    favMenu.add(resultSet.getString(0));
                }
            }
        }
    }

    private void goToNFLScores(boolean favorites) {
        APIWork api = new APIWork();
        Intent intent = new Intent(getApplicationContext(), ScoresSummary.class);
        intent.putExtra("CATEGORY", "NFL");
        if (favorites) {
            intent.putExtra("FAV","True");
        }
        intent.putExtra("DATE", api.getDateAsDBString(Calendar.getInstance()));
        startActivity(intent);
    }

    private void addScoresToHome(String tableName, LinearLayout layout, boolean favorites) {
        int count;
        Cursor resultSet;

        if (!favorites) {
            resultSet = myDatabase.rawQuery("Select * from " + tableName + " ORDER BY date DESC",null);
        }
        else {
            resultSet = myDatabase.rawQuery("Select * from " + tableName + " WHERE " +
                    "homeTeam IN (" +
                    "SELECT team FROM MyFavorites WHERE fav = 1) OR " +
                    "awayTeam IN (" +
                    "SELECT team FROM MyFavorites WHERE fav = 1) ORDER BY date DESC",null);
        }
        count = resultSet.getCount();
        if (count > 0 && favorites) {
            layout.removeView(findViewById(R.id.favNone));
        }

        if (count != 0) {
            int length = favorites ? 2 : 3;
            for (int i = 0; i < length; i++) {
                resultSet.moveToNext();
                LayoutInflater inflater = LayoutInflater.from(getApplicationContext());
                View inflatedLayout= inflater.inflate(R.layout.game_score, null, false);
                TextView homeTeam = (TextView) inflatedLayout.findViewById(R.id.homeTeamTxt);
                TextView awayTeam = (TextView) inflatedLayout.findViewById(R.id.awayTeamTxt);
                TextView homeScore = (TextView) inflatedLayout.findViewById(R.id.homeTeamScore);
                TextView awayScore = (TextView) inflatedLayout.findViewById(R.id.awayTeamScore);
                TextView gameInfo = (TextView) inflatedLayout.findViewById(R.id.gameInfoTxt);

                final String ht = resultSet.getString(0);
                final String at = resultSet.getString(1);

                homeTeam.setText(ht);
                awayTeam.setText(at);
                homeScore.setText(resultSet.getString(2));
                awayScore.setText(resultSet.getString(3));
                gameInfo.setText(getGameInfo(resultSet));

                homeTeam.setTextColor(getResources().getColor(R.color.TextGray));
                awayTeam.setTextColor(getResources().getColor(R.color.TextGray));
                homeScore.setTextColor(getResources().getColor(R.color.TextGray));
                awayScore.setTextColor(getResources().getColor(R.color.TextGray));
                gameInfo.setTextColor(getResources().getColor(R.color.TextGray));

                homeTeam.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        loadTeamActivity(ht);
                    }
                });

                awayTeam.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        loadTeamActivity(at);
                    }
                });


                layout.addView(inflatedLayout);
            }
        }
    }

    private String getGameInfo(Cursor resultSet) {
        String info = "Final";

        //check if game played (yes == 1)
        if (resultSet.getInt(7) == 0) {
            //Game is in progress
            if (resultSet.getInt(6) == 1) {
                info = resultSet.getString(5);
                info = info + "\n" + "Qtr: " + resultSet.getInt(8);
            }
            else {
                info = "Kickoff\n" + resultSet.getString(5);
            }
        }

        return info;
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);

        searchMenu = menu;

        SearchManager manager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        final SearchView search = (SearchView) menu.findItem(R.id.action_search).getActionView();

        search.setSearchableInfo(manager.getSearchableInfo(getComponentName()));
        search.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                query = query.substring(0, 1).toUpperCase() + query.substring(1).toLowerCase();
                Log.d("SUBMIT", query);
                if (Arrays.asList(nflTeams).contains(query)) {
                    loadTeamActivity(query);
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String query) {
                loadHistory(query);
                return true;
            }
        });

        search.setOnSuggestionListener(new SearchView.OnSuggestionListener() {
            @Override
            public boolean onSuggestionSelect(int position) {
                Log.d("CHECK 1", String.valueOf(position));
                return true;
            }

            @Override
            public boolean onSuggestionClick(int position) {
                Cursor cursor = (Cursor) search.getSuggestionsAdapter().getItem(position);
                String teamName = cursor.getString(1);
                cursor.close();

                loadTeamActivity(teamName);
                return true;
            }
        });

        return true;
    }

    private void loadHistory(String query) {
        Cursor cursor = myDatabase.rawQuery("Select rowid _id, team from (" +
                "Select homeTeam as team From NFLGames " +
                "UNION " +
                "Select awayTeam as team From NFLGames)T WHERE " +
                "team LIKE '" + query + "%'",null);
        SearchManager manager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView search = (SearchView) searchMenu.findItem(R.id.action_search).getActionView();

        search.setSearchableInfo(manager.getSearchableInfo(getComponentName()));

        search.setSuggestionsAdapter(new SearchAdapter(getSupportActionBar().getThemedContext(), cursor));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_search) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_football) {
            goToNFLScores(false);
        } else if (item.getTitle().toString().equals("Edit Favorites")) {
            Intent intent = new Intent(getApplicationContext(), FavoritesActivity.class);
            startActivityForResult(intent, 14);
        }
        else if (id == R.id.app_info) {
            Intent intent = new Intent(getApplicationContext(), AppInfoActivity.class);
            startActivity(intent);
        }
        else {
            loadTeamActivity(item.getTitle().toString());
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void loadTeamActivity(String team) {
        APIWork api = new APIWork();
        Intent intent = new Intent(getApplicationContext(), ScoresSummary.class);
        intent.putExtra("CATEGORY", "NFL");
        intent.putExtra("TEAM", team);
        intent.putExtra("DATE", api.getDateAsDBString(Calendar.getInstance()));
        startActivity(intent);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 14) {
            if (resultCode == RESULT_OK) {
                setUpFavoritesMenu(myDatabase);
                LinearLayout favTeams = (LinearLayout) findViewById(R.id.favTeams);
                favTeams.removeAllViews();
                addScoresToHome("NFLGames", favTeams, true);
            }
        }
    }

    private class checkForLoad extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            SQLiteDatabase myDatabase = openOrCreateDatabase("MobAppDev",MODE_PRIVATE,null);
            Cursor resultSet;
            int count = 0;
            while (count < 3) {
                resultSet = myDatabase.rawQuery("Select * from NFLGames",null);
                count = resultSet.getCount();
                resultSet.close();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            setUpFavoritesMenu(myDatabase);
            addScoresToHome("NFLGames", (LinearLayout) findViewById(R.id.nflLL), false);
            addScoresToHome("NFLGames", (LinearLayout) findViewById(R.id.favTeams), true);
            initializeNFLTeams();
        }

    }
}
