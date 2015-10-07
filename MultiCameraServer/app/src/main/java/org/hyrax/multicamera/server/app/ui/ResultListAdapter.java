package org.hyrax.multicamera.server.app.ui;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import org.hyrax.multicamera.server.app.R;

import java.util.ArrayList;

/**
 * Created by utsav on 10/6/15.
 */
public class ResultListAdapter extends ArrayAdapter<ResultItem>
{
    private Activity mActivity;
    private ArrayList<ResultItem> mList;
    /**
     * Constructor
     * @param context  The current context.
     */
    public ResultListAdapter(Activity context, ArrayList<ResultItem> mlist)
    {
        super(context, R.layout.result_list_row, mlist);
        mList = mlist;
        mActivity = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        View rowView = convertView;
        if(rowView == null)
        {
            LayoutInflater inflater = mActivity.getLayoutInflater();
            rowView = inflater.inflate(R.layout.result_list_row, null, false);
        }
        TextView tagtext = (TextView) rowView.findViewById(R.id.txt);
        ImageView imageView = (ImageView) rowView.findViewById(R.id.img);
        tagtext.setText(super.getItem(position).device);
        imageView.setImageBitmap(super.getItem(position).thumbnail);
        return rowView;
    }

    public void add(String dev, Bitmap thumb)
    {
        if(mList.size() > 20)
            mList.remove(0);
        mList.add(new ResultItem(thumb, dev));
        this.notifyDataSetChanged();
    }

}
