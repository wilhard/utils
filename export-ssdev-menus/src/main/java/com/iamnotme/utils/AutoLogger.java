/**
 * @author wangjianbo
 * @created 2018-10-10
 */
package com.iamnotme.utils;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.client.ClientProtocolException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.iamnotme.dao.BaseDAO;
import com.iamnotme.dao.DaoException;

import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import ctd.util.MD5StringUtil;
import ctd.util.context.Context;
import ctd.util.context.ContextUtils;


/**
 * @author wangjianbo
 * 
 */
public class AutoLogger {
	static Logger logger = LoggerFactory
			.getLogger(AutoLogger.class);
	public static String jsonRequestUrl="https://web.bsoft.com.cn/portal/*.jsonRequest";
	public static String loadRoles="https://web.bsoft.com.cn/portal/logon/myRoles";
	public static String loadApps="https://web.bsoft.com.cn/portal/logon/myApps?urt=%s&deep=3&number=";
	
	public static void main(String[] args) {
		try {
//			writeByDate("6102", "asd123","王建波",DateUtils.parse("2020-01-09"));
			deleteLog("6076836","BB0E89BBA4B34FDE70DF3BB0697840B8");
		} catch (Exception e) {
			logger.error("日志补录错误",e);
		}
	}
	public static void deleteLog(String rzid,String sessionId) throws ClientProtocolException, IOException{
		Map<String, String> headers = new HashMap<String, String>();
		headers.put("Content-Type", "application/json");
		headers.put("encoding", "utf-8");
		headers.put("Cookie", "JSESSIONID="+sessionId);
		String json=String.format("{\"schema\":\"portal.main.entry.KQ_GZRZ\",\"serviceId\":\"portal.WorkLogService\",\"method\":\"deleteGzrz\",\"body\":[%s]}", rzid);
		Map<String, Object> result=HttpClientUtil
				.post(jsonRequestUrl,json, headers,null);
		int kqid=(Integer) ((Map<String,Object>)result.get("body")).get("kqid");
		json=String.format("{\"schema\":\"portal.main.entry.T_KQB\",\"serviceId\":\"portal.CheckWorkService\",\"method\":\"updateGZRZ\",\"body\":[%s]}", kqid);
		result=HttpClientUtil.post(jsonRequestUrl,json, headers,null);
		System.out.println(result);
	}

