/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.id;

import java.io.Serializable;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.ManyToOne;


import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/**
 * @author Vlad Mihalcea
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey( value = "HHH-12251" )
@DomainModel(annotatedClasses = {
		IdClassManyToOneCascadeTest.SomeEntity.class,
		IdClassManyToOneCascadeTest.ReferencedEntity.class
})
@SessionFactory
public class IdClassManyToOneCascadeTest {
	@AfterEach
	public void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	public void testMergeCascadesToManyToOne(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( entityManager -> {
			ReferencedEntity referencedEntity = new ReferencedEntity();
			referencedEntity.setId( 42L );

			SomeEntity someEntity = new SomeEntity();
			someEntity.setId( 23L );
			someEntity.setReferencedEntity( referencedEntity );

			SomeEntity merged = entityManager.merge(someEntity);

			Assertions.assertTrue( entityManager.contains( merged.getReferencedEntity() ) );
		} );
	}

	@Test
	public void testPersistCascadesToManyToOne(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( entityManager -> {
			ReferencedEntity referencedEntity = new ReferencedEntity();
			referencedEntity.setId( 42L );

			SomeEntity someEntity = new SomeEntity();
			someEntity.setId( 23L );
			someEntity.setReferencedEntity( referencedEntity );

			entityManager.persist( someEntity );

			Assertions.assertTrue( entityManager.contains( referencedEntity ) );
		} );
	}

	@Entity(name = "SomeEntity")
	@IdClass(SomeEntityPK.class)
	public static class SomeEntity {

		@Id
		private long id;

		@Id
		@ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
		private ReferencedEntity referencedEntity;

		public ReferencedEntity getReferencedEntity() {
			return referencedEntity;
		}

		public void setReferencedEntity(ReferencedEntity referencedEntity) {
			this.referencedEntity = referencedEntity;
		}

		public long getId() {
			return id;
		}

		public void setId(long id) {
			this.id = id;
		}
	}

	public static class SomeEntityPK implements Serializable {

		private Long id;
		private Long referencedEntity;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Long getReferencedEntity() {
			return referencedEntity;
		}

		public void setReferencedEntity(Long referencedEntity) {
			this.referencedEntity = referencedEntity;
		}
	}

	@Entity(name = "ReferencedEntity")
	public static class ReferencedEntity {

		@Id
		private Long id;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}
	}
}
