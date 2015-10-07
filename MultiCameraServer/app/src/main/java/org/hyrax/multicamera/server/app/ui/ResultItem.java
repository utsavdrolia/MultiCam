package org.hyrax.multicamera.server.app.ui;

import android.graphics.Bitmap;

/**
 * Created by utsav on 10/6/15.
 */
public class ResultItem
{
    public Bitmap thumbnail;
    public String device;

    public ResultItem(Bitmap thumbnail, String device)
    {
        this.thumbnail = thumbnail;
        this.device = device;
    }
}
