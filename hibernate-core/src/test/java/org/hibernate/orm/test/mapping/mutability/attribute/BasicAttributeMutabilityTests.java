/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.mutability.attribute;

import java.time.Instant;
import java.util.Date;

import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Mutability;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.descriptor.java.Immutability;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tested premises:
 *
 * 1. `@Immutable` on a basic attribute -
 * 		* not-updateable
 * 		* immutable
 * 2. `@Mutability(Immutability.class)` on basic attribute
 * 		* updateable
 * 		* immutable
 *
 * @author Steve Ebersole
 */
@DomainModel( annotatedClasses = BasicAttributeMutabilityTests.TheEntity.class )
@SessionFactory
public class BasicAttributeMutabilityTests {
	private static final Instant START = Instant.ofEpochMilli( 1676049527493L );

	@Test
	public void verifyDomainModel(DomainModelScope domainModelScope, SessionFactoryScope sfSessionFactoryScope) {
		final PersistentClass persistentClass = domainModelScope
				.getDomainModel()
				.getEntityBinding( TheEntity.class.getName() );
		final EntityPersister entityDescriptor = sfSessionFactoryScope.getSessionFactory()
				.getRuntimeMetamodels()
				.getMappingMetamodel()
				.getEntityDescriptor( TheEntity.class );

		// `@Immutable`
		final Property theDateProperty = persistentClass.getProperty( "theDate" );
		assertThat( theDateProperty.isUpdateable() ).isFalse();
		final AttributeMapping theDateAttribute = entityDescriptor.findAttributeMapping( "theDate" );
		assertThat( theDateAttribute.getExposedMutabilityPlan().isMutable() ).isFalse();

		// `@Mutability(Immutability.class)`
		final Property anotherDateProperty = persistentClass.getProperty( "anotherDate" );
		assertThat( anotherDateProperty.isUpdateable() ).isTrue();
		final AttributeMapping anotherDateAttribute = entityDescriptor.findAttributeMapping( "anotherDate" );
		assertThat( anotherDateAttribute.getExposedMutabilityPlan().isMutable() ).isFalse();
	}

