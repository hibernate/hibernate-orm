/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.basic;

import jakarta.persistence.TransactionRequiredException;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.orm.test.envers.entities.StrTestEntity;
import org.hibernate.orm.test.envers.integration.collection.norevision.Name;
import org.hibernate.orm.test.envers.integration.collection.norevision.Person;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@JiraKey(value = "HHH-5565")
@SkipForDialect(dialectClass = MySQLDialect.class, reason = "The test hangs on")
@EnversTest
@Jpa(annotatedClasses = {StrTestEntity.class, Person.class, Name.class},
		integrationSettings = {
				@Setting(name = EnversSettings.STORE_DATA_AT_DELETE, value = "true"),
				@Setting(name = EnversSettings.REVISION_ON_COLLECTION_CHANGE, value = "true")
		})
public class OutsideTransactionTest {
	@Test
	public void testInsertOutsideActiveTransaction(EntityManagerFactoryScope scope) {
		assertThrows( TransactionRequiredException.class, () -> {
			scope.inEntityManager( em -> {
				// Illegal insertion of entity outside of active transaction.
				StrTestEntity entity = new StrTestEntity( "data" );
				em.persist( entity );
				em.flush();
			} );
		} );
	}

	@Test
	public void testMergeOutsideActiveTransaction(EntityManagerFactoryScope scope) {
		final StrTestEntity entity = scope.fromTransaction( em -> {
			StrTestEntity e = new StrTestEntity( "data" );
			em.persist( e );
			return e;
		} );

		assertThrows( TransactionRequiredException.class, () -> {
			scope.inEntityManager( em -> {
				// Illegal modification of entity state outside of active transaction.
				entity.setStr( "modified data" );
				em.merge( entity );
				em.flush();
			} );
		} );
	}

	@Test
	public void testDeleteOutsideActiveTransaction(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			em.getTransaction().begin();
			final StrTestEntity entity = new StrTestEntity( "data" );
			em.persist( entity );
			em.getTransaction().commit();
			assertThrows( TransactionRequiredException.class, () -> {
				// Illegal removal of entity outside of active transaction.
				em.remove( entity );
				em.flush();
			} );
		} );
	}

	@Test
	public void testCollectionUpdateOutsideActiveTransaction(EntityManagerFactoryScope scope) {
		final Person person = scope.fromTransaction( em -> {
			Person p = new Person();
			Name name = new Name();
			name.setName( "Name" );
			p.getNames().add( name );
			em.persist( p );
			return p;
		} );

		assertThrows( TransactionRequiredException.class, () -> {
			scope.inEntityManager( em -> {
				// Illegal collection update outside of active transaction.
				person.getNames().clear();
				em.merge( person );
				em.flush();
			} );
		} );
	}

	@Test
	public void testCollectionRemovalOutsideActiveTransaction(EntityManagerFactoryScope scope) {
		final Person person = scope.fromTransaction( em -> {
			Person p = new Person();
			Name name = new Name();
			name.setName( "Name" );
			p.getNames().add( name );
			em.persist( p );
			return p;
		} );

		assertThrows( TransactionRequiredException.class, () -> {
			scope.inEntityManager( em -> {
				// Illegal collection removal outside of active transaction.
				person.setNames( null );
				em.merge( person );
				em.flush();
			} );
		} );
	}
}
