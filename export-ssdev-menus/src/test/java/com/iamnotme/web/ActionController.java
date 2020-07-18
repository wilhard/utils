/**
 * @author wangjianbo
 * @created 2018-9-29
 */
package com.iamnotme.web;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import action.BaseAction;

import com.iamnotme.dao.BaseDAO;

import ctd.account.AccountCenter;
import ctd.account.UserRoleToken;
import ctd.account.user.User;
import ctd.mvc.controller.util.UserRoleTokenUtils;
import ctd.util.AppContextHolder;
import ctd.util.ServletUtils;
import ctd.util.context.Context;
import ctd.util.context.ContextUtils;

/**
 * @author wangjianbo
 *
 */
@Controller("TempController")
public class ActionController {
	Logger logger=LoggerFactory.getLogger(ActionController.class);
	@RequestMapping(value = "/action", method = {RequestMethod.GET,RequestMethod.POST})
    public void execute(HttpServletRequest request, HttpServletResponse response) throws UnsupportedEncodingException{
		request.setCharacterEncoding(ServletUtils.DEFAULT_ENCODING);
		response.setCharacterEncoding(ServletUtils.DEFAULT_ENCODING);
		Context ctx=ContextUtils.getContext();
		try{
			if(!ctx.has(Context.DB_SESSION)){
				SessionFactory sf = AppContextHolder.getBean(AppContextHolder.DEFAULT_SESSION_FACTORY, SessionFactory.class);
				ctx.put(Context.DB_SESSION, sf.openSession());
			}
			BaseDAO dao=new BaseDAO();
			HttpSession httpSession = request.getSession(false);
			if (httpSession != null) {
				String uid=(String) httpSession.getAttribute(UserRoleTokenUtils.SESSION_UID_KEY);
				if(uid!=null){
					
					User user = AccountCenter.getUser(uid);
					if(user!=null){
						
						int urt=(Integer) httpSession.getAttribute(UserRoleTokenUtils.SESSION_TOKEN_KEY);
						UserRoleToken token = user.getUserRoleToken(urt);
						ContextUtils.put(Context.USER_ROLE_TOKEN, token);
					}
				}
			}
			Object ret=this.action(request, response, dao);
			
			response.getWriter().write(ret.toString());
		}catch(Exception e){
			e.printStackTrace();
			try {
				response.getWriter().write(e.getMessage());
			} catch (IOException e1) {
				e1.printStackTrace();
			}
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
	
	
	public Object action(HttpServletRequest request, HttpServletResponse response,BaseDAO dao) throws Exception{
		String className=request.getParameter("className");
		Class<?> c=Class.forName("action.impl."+className);
		BaseAction action=(BaseAction) c.newInstance();
		return action.doAction(request, response, dao);
	}
	
}
