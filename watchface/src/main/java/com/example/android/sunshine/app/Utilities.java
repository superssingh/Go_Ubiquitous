/*
 * Created by Santosh Kumar Singh on 18/04/2016 on 6:18 PM
 * Copyright (c) 2016 All rights reserved.
 *
 *  Last modified 11/28/16 7:17 PM
 *
 */

package com.example.android.sunshine.app;

/**
 * Created by Stark on 11/28/2016.
 */

public class Utilities extends MyWatchFace {
    public Utilities() {
    }

    public static int getWeatherIcon(int weatherId) {
        // Based on weather code data found at:
        // http://bugs.openweathermap.org/projects/api/wiki/Weather_Condition_Codes
        if (weatherId >= 200 && weatherId <= 232) {
            return R.drawable.ic_storm;
        } else if (weatherId >= 300 && weatherId <= 321) {
            return R.drawable.ic_light_rain;
        } else if (weatherId >= 500 && weatherId <= 504) {
            return R.drawable.ic_rain;
        } else if (weatherId == 511) {
            return R.drawable.ic_snow;
        } else if (weatherId >= 520 && weatherId <= 531) {
            return R.drawable.ic_rain;
        } else if (weatherId >= 600 && weatherId <= 622) {
            return R.drawable.ic_snow;
        } else if (weatherId >= 701 && weatherId <= 761) {
            return R.drawable.ic_fog;
        } else if (weatherId == 761 || weatherId == 781) {
            return R.drawable.ic_storm;
        } else if (weatherId == 800) {
            return R.drawable.ic_clear;
        } else if (weatherId == 801) {
            return R.drawable.ic_light_clouds;
        } else if (weatherId >= 802 && weatherId <= 804) {
            return R.drawable.ic_cloudy;
        }
        return R.drawable.ic_status;
    }

    public static String getWeatherName(int weatherId) {
        // Based on weather code data found at:
        // http://bugs.openweathermap.org/projects/api/wiki/Weather_Condition_Codes
        if (weatherId >= 200 && weatherId <= 232) {
            return "Thunderstorm";
        } else if (weatherId >= 300 && weatherId <= 321) {
            return "Drizzle";
        } else if (weatherId >= 500 && weatherId <= 504) {
            return "Rain";
        } else if (weatherId == 511) {
            return "Snow";
        } else if (weatherId >= 520 && weatherId <= 531) {
            return "Rain";
        } else if (weatherId >= 600 && weatherId <= 622) {
            return "Snow";
        } else if (weatherId >= 701 && weatherId <= 761) {
            return "Foggy";
        } else if (weatherId == 761 || weatherId == 781) {
            return "Storm";
        } else if (weatherId == 800) {
            return "Clear Sky";
        } else if (weatherId == 801) {
            return "Light Clouds";
        } else if (weatherId >= 802 && weatherId <= 804) {
            return "Cloudy";
        }
        return null;
    }

    public static String month(int month) {
        if (month >= 0 && month <= 11) {
            if (month == 0)
                return "Jan";
            else if (month == 1)
                return "Feb";
            else if (month == 2)
                return "Mar";
            else if (month == 3)
                return "Apr";
            else if (month == 4)
                return "May";
            else if (month == 5)
                return "Jun";
            else if (month == 6)
                return "Jul";
            else if (month == 7)
                return "Aug";
            else if (month == 8)
                return "Sep";
            else if (month == 9)
                return "Oct";
            else if (month == 10)
                return "Nov";
            else if (month == 11)
                return "Dec";
        }
        return null;
    }

    public static String day(int day) {
        if (day >= 1 && day <= 7) {
            if (day == 1)
                return "Sunday";
            else if (day == 2)
                return "Monday";
            else if (day == 3)
                return "Tuesday";
            else if (day == 4)
                return "Wednesday";
            else if (day == 5)
                return "Thursday";
            else if (day == 6)
                return "Friday";
            else if (day == 7)
                return "Saturday";
        }
        return null;
    }

}
