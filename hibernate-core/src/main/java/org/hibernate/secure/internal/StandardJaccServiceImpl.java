/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.secure.internal;

import java.security.AccessController;
import java.security.CodeSource;
import java.security.Policy;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.util.Map;
import java.util.Set;
import javax.security.auth.Subject;
import javax.security.jacc.EJBMethodPermission;
import javax.security.jacc.PolicyConfiguration;
import javax.security.jacc.PolicyConfigurationFactory;
import javax.security.jacc.PolicyContext;
import javax.security.jacc.PolicyContextException;

import org.hibernate.HibernateException;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.secure.spi.GrantedPermission;
import org.hibernate.secure.spi.IntegrationException;
import org.hibernate.secure.spi.JaccService;
import org.hibernate.secure.spi.PermissibleAction;
import org.hibernate.secure.spi.PermissionCheckEntityInformation;
import org.hibernate.service.spi.Configurable;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class StandardJaccServiceImpl implements JaccService, Configurable {
	private static final Logger log = Logger.getLogger( StandardJaccServiceImpl.class );

	private String contextId;
	private PolicyConfiguration policyConfiguration;

	@Override
	public void configure(Map configurationValues) {
		this.contextId = (String) configurationValues.get( AvailableSettings.JACC_CONTEXT_ID );
	}

	@Override
	public String getContextId() {
		return contextId;
	}

	@Override
	public void addPermission(GrantedPermission permissionDeclaration) {
		// todo : do we need to wrap these PolicyConfiguration calls in privileged actions like we do during permission checks?

		if ( policyConfiguration == null ) {
			policyConfiguration = locatePolicyConfiguration( contextId );
		}

		for ( String grantedAction : permissionDeclaration.getPermissibleAction().getImpliedActions() ) {
			final EJBMethodPermission permission = new EJBMethodPermission(
					permissionDeclaration.getEntityName(),
					grantedAction,
					null, // interfaces
					null // arguments
			);

			log.debugf( "Adding permission [%s] to role [%s]", grantedAction, permissionDeclaration.getRole() );
			try {
				policyConfiguration.addToRole( permissionDeclaration.getRole(), permission );
			}
			catch (PolicyContextException pce) {
				throw new HibernateException( "policy context exception occurred", pce );
			}
		}
	}

	private PolicyConfiguration locatePolicyConfiguration(String contextId) {
		try {
			return PolicyConfigurationFactory
					.getPolicyConfigurationFactory()
					.getPolicyConfiguration( contextId, false );
		}
		catch (Exception e) {
			throw new IntegrationException( "Unable to access JACC PolicyConfiguration" );
		}
	}

	@Override
	public void checkPermission(PermissionCheckEntityInformation entityInformation, PermissibleAction action) {
		if ( action == PermissibleAction.ANY ) {
			throw new HibernateException( "ANY action (*) is not legal for permission check, only for configuration" );
		}

		final String originalContextId = AccessController.doPrivileged( new ContextIdSetAction( contextId ) );
		try {
			doPermissionCheckInContext( entityInformation, action );
		}
		finally {
			AccessController.doPrivileged( new ContextIdSetAction( originalContextId ) );
		}
	}

	private static class ContextIdSetAction implements PrivilegedAction<String> {
		private final String contextId;

		private ContextIdSetAction(String contextId) {
			this.contextId = contextId;
		}

		@Override
		public String run() {
			String previousID = PolicyContext.getContextID();
			PolicyContext.setContextID( contextId );
			return previousID;
		}
	}

	private void doPermissionCheckInContext(PermissionCheckEntityInformation entityInformation, PermissibleAction action) {
		final Policy policy = Policy.getPolicy();
		final Principal[] principals = getCallerPrincipals();

		final CodeSource codeSource = entityInformation.getEntity().getClass().getProtectionDomain().getCodeSource();
		final ProtectionDomain pd = new ProtectionDomain( codeSource, null, null, principals );

		// the action is known as 'method name' in JACC
		final EJBMethodPermission jaccPermission = new EJBMethodPermission(
				entityInformation.getEntityName(),
				action.getImpliedActions()[0],
				null,
				null
		);

		if ( ! policy.implies( pd, jaccPermission) ) {
			throw new SecurityException(
					String.format(
							"JACC denied permission to [%s.%s] for [%s]",
							entityInformation.getEntityName(),
							action.getImpliedActions()[0],
							join( principals )
					)
			);
		}
	}

	private String join(Principal[] principals) {
		String separator = "";
		final StringBuilder buffer = new StringBuilder();
		for ( Principal principal : principals ) {
			buffer.append( separator ).append( principal.getName() );
			separator = ", ";
		}
		return buffer.toString();
	}

	protected Principal[] getCallerPrincipals() {
		final Subject caller = getContextSubjectAccess().getContextSubject();
		if ( caller == null ) {
			return new Principal[0];
		}

		final Set<Principal> principalsSet = caller.getPrincipals();
		return principalsSet.toArray( new Principal[ principalsSet.size()] );
	}

	private ContextSubjectAccess getContextSubjectAccess() {
		return ( System.getSecurityManager() == null )
				? NonPrivilegedContextSubjectAccess.INSTANCE
				: PrivilegedContextSubjectAccess.INSTANCE;
	}

	protected static interface ContextSubjectAccess {
		public static final String SUBJECT_CONTEXT_KEY = "javax.security.auth.Subject.container";

		public Subject getContextSubject();
	}

	protected static class PrivilegedContextSubjectAccess implements ContextSubjectAccess {
		public static final PrivilegedContextSubjectAccess INSTANCE = new PrivilegedContextSubjectAccess();

		private final PrivilegedAction<Subject> privilegedAction = new PrivilegedAction<Subject>() {
			public Subject run() {
				return NonPrivilegedContextSubjectAccess.INSTANCE.getContextSubject();
			}
		};

		@Override
		public Subject getContextSubject() {
			return AccessController.doPrivileged( privilegedAction );
		}
	}

	protected static class NonPrivilegedContextSubjectAccess implements ContextSubjectAccess {
		public static final NonPrivilegedContextSubjectAccess INSTANCE = new NonPrivilegedContextSubjectAccess();

		@Override
		public Subject getContextSubject() {
			try {
				return (Subject) PolicyContext.getContext( SUBJECT_CONTEXT_KEY );
			}
			catch (PolicyContextException e) {
				throw new HibernateException( "Unable to access JACC PolicyContext in order to locate calling Subject", e );
			}
		}
	}

}
