/**
 * @author wangjianbo
 * @created 2020-1-10
 */
package action.impl;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import action.BaseAction;

import com.iamnotme.dao.BaseDAO;
import com.iamnotme.utils.AutoLogger;


/**
 * @author wangjianbo
 *
 */
public class TestAction implements BaseAction {

	public Object doAction(HttpServletRequest request,
			HttpServletResponse response, BaseDAO dao) throws Exception {
		
		AutoLogger.batchWriteLog(null);		
		
		return "success";
	}

}
