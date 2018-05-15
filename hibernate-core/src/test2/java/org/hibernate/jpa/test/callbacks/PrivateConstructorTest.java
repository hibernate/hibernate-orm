/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.callbacks;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.proxy.ProxyFactory;
import org.hibernate.proxy.pojo.bytebuddy.ByteBuddyProxyFactory;
import org.hibernate.proxy.pojo.javassist.JavassistProxyFactory;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.logger.LoggerInspectionRule;
import org.hibernate.testing.logger.Triggerable;
import org.hibernate.testing.util.ExceptionUtil;
import org.junit.Rule;
import org.junit.Test;

import org.jboss.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@TestForIssue(jiraKey = "HHH-13020")
public class PrivateConstructorTest extends BaseEntityManagerFunctionalTestCase {

	@Rule
	public LoggerInspectionRule logInspection = new LoggerInspectionRule( Logger.getMessageLogger(
			CoreMessageLogger.class,
			proxyFactoryClass()
					.getName()
	) );

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Parent.class,
				Child.class
		};
	}

	@Test
	public void test() {
		Child child = new Child();

		EntityManager entityManager = null;
		EntityTransaction txn = null;
		try {
			entityManager = createEntityManager();
			txn = entityManager.getTransaction();
			txn.begin();
			entityManager.persist( child );
			txn.commit();

			entityManager.clear();

			Integer childId = child.getId();

			Triggerable triggerable = logInspection.watchForLogMessages( "HHH000143:" );

			Child childReference = entityManager.getReference( Child.class, childId );
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
		catch (Throwable e) {
			if ( txn != null && txn.isActive() ) {
				txn.rollback();
			}
			throw e;
		}
		finally {
			if ( entityManager != null ) {
				entityManager.close();
			}
		}
	}

	private static Class<? extends ProxyFactory> proxyFactoryClass() {
		String byteCodeProvider = Environment.getProperties().getProperty( AvailableSettings.BYTECODE_PROVIDER );
		if ( byteCodeProvider == null || Environment.BYTECODE_PROVIDER_NAME_BYTEBUDDY.equals( byteCodeProvider ) ) {
			return ByteBuddyProxyFactory.class;
		}
		else if ( Environment.BYTECODE_PROVIDER_NAME_JAVASSIST.equals( byteCodeProvider ) ) {
			return JavassistProxyFactory.class;
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
