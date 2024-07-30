package com.lightweight.lightweightrangeslider.Utils;

/**
 * Created by usrc on 17. 12. 28.
 */

import android.content.Context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by regar007.
 *
 * <p>
 *     This is created to read a read text from a file on system.
 * </p>
 */

public class RawResourceReader
{
    /**
     * Use this function to read text from a file on system.
     * @param context
     * @param resourceId
     * @return string.
     */
    public static String readTextFileFromRawResource(final Context context,
                                                     final int resourceId)
    {
        final InputStream inputStream = context.getResources().openRawResource(
                resourceId);
        final InputStreamReader inputStreamReader = new InputStreamReader(
                inputStream);
        final BufferedReader bufferedReader = new BufferedReader(
                inputStreamReader);

        String nextLine;
        final StringBuilder body = new StringBuilder();

        try
        {
            while ((nextLine = bufferedReader.readLine()) != null)
            {
                body.append(nextLine);
                body.append('\n');
            }
        }
        catch (IOException e)
        {
            return null;
        }

        return body.toString();
    }
}
