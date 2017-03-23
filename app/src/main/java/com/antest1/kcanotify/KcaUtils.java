package com.antest1.kcanotify;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.common.io.ByteStreams;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;


import org.apache.commons.httpclient.ChunkedInputStream;
import org.apache.commons.httpclient.ChunkedOutputStream;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static com.antest1.kcanotify.KcaApiData.kcShipTranslationData;
import static com.antest1.kcanotify.KcaApiData.loadItemTranslationDataFromAssets;
import static com.antest1.kcanotify.KcaApiData.loadQuestInfoDataFromAssets;
import static com.antest1.kcanotify.KcaApiData.loadShipTranslationDataFromAssets;
import static com.antest1.kcanotify.KcaConstants.KC_PACKAGE_NAME;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_LANGUAGE;

public class KcaUtils {
    public static String getStringFromException(Exception ex) {
        StringWriter errors = new StringWriter();
        ex.printStackTrace(new PrintWriter(errors));
        return errors.toString().replaceAll("\n", " / ").replaceAll("\t", "");
    }

    public static String joinStr(List<String> list, String delim) {
        String resultStr = "";
        int i;
        for (i = 0; i < list.size() - 1; i++) {
            resultStr = resultStr.concat(list.get(i));
            resultStr = resultStr.concat(delim);
        }
        resultStr = resultStr.concat(list.get(i));
        return resultStr;
    }

    public static void writeCacheData(Context ctx, byte[] data, String filename) throws IOException {
        FileOutputStream fos = ctx.openFileOutput(filename, Context.MODE_PRIVATE);
        fos.write(data);
        fos.close();
    }

    public static String getStringPreferences(Context ctx, String key) {
        SharedPreferences pref = ctx.getSharedPreferences("pref", Context.MODE_PRIVATE);
        return pref.getString(key, "");
    }

    public static Boolean getBooleanPreferences(Context ctx, String key) {
        SharedPreferences pref = ctx.getSharedPreferences("pref", Context.MODE_PRIVATE);
        return pref.getBoolean(key, false);
    }

    // 값 저장하기
    public static void setPreferences(Context ctx, String key, Object value) {
        SharedPreferences pref = ctx.getSharedPreferences("pref", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        if (value instanceof String) {
            editor.putString(key, (String) value);
        } else if (value instanceof Boolean) {
            editor.putBoolean(key, (Boolean) value);
        } else if (value instanceof Integer) {
            editor.putInt(key, (Integer) value);
        } else {
            editor.putString(key, value.toString());
        }
        editor.commit();
    }

    public static JsonObject readCacheData(Context ctx, String filename) {
        try {
            JsonObject data = new JsonObject();
            JsonParser parser = new JsonParser();

            FileInputStream fis = ctx.openFileInput(filename);
            InputStreamReader fisr = new InputStreamReader(fis);
            JsonElement jsonElement = parser.parse(fisr);
            data = jsonElement.getAsJsonObject();
            return data;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            //new retrieveApiStartData().execute("", "down", "");
            return null;
        }
    }

    public static byte[] gzipcompress(String value) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        GZIPOutputStream gzipOutStream = new GZIPOutputStream(
                new BufferedOutputStream(byteArrayOutputStream));
        gzipOutStream.write(value.getBytes());
        gzipOutStream.finish();
        gzipOutStream.close();

        return byteArrayOutputStream.toByteArray();
    }

