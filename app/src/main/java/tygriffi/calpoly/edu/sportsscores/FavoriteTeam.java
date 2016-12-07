package tygriffi.calpoly.edu.sportsscores;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Tyler on 11/23/2016.
 */

public class FavoriteTeam implements Parcelable {
    public String team;
    public boolean favorite;

    public FavoriteTeam(String team, boolean cb) {
        this.team = team;
        favorite = cb;
    }

    protected FavoriteTeam(Parcel in) {
        team = in.readString();
        favorite = in.readByte() == 1;
    }

    public static final Creator<FavoriteTeam> CREATOR = new Creator<FavoriteTeam>() {
        @Override
        public FavoriteTeam createFromParcel(Parcel in) {
            return new FavoriteTeam(in);
        }

        @Override
        public FavoriteTeam[] newArray(int size) {
            return new FavoriteTeam[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(team);
        dest.writeByte((byte) (favorite ? 1 : 0));
    }
}