	/**
	 * `@Immutable` attribute while managed - no update
	 */
	@Test
	public void testImmutableManaged(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.persist( new TheEntity( 1, "@Immutable test", Date.from( START ) ) );
		} );

		// try to update the managed form
		scope.inTransaction( (session) -> {
			//tag::attribute-immutable-managed-example[]
			final TheEntity theEntity = session.find( TheEntity.class, 1 );
			// this change will be ignored
			theEntity.theDate.setTime( Instant.EPOCH.toEpochMilli() );
			//end::attribute-immutable-managed-example[]
		} );

		// verify the value did not change
		scope.inTransaction( (session) -> {
			final TheEntity theEntity = session.find( TheEntity.class, 1 );
			assertThat( theEntity.theDate.getTime() ).isEqualTo( START.toEpochMilli() );
		} );
	}

	/**
	 * `@Immutable` attribute on merged detached value - no update (its non-updateable)
	 */
	@Test
	public void testImmutableDetached(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.persist( new TheEntity( 1, "@Immutable test", Date.from( START ) ) );
		} );

		// load a detached reference
		final TheEntity detached = scope.fromTransaction( (session) -> {
			final TheEntity theEntity = session.find( TheEntity.class, 1 );
			assertThat( theEntity.theDate.getTime() ).isEqualTo( START.toEpochMilli() );
			return theEntity;
		} );

		// make the change again, this time to a detached instance and merge it.
		//tag::attribute-immutable-merge-example[]
		detached.theDate.setTime( Instant.EPOCH.toEpochMilli() );
		scope.inTransaction( (session) -> session.merge( detached ) );
		//end::attribute-immutable-merge-example[]

		// verify the value did not change via the merge
		scope.inTransaction( (session) -> {
			final TheEntity theEntity = session.find( TheEntity.class, 1 );
			assertThat( theEntity.theDate.getTime() ).isEqualTo( START.toEpochMilli() );
		} );
	}

	/**
	 * `@Mutability(Immutability.class)` attribute while managed - no update
	 */
	@Test
	public void testImmutabilityManaged(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.persist( new TheEntity( 1, "@Mutability test", Date.from( START ) ) );
		} );

		// try to update the managed form
		scope.inTransaction( (session) -> {
			//tag::attribute-immutability-managed-example[]
			final TheEntity theEntity = session.find( TheEntity.class, 1 );
			theEntity.anotherDate.setTime( Instant.EPOCH.toEpochMilli() );
			//end::attribute-immutability-managed-example[]
		} );

		// reload it and verify the value did not change
		scope.inTransaction( (session) -> {
			final TheEntity theEntity = session.find( TheEntity.class, 1 );
			assertThat( theEntity.anotherDate.getTime() ).isEqualTo( START.toEpochMilli() );
		} );
	}

	/**
	 * `@Mutability(Immutability.class)` attribute while managed - update because
	 * we cannot distinguish one type of change from another while detached
	 */
	@Test
	public void testImmutabilityDetached(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.persist( new TheEntity( 1, "@Mutability test", Date.from( START ) ) );
		} );

		// load a detached reference
		final TheEntity detached = scope.fromTransaction( (session) -> session.find( TheEntity.class, 1 ) );

		//tag::attribute-immutability-merge-example[]
		detached.anotherDate.setTime( Instant.EPOCH.toEpochMilli() );
		scope.inTransaction( (session) -> session.merge( detached ) );
		//end::attribute-immutability-merge-example[]

		// verify the change was persisted
		scope.inTransaction( (session) -> {
			final TheEntity theEntity = session.find( TheEntity.class, 1 );
			assertThat( theEntity.anotherDate.getTime() ).isEqualTo( Instant.EPOCH.toEpochMilli() );
		} );
	}

	/**
	 * Normal mutable value while managed
	 */
	@Test
	public void testMutableManaged(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.persist( new TheEntity( 1, "Baseline mutable test", Date.from( START ) ) );
		} );

		// try to mutate the managed form
		scope.inTransaction( (session) -> {
			final TheEntity theEntity = session.find( TheEntity.class, 1 );
			theEntity.mutableDate.setTime( Instant.EPOCH.toEpochMilli() );
		} );

		// verify the value did change
		scope.inTransaction( (session) -> {
			final TheEntity theEntity = session.find( TheEntity.class, 1 );
			assertThat( theEntity.mutableDate.getTime() ).isEqualTo( Instant.EPOCH.toEpochMilli() );
		} );
	}

	/**
	 * Normal mutable value while detached
	 */
	@Test
	public void testMutableMerge(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.persist( new TheEntity( 1, "Baseline mutable test", Date.from( START ) ) );
		} );

		// load a detached reference
		final TheEntity detached = scope.fromTransaction( (session) -> session.find( TheEntity.class, 1 ) );

		// now mutate the detached state and merge
		detached.mutableDate.setTime( START.toEpochMilli() );
		scope.inTransaction( (session) -> session.merge( detached ) );

		// verify the value did change
		scope.inTransaction( (session) -> {
			final TheEntity theEntity = session.find( TheEntity.class, 1 );
			assertThat( theEntity.mutableDate.getTime() ).isEqualTo( START.toEpochMilli() );
		} );
	}

	@AfterEach
	void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Entity( name = "TheEntity" )
	@Table( name = "TheEntity" )
	public static class TheEntity {
		@Id
		private Integer id;

		@Basic
		private String name;

		//tag::attribute-immutable-example[]
		@Immutable
		private Date theDate;
		//end::attribute-immutable-example[]

		//tag::attribute-immutability-example[]
		@Mutability(Immutability.class)
		private Date anotherDate;
		//end::attribute-immutability-example[]

		private Date mutableDate;

		private TheEntity() {
			// for use by Hibernate
		}

		public TheEntity(Integer id, String name, Date aDate) {
			this.id = id;
			this.name = name;
			this.theDate = aDate;
			this.anotherDate = aDate;
			this.mutableDate = aDate;
		}
	}
}
