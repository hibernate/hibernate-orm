/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.converted.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Steve Ebersole
 */
@DomainModel(annotatedClasses = {DirtyCheckingTest.SomeEntity.class})
@SessionFactory
public class DirtyCheckingTest {

	@Test
	public void dirtyCheckAgainstNewNameInstance(SessionFactoryScope scope) {
		SomeEntity simpleEntity = new SomeEntity();
		simpleEntity.setId( 1L );
		simpleEntity.setName( new Name( "Steven" ) );

		scope.inTransaction( session -> session.persist( simpleEntity ) );

		scope.inTransaction( session -> {
			SomeEntity loaded = session.find( SomeEntity.class, 1L );
			loaded.setName( new Name( "Steve" ) );
		} );

		scope.inTransaction( session -> {
			SomeEntity loaded = session.find( SomeEntity.class, 1L );
			assertEquals( "Steve", loaded.getName().getText() );
			session.remove( loaded );
		} );
	}

	@Test
	public void dirtyCheckAgainstMutatedNameInstance(SessionFactoryScope scope) {
		SomeEntity simpleEntity = new SomeEntity();
		simpleEntity.setId( 1L );
		simpleEntity.setName( new Name( "Steven" ) );

		scope.inTransaction( session -> session.persist( simpleEntity ) );

		scope.inTransaction( session -> {
			SomeEntity loaded = session.find( SomeEntity.class, 1L );
			loaded.getName().setText( "Steve" );
		} );

		scope.inTransaction( session -> {
			SomeEntity loaded = session.find( SomeEntity.class, 1L );
			assertEquals( "Steve", loaded.getName().getText() );
			session.remove( loaded );
		} );
	}

	@Test
	public void dirtyCheckAgainstNewNumberInstance(SessionFactoryScope scope) {
		// numbers (and most other java types) are actually immutable...
		SomeEntity simpleEntity = new SomeEntity();
		simpleEntity.setId( 1L );
		simpleEntity.setNumber( 1 );

		scope.inTransaction( session -> session.persist( simpleEntity ) );

		scope.inTransaction( session -> {
			SomeEntity loaded = session.find( SomeEntity.class, 1L );
			loaded.setNumber( 2 );
		} );

		scope.inTransaction( session -> {
			SomeEntity loaded = session.find( SomeEntity.class, 1L );
			assertEquals( 2, loaded.getNumber().intValue() );
			session.remove( loaded );
		} );
	}

	@Test
	public void checkConverterMutabilityPlans(SessionFactoryScope scope) {
		final EntityPersister persister = scope.getSessionFactory().getMappingMetamodel().getEntityDescriptor(SomeEntity.class.getName());
		final AttributeMapping numberMapping = persister.findAttributeMapping( "number" );
		final AttributeMapping nameMapping = persister.findAttributeMapping( "name" );
		assertFalse( numberMapping.getExposedMutabilityPlan().isMutable() );
		assertTrue( nameMapping.getExposedMutabilityPlan().isMutable() );
	}

	public static class Name {
		private String text;

		public Name() {
		}

		public Name(String text) {
			this.text = text;
		}

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}
	}

	public static class NameConverter implements AttributeConverter<Name, String> {
		public String convertToDatabaseColumn(Name name) {
			return name == null ? null : name.getText();
		}

		public Name convertToEntityAttribute(String s) {
			return s == null ? null : new Name( s );
		}
	}

	public static class IntegerConverter implements AttributeConverter<Integer, String> {
		public String convertToDatabaseColumn(Integer value) {
			return value == null ? null : value.toString();
		}

		public Integer convertToEntityAttribute(String s) {
			return s == null ? null : Integer.parseInt( s );
		}
	}

	@Entity(name = "SomeEntity")
	public static class SomeEntity {
		@Id
		private Long id;

		@Convert(converter = IntegerConverter.class)
		@Column(name = "num")
		private Integer number;

		@Convert(converter = NameConverter.class)
		@Column(name = "name")
		private Name name = new Name();

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Integer getNumber() {
			return number;
		}

		public void setNumber(Integer number) {
			this.number = number;
		}

		public Name getName() {
			return name;
		}

		public void setName(Name name) {
			this.name = name;
		}
	}
}
