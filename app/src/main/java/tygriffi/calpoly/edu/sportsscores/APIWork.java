package tygriffi.calpoly.edu.sportsscores;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Base64;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Tyler on 11/14/2016.
 */

public class APIWork {
    private SQLiteDatabase myDatabase;
    private Calendar calendar = Calendar.getInstance();
    private RequestQueue queue;

    public APIWork(SQLiteDatabase db, RequestQueue queue) {
        myDatabase = db;
        this.queue = queue;
    }

    public APIWork() {

    }

    public void apiSetup() {
        myDatabase.execSQL("CREATE TABLE IF NOT EXISTS NFLGames(homeTeam VARCHAR, awayTeam VARCHAR," +
                "  homeScore INT, awayScore INT, date DATE, time VARCHAR, inProgress BOOL, " +
                "isPlayed Bool, quarter INT);");
        Cursor resultSet = myDatabase.rawQuery("Select * from NFLGames ORDER BY date ASC",null);
        if (resultSet.getCount() == 0) {
            //Season first game
            performRequestsBetweenDates("NFL", 2016, 8, 8);
        }
        else {
            resultSet.moveToNext();
            String date = resultSet.getString(4);
            if (!date.equals("2016-09-08")) {
                performRequestsBetweenDates("NFL", 2016, 8, 8);
            }
            else {
                resultSet = myDatabase.rawQuery("Select * from NFLGames ORDER BY date DESC",null);
                resultSet.moveToNext();
                String[] dateArr = resultSet.getString(4).split("-");
                performRequestsBetweenDates("NFL", Integer.valueOf(dateArr[0]), Integer.valueOf(dateArr[1]) - 1,
                        Integer.valueOf(dateArr[2]));
            }
        }
    }

    private void performRequestsBetweenDates(String sport, int year, int mon, int day) {
        Calendar lastUpdate = Calendar.getInstance();
        JsonObjectRequest request;
        lastUpdate.set(Calendar.YEAR, year);
        lastUpdate.set(Calendar.MONTH, mon);
        lastUpdate.set(Calendar.DAY_OF_MONTH,day);
        lastUpdate.add(Calendar.DAY_OF_MONTH, -1);
        while (calendar.after(lastUpdate)) {
            if (sport.equals("NFL")) {
                request = getNFLRequestObj(getDateAsAPIString(calendar), "NEW");
            }
            else if (sport.equals("NHL")) {
                request = getNHLRequestObj(getDateAsAPIString(calendar));
            }
            else {
                request = getMLBRequestObj(getDateAsAPIString(calendar));
            }
            queue.add(request);
            calendar.add(Calendar.DAY_OF_MONTH, -1);
        }
    }

    public String getDateAsAPIString(Calendar cal) {
        int mon = cal.get(Calendar.MONTH) + 1;
        String date = Integer.toString(cal.get(Calendar.YEAR));
        date = mon < 10 ? date + "0" + Integer.toString(mon) : date + Integer.toString(mon);
        date = cal.get(Calendar.DAY_OF_MONTH) < 10 ? date + "0" + Integer.toString(cal.get(Calendar.DAY_OF_MONTH))
                : date + Integer.toString(cal.get(Calendar.DAY_OF_MONTH));
        return date;
    }

    public String getDateAsDBString(Calendar cal) {
        int mon = cal.get(Calendar.MONTH) + 1;
        int day = cal.get(Calendar.DAY_OF_MONTH);
        String date = Integer.toString(cal.get(Calendar.YEAR)) + "-";
        date = mon < 10 ?  date + "0" + mon + "-" : date + mon + "-";
        date = day < 10 ? date + "0" + day : date + day;
        return date;
    }

    private JsonObjectRequest getNFLRequestObj(final String date, final String option) {
        String url = "https://www.mysportsfeeds.com/api/feed/pull/nfl/2016-2017-regular/" +
                "scoreboard.json?fordate=" + date;
        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (Request.Method.GET, url, null,
                        new Response.Listener<JSONObject>() {
                            @Override
                            public void onResponse(JSONObject response) {
                                try {
                                    JSONObject sb = response.getJSONObject("scoreboard");
                                    if (sb.has("gameScore")) {
                                        JSONArray parse = sb.getJSONArray("gameScore");
                                        for (int i=0; i < parse.length(); i++) {
                                            JSONObject game = (JSONObject) parse.get(i);
                                            String hometeam = game.getJSONObject("game").getJSONObject("homeTeam").getString("Name");
                                            String awayteam = game.getJSONObject("game").getJSONObject("awayTeam").getString("Name");
                                            String date = game.getJSONObject("game").getString("date");
                                            int awayScore = 0;
                                            int homeScore = 0;
                                            int quarter = -1;
                                            boolean played = game.getBoolean("isCompleted");
                                            boolean inProgress = game.getBoolean("isInProgress");

                                            String time = game.getJSONObject("game").getString("time");

                                            if (inProgress) {
                                                quarter = game.getInt("currentQuarter");
                                                int secLeft = game.getInt("currentQuarterSecondsRemaining");
                                                int min = (int) Math.round(Math.floor(secLeft / 60));
                                                secLeft = secLeft - (min * 60);
                                                String sec = secLeft < 10 ? "0" + secLeft: String.valueOf(secLeft);
                                                time = min + ":" + sec;
                                            }

                                            if (played | inProgress | game.getInt("homeScore") +
                                              game.getInt("awayScore") > 0) {
                                                homeScore = game.getInt("homeScore");
                                                awayScore = game.getInt("awayScore");
                                            }

                                            ContentValues values = new ContentValues();
                                            values.put("homeTeam", hometeam);
                                            values.put("awayTeam", awayteam);
                                            values.put("homeScore", homeScore);
                                            values.put("awayScore", awayScore);
                                            values.put("date", date);
                                            values.put("time", time);
                                            values.put("inProgress", inProgress);
                                            values.put("isPlayed", played);
                                            values.put("quarter", quarter);

                                            if (option.equals("New")) {
                                                myDatabase.insert("NFLGames", null, values);
                                            }
                                            else {
                                                Cursor resultSet = myDatabase.rawQuery("Select * from NFLGames" +
                                                  " WHERE homeTeam = '" + hometeam + "' AND awayTeam = '" + awayteam +
                                                  "' AND date = '" + date + "';",null);
                                                if (resultSet.getCount() == 0) {
                                                    myDatabase.insert("NFLGames", null, values);
                                                }
                                                else {
                                                    String[] args = {hometeam, awayteam, date};
                                                    myDatabase.update("NFLGames", values, "homeTeam=? AND " +
                                                            "awayTeam=? AND date=?", args);
                                                }
                                            }
                                        }
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // TODO Auto-generated method stub
                    }
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                String creds = String.format("%s:%s", "beastmode206", "sportsfreek13");
                String encodedCredentials = Base64.encodeToString(creds.getBytes(), Base64.NO_WRAP);
                headers.put("Authorization", "Basic " + encodedCredentials);

                return headers;
            }
        };
        return jsObjRequest;
    }

    private JsonObjectRequest getNHLRequestObj(String date) {
        return null;
    }

    private JsonObjectRequest getMLBRequestObj(String date) {
        return null;
    }
}
