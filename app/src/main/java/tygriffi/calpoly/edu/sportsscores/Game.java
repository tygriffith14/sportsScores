package tygriffi.calpoly.edu.sportsscores;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Tyler on 10/30/2016.
 */

public class Game implements Parcelable {
    public String league;
    public String hometeam;
    public String awayteam;
    public int homeScore;
    public int awayScore;
    public String date;
    public String time;
    public boolean played;
    public boolean inProgress;
    public int quarter;

    public Game(String league, String hometeam, String awayteam, int homeScore, int awayScore,
                String date, String time, boolean played, boolean inProgress, int quarter) {
        this.league = league;
        this.hometeam = hometeam;
        this.awayteam = awayteam;
        this.homeScore = homeScore;
        this.awayScore = awayScore;
        this.date = date;
        this.time = time;
        this.played = played;
        this.inProgress = inProgress;
        this.quarter = quarter;
    }

    protected Game(Parcel in) {
        league = in.readString();
        hometeam = in.readString();
        awayteam = in.readString();
        homeScore = in.readInt();
        awayScore = in.readInt();
        date = in.readString();
        time = in.readString();
        played = in.readByte() == 1;
        inProgress = in.readByte() == 1;
        quarter = in.readInt();
    }

    public static final Creator<Game> CREATOR = new Creator<Game>() {
        @Override
        public Game createFromParcel(Parcel in) {
            String league = in.readString();
            String homeTeam = in.readString();
            String awayTeam = in.readString();
            int homeScore = in.readInt();
            int awayScore = in.readInt();
            String date = in.readString();
            String time = in.readString();
            boolean[] bool = new boolean[2];
            in.readBooleanArray(bool);
            int quarter = in.readInt();
            return new Game(league, homeTeam, awayTeam, homeScore, awayScore, date, time, bool[0],
                    bool[1], quarter);
        }

        @Override
        public Game[] newArray(int size) {
            return new Game[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(league);
        dest.writeString(hometeam);
        dest.writeString(awayteam);
        dest.writeInt(homeScore);
        dest.writeInt(awayScore);
        dest.writeString(date);
        dest.writeString(time);
        boolean[] bool = {played, inProgress};
        dest.writeBooleanArray(bool);
        dest.writeInt(quarter);
    }
}
