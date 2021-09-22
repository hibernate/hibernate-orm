/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.callbacks;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.proxy.ProxyFactory;
import org.hibernate.proxy.pojo.bytebuddy.ByteBuddyProxyFactory;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.logger.LoggerInspectionRule;
import org.hibernate.testing.logger.Triggerable;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.util.ExceptionUtil;

import org.junit.Rule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.jboss.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestForIssue(jiraKey = "HHH-13020")
@Jpa(annotatedClasses = {
		PrivateConstructorTest.Parent.class,
		PrivateConstructorTest.Child.class
})
public class PrivateConstructorTest {

	@Rule
	public LoggerInspectionRule logInspection = new LoggerInspectionRule( Logger.getMessageLogger(
			CoreMessageLogger.class,
			proxyFactoryClass()
					.getName()
	) );

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					entityManager.createQuery( "delete from Child" ).executeUpdate();
					entityManager.createQuery( "delete from Parent" ).executeUpdate();
				}
		);
	}

	@Test
	public void test(EntityManagerFactoryScope scope) {
		Child child = new Child();

		scope.inTransaction(
				entityManager -> entityManager.persist( child )
		);

		scope.inTransaction(
				entityManager -> {
					Triggerable triggerable = logInspection.watchForLogMessages( "HHH000143:" );
					Child childReference = entityManager.getReference( Child.class, child.getId() );
					try {
						assertEquals( child.getParent().getName(), childReference.getParent().getName() );
					}
					catch (Exception expected) {
						assertEquals( NoSuchMethodException.class, ExceptionUtil.rootCause( expected ).getClass() );
						assertTrue( expected.getMessage().contains(
								"Bytecode enhancement failed because no public, protected or package-private default constructor was found for entity"
						) );
					}
					assertTrue( triggerable.wasTriggered() );
				}
		);
	}

	private static Class<? extends ProxyFactory> proxyFactoryClass() {
		String byteCodeProvider = Environment.getProperties().getProperty( AvailableSettings.BYTECODE_PROVIDER );
		if ( byteCodeProvider == null || Environment.BYTECODE_PROVIDER_NAME_BYTEBUDDY.equals( byteCodeProvider ) ) {
			return ByteBuddyProxyFactory.class;
		}
		else {
			throw new UnsupportedOperationException( "Unknown bytecode provider:" + byteCodeProvider );
		}
	}

	@Entity(name = "Parent")
	public static class Parent {

		private Integer id;
		private String name;

		private Parent() {
			name = "Empty";
		}

		public Parent(String s) {
			this.name = s;
		}

		@Id
		@Column(name = "id")
		@GeneratedValue(strategy = GenerationType.AUTO)
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}


	@Entity(name = "Child")
	public static class Child {

		private Integer id;
		private Parent parent;

		public Child() {
			this.parent = new Parent( "Name" );
		}

		@Id
		@Column(name = "id")
		@GeneratedValue(strategy = GenerationType.AUTO)
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
		public Parent getParent() {
			return parent;
		}

		public void setParent(Parent parent) {
			this.parent = parent;
		}
	}
}
