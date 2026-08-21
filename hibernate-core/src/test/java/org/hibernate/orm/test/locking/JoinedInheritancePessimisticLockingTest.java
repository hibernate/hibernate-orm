/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.locking;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Version;
import org.hibernate.community.dialect.InformixDialect;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings("JUnitMalformedDeclaration")
@Jpa(
		annotatedClasses =  {
				JoinedInheritancePessimisticLockingTest.BaseThing.class,
				JoinedInheritancePessimisticLockingTest.ConcreteThing.class,
				JoinedInheritancePessimisticLockingTest.AnotherConcreteThing.class,
		}
)
@Jira("https://hibernate.atlassian.net/browse/HHH-7315")
public class JoinedInheritancePessimisticLockingTest {

	@BeforeEach
	public void setup(EntityManagerFactoryScope scope) {
		scope.inTransaction(entityManager -> {
			var t1 = new ConcreteThing();
			t1.id = 1L;
			t1.name = "t1";
			t1.aProp = "abc";
			entityManager.persist( t1 );

			var t2 = new AnotherConcreteThing();
			t2.id = 2L;
			t2.name = "t2";
			t2.anotherProp = "def";
			entityManager.persist( t2 );
		} );
	}

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.dropData();
	}

	@Test
	@SkipForDialect(dialectClass = InformixDialect.class,
			reason = "Informix disallows FOR UPDATE with multi-table queries")
	public void findWithLock(EntityManagerFactoryScope scope) {
		scope.inTransaction(entityManager -> {
			BaseThing t = entityManager.find( BaseThing.class, 1L, LockModeType.PESSIMISTIC_WRITE );
			assertEquals( LockModeType.PESSIMISTIC_WRITE, entityManager.getLockMode( t ) );
		});
	}

	@Test
	public void findThenLock(EntityManagerFactoryScope scope) {
		scope.inTransaction(entityManager -> {
			BaseThing t = entityManager.find( BaseThing.class, 1L );
			entityManager.lock( t, LockModeType.PESSIMISTIC_WRITE );
			assertEquals( LockModeType.PESSIMISTIC_WRITE, entityManager.getLockMode( t ) );
		});
	}

	@Entity(name = "BaseThing")
	@Inheritance(strategy = InheritanceType.JOINED)
	public static abstract class BaseThing  {
		@Id
		@Column(nullable = false)
		Long id;

		@Basic(optional = false)
		@Column(nullable = false)
		String name;

		@Version
		@Column(name = "version")
		int version;

	}
	@Entity(name = "ConcreteThing")
	public static class ConcreteThing extends BaseThing {
		@Basic(optional = false)
		@Column(nullable = false)
		String aProp;
	}


	@Entity(name = "AnotherConcreteThing")
	public static class AnotherConcreteThing extends BaseThing {
		@Basic(optional = false)
		@Column(nullable = false)
		String anotherProp;
	}
}
