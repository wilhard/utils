/*
 * @(#)BschisDAO.java Created on 2011-12-15 下午3:03:44
 *
 * 版权：版权所有 bsoft.com.cn 保留所有权力。
 */
package com.iamnotme.dao;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.transform.Transformers;

import ctd.controller.exception.ControllerException;
import ctd.dao.QueryResult;
import ctd.dao.SimpleDAO;
import ctd.dao.exception.DataAccessException;
import ctd.schema.Schema;
import ctd.schema.SchemaController;
import ctd.service.core.ServiceException;
import ctd.util.AppContextHolder;
import ctd.util.context.Context;
import ctd.util.context.ContextUtils;
import ctd.validator.ValidateException;
import ctd.validator.Validator;

public class BaseDAO {

	public static final String NEED_VALIDATION = "$needValidation";
	public static final String PAGE_NO = "pageNo";
	public static final String PAGE_SIZE = "pageSize";
	private Session session;
	private Context ctx;
	private Transaction trx;

	/**
	 * @param ctx
	 * @throws DataAccessException
	 */
	public BaseDAO() {
		this.ctx = ContextUtils.getContext();
		this.session = (Session) ctx.get(Context.DB_SESSION);
		if (this.session == null || !this.session.isOpen()) {
			SessionFactory sf = AppContextHolder.getBean(
					AppContextHolder.DEFAULT_SESSION_FACTORY,
					SessionFactory.class);
			this.session = sf.openSession();
			ctx.put(Context.DB_SESSION, this.session);
		}
	}
	
	/**
	 * @param ctx
	 * @throws DataAccessException
	 */
	public BaseDAO(Context ctx) {
		this.ctx = ctx;
		this.session = (Session) ctx.get(Context.DB_SESSION);
	}
	
	public BaseDAO(Context ctx, Session ss) {
		this.ctx = ctx;
		this.session = ss;
	}

	public Context getContext() {
		return this.ctx;
	}

	/**
	 * @return
	 */
	public boolean isReady() {
		return (session != null && session.isOpen());
	}

	/**
	 * 更新一条记录。注意：hql语句中日期型字段不要加“str（）”函数。
	 * 
	 * @param hql
	 * @param parameters
	 * @throws DaoException
	 */
	public int doUpdate(String hql, Map<String, Object> parameters)
			throws DaoException {
		if (!isReady()) {
			throw new DaoException("DAO is not ready.");
		}
		try {
			Query query = session.createQuery(hql);
			if (parameters != null && !parameters.isEmpty()) {
				for (String key : parameters.keySet()) {
					Object obj = parameters.get(key);
					setParameter(query, key, obj);
				}
			}
			int c = query.executeUpdate();
			session.flush();
			return c;
		} catch (HibernateException e) {
			throw new DaoException(e);
		}
	}

	/**
	 * 根据主键删除一条数据。
	 * 
	 * @param pkey
	 * @param sc
	 * @throws DaoException
	 */
	public void doRemove(Object pkey, String entryName)
			throws DaoException {
		if (!isReady()) {
			throw new DaoException("DAO is not ready.");
		}
		Schema sc = null;
		try {
			sc = SchemaController.instance().get(entryName);
		} catch (ControllerException e1) {
			throw new DaoException(e1);
		}
		;
		if (sc == null) {
			throw new DaoException(
					"Schema is not defined: " + entryName);
		}
		SimpleDAO dao = null;
		try {
			dao = new SimpleDAO(sc, ctx);
			dao.remove(pkey);
			session.flush();
		} catch (Exception e) {
			throw new DaoException(e);
		}
	}

