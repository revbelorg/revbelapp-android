package com.news.revbel.utilities;

import android.graphics.Bitmap;

import java.io.ByteArrayOutputStream;

import javax.activation.DataHandler;
import javax.mail.util.ByteArrayDataSource;


public class Attachment {
    String fileName;
    private Bitmap bitmap;

    public Attachment(String fileName, Bitmap bitmap) {
        this.fileName = fileName + ".jpg";

        this.bitmap = bitmap;
    }

    DataHandler getDataHandler() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream);
        byte[] byteArray = stream.toByteArray();

        return new DataHandler(new ByteArrayDataSource(byteArray, "image/jpeg"));
    }
}