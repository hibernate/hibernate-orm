/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.exceptionhandling;

import java.sql.Timestamp;

import org.hibernate.dialect.H2Dialect;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.Version;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Jan Schatteman
 */
@JiraKey( value = "HHH-1661")
@RequiresDialect( H2Dialect.class )
public class StaleObjectMergeTest {

	@DomainModel(
			annotatedClasses = { A.class }
	)
	@SessionFactory
	@Test
	public void testStaleNonVersionEntityMerged(SessionFactoryScope scope) {
		A a = new A();
		scope.inTransaction(
				session -> session.persist( a )
		);

		scope.inTransaction(
				session -> {
					A aGet = session.get( A.class, a.getId() );
					session.remove( aGet );
				}
		);

		assertThrows(
				OptimisticLockException.class,
				() -> scope.inTransaction(
						session -> session.merge( a )
				)
		);
	}

	@DomainModel(
			annotatedClasses = { B.class, C.class }
	)
	@SessionFactory
	@Test
	public void testStalePrimitiveAndWrapperVersionEntityMerged(SessionFactoryScope scope) {
		B b = new B();
		// this is a workaround because the version is seeded to 0, so there's no way of differentiating
		// a new instance from a detached one for primitive types
		b.setVersion( 1 );
		scope.inTransaction(
				session -> session.persist( b )
		);

		scope.inTransaction(
				session -> {
					B bGet = session.get( B.class, b.getId() );
					session.remove( bGet );
				}
		);

		assertThrows(
				OptimisticLockException.class,
				() -> {
					scope.inTransaction(
							session -> {
								session.merge( b );
							}
					);
				}
		);

		C c = new C();
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
		@GeneratedValue
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
		@GeneratedValue
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
		@GeneratedValue
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
		@GeneratedValue
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