	/**
	 * 根据某一字段值删除数据
	 * 
	 * @param field
	 * @param value
	 * @throws DaoException
	 */
	public void doRemove(String field, Object value, String entryName)
			throws DaoException {
		if (!isReady()) {
			throw new DaoException("DAO is not ready.");
		}
		Schema sc = null;
		try {
			sc = SchemaController.instance().get(entryName);
		} catch (ControllerException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		if (sc == null) {
			throw new DaoException(
					"Schema is not defined: " + entryName);
		}
		SimpleDAO dao = null;
		try {
			dao = new SimpleDAO(sc, ctx);
			dao.removeByFieldValue(field, value);
			session.flush();
		} catch (Exception e) {
			throw new DaoException(e);
		}
	}

	/**
	 * 不处理字典 以Map对象返回唯一查询结果，map的key值由hql语句中定义。 hql中字段必须带别名。
	 * 注意：hql语句中日期型字段不要加“str（）”函数。
	 * 
	 * @param hql
	 * @param parameters
	 * @return
	 * @throws DaoException
	 */
	@SuppressWarnings("unchecked")
	public Map<String, Object> doLoad(String hql, Map<String, Object> parameters)
			throws DaoException {
		if (!isReady()) {
			throw new DaoException("DAO is not ready.");
		}
		try {
			Query query = session.createQuery(hql).setResultTransformer(
					Transformers.ALIAS_TO_ENTITY_MAP);
			if (parameters != null && !parameters.isEmpty()) {
				for (String key : parameters.keySet()) {
					Object obj = parameters.get(key);
					setParameter(query, key, obj);
				}
			}
			Map<String, Object> m = (Map<String, Object>) query.uniqueResult();
			session.flush();
			return m;
		} catch (HibernateException e) {
			throw new DaoException(e);
		}
	}

	/**
	 * 单表查询、不对结果进行字典处理、支持分页
	 * 查询所有符合条件的记录，以一个列表返回，每条记录以一个Map对象表示，map的key值由hql语句中定义。 hql中字段必须带别名。
	 * 注意：1.hql语句中日期型字段不要加“str（）”函数。 2.在参数列表中可以指定”first“和”max“实现返回结果分页。 ADD BY
	 * LYL: first的值应该是 (当前页面-1)*每页条数，max 是每页的条数
	 * 
	 * @param hql
	 * @param parameters
	 * @return
	 * @throws DaoException
	 */
	@SuppressWarnings("unchecked")
	public List<Map<String, Object>> doQuery(String hql,
			Map<String, Object> parameters)
			throws DaoException {
		if (!isReady()) {
			throw new DaoException("DAO is not ready.");
		}
		try {
			Query query = session.createQuery(hql);
			if (hql.indexOf(" as ") > 0) {
				query = query
						.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);
			}
			if (parameters != null && !parameters.isEmpty()) {
				for (String key : parameters.keySet()) {
					if (key.equals("first")) {
						query.setFirstResult((Integer) parameters.get(key));
					} else if (key.equals("max")) {
						query.setMaxResults((Integer) parameters.get(key));
					} else {
						setParameter(query, key, parameters.get(key));
					}
				}
			}
			List<Map<String, Object>> l = (List<Map<String, Object>>) query
					.list();
			session.flush();
			return l;
		} catch (HibernateException e) {
			throw new DaoException(e);
		}
	}

	/**
	 * schema多表关联、处理字典项、无分页。
	 * 
	 * @param cnd
	 * @param orderBy
	 * @param schema
	 * @return
	 * @throws DaoException
	 */
	@SuppressWarnings("unchecked")
	public List<Map<String, Object>> doList(List<?> cnd, String orderBy,
			String schema) throws DaoException {
		return (List<Map<String, Object>>) doList(cnd, orderBy, schema, 1, -1,
				"1").get("body");
	}

