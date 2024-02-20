package org.hibernate.orm.test.any.annotations;

import java.util.List;

import jakarta.persistence.TypedQuery;
import org.hibernate.Hibernate;
import org.hibernate.annotations.Any;
import org.hibernate.annotations.AnyDiscriminator;
import org.hibernate.annotations.AnyDiscriminatorValue;
import org.hibernate.annotations.AnyDiscriminatorValues;
import org.hibernate.annotations.AnyKeyJavaClass;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Query;
import jakarta.persistence.Table;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Jpa(
		annotatedClasses = {
				EagerAnyDiscriminatorQueryTest.PropertyHolder.class,
				EagerAnyDiscriminatorQueryTest.StringProperty.class
		}
)
@TestForIssue(jiraKey = "HHH-15423")
public class EagerAnyDiscriminatorQueryTest {
	private static final Long PROPERTY_HOLDER_ID = 2l;
	private static final Long STRING_PROPERTY_ID = 1l;
	private static final String STRING_PROPERTY_VALUE = "it is a string";

	@BeforeAll
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					StringProperty property = new StringProperty(
							STRING_PROPERTY_ID,
							STRING_PROPERTY_VALUE
					);
					PropertyHolder propertyHolder = new PropertyHolder(
							PROPERTY_HOLDER_ID,
							property
					);
					entityManager.persist( property );
					entityManager.persist( propertyHolder );
				}
		);
	}

	@Test
	public void testNativeQuery(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Query q = entityManager.createNativeQuery(
							"select * from PROPERTY_HOLDER_TABLE",
							PropertyHolder.class
					);
					List<PropertyHolder> results = q.getResultList();
					assertThat( results.size() ).isEqualTo( 1 );
					PropertyHolder propertyHolder = results.get( 0 );

					Property property = propertyHolder.getProperty();

					assertTrue( Hibernate.isInitialized( property ) );

					assertThat( property.getId() ).isEqualTo( STRING_PROPERTY_ID );
					assertThat( property.getValue() ).isEqualTo( STRING_PROPERTY_VALUE );
				}
		);
	}

	@Test
	public void testHQLQuery(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					TypedQuery<PropertyHolder> q = entityManager.createQuery(
							"select p from PropertyHolder p",
							PropertyHolder.class
					);
					List<PropertyHolder> results = q.getResultList();
					assertThat( results.size() ).isEqualTo( 1 );
					PropertyHolder propertyHolder = results.get( 0 );

					Property property = propertyHolder.getProperty();

					assertTrue( Hibernate.isInitialized( property ) );

					assertThat( property.getId() ).isEqualTo( STRING_PROPERTY_ID );
					assertThat( property.getValue() ).isEqualTo( STRING_PROPERTY_VALUE );
				}
		);
	}

	@Test
	public void testFind(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					PropertyHolder propertyHolder = entityManager.find(
							PropertyHolder.class,
							PROPERTY_HOLDER_ID
					);
					assertThat( propertyHolder ).isNotNull();
					Property property = propertyHolder.getProperty();

					assertTrue( Hibernate.isInitialized( property ) );

					assertThat( property.getId() ).isEqualTo( STRING_PROPERTY_ID );
					assertThat( property.getValue() ).isEqualTo( STRING_PROPERTY_VALUE );
				}
		);
	}

	@Entity(name = "PropertyHolder")
	@Table(name = "PROPERTY_HOLDER_TABLE")
	public static class PropertyHolder {
		@Id
		private Long id;

		@Any
		@AnyDiscriminator(DiscriminatorType.STRING)
		@AnyDiscriminatorValues({
				@AnyDiscriminatorValue(discriminator = "S", entity = StringProperty.class),
		})
		@AnyKeyJavaClass(Long.class)
		@Column(name = "property_type")
		@JoinColumn(name = "property_id")
		private Property property;

		public PropertyHolder() {
		}

		public PropertyHolder(Long id, Property property) {
			this.id = id;
			this.property = property;
		}

		public Long getId() {
			return id;
		}

		public Property getProperty() {
			return property;
		}
	}

	public interface Property<T> {
		Long getId();

		T getValue();
	}


	@Entity(name = "StringProperty")
	public static class StringProperty implements Property<String> {
		@Id
		private Long id;

		@Column(name = "VALUE_COLUMN")
		private String value;


		public StringProperty() {
		}

		public StringProperty(Long id, String value) {
			this.id = id;
			this.value = value;
		}

		@Override
		public Long getId() {
			return id;
		}

		@Override
		public String getValue() {
			return value;
		}
	}
}
