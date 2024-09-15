/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.proxy;

import java.util.List;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Christian Beikov
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey("HHH-9638")
@DomainModel(
		annotatedClasses = {
				ProxyReferenceEqualityTest.A.class,
				ProxyReferenceEqualityTest.B.class
		}
)
@SessionFactory
public class ProxyReferenceEqualityTest {
	@AfterEach
	public void tearDown(SessionFactoryScope scope){
		scope.inTransaction(
				session -> {
					session.createQuery( "delete from A" ).executeUpdate();
					session.createQuery( "delete from B" ).executeUpdate();
				}
		);
	}

	@Test
	public void testProxyFromQuery(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			A a = new A();
			a.id = 1L;
			a.b = new B();
			a.b.id = 1L;
			s.persist( a );
		} );

		scope.inTransaction( s -> {
			A a = s.find( A.class, 1L );
			List<B> result = s.createQuery( "FROM " + B.class.getName() + " b", B.class ).getResultList();
			assertEquals( 1, result.size() );
			assertTrue( a.b == result.get( 0 ) );
		} );
	}

	@Entity(name = "A")
	public static class A {
		@Id
		Long id;

		@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
		B b;

		String name;

	}

	@Entity(name = "B")
	public static class B {
		@Id
		Long id;

		String name;
	}
}
