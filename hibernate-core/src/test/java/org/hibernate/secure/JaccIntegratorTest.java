/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.secure;

import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Policy;
import java.security.ProtectionDomain;
import java.security.Provider;
import java.util.Collections;
import java.util.Map;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.security.auth.Subject;
import javax.security.jacc.PolicyContext;
import javax.security.jacc.PolicyContextException;
import javax.security.jacc.PolicyContextHandler;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
@TestForIssue( jiraKey = "HHH-11805" )
public class JaccIntegratorTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Person.class,
		};
	}

	@Override
	protected void addConfigOptions(Map options) {
		options.put( AvailableSettings.JACC_ENABLED, Boolean.TRUE.toString() );
		options.put( AvailableSettings.JACC_CONTEXT_ID, "JACC_CONTEXT_ID" );
		options.put( "hibernate.jacc.allowed.org.hibernate.secure.Customer", "insert" );
	}

	@Override
	protected void afterEntityManagerFactoryBuilt() {
		PolicyContextHandler policyContextHandler = new PolicyContextHandler() {
			@Override
			public Object getContext(String key, Object data) throws PolicyContextException {
				Subject subject = new Subject( true, Collections.singleton(new java.security.Principal() {

					@Override
					public String getName() {
						return "org.hibernate.secure.JaccIntegratorTest$Person";
					}

					@Override
					public boolean implies(Subject subject) {
						return true;
					}
				}), Collections.emptySet(), Collections.emptySet());
				return subject;
			}

			@Override
			public String[] getKeys() throws PolicyContextException {
				return new String[0];
			}

			@Override
			public boolean supports(String key) throws PolicyContextException {
				return true;
			}
		};
		try {
			PolicyContext.registerHandler( "javax.security.auth.Subject.container", policyContextHandler, true);
		}
		catch (PolicyContextException e) {
			fail(e.getMessage());
		}
	}

	protected void setPolicy(boolean allow) {
		Policy.setPolicy( new Policy() {
			@Override
			public Provider getProvider() {
				return super.getProvider();
			}

			@Override
			public String getType() {
				return super.getType();
			}

			@Override
			public Parameters getParameters() {
				return super.getParameters();
			}

			@Override
			public PermissionCollection getPermissions(CodeSource codesource) {
				return super.getPermissions( codesource );
			}

			@Override
			public PermissionCollection getPermissions(ProtectionDomain domain) {
				return super.getPermissions( domain );
			}

			@Override
			public boolean implies(ProtectionDomain domain, Permission permission) {
				return allow;
			}

			@Override
			public void refresh() {
				super.refresh();
			}
		} );
	}

	@Test
	public void testAllow() {
		setPolicy( true );

		doInJPA( this::entityManagerFactory, entityManager -> {
			Person person = new Person();
			person.id = 1L;
			person.name = "John Doe";

			entityManager.persist( person );
		} );
	}

	@Test
	public void testDisallow() {
		setPolicy( false );

		try {
			doInJPA( this::entityManagerFactory, entityManager -> {
				Person person = new Person();
				person.id = 1L;
				person.name = "John Doe";

				entityManager.persist( person );
			} );

			fail("Should have thrown SecurityException");
		}
		catch (Exception e) {
			assertTrue( e.getCause() instanceof SecurityException );
		}
	}

	@Entity
	public static class Person {

		@Id
		private Long id;

		private String name;
	}
}
