package org.hibernate.test.converter.generics;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Table;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Christian Koppen
 * @author Nathan Xu
 */
public class InheritanceGenericsFieldTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { FooConverter.class, FooContainer.class };
	}

	@Test
	@TestForIssue( jiraKey = "HHH-13913")
	public void testConverterAutoApplied() {
		EntityManager entityManager = getOrCreateEntityManager();
		entityManager.getTransaction().begin();
		final FooContainer e1 = new FooContainer( Foo.from( "42" ) );
		entityManager.persist( e1 );
		entityManager.getTransaction().commit();
		entityManager.close();

		entityManager = getOrCreateEntityManager();
		entityManager.getTransaction().begin();
		final FooContainer e2 = entityManager.find( FooContainer.class, e1.getId() );
		assertEquals( e1.getValue().value(), e2.getValue().value() );
		entityManager.getTransaction().commit();
	}

	@MappedSuperclass
	public static abstract class AbstractContainer<T> {

		@Id
		@GeneratedValue
		private Integer id;

		private T value;

		protected AbstractContainer() {
			super();
		}

		protected AbstractContainer(final T value) {
			super();
			setValue(value);
		}

		public Integer getId() {
			return id;
		}

		public T getValue() {
			return value;
		}

		public void setValue(final T value) {
			this.value = value;
		}
	}

	@Entity(name = "FooContainer")
	@Table(name = "FooContainer")
	public static class FooContainer extends AbstractContainer<Foo> {
		protected FooContainer() {
			super();
		}

		public FooContainer(Foo value) {
			super(value);
		}
	}

	@Converter(autoApply = true)
	public static class FooConverter implements AttributeConverter<Foo, String> {

		@Override
		public String convertToDatabaseColumn(final Foo attribute) {
			return attribute.value();
		}

		@Override
		public Foo convertToEntityAttribute(final String dbData) {
			return Foo.from(dbData);
		}
	}

	// didn't implement Serializable so mapping exception will be thrown if converter doesn't take effect!
	public static class Foo {
		private String value;
		private Foo(String value) {
			this.value = value;
		}
		public static Foo from(String value) {
			return new Foo(value);
		}
		public String value() {
			return this.value;
		}
	}
}
