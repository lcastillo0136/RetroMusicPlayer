/*
 * Copyright (c) 2019 Hemanth Savarala.
 *
 * Licensed under the GNU General Public License v3
 *
 * This is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by
 *  the Free Software Foundation either version 3 of the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 */

package code.name.monkey.retromusic.lyrics;

import android.content.Context;

import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Desc : 歌词解析
 * Author : Lauzy
 * Date : 2017/10/13
 * Blog : http://www.jianshu.com/u/e76853f863a9
 * Email : freedompaladin@gmail.com
 */
public class LrcHelper {

    private static final String CHARSET = "utf-8";
    //[03:56.00][03:18.00][02:06.00][01:07.00]原谅我这一生不羁放纵爱自由
    private static final String LINE_REGEX = "((\\[\\d{1,2}:\\d{1,2}\\.\\d{1,3}])+)(.*)";
    private static final String LINE_REGEX_OLD = "((\\[\\d{1,4}\\.\\d{1,10}])+)(.*)";
    private static final String TIME_REGEX = "\\[(\\d{1,2}):(\\d{1,2})\\.(\\d{1,3})]";
    private static final String TIME_REGEX_OLD = "\\[(\\d{1,4}).(\\d{1,10})]";

    public static List<Lrc> parseLrcFromAssets(Context context, String fileName) {
        try {
            return parseInputStream(context.getResources().getAssets().open(fileName));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static List<Lrc> parseLrcFromFile(File file) {
        try {
            return parseInputStream(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static List<Lrc> parseFromComments(File file) {
        List<Lrc> lrcs = new ArrayList<>();
        try {
            String lyricsTmp = AudioFileIO.read(file).getTagOrCreateDefault().getFirst(FieldKey.COMMENT);
            String lyrics = "";
            if (!lyricsTmp.trim().isEmpty()) {
                for (String s : lyricsTmp.split("\n")) {
                    List<Lrc> lrcList = parseLrc(s);
                    if (lrcList != null && lrcList.size() != 0) {
                        lrcs.addAll(lrcList);
                    }
                }
            }
            sortLrcs(lrcs);
            return lrcs;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static List<Lrc> parseInputStream(InputStream inputStream) {
        List<Lrc> lrcs = new ArrayList<>();
        InputStreamReader isr = null;
        BufferedReader br = null;
        try {
            isr = new InputStreamReader(inputStream, CHARSET);
            br = new BufferedReader(isr);
            String line;
            while ((line = br.readLine()) != null) {
                List<Lrc> lrcList = parseLrc(line);
                if (lrcList != null && lrcList.size() != 0) {
                    lrcs.addAll(lrcList);
                }
            }
            sortLrcs(lrcs);
            return lrcs;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (isr != null) {
                    isr.close();
                }
                if (br != null) {
                    br.close();
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        return lrcs;
    }

    private static void sortLrcs(List<Lrc> lrcs) {
        Collections.sort(lrcs, new Comparator<Lrc>() {
            @Override
            public int compare(Lrc o1, Lrc o2) {
                return (int) (o1.getTime() - o2.getTime());
            }
        });
    }

    private static List<Lrc> parseLrc(String lrcLine) {
        if (lrcLine.trim().isEmpty()) {
            return null;
        }
        List<Lrc> lrcs = new ArrayList<>();

        Matcher oldMatch = Pattern.compile(LINE_REGEX_OLD).matcher(lrcLine);
        if (oldMatch.find()) {
            String time = oldMatch.group(1);
            String content = oldMatch.group(3);
            Matcher timeMatcher = Pattern.compile(TIME_REGEX_OLD).matcher(time);
            if (timeMatcher.find()) {
                String sec = timeMatcher.group(1);
                String mil = timeMatcher.group(2);
                String min = "0";
                if (Long.parseLong(sec) > 60) {
                    min =  (Long.parseLong(sec) / 60) + "";
                    sec = (Long.parseLong(sec) % 60) + "";
                }

                if (mil.length() > 3) {
                    mil = mil.substring(0,3);
                }

                lrcLine = "[" + adjustFormat(Integer.parseInt(min)) + ":" + adjustFormat(Integer.parseInt(sec)) + "." + mil + "] " + content;
            }
        }

        Matcher matcher = Pattern.compile(LINE_REGEX).matcher(lrcLine);
        if (!matcher.find()) {
            return null;
        }

        String time = matcher.group(1);
        String content = matcher.group(3);
        Matcher timeMatcher = Pattern.compile(TIME_REGEX).matcher(time);

        while (timeMatcher.find()) {
            String min = timeMatcher.group(1);
            String sec = timeMatcher.group(2);
            String mil = timeMatcher.group(3);
            Lrc lrc = new Lrc();
            if (content != null && content.length() != 0) {
                if (Integer.parseInt(mil) < 100) {
                    lrc.setTime(Long.parseLong(min) * 60 * 1000 + Long.parseLong(sec) * 1000
                            + Long.parseLong(mil) * 10);
                } else {
                    lrc.setTime(Long.parseLong(min) * 60 * 1000 + Long.parseLong(sec) * 1000
                            + Long.parseLong(mil)
                    );
                }
                lrc.setText(content);
                lrcs.add(lrc);
            }
        }
        return lrcs;
    }

    public static String formatTime(long time) {
        int min = (int) (time / 60000);
        int sec = (int) (time / 1000 % 60);
        return adjustFormat(min) + ":" + adjustFormat(sec);
    }

    private static String adjustFormat(int time) {
        if (time < 10) {
            return "0" + time;
        }
        return time + "";
    }
}
