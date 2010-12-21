/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 *
 */
package org.hibernate.secure;

import static org.jboss.logging.Logger.Level.DEBUG;
import java.util.StringTokenizer;
import javax.security.jacc.EJBMethodPermission;
import javax.security.jacc.PolicyConfiguration;
import javax.security.jacc.PolicyConfigurationFactory;
import javax.security.jacc.PolicyContextException;
import org.hibernate.HibernateException;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;

/**
 * Adds Hibernate permissions to roles via JACC
 *
 * @author Gavin King
 */
public class JACCConfiguration {

    private static final Logger LOG = org.jboss.logging.Logger.getMessageLogger(Logger.class,
                                                                                JACCConfiguration.class.getPackage().getName());

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

            LOG.addingPermissionToRole(role, permission);
			try {
				policyConfiguration.addToRole( role, permission );
			}
			catch (PolicyContextException pce) {
				throw new HibernateException( "policy context exception occurred", pce );
			}
		}
	}

    /**
     * Interface defining messages that may be logged by the outer class
     */
    @MessageLogger
    interface Logger extends BasicLogger {

        @LogMessage( level = DEBUG )
        @Message( value = "Adding permission to role %s: %s" )
        void addingPermissionToRole( String role,
                                     EJBMethodPermission permission );
    }
}
