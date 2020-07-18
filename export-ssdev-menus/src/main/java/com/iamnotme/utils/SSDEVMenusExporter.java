/**
 * @author wangjianbo
 * @created 2018-10-10
 */
package com.iamnotme.utils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.http.client.ClientProtocolException;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iamnotme.dao.BaseDAO;
import com.iamnotme.dao.DaoException;

import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import ctd.util.context.Context;
import ctd.util.context.ContextUtils;


/**
 * 在线导出创业应用平台菜单目录
 * @author wangjianbo
 * 
 */
public class SSDEVMenusExporter {
	static Logger logger = LoggerFactory
			.getLogger(SSDEVMenusExporter.class);
	public static String jsonRequestUrl="https://web.bsoft.com.cn/portal/*.jsonRequest";
	public static String loadRoles="http://192.168.72.213:8090/platform/logon/myRoles";
	public static String loadApps="http://192.168.72.213:8090/platform/logon/myApps?urt=146514&deep=3";
	
	public static void main(String[] args) {
		Properties cfg = FileUtil.loadProps("config.properties");
		try {
			logonAndBuildHeader(cfg.getProperty("username"), cfg.getProperty("password"));
		} catch (Exception e) {
			logger.error("日志补录错误",e);
		}
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
		Properties cfg = FileUtil.loadProps("config.properties");
		
		//请求参数
		Map<String, Object> params = new LinkedHashMap<String, Object>();
//		params.put("pwd", MD5StringUtil.MD5Encode(pwd));
		params.put("pwd", userId+pwd);
		params.put("uid", userId);
		
		//请求头
		Map<String, String> headers = new HashMap<String, String>();
		headers.put("Content-Type", "application/json");
		headers.put("encoding", "utf-8");
		
		//响应头，接受响应头信息
		Map<String, String> responseHeaders = new HashMap<String, String>();
		
		//加载角色
		Map<String,Object> result=HttpClientUtil
				.post(cfg.getProperty("roles_url")+"?"+Math.random(),
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
					headers.put("Cookie", "JSESSIONID="+sessionId+"");
					break;
				}
			}
		}
		//获取角色列表
		Map<String,Object> body=(Map<String, Object>) result.get("body");
		//获取urt
		int urt = 0 ;
		if(body!=null && body.size()>0){
			List<Map<String,Object>> tokens=(List<Map<String, Object>>) body.get("tokens");
			if(tokens!=null && tokens.size()>0){
				//获取第一个角色urt
				urt=(Integer) tokens.get(0).get("id");
			}
		}
		//加载首页
		String json=HttpClientUtil
		.get(String.format(cfg.getProperty("apps_url"), urt),
				params, headers);
		
		System.out.println(json);
		
		Map<String,Object> tree=JSONUtils.parse(json, Map.class);
		if((Integer)tree.get("code")==200){
			
			HSSFWorkbook wb=new HSSFWorkbook();
			HSSFSheet sheet = wb.createSheet("医院平台");
			
			body=(Map<String, Object>) tree.get("body");
			List<Map<String,Object>> apps=(List<Map<String, Object>>) body.get("apps");
			
			int rownum=0;
			//创建表头
			HSSFRow header = sheet.createRow(rownum++);
			HSSFCell serialNum=header.createCell(0);
			serialNum.setCellValue("序号");
			HSSFCell category1=header.createCell(1);
			category1.setCellValue("大类");
			HSSFCell category2=header.createCell(2);
			category2.setCellValue("小类");
			HSSFCell menu=header.createCell(3);
			menu.setCellValue("菜单");
			HSSFCellStyle style = wb.createCellStyle();
	        //style.setAlignment(HSSFCellStyle.ALIGN_CENTER);
	        style.setVerticalAlignment(HSSFCellStyle.VERTICAL_CENTER);
			for(int k=0;k<apps.size();k++){
				int menu3size=0;
				Map<String,Object> app=apps.get(k);
				String menu1name=(String) app.get("name");
				HSSFRow row = sheet.createRow(rownum++);
				HSSFCell cell0=row.createCell(0);
				cell0.setCellValue(rownum-1);
				HSSFCell cell1=row.createCell(1);
				cell1.setCellValue(menu1name);
				HSSFCell cell2=row.createCell(2);
				HSSFCell cell3=row.createCell(3);
				System.out.print(formatString(menu1name,0, 20));
				List<Map<String,Object>> menu2s=(List<Map<String, Object>>) app.get("items");
				for(int i=0;i<menu2s.size();i++){
					Map<String,Object> menu2=menu2s.get(i);
					String menu2name=(String) menu2.get("name");
					if(i>0){
						row = sheet.createRow(rownum++);
						cell2=row.createCell(2);
						cell2.setCellValue(menu2name);
						cell0=row.createCell(0);
						cell0.setCellValue(rownum-1);
					}else{
						cell2=row.createCell(2);
						cell2.setCellValue(menu2name);
					}
					
					List<Map<String,Object>> menu3s=(List<Map<String, Object>>) menu2.get("modules");
					
					
					if(menu3s==null) {
						menu3size+=1;
						continue;
					}
					
					
					for(int j=0;j<menu3s.size();j++){
						Map<String,Object> menu3=menu3s.get(j);
						String menu3name=(String) menu3.get("name");
						if(j>0){
							row = sheet.createRow(rownum++);
							cell3=row.createCell(3);
							cell3.setCellValue(menu3name);
							cell0=row.createCell(0);
							cell0.setCellValue(rownum-1);
						}else{
							cell3=row.createCell(3);
							cell3.setCellValue(menu3name);
						}
						
					}	
					
					if(menu3s.size()>1) {
						CellRangeAddress region2 = new CellRangeAddress(cell2.getRow().getRowNum(), cell2.getRow().getRowNum()+menu3s.size()-1, 2, 2);
						sheet.addMergedRegion(region2);
						cell2.setCellStyle(style);
					}
					menu3size+=menu3s.size();
				}
				if(menu3size>1) {
					CellRangeAddress region1 = new CellRangeAddress(cell1.getRow().getRowNum(), cell1.getRow().getRowNum()+menu3size-1, 1, 1);
					sheet.addMergedRegion(region1);
					cell1.setCellStyle(style);
				}
			}
			FileOutputStream output=new FileOutputStream("d:\\workbook.xls");
			wb.write(output);
			wb.close();
			output.flush();
		}
		
		return headers;
	}
	static String formatString(String str,int leftSpace,int targetLength){
		StringBuffer sb=new StringBuffer();
		for(int i=0;i<leftSpace;i++){
			sb.append(" ");
		}
		sb.append(str);
		int addLength=targetLength-str.length();
		for(int i=0;i<addLength;i++){
			sb.append(" ");
		}
		return sb.toString();
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
				SSDEVMenusExporter.writeByDate((String)emp.get("EMP_NO"), (String)emp.get("PASSWORD"),(String)emp.get("EMP_NAME"), yesterday);
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
