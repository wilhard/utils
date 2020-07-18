package com.iamnotme.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.springframework.jdbc.core.namedparam.ParsedSql;

public class DateUtils {
	public static SimpleDateFormat FMT_DATE=new SimpleDateFormat("yyyy-MM-dd");
	/**
     * date2比date1多的天数
     * @param date1    
     * @param date2
     * @return    
     */
    public static int differentDays(Date date1,Date date2){
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(date1);
        
        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(date2);
       int day1= cal1.get(Calendar.DAY_OF_YEAR);
        int day2 = cal2.get(Calendar.DAY_OF_YEAR);
        
        int year1 = cal1.get(Calendar.YEAR);
        int year2 = cal2.get(Calendar.YEAR);
        if(year1 != year2)   //同一年
        {
            int timeDistance = 0 ;
            for(int i = year1 ; i < year2 ; i ++)
            {
                if(i%4==0 && i%100!=0 || i%400==0)    //闰年            
                {
                    timeDistance += 366;
                }
                else    //不是闰年
                {
                    timeDistance += 365;
                }
            }
            
            return timeDistance + (day2-day1) ;
        }
        else    //不同年
        {
            return day2-day1;
        }
    }
    
    /**
     * date前i天
     * @param date1    
     * @param date2
     * @return    
     */
    public static Date dateBefore(Date date,int i){
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.add(Calendar.DAY_OF_MONTH, -i);
		Date old_date = calendar.getTime();
		return old_date;
    }
    public static Date dateBefore(String date,int i){
    	Calendar calendar = Calendar.getInstance();
    	calendar.setTime(DateUtils.parse(date));
    	calendar.add(Calendar.DAY_OF_MONTH, -i);
    	Date old_date = calendar.getTime();
    	return old_date;
    }
    
    public static Date hourBefore(Date date,int i){
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.add(Calendar.HOUR_OF_DAY, -i);
		Date old_date = calendar.getTime();
		return old_date;
    }
    
    public static String format(Date date){
    	String str=FMT_DATE.format(date);
    	return str;
    }
    public static Date parse(String dateString){
    	try {
			return FMT_DATE.parse(dateString);
		} catch (ParseException e) {
			throw new IllegalArgumentException("不是yyyy-MM-dd的日期格式",e);
		}
    }
    public static String yesterdayOf(String dateString){
    	return format(dateBefore(parse(dateString), 1));
    }
    public static String yesterdayOf(Date date){
    	return format(dateBefore(date , 1));
    }
}