	/**
	 * 获取指定时间段内最近一次日志信息
	 * @author wangjianbo
	 * @created 2020-1-13
	 * @param userId
	 * @param dateStart
	 * @param dateEnd
	 * @param headers
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	private static Map<String,Object> getLogByDateRange(String userId,Date dateStart,Date dateEnd,Map<String, String> headers) throws ClientProtocolException, IOException{
		//查询是否有考勤
		String json=String.format("{\"serviceId\":\"portal.CheckWorkViewService\",\"method\":\"find\",\"pageSize\":25,\"body\":[{\"pageNo\":1,\"pageSize\":25,\"cnds\":[\"and\",[\"eq\",[\"$\",\"yggh\"],[\"s\",\"%s\"]],[\"and\",[\"ge\",[\"$\",\"str(kqrq,'yyyy-MM-dd')\"],[\"s\",\"%s\"]],[\"le\",[\"$\",\"str(kqrq,'yyyy-MM-dd HH24:mi:ss')\"],[\"s\",\"%s 23:59:59\"]]]],\"orderBy\":\"kqrq desc\"}],\"page\":1,\"start\":0,\"limit\":25}", userId,DateUtils.format(dateStart),DateUtils.format(dateEnd));
		Map<String, Object> result=HttpClientUtil
				.post(jsonRequestUrl,json, headers,null);
		Map<String,Object> body=(Map<String, Object>) result.get("body");
		if(body!=null){
			List<Map<String,Object>> items=(List<Map<String, Object>>) body.get("items");
			if(items!=null && items.size()>0){
				for(Map<String,Object> item:items){
					if(item.get("gzrz")!=null){
						return item;
					}
				}
			}
			
		}
		
		return null;
		
	}
	/**
	 * 调用登陆接口，并创建通用的请求头信息
	 * @author wangjianbo
	 * @created 2019-1-3
	 * @param userId
	 * @param pwd
	 * @return
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, String> logonAndBuildHeader(String userId,String pwd) throws ClientProtocolException, IOException{
		//请求参数
		Map<String, Object> params = new LinkedHashMap<String, Object>();
		params.put("pwd", MD5StringUtil.MD5Encode(pwd));
		params.put("uid", userId);
		
		//请求头
		Map<String, String> headers = new HashMap<String, String>();
		headers.put("Content-Type", "application/json");
		headers.put("encoding", "utf-8");
		
		//响应头，接受响应头信息
		Map<String, String> responseHeaders = new HashMap<String, String>();
		
		//加载角色
		Map<String,Object> result=HttpClientUtil
				.post(loadRoles+"?"+Math.random(),
						params, headers,responseHeaders);
		//获取sessionId
		String sessionId="";
		if(responseHeaders.containsKey("Set-Cookie")){
			String cookieStr=responseHeaders.get("Set-Cookie");
			for(String entrys:cookieStr.split(";")){
				String[] arr=entrys.split("=");
				if(arr.length==2 && arr[0].equals("JSESSIONID")){
					sessionId=arr[1];
					//放入请求头map
					headers.put("Cookie", "JSESSIONID="+sessionId+";6102=%u738B%u5EFA%u6CE2@undefined");
					break;
				}
			}
		}
		//获取角色列表
		List<Map<String,Object>> body=(List<Map<String, Object>>) result.get("body");
		//获取urt
		int urt = 0 ;
		if(body!=null && body.size()>0){
			//获取第一个角色urt
			urt=(Integer) body.get(0).get("id");
		}
		
		//加载首页
		HttpClientUtil
		.get(String.format(loadApps+Math.random(), urt),
				params, headers);
		return headers;
	}
	
	/**
	 * 补录指定日期日志
	 * @author wangjianbo
	 * @created 2018-10-25
	 * @param userId
	 * @param pwd
	 * @param date
	 * @param content
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws DaoException 
	 */
	@SuppressWarnings("unchecked")
	public static void writeByDate(String userId,String pwd,String name,Date date) throws ClientProtocolException, IOException, DaoException{
		//请求头
		Map<String, String> headers = logonAndBuildHeader(userId, pwd);
		
		//查询这天是否写过日志
		Map<String, Object> logInfo=getLogByDateRange(userId, date,date,headers);//日志内容
		if(logInfo!=null){
			String msg=String.format("工号:%s，日期:%s，已写日志，不需要补录！", userId,DateUtils.format(date));
			logger.info(msg);
			return;
		}
		
		//需要补录日志
		//查询该工号前{days}天有没有写过日志，并返回最近一次日志信息
		int days=10;
		logInfo=getLogByDateRange(userId,DateUtils.dateBefore(date, days), DateUtils.dateBefore(date, 1), headers);
		if(logInfo==null){
			String msg=String.format("工号:%s，过去%s天都没写过日志，无法自动补录！", userId,days);
			logger.error(msg);
			return;
		}
		
		String json;
		Map<String, Object> body;
		
		//获取最近一次日志内容
		json=String.format("{\"serviceId\":\"portal.WorkLogViewService\",\"method\":\"findBykqid\",\"schema\":\"portal.main.entry.KQ_GZRZ_View\",\"pageSize\":25,\"body\":[%s],\"pageNo\":1}", logInfo.get("id"));
		body=(Map<String, Object>) doPost(headers,userId,json);
		if(body==null){
			return;
		}
		List<Map<String, Object>> items=(List<Map<String, Object>>) body.get("items");
		//日志内容
		String content=(String) items.get(0).get("gzrz");
		content=content.replace("\n","\\n").replace("\r","\\r");
		
		
		//选择要补录的日期，点补录
		
		//1
		json=String.format("{\"serviceId\":\"portal.CheckWorkService\",\"method\":\"findByYgghAndKqrq\",\"body\":[\"%s\",\"%s\",2,\"%s\"]}", userId,DateUtils.format(date),DateUtils.format(date));
		body=(Map<String, Object>) doPost(headers,userId,json);
		if(body==null){
			return;
		}
		
		//日志项目ID
		String projectId=(String)body.get("projectId");
		//区域编码
		String area=(String)body.get("area");
		//考勤ID
		int kqid=(Integer) ((Map<String,Object>)body).get("id");
		
		//2
		json=String.format("{\"serviceId\":\"portal.WorkLogService\",\"method\":\"findListBykqid\",\"body\":[%s]}", kqid);
		doPost(headers, userId, json);
		
		//3
		json=String.format("{\"schema\":\"portal.main.entry.T_KQB\",\"serviceId\":\"portal.CheckWorkService\",\"method\":\"findByYgghAndKqrq\",\"body\":[\"%s\",\"%s\",2,\"%s\"]}", userId,DateUtils.format(date),DateUtils.format(date));
		doPost(headers, userId, json);

		//4
		json=String.format("{\"serviceId\":\"portal.WorkLogViewService\",\"method\":\"findBykqid\",\"schema\":\"portal.main.entry.KQ_GZRZ_View\",\"pageSize\":25,\"body\":[%s],\"pageNo\":1}", kqid);
		doPost(headers, userId, json);
		
		
		//录入日志，点击保存
		
		//1
		json=String.format("{\"schema\":\"portal.main.entry.T_KQB\",\"serviceId\":\"portal.CheckWorkService\",\"method\":\"saveKqb\",\"body\":[{\"yggh\":\"%s\",\"gzrz\":\"\",\"flag\":5,\"housename\":\"\",\"rzqk\":\"1\",\"zfid\":\"\",\"kqrq\":\"%s\",\"ccbz\":0},{\"gzrz\":\"%s\",\"gsxm\":\"%s\",\"gzsj\":8,\"id\":\"\",\"xmbl\":\"0\",\"xmbm\":\"%s\",\"xmlb\":\"1\",\"flag\":5},2]}", userId,DateUtils.format(date),content,projectId,area);
		doPost(headers, userId, json);
		
		//2
		json=String.format("{\"schema\":\"portal.main.entry.T_KQB\",\"serviceId\":\"portal.CheckWorkService\",\"method\":\"findByYgghAndKqrq\",\"body\":[\"%s\",\"%s\",2,\"%s\"]}", userId,DateUtils.format(date),DateUtils.format(date));
		doPost(headers, userId, json);
		
		//3
		json=String.format("{\"serviceId\":\"portal.CheckWorkViewService\",\"method\":\"find\",\"pageSize\":25,\"body\":[{\"pageNo\":1,\"pageSize\":25,\"cnds\":[\"and\",[\"eq\",[\"$\",\"yggh\"],[\"s\",\"%s\"]],[\"and\",[\"ge\",[\"$\",\"str(kqrq,'yyyy-MM-dd')\"],[\"s\",\"%s\"]],[\"le\",[\"$\",\"str(kqrq,'yyyy-MM-dd HH24:mi:ss')\"],[\"s\",\"%s 23:59:59\"]]]],\"orderBy\":\"kqrq desc\"}],\"page\":1,\"start\":0,\"limit\":25}", userId,DateUtils.format(date),DateUtils.format(date));
		doPost(headers, userId, json);
		
		//4
		json=String.format("{\"serviceId\":\"portal.WorkLogViewService\",\"method\":\"findBykqid\",\"schema\":\"portal.main.entry.KQ_GZRZ_View\",\"pageSize\":25,\"body\":[%s],\"pageNo\":1}", kqid);
		body=(Map<String, Object>) doPost(headers, userId, json);
		
		String msg=String.format("工号:%s，姓名:%s，日期：%s，日志补录成功,%s", userId,name,DateUtils.format(date),JSONUtils.toString(body.get("items")));
		logger.info(msg);
		try{
			BaseDAO dao=new BaseDAO();
			Map2 record=Map2.instance()
			.put("EMP_NO", userId)
			.put("EMP_NAME", name)
			.put("WORK_DATE", date)
			.put("WORK_CONTENT", content)
			.put("OPERATE_TIME", new Date())
			.put("JSON", JSONUtils.toString(body));
			
			dao.doSqlExecute("insert into LOG_REPAIR_RECORD values(:EMP_NO ,:EMP_NAME ,:WORK_DATE ,:WORK_CONTENT ,:OPERATE_TIME ,:JSON )", record);
		
		}finally{
			Context ctx=ContextUtils.getContext();
			if(ctx.has(Context.DB_SESSION)){
				Session ss = (Session) ctx.get(Context.DB_SESSION);
				if(ss != null && ss.isOpen()){
					ss.close();
				}
			}
			ContextUtils.clear();
		}
	}

