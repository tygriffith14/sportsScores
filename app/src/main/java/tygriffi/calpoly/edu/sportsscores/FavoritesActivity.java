package tygriffi.calpoly.edu.sportsscores;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by Tyler on 11/23/2016.
 */

public class FavoritesActivity extends AppCompatActivity {

    private ArrayList<FavoriteTeam> entries;
    private MyFavAdapter adapter;
    private SQLiteDatabase myDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Favorites");
        setContentView(R.layout.add_favorites);

        myDatabase = openOrCreateDatabase("MobAppDev",MODE_PRIVATE,null);
        if (savedInstanceState == null) {
            Cursor resultSet = myDatabase.rawQuery("Select * from MyFavorites",null);
            if (resultSet.getCount() == 0) {
                entries = setUpFavoritesDB(myDatabase);
            }
            else {
                entries = getMyFavs(resultSet);
            }

        }
        else {
            entries = savedInstanceState.getParcelableArrayList("FAVORITES");
        }

        //Set up Recycler
        RecyclerView rv = (RecyclerView) findViewById(R.id.favRV);
        assert rv != null;
        adapter = new MyFavAdapter(entries);
        rv.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        rv.setItemAnimator(new DefaultItemAnimator());
        rv.setAdapter(adapter);


    }

    private ArrayList<FavoriteTeam> getMyFavs(Cursor resultSet) {
        ArrayList<FavoriteTeam> teams = new ArrayList<>();
        while (resultSet.moveToNext()) {
            teams.add(new FavoriteTeam(resultSet.getString(0), resultSet.getInt(1) == 1));
        }
        return teams;
    }

    private ArrayList<FavoriteTeam> setUpFavoritesDB(SQLiteDatabase myDatabase) {
        ArrayList<FavoriteTeam> teams = new ArrayList<>();
        Cursor resultSet = myDatabase.rawQuery("Select DISTINCT * from (" +
                "Select homeTeam From NFLGames " +
                "UNION " +
                "Select awayTeam From NFLGames)T ",null);

        while (resultSet.moveToNext()) {
            String name = resultSet.getString(0);
            teams.add(new FavoriteTeam(name, false));
            ContentValues values = new ContentValues();
            values.put("team", name);
            values.put("fav", false);
            myDatabase.insert("MyFavorites", null, values);
        }
        return teams;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        //Save array of entries
        outState.putParcelableArrayList("FAVORITES", entries);
        super.onSaveInstanceState(outState);
    }

    public class MyFavAdapter extends RecyclerView.Adapter<FavoritesActivity.MyFavAdapter.MyFavViewHolder> {
        private ArrayList<FavoriteTeam> mEntries;
        public class MyFavViewHolder extends RecyclerView.ViewHolder {
            public TextView team;
            public CheckBox cb;

            public MyFavViewHolder(View view) {
                super(view);
                team = (TextView) view.findViewById(R.id.favTeamName);
                cb = (CheckBox) view.findViewById(R.id.cb);
            }
        }

        public MyFavAdapter(ArrayList<FavoriteTeam> entries) {
            this.mEntries = entries;
        }

        @Override
        public int getItemViewType(int position) {
            return R.layout.team_entry;
        }

        @Override
        public MyFavViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.team_entry, parent, false);
            return new MyFavViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(final MyFavViewHolder holder, final int position) {
            final FavoriteTeam team = mEntries.get(position);
            holder.team.setText(team.team);
            holder.cb.setChecked(team.favorite);

            holder.cb.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    team.favorite = !team.favorite;
                    entries.set(position, team);
                    mEntries.set(position, team);
                    adapter.notifyItemChanged(position);
                    SQLiteDatabase myDatabase = openOrCreateDatabase("MobAppDev",MODE_PRIVATE,null);
                    ContentValues values = new ContentValues();
                    values.put("fav", team.favorite);
                    String[] args = {team.team};
                    myDatabase.update("MyFavorites", values, "team=?", args);
                }
            });
        }

        @Override
        public int getItemCount() {
            return mEntries.size();
        }
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        setResult(RESULT_OK, intent);
        finish();
    }

}
