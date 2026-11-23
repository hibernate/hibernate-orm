package org.hibernate.orm.test.mapping.converted.converter.generics;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Converter;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import org.hibernate.boot.MetadataSources;
import org.hibernate.cfg.SchemaToolingSettings;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.util.ServiceRegistryUtil;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Verifies that an auto-applied converter uses the concrete type of an inherited
/// type-variable attribute, as supplied by the entity subclass.
///
/// @author Steve Ebersole
@JiraKey("HHH-13913")
public class GenericInheritedAttributeConverterTest {

	@Test
	@FailureExpected(jiraKey = "HHH-13913", reason = "Converter is not auto-applied to the inherited type-variable field")
	void testAutoAppliedConverterForInheritedTypeVariable() {
		// Build inside the test so FailureExpected can capture the bootstrap failure.
		try (var registry = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( SchemaToolingSettings.HBM2DDL_AUTO, "create-drop" )
				.build()) {
			try (var factory = new MetadataSources( registry )
					.addAnnotatedClass( FooContainer.class )
					.addAnnotatedClass( FooConverter.class )
					.buildMetadata()
					.buildSessionFactory()) {
				final FooContainer container = new FooContainer( Foo.from( "42" ) );
				factory.inTransaction( session -> session.persist( container ) );

				factory.inTransaction( session -> {
					assertThat( session.createNativeQuery(
							"select converted_value from FooContainer where id = :id", String.class )
							.setParameter( "id", container.getId() )
							.getSingleResult() ).isEqualTo( "42" );

					final FooContainer loaded = session.find( FooContainer.class, container.getId() );
					assertThat( loaded ).isNotNull().isNotSameAs( container );
					assertThat( loaded.getValue() ).isNotNull();
					assertThat( loaded.getValue().value() ).isEqualTo( "42" );
				} );
			}
		}
	}

	public interface Foo {
		String value();

		static Foo from(String value) {
			return () -> value;
		}
	}

	@Converter(autoApply = true)
	public static class FooConverter implements AttributeConverter<Foo, String> {
		@Override
		public String convertToDatabaseColumn(Foo attribute) {
			return attribute == null ? null : attribute.value();
		}

		@Override
		public Foo convertToEntityAttribute(String dbData) {
			return dbData == null ? null : Foo.from( dbData );
		}
	}

	@MappedSuperclass
	public static abstract class AbstractContainer<T> {
		@Id
		@GeneratedValue
		private Integer id;

		@Column(name = "converted_value")
		private T value;

		protected AbstractContainer() {
		}

		protected AbstractContainer(T value) {
			this.value = value;
		}

		public Integer getId() {
			return id;
		}

		public T getValue() {
			return value;
		}
	}

	@Entity(name = "FooContainer")
	public static class FooContainer extends AbstractContainer<Foo> {
		public FooContainer() {
		}

		public FooContainer(Foo value) {
			super( value );
		}
	}
}