	/**
	 * schema多表关联、处理字典项、可分页
	 * 
	 * @param cnd
	 * @param orderBy
	 * @param schema
	 * @param PageSize
	 * @param pageNo
	 * @param queryCndsType
	 *            值为""会查询出所有的值，跳过用户权限的数据过滤。
	 * @return Map key{body : 查询结果记录集, totalCount : 总记录数}
	 * @throws DaoException
	 */
	public Map<String, Object> doList(List<?> cnd, String orderBy,
			String schema, int pageNo, int pageSize, String queryCndsType)
			throws DaoException {
		if (!isReady()) {
			throw new DaoException("DAO is not ready.");
		}

		Schema sc = null;
		try {
			sc = SchemaController.instance().get(schema);
		} catch (ControllerException e) {
			throw new DaoException(e);
		}
		if (sc == null) {
			throw new DaoException(
					"Schema is not defined: " + schema);
		}

		SimpleDAO dao = null;
		try {
			dao = new SimpleDAO(sc, ctx);
			QueryResult rs = dao.find(cnd, pageNo, pageSize, queryCndsType,
					orderBy);
			Map<String, Object> rsMap = new HashMap<String, Object>();
			rsMap.put("body", rs.getRecords());
			rsMap.put("totalCount", rs.getTotalCount());
			return rsMap;
		} catch (Exception e) {
			throw new DaoException(e);
		}
	}

	/**
	 * 设置查询参数。
	 * 
	 * @param query
	 * @param name
	 * @param value
	 */
	private void setParameter(Query query, String name, Object value) {
		if (value instanceof Collection<?>) {
			query.setParameterList(name, (Collection<?>) value);
		} else if (value instanceof Object[]) {
			query.setParameterList(name, (Object[]) value);
		} else {
			query.setParameter(name, value);
		}
	}

	/**
	 * 根据指定字段条件删除数据。
	 * 
	 * @param pkey
	 * @param sc
	 * @throws DaoException
	 */
	public void removeByFieldValue(String fName, Object v, String entryName)
			throws DaoException {
		if (!isReady()) {
			throw new DaoException("DAO is not ready.");
		}
		Schema sc = null;
		try {
			sc = SchemaController.instance().get(entryName);
		} catch (ControllerException e) {
			throw new DaoException(e);
		}
		if (sc == null) {
			throw new DaoException(
					"Schema is not defined: " + entryName);
		}
		SimpleDAO dao = null;
		try {
			dao = new SimpleDAO(sc, ctx);
			dao.removeByFieldValue(fName, v);
		} catch (Exception e) {
			throw new DaoException(e);
		}
	}