	/**
	 * 批量补录日志
	 * @author wangjianbo
	 * @created 2020-1-13
	 * @param yesterday 要补录的日期
	 */
	@SuppressWarnings("unchecked")
	public static void batchWriteLog(Date yesterday){
		Context ctx=ContextUtils.getContext();
		try{
			if(!ctx.has(Context.DB_SESSION)){
				SessionFactory sf = AppContextHolder.getBean(AppContextHolder.DEFAULT_SESSION_FACTORY, SessionFactory.class);
				ctx.put(Context.DB_SESSION, sf.openSession());
			}
			BaseDAO dao=new BaseDAO();
			List<Map<String,Object>> persons=dao.doSqlQueryMap("select * from LOG_EMP where status=1");
			if(persons.size()==0){
				logger.error("LOG_EMP人员信息表未正确配置");
				return;
			}
			for(Map<String,Object> emp:persons){
				AutoLogger.writeByDate((String)emp.get("EMP_NO"), (String)emp.get("PASSWORD"),(String)emp.get("EMP_NAME"), yesterday);
			}
		}catch(Exception e){
			logger.error("批量补录日志错误",e);
		}finally{
			if(ctx.has(Context.DB_SESSION)){
				Session ss = (Session) ctx.get(Context.DB_SESSION);
				if(ss != null && ss.isOpen()){
					ss.close();
				}
			}
			ContextUtils.clear();
		}
	}
	
	private static Object doPost(Map<String, String> headers,String userId,String json) 
			throws ClientProtocolException, IOException{
		Map<String, Object> result=HttpClientUtil
				.post(jsonRequestUrl,json, headers,null);
		if((Integer)result.get("code")>200){
			String msg=String.format("工号:%s，日志补录出错，返回信息：%s！", userId,result.get("msg"));
			logger.error(msg);
			return null;
		}
		return result.get("body");
	}
}
