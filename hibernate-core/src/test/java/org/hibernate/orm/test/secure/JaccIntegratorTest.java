/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.secure;

import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Policy;
import java.security.ProtectionDomain;
import java.security.Provider;
import java.util.Collections;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import javax.security.auth.Subject;
import jakarta.security.jacc.PolicyContext;
import jakarta.security.jacc.PolicyContextException;
import jakarta.security.jacc.PolicyContextHandler;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
@Jpa(annotatedClasses = {
		JaccIntegratorTest.Person.class
}, properties = {
		@Setting( name = AvailableSettings.JACC_ENABLED, value = "true"),
		@Setting( name = AvailableSettings.JACC_CONTEXT_ID, value = "JACC_CONTEXT_ID"),
		@Setting( name = "hibernate.jacc.allowed.org.hibernate.secure.Customer", value = "insert")
})
@TestForIssue( jiraKey = "HHH-11805" )
public class JaccIntegratorTest {

	@BeforeEach
	protected void afterEntityManagerFactoryBuilt(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory();
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
	public void testAllow(EntityManagerFactoryScope scope) {
		setPolicy( true );

		scope.inTransaction( entityManager -> {
			Person person = new Person();
			person.id = 1L;
			person.name = "John Doe";

			entityManager.persist( person );
		} );
	}

	@Test
	public void testDisallow(EntityManagerFactoryScope scope) {
		setPolicy( false );

		try {
			scope.inTransaction( entityManager -> {
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

	@Entity(name = "Person")
	public static class Person {

		@Id
		private Long id;

		private String name;
	}
}
