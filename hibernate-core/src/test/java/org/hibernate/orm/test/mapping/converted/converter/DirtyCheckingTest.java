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

import org.hibernate.Session;

import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Steve Ebersole
 */
public class DirtyCheckingTest extends BaseNonConfigCoreFunctionalTestCase {

	@Test
	public void dirtyCheckAgainstNewNameInstance() {
		SomeEntity simpleEntity = new SomeEntity();
		simpleEntity.setId( 1L );
		simpleEntity.setName( new Name( "Steven" ) );

		Session session = openSession();
		session.getTransaction().begin();
		session.persist( simpleEntity );
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.getTransaction().begin();
		SomeEntity loaded = session.byId( SomeEntity.class ).load( 1L );
		loaded.setName( new Name( "Steve" ) );
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.getTransaction().begin();
		loaded = session.byId( SomeEntity.class ).load( 1L );
		assertEquals( "Steve", loaded.getName().getText() );
		session.remove( loaded );
		session.getTransaction().commit();
		session.close();
	}

	@Test
	public void dirtyCheckAgainstMutatedNameInstance() {
		SomeEntity simpleEntity = new SomeEntity();
		simpleEntity.setId( 1L );
		simpleEntity.setName( new Name( "Steven" ) );

		Session session = openSession();
		session.getTransaction().begin();
		session.persist( simpleEntity );
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.getTransaction().begin();
		SomeEntity loaded = session.byId( SomeEntity.class ).load( 1L );
		loaded.getName().setText( "Steve" );
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.getTransaction().begin();
		loaded = session.byId( SomeEntity.class ).load( 1L );
		assertEquals( "Steve", loaded.getName().getText() );
		session.remove( loaded );
		session.getTransaction().commit();
		session.close();
	}

	@Test
	public void dirtyCheckAgainstNewNumberInstance() {
		// numbers (and most other java types) are actually immutable...
		SomeEntity simpleEntity = new SomeEntity();
		simpleEntity.setId( 1L );
		simpleEntity.setNumber( 1 );

		Session session = openSession();
		session.getTransaction().begin();
		session.persist( simpleEntity );
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.getTransaction().begin();
		SomeEntity loaded = session.byId( SomeEntity.class ).load( 1L );
		loaded.setNumber( 2 );
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.getTransaction().begin();
		loaded = session.byId( SomeEntity.class ).load( 1L );
		assertEquals( 2, loaded.getNumber().intValue() );
		session.remove( loaded );
		session.getTransaction().commit();
		session.close();
	}

	@Test
	public void checkConverterMutabilityPlans() {
		final EntityPersister persister = sessionFactory().getMappingMetamodel().getEntityDescriptor(SomeEntity.class.getName());
		final AttributeMapping numberMapping = persister.findAttributeMapping( "number" );
		final AttributeMapping nameMapping = persister.findAttributeMapping( "name" );

		assertFalse( persister.getPropertyType( "number" ).isMutable() );
		assertTrue( persister.getPropertyType( "name" ).isMutable() );
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {SomeEntity.class};
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
