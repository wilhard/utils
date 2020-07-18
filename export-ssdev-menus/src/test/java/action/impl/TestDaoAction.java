/**
 * @author wangjianbo
 * @created 2020-1-10
 */
package action.impl;

import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Session;

import action.BaseAction;

import com.iamnotme.dao.BaseDAO;
import com.iamnotme.utils.Map2;

import ctd.util.JSONUtils;
import ctd.util.context.Context;
import ctd.util.context.ContextUtils;


/**
 * @author wangjianbo
 *
 */
public class TestDaoAction implements BaseAction {

	public Object doAction(HttpServletRequest request,
			HttpServletResponse response, BaseDAO dao) throws Exception {
		try{
			
			
			Map2 record=Map2.instance()
			.put("EMP_NO", "111")
			.put("EMP_NAME", "对方的")
			.put("WORK_DATE", new Date())
			.put("WORK_CONTENT", "工作内容")
			.put("OPERATE_TIME", new Date())
			.put("JSON", JSONUtils.toString(Map2.instance().put("key", "value")));
			
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
		
		return "success";
	}

}
