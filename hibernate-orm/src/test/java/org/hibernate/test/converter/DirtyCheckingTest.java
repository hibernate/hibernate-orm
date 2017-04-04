/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.converter;

import javax.persistence.AttributeConverter;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.Session;

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
		session.save( simpleEntity );
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
		session.delete( loaded );
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
		session.save( simpleEntity );
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
		session.delete( loaded );
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
		session.save( simpleEntity );
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
		session.delete( loaded );
		session.getTransaction().commit();
		session.close();
	}

	@Test
	public void checkConverterMutabilityPlans() {
		final EntityPersister persister = sessionFactory().getEntityPersister( SomeEntity.class.getName() );
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
