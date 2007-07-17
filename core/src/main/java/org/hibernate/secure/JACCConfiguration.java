// $Id: JACCConfiguration.java 7592 2005-07-21 04:56:17Z oneovthafew $
package org.hibernate.secure;

import java.util.StringTokenizer;

import javax.security.jacc.EJBMethodPermission;
import javax.security.jacc.PolicyConfiguration;
import javax.security.jacc.PolicyConfigurationFactory;
import javax.security.jacc.PolicyContextException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;

/**
 * Adds Hibernate permissions to roles via JACC
 * 
 * @author Gavin King
 */
public class JACCConfiguration {

	private static final Log log = LogFactory.getLog( JACCConfiguration.class );

	private final PolicyConfiguration policyConfiguration;

	public JACCConfiguration(String contextId) throws HibernateException {
		try {
			policyConfiguration = PolicyConfigurationFactory
					.getPolicyConfigurationFactory()
					.getPolicyConfiguration( contextId, false );
		}
		catch (ClassNotFoundException cnfe) {
			throw new HibernateException( "JACC provider class not found", cnfe );
		}
		catch (PolicyContextException pce) {
			throw new HibernateException( "policy context exception occurred", pce );
		}
	}

	public void addPermission(String role, String entityName, String action) {

		if ( action.equals( "*" ) ) {
			action = "insert,read,update,delete";
		}

		StringTokenizer tok = new StringTokenizer( action, "," );

		while ( tok.hasMoreTokens() ) {
			String methodName = tok.nextToken().trim();
			EJBMethodPermission permission = new EJBMethodPermission( 
					entityName, 
					methodName, 
					null, // interfaces
					null // arguments
				);

			if ( log.isDebugEnabled() ) {
				log.debug( "adding permission to role " + role + ": " + permission );
			}
			try {
				policyConfiguration.addToRole( role, permission );
			}
			catch (PolicyContextException pce) {
				throw new HibernateException( "policy context exception occurred", pce );
			}
		}
	}

}
