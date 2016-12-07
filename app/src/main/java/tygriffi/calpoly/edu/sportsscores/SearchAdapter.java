package tygriffi.calpoly.edu.sportsscores;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by Tyler on 11/29/2016.
 */

public class SearchAdapter extends android.support.v4.widget.CursorAdapter {

    public SearchAdapter(Context context, Cursor cursor) {
        super(context, cursor, false);

    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.search_item, parent, false);

        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        TextView text = (TextView) view.findViewById(R.id.itemText);
        text.setText(cursor.getString(1));
    }
}
