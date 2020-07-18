/**
 * @author wangjianbo
 * @created 2018-10-25
 */
package com.iamnotme.utils;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.apache.http.client.ClientProtocolException;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ctd.util.JSONUtils;

/**
 * @author wangjianbo
 *
 */
public class AutoLoggerJob {
	/**
	 * 当前日期前多少天，默认1，今天补昨天的日志
	 */
	private int dayBefore=1;
	
	/**
	 * @param dayBefore the dayBefore to set
	 */
	public void setDayBefore(int dayBefore) {
		this.dayBefore = dayBefore;
	}

	Logger logger = LoggerFactory
			.getLogger(AutoLoggerJob.class);
	public void execute() throws JobExecutionException {
		Date yesterday=DateUtils.dateBefore(new Date(),dayBefore);
		try {
			/**
			 * 判断这一天是不是工作日
			 */
			if(!isWorkday(yesterday)){
				logger.info(DateUtils.format(yesterday)+"非工作日，不需要写日志");
				return;
			}
			AutoLogger.batchWriteLog(yesterday);
		} catch (Exception e) {
			logger.error("日志补录出错", e);
		}
	}
	
	/**
	 * 判断指定日期是否为工作日
	 * @author wangjianbo
	 * @created 2018-11-21
	 * @param yesterday
	 * @return
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 */
	@SuppressWarnings("unchecked")
	private static boolean isWorkday(Date yesterday) throws ClientProtocolException, IOException{
		DateFormat df = new SimpleDateFormat("yyyyMMdd");
		String time = df.format(yesterday);
		//智能节假日 接口API地址
		String json=HttpClientUtil.get("http://tool.bitefu.net/jiari//?d="+time+"&back=json", null, null);
		Map<String,Object> map=JSONUtils.parse(json, Map.class);
		return "0".equals(map.get(time));
	}
	
	public static void main(String[] args) throws ClientProtocolException, IOException {
//		AutoLoggerJob job=new AutoLoggerJob();
//		System.out.println(job.loadConfig());
		
		/*Calendar c=Calendar.getInstance();
		c.setTime(DateUtils.parse("2018-11-18"));
		int weekday=c.get(Calendar.DAY_OF_WEEK);
		System.out.println(weekday);*/
		
//		System.out.println(isWorkday(DateUtils.parse("2019-01-16")));
		
		
	}
}
