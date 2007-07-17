//$Id: WebSphereTransactionManagerLookup.java 8469 2005-10-26 22:03:03Z oneovthafew $
package org.hibernate.transaction;

import java.util.Properties;

import javax.transaction.TransactionManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hibernate.HibernateException;

/**
 * TransactionManager lookup strategy for WebSphere (versions 4, 5.0 and 5.1)
 * @author Gavin King
 */
public class WebSphereTransactionManagerLookup implements TransactionManagerLookup {

	private static final Log log = LogFactory.getLog(WebSphereTransactionManagerLookup.class);
	private final int wsVersion;
	private final Class tmfClass;
	
	public WebSphereTransactionManagerLookup() {
		try {
			Class clazz;
			int version;
			try {
				clazz = Class.forName("com.ibm.ws.Transaction.TransactionManagerFactory");
				version = 5;
				log.info("WebSphere 5.1");
			}
			catch (Exception e) {
				try {
					clazz = Class.forName("com.ibm.ejs.jts.jta.TransactionManagerFactory");
					version = 5;
					log.info("WebSphere 5.0");
				} 
				catch (Exception e2) {
					clazz = Class.forName("com.ibm.ejs.jts.jta.JTSXA");
					version = 4;
					log.info("WebSphere 4");
				}
			}

			tmfClass=clazz;
			wsVersion=version;
		}
		catch (Exception e) {
			throw new HibernateException( "Could not obtain WebSphere TransactionManagerFactory instance", e );
		}
	}

	public TransactionManager getTransactionManager(Properties props) throws HibernateException {
		try {
			return (TransactionManager) tmfClass.getMethod("getTransactionManager", null).invoke(null, null);
		}
		catch (Exception e) {
			throw new HibernateException( "Could not obtain WebSphere TransactionManager", e );
		}
	}

	public String getUserTransactionName() {
		return wsVersion==5 ?
			"java:comp/UserTransaction":
			"jta/usertransaction";
	}

}