    public static byte[] gzipdecompress(byte[] contentBytes) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ByteStreams.copy(new GZIPInputStream(new ByteArrayInputStream(contentBytes)), out);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return out.toByteArray();
    }

    private static int bytetoint(byte[] arr) {
        int csize = 0;
        for (int i = 0; i < arr.length; i++) {
            csize = csize << 4;
            if (arr[i] >= 0x30 && arr[i] <= 0x39) {
                csize += arr[i] - 0x30; // (0x30 = '0')
            } else if (arr[i] >= 0x61 && arr[i] <= 0x66) {
                csize += arr[i] - 0x61 + 0x0a; // (0x61 = 'a')
            } else if (arr[i] >= 0x41 && arr[i] <= 0x46) {
                csize += arr[i] - 0x41 + 0x0a; // (0x41 = 'A')
            }
        }
        return csize;
    }

    public static byte[] unchunkdata(byte[] contentBytes) throws IOException {
        byte[] unchunkedData = null;
        byte[] buffer = new byte[1024];
        ByteArrayInputStream bis = new ByteArrayInputStream(contentBytes);
        ChunkedInputStream cis = new ChunkedInputStream(bis);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        int read = -1;
        while ((read = cis.read(buffer)) != -1) {
            bos.write(buffer, 0, read);
        }
        unchunkedData = bos.toByteArray();
        bos.close();

        return unchunkedData;
    }

    public static String byteArrayToHex(byte[] a) {
        StringBuilder sb = new StringBuilder();
        for (final byte b : a)
            sb.append(String.format("%02x ", b & 0xff));
        return sb.toString();
    }

    public static boolean[] makeExcludeFlag(int[] list) {
        boolean[] flag = {false, false, false, false, false, false};
        for (int i = 0; i < list.length; i++) {
            flag[list[i]] = true;
        }
        return flag;
    }

    public static boolean isPackageExist(Context context, String name) {
        boolean isExist = false;

        PackageManager pkgMgr = context.getPackageManager();
        List<ResolveInfo> mApps;
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        mApps = pkgMgr.queryIntentActivities(mainIntent, 0);

        try {
            for (int i = 0; i < mApps.size(); i++) {
                if (mApps.get(i).activityInfo.packageName.startsWith(name)) {
                    isExist = true;
                    break;
                }
            }
        } catch (Exception e) {
            isExist = false;
        }
        return isExist;
    }

    public static Intent getKcIntent(Context context) {
        Intent kcIntent;
        if (isPackageExist(context, KC_PACKAGE_NAME)) {
            kcIntent = context.getPackageManager().getLaunchIntentForPackage(KC_PACKAGE_NAME);
            kcIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            return kcIntent;
        } else {
            return null;
        }
    }

    public static Context getContextWithLocale(Context ac, Context bc) {
        Locale locale;
        String[] pref_locale = getStringPreferences(ac, PREF_KCA_LANGUAGE).split("-");
        if (pref_locale[0].equals("default")) {
            locale = KcaApplication.defaultLocale;
        } else {
            locale = new Locale(pref_locale[0], pref_locale[1]);
        }
        Configuration configuration = new Configuration(ac.getResources().getConfiguration());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            configuration.setLocale(locale);
            return bc.createConfigurationContext(configuration);
        } else {
            configuration.locale = locale;
            DisplayMetrics metrics = new DisplayMetrics();
            bc.getResources().updateConfiguration(configuration, bc.getResources().getDisplayMetrics());
            return bc;
        }
    }

    public static String getStringWithLocale(Context ac, Context bc, int id) {
        return getContextWithLocale(ac, bc).getString(id);
    }

    public static void playNotificationSound(MediaPlayer mediaPlayer, Context context, Uri uri) {
        try {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
                mediaPlayer.reset();
            }
            mediaPlayer.setDataSource(context, uri);
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_NOTIFICATION);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (IllegalArgumentException | SecurityException | IllegalStateException | IOException e) {
            e.printStackTrace();
        }
    }

    public static int getNotificationId(int type, int n) {
        return n + 1000 * type;
    }

    public static int getId(String resourceName, Class<?> c) {
        try {
            Field idField = c.getDeclaredField(resourceName);
            return idField.getInt(idField);
        } catch (Exception e) {
            throw new RuntimeException("No resource ID found for: "
                    + resourceName + " / " + c, e);
        }
    }

    public static int adjustAlpha(int color, float factor) {
        int alpha = Math.round(Color.alpha(color) * factor);
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        return Color.argb(alpha, red, green, blue);
    }

    // True: latest, False: need to update
    public static boolean compareVersion(String version_current, String version_default) {
        if (version_current.equals(version_default)) return true;
        String[] current_split = version_current.replace("r",".0.").split("\\.");
        String[] default_split = version_default.replace("r",".0.").split("\\.");
        int min_length = Math.min(current_split.length, default_split.length);
        for (int i = 0; i < min_length; i++) {
            if (Integer.parseInt(current_split[i]) > Integer.parseInt(default_split[i])) return true;
            else if (Integer.parseInt(current_split[i]) < Integer.parseInt(default_split[i])) return false;
        }
        return current_split.length > default_split.length;
    }
}
