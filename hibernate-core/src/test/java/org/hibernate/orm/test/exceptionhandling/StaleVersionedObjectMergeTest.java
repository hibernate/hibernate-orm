/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.exceptionhandling;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.Id;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.Version;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Jan Schatteman
 */
@JiraKey( value = "HHH-1661")
@RequiresDialect( H2Dialect.class )
public class StaleVersionedObjectMergeTest {

	@DomainModel(
			annotatedClasses = { A.class }
	)
	@SessionFactory
	@Test
	public void testStaleNonVersionEntityMerged(SessionFactoryScope scope) {
		A a = new A();
		a.id = 3;
		scope.inTransaction(
				session -> session.persist( a )
		);

		scope.inTransaction(
				session -> {
					A aGet = session.get( A.class, a.getId() );
					session.remove( aGet );
				}
		);

		scope.inTransaction(
				session -> session.merge( a )
		);
	}

	@DomainModel(
			annotatedClasses = { B.class }
	)
	@SessionFactory
	@Test
	public void testStalePrimitiveVersionEntityMerged(SessionFactoryScope scope) {
		B b = new B();
		b.id = 3;
		scope.inTransaction(
				session -> session.persist( b )
		);

		scope.inTransaction(
				session -> {
					B aGet = session.get( B.class, b.getId() );
					session.remove( aGet );
				}
		);

		// we have no way to detect that the instance
		// was removed so it is treated as new
		scope.inTransaction(
				session -> session.merge( b )
		);
	}

	@DomainModel(
			annotatedClasses = { C.class }
	)
	@SessionFactory
	@Test
	public void testStalePrimitiveAndWrapperVersionEntityMerged(SessionFactoryScope scope) {
		C b = new C();
		b.id = 5;
		b.version = 1;
		assertThrows(
				EntityExistsException.class,
				() -> {
					scope.inTransaction(
							session -> session.persist( b )
					);
				}
		);

		C c = new C();
		c.id = 6;
		scope.inTransaction(
				session -> session.persist( c )
		);

		scope.inTransaction(
				session -> {
					C cGet = session.get( C.class, c.getId() );
					session.remove( cGet );
				}
		);

		assertThrows(
				OptimisticLockException.class,
				() -> {
					scope.inTransaction(
							session -> {
								session.merge( c );
							}
					);
				}
		);
	}

	@DomainModel(
			annotatedClasses = { D.class }
	)
	@SessionFactory
	@Test
	public void testStaleTimestampVersionEntityMerged(SessionFactoryScope scope) {
		D d = new D();
		d.id = 8;
		scope.inTransaction(
				session -> session.persist( d )
		);

		scope.inTransaction(
				session -> {
					D dGet = session.get( D.class, d.getId() );
					session.remove( dGet );
				}
		);

		assertThrows(
				OptimisticLockException.class,
				() -> {
					scope.inTransaction(
							session -> {
								session.merge( d );
							}
					);
				}
		);
	}

	@Entity(name = "A")
	public static class A {
		@Id
		private long id;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}
	}

	@Entity(name = "B")
	public static class B {
		@Id
		private long id;

		@Version
		@Column(name = "ver")
		private int version;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public int getVersion() {
			return version;
		}

		public void setVersion(int version) {
			this.version = version;
		}
	}

	@Entity(name = "C")
	public static class C {
		@Id
		private long id;

		@Version
		@Column(name = "ver")
		private Integer version;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}
	}

	@Entity(name = "D")
	public static class D {
		@Id
		private long id;

		@Version
		@Column(name = "ver")
		private Timestamp version;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}
	}
}
