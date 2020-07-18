/**
 * @author wangjianbo
 * @created 2020-1-10
 */
package com.iamnotme.dao;

/**
 * @author wangjianbo
 *
 */
public class DaoException extends Exception {

	/**
	 * 
	 */
	public DaoException() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param message
	 * @param cause
	 * @param enableSuppression
	 * @param writableStackTrace
	 */
	public DaoException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param message
	 * @param cause
	 */
	public DaoException(String message, Throwable cause) {
		super(message, cause);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param message
	 */
	public DaoException(String message) {
		super(message);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param cause
	 */
	public DaoException(Throwable cause) {
		super(cause);
		// TODO Auto-generated constructor stub
	}

}