	/**
	 * 
	 * @author caijy
	 * @createDate 2012-6-7
	 * @description 支持sql查询的数据库操作方法,其他类似doQuery(String hql,Map<String, Object>
	 *              parameters)
	 * @updateInfo
	 * @param sql
	 * @param parameters
	 * @return
	 * @throws DaoException
	 */
	@SuppressWarnings("unchecked")
	public List<Map<String, Object>> doSqlQuery(String sql,
			Map<String, Object> parameters)
			throws DaoException {
		if (!isReady()) {
			throw new DaoException("DAO is not ready.");
		}
		try {
			SQLQuery query = session.createSQLQuery(sql);
			if (parameters != null && !parameters.isEmpty()) {
				for (String key : parameters.keySet()) {
					if (key.equals("first")) {
						query.setFirstResult((Integer) parameters.get(key));
					} else if (key.equals("max")) {
						query.setMaxResults((Integer) parameters.get(key));
					} else {
						setParameter(query, key, parameters.get(key));
					}
				}
			}
			if (sql.indexOf(" as ") > 0) {
				query.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);
			}
			List<Map<String, Object>> l = (List<Map<String, Object>>) query
					.list();
			session.flush();
			return l;
		} catch (HibernateException e) {
			throw new DaoException(e);
		}
	}

	/**
	 * 
	 * @author caijy
	 * @createDate 2012-6-7
	 * @description 支持sql语句的update操作,其他同doUpdate(String hql,Map<String, Object>
	 *              parameters)
	 * @updateInfo
	 * @param sql
	 * @param parameters
	 * @return
	 * @throws DaoException
	 */
	public int doSqlUpdate(String sql, Map<String, Object> parameters)
			throws DaoException {
		if (!isReady()) {
			throw new DaoException("DAO is not ready.");
		}
		try {
			SQLQuery query = session.createSQLQuery(sql);
			if (parameters != null && !parameters.isEmpty()) {
				for (String key : parameters.keySet()) {
					Object obj = parameters.get(key);
					setParameter(query, key, obj);
				}
			}
			int c = query.executeUpdate();
			session.flush();
			return c;
		} catch (HibernateException e) {
			throw new DaoException(e);
		}
	}

	/**
	 * 
	 * @author caijy
	 * @createDate 2012-6-11
	 * @description 查询记录条数
	 * @updateInfo
	 * @param tableName
	 *            表名(支持多表)
	 * @param hqlWhere
	 *            where条件(不带"where")
	 * @param parameters
	 *            参数
	 * @return 记录条数
	 * @throws DaoException
	 */
	public Long doCount(String tableName, String hqlWhere,
			Map<String, Object> parameters)
			throws DaoException {
		StringBuffer hql = new StringBuffer();
		hql.append("select count(*) as total from ").append(tableName)
				.append(" where ").append(hqlWhere);
		try {
			Map<String, Object> count = doLoad(hql.toString(), parameters);
			return (Long) count.get("total");
		} catch (DaoException e) {
			throw new DaoException(e);
		}
	}
	/**
	 * 执行sql，返回多条记录
	 * @author wangjianbo
	 * @created 2017-10-11
	 * @param sql
	 * @param parameters 可变长参数，无参可省略
	 * @return
	 * @throws DaoException
	 */
	@SuppressWarnings("unchecked")
	public List<Map<String, Object>> doSqlQueryMap(String sql,
			Map<String, Object>... parameters)
					throws DaoException {
		if (!isReady()) {
			throw new DaoException("DAO is not ready.");
		}
		try {
			SQLQuery query = session.createSQLQuery(sql);
			query.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);
			if (parameters != null && parameters.length>0) {
				Map<String, Object> parameter=parameters[0];
				if(parameter!=null && !parameter.isEmpty()){
					setParameters(query, parameter);
				}
			}
			List<Map<String, Object>> l = (List<Map<String, Object>>) query
					.list();
			session.flush();
			return l;
		} catch (HibernateException e) {
			throw new DaoException(e);
		}
	}
	/**
	 * 执行sql，返回单条记录
	 * @author wangjianbo
	 * @created 2017-10-11
	 * @param sql
	 * @param parameters 可变长参数，无参可省略
	 * @return
	 * @throws DaoException
	 */
	public Map<String, Object> doSqlLoad(String sql,
			Map<String, Object>... parameters)
					throws DaoException {
		List<Map<String, Object>> list=this.doSqlQueryMap(sql, parameters);
		if(list==null || list.size()==0){
			return null;
		}
		if(list.size()>1){
			throw new DaoException("单记录查询返回多条记录！");
		}
		return list.get(0);
	}
	/**
	 * 执行HQL语句
	 * @author wangjianbo
	 * @created 2017-10-11
	 * @param sql
	 * @param parameters 可变长参数，无参可省略
	 * @return
	 * @throws DaoException
	 */
	public int doSqlExecute(String sql, Map<String, Object>... parameters)
			throws DaoException {
		if (!isReady()) {
			throw new DaoException("DAO is not ready.");
		}
		try {
			SQLQuery query = session.createSQLQuery(sql);
			if (parameters != null && parameters.length>0) {
				Map<String, Object> parameter=parameters[0];
				if(parameter!=null && !parameter.isEmpty()){
					setParameters(query, parameter);
				}
			}
			int c = query.executeUpdate();
			session.flush();
			return c;
		} catch (HibernateException e) {
			throw new DaoException(e);
		}
	}
	/**
	 * 执行HQL语句
	 * @author wangjianbo
	 * @created 2017-10-11
	 * @param hql
	 * @param parameters 可变长参数，无参可省略
	 * @return
	 * @throws DaoException
	 */
	public int doExecute(String hql, Map<String, Object>... parameters)
			throws DaoException {
		if (!isReady()) {
			throw new DaoException("DAO is not ready.");
		}
		try {
			Query query = session.createQuery(hql);
			if (parameters != null && parameters.length>0) {
				Map<String, Object> parameter=parameters[0];
				if(parameter!=null && !parameter.isEmpty()){
					setParameters(query, parameter);
				}
			}
			int c = query.executeUpdate();
			session.flush();
			return c;
		} catch (HibernateException e) {
			throw new DaoException(e);
		}
	}
	/**
	 * 根据sql语句中占位符的个数设置参数值
	 * @param query
	 * @param parameters
	 */
	private void setParameters(Query query,Map<String, Object> parameters){
		String[] paramNames=query.getNamedParameters();
		if (paramNames!=null && paramNames.length>0 && parameters != null && !parameters.isEmpty()) {
			for(String name:paramNames){
				Object obj = parameters.get(name);
				if(obj==null){
					if(name.matches("%[^%]+")){
						obj = "%"+parameters.get(name.substring(1));
					}else if(name.matches("[^%]+%")){
						obj = parameters.get(name.subSequence(0, name.length()-1))+"%";
					}else if(name.matches("%[^%]+%")){
						obj = "%"+parameters.get(name.subSequence(1, name.length()-1))+"%";
					}
				}
				if(obj!=null){
					setParameter(query, name, obj);
				}
			}
		}
	}
	/**
	 * 分页查询
	 * @author wangjianbo
	 * @created 2017-11-30
	 * @param sql
	 * @param request 当前页号、每页记录数Map
	 * @param sqlParams sql参数，没有可以省略
	 * @return
	 * @throws DaoException
	 */
	@SuppressWarnings("unchecked")
	public Map<String,Object> doSqlPagingQuery(String sql,Map<String,Object> request,Map<String,Object>... sqlParams) throws DaoException{
		Map<String,Object> result=new HashMap<String, Object>();
		if(request.get(PAGE_NO)==null){
			throw new DaoException("paging query needs 'pageNo' parameter.");
		}
		if(request.get(PAGE_SIZE)==null){
			throw new DaoException("paging query needs 'pageSize' parameter.");
		}
		if (!isReady()) {
			throw new DaoException("DAO is not ready.");
		}
		try {
			String countSql="select count(1) from ("+sql+")";
			SQLQuery query = session.createSQLQuery(countSql);
			if (sqlParams.length>0) {
				this.setParameters(query, sqlParams[0]);
			}
			/**
			 * 先查询总记录数，如果记录数为0，不要执行查数据的SQL
			 */
			int totalCount=((BigDecimal) query.uniqueResult()).intValue();
			if(totalCount==0){
				result.put("body", new ArrayList<Map<String, Object>>());
				result.put("totalCount", totalCount);
				return result;
			}
			int pageNo=(Integer)request.get(PAGE_NO);
			int pageSize=(Integer)request.get(PAGE_SIZE);
			int recordStart=(pageNo-1)*pageSize;
			query = session.createSQLQuery(sql);
			query.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);
			query.setFirstResult(recordStart);
			query.setMaxResults(pageSize);
			if (sqlParams.length>0) {
				this.setParameters(query, sqlParams[0]);
			}
			List<Map<String, Object>> record = (List<Map<String, Object>>) query.list();
			result.put("body", record);
			result.put("totalCount", totalCount);
			return result;
		} catch (HibernateException e) {
			throw new DaoException(e);
		}
	}

	public void beginTransaction() throws DataAccessException {
		try {
			trx = session.getTransaction();
			trx.begin();
		} catch (HibernateException e) {
			throw new DataAccessException("BeginTransactionFailed:"
					+ e.getMessage(), e);
		}
	}

	public void commitTransaction() throws ServiceException {
		if (trx == null) {
			return;
		}
		try {
			trx.commit();
		} catch (HibernateException e) {
			throw new ServiceException("CommitTransactionFailed:"
					+ e.getMessage(), e);
		}
	}
}
