/**
 * @author wangjianbo
 * @created 2018-9-29
 */
package com.iamnotme.web;

import java.io.PrintWriter;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.iamnotme.utils.AutoLogger;

import ctd.util.JSONUtils;
import ctd.util.ServletUtils;

/**
 * @author wangjianbo
 *
 */
@Controller("deleteLogController")
public class DeleteLogController {
	@RequestMapping(value = "/delete", method = {RequestMethod.GET,RequestMethod.POST})
    public void execute(HttpServletRequest request, HttpServletResponse response){
		response.setCharacterEncoding(ServletUtils.DEFAULT_ENCODING);
		PrintWriter out=null;
		try{
			out=response.getWriter();
			String rzid=(String) request.getParameter("rzid");
			String sessionId=(String) request.getParameter("sessionId");
			if(StringUtils.isBlank(rzid) || StringUtils.isBlank(sessionId)){
				out.write("three parameters are required:rzid,sessionId");
				return;
			}
			AutoLogger.deleteLog(rzid, sessionId);
			out.write("delete log success");
		}catch(Exception e){
			e.printStackTrace();
			if(out!=null){
				out.write("delete log failed:"+e);
			}
		}finally{
			
		}
	}
	
	
}
