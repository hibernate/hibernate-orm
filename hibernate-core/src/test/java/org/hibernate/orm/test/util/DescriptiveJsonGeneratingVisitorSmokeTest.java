/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.jdbc.spi.DescriptiveJsonGeneratingVisitor;
import org.hibernate.type.format.StringJsonDocumentWriter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@DomainModel(annotatedClasses = {
		DescriptiveJsonGeneratingVisitorSmokeTest.Company.class,
		DescriptiveJsonGeneratingVisitorSmokeTest.Address.class,
		DescriptiveJsonGeneratingVisitorSmokeTest.Employee.class,
		DescriptiveJsonGeneratingVisitorSmokeTest.EntityOfArrays.class,
})
@SessionFactory
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJsonArray.class)
public class DescriptiveJsonGeneratingVisitorSmokeTest {
	private final DescriptiveJsonGeneratingVisitor visitor = new DescriptiveJsonGeneratingVisitor();

	private final ObjectMapper mapper = new ObjectMapper();

	@Test
	public void testCompany(SessionFactoryScope scope) {
		final SessionFactoryImplementor sessionFactory = scope.getSessionFactory();
		final EntityPersister entityDescriptor = sessionFactory.getMappingMetamodel()
				.findEntityDescriptor( Company.class );

		scope.inTransaction( session -> {
			final Company company = session.createSelectionQuery(
					"from Company where id = 1",
					Company.class
			).getSingleResult();

			try {
				final StringJsonDocumentWriter writer = new StringJsonDocumentWriter();
				visitor.visit( entityDescriptor.getEntityMappingType(), company, sessionFactory.getWrapperOptions(), writer );
				final String result = writer.toString();

				final JsonNode jsonNode = mapper.readTree( result );
				assertThat( jsonNode.get( "id" ).intValue() ).isEqualTo( 1 );
				assertThat( jsonNode.get( "name" ).textValue() ).isEqualTo( "Red Hat" );
				assertThat( jsonNode.get( "employees" ).textValue() ).isEqualTo( "<uninitialized>" );

				final JsonNode address = jsonNode.get( "address" );
				assertThat( address.get( "city" ).textValue() ).isEqualTo( "Milan" );
				assertThat( address.get( "street" ).textValue() ).isEqualTo( "Via Gustavo Fara" );
			}
			catch (Exception e) {
				fail( "Test failed with exception", e );
			}
		} );
	}

	@Test
	public void testCompanyFetchEmployees(SessionFactoryScope scope) {
		final SessionFactoryImplementor sessionFactory = scope.getSessionFactory();
		final EntityPersister entityDescriptor = sessionFactory.getMappingMetamodel()
				.findEntityDescriptor( Company.class );

		scope.inTransaction( session -> {
			final Company company = session.createSelectionQuery(
					"from Company c join fetch c.employees where c.id = 1",
					Company.class
			).getSingleResult();

			try {
				final StringJsonDocumentWriter writer = new StringJsonDocumentWriter();
				visitor.visit( entityDescriptor.getEntityMappingType(), company, sessionFactory.getWrapperOptions(), writer );
				final String result = writer.toString();

				final JsonNode jsonNode = mapper.readTree( result );
				assertThat( jsonNode.get( "id" ).intValue() ).isEqualTo( 1 );
				assertThat( jsonNode.get( "name" ).textValue() ).isEqualTo( "Red Hat" );

				final JsonNode employees = jsonNode.get( "employees" );
				assertThat( employees.isArray() ).isTrue();
				employees.forEach( employee -> {
					assertDoesNotThrow( () -> UUID.fromString( employee.get( "uniqueIdentifier" ).asText() ) );
					assertThat( employee.get( "firstName" ).textValue() ).startsWith( "Ma" );
					final JsonNode c = employee.get( "company" );
					assertThat( c.get( "id" ).intValue() ).isEqualTo( 1 );
					assertThat( c.properties().stream().map( Map.Entry::getKey ) )
							.containsOnly( "id" ); // circular relationship
				} );
			}
			catch (Exception e) {
				fail( "Test failed with exception", e );
			}
		} );
	}

	@Test
	public void testEntityOfArrays(SessionFactoryScope scope) {
		final SessionFactoryImplementor sessionFactory = scope.getSessionFactory();
		final EntityPersister entityDescriptor = sessionFactory.getMappingMetamodel()
				.findEntityDescriptor( EntityOfArrays.class );

		scope.inTransaction( session -> {
			final var entity = session.createSelectionQuery(
					"from EntityOfArrays e where id = 1",
					EntityOfArrays.class
			).getSingleResult();

			try {
				final StringJsonDocumentWriter writer = new StringJsonDocumentWriter();
				visitor.visit( entityDescriptor.getEntityMappingType(), entity, sessionFactory.getWrapperOptions(), writer );
				final String result = writer.toString();

				final JsonNode jsonNode = mapper.readTree( result );
				System.out.println("Result: "+jsonNode.toPrettyString());

				assertJsonArray( jsonNode.get( "integerArray" ), Arrays.asList( 1, null, 3 ) );
				assertJsonArray( jsonNode.get( "stringCollectionArray" ), List.of( "one", "two", "three" ) );
				assertJsonArray( jsonNode.get( "longArray" ), List.of( 10L, 20L, 30L ) );
				assertJsonArray( jsonNode.get( "booleanListArray" ), List.of( true, false, true ) );
			}
			catch (Exception e) {
				fail( "Test failed with exception", e );
			}
		} );
	}

	private static void assertJsonArray(JsonNode jsonNode, List<?> expectedValues) {
		assertThat( jsonNode.isArray() ).isTrue();
		assertThat( jsonNode.size() ).isEqualTo( expectedValues.size() );
		for (int i = 0; i < expectedValues.size(); i++) {
			final Object expectedValue = expectedValues.get( i );
			final JsonNode element = jsonNode.get( i );
			if ( expectedValue == null ) {
				assertThat( element.isNull() ).isTrue();
			}
			else if ( expectedValue instanceof Boolean ) {
				assertThat( element.booleanValue() ).isEqualTo( expectedValue );
			}
			else if ( expectedValue instanceof Integer ) {
				assertThat( element.intValue() ).isEqualTo( expectedValue );
			}
			else if ( expectedValue instanceof Long ) {
				assertThat( element.longValue() ).isEqualTo( expectedValue );
			}
			else if ( expectedValue instanceof String ) {
				assertThat( element.textValue() ).isEqualTo( expectedValue );
			}
			else {
				fail( "Unsupported element type: " + expectedValue.getClass() );
			}
		}
	}

	@BeforeAll
	public void beforeAll(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Company rh = new Company( 1L, "Red Hat", new Address( "Milan", "Via Gustavo Fara" ) );
			session.persist( rh );
			session.persist( new Employee( UUID.randomUUID(), "Marco", "Belladelli", 100_000, rh ) );
			session.persist( new Employee( UUID.randomUUID(), "Matteo", "Cauzzi", 50_000, rh ) );
			final var ofArrays = new EntityOfArrays();
			ofArrays.id = 1L;
			ofArrays.integerArray = new Integer[] { 1, null, 3 };
			ofArrays.stringCollectionArray = List.of( "one", "two", "three" );
			ofArrays.longArray = new long[] { 10, 20, 30 };
			ofArrays.booleanListArray = List.of( true, false, true );
			session.persist( ofArrays );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Entity(name = "Company")
	static class Company implements Serializable {
		@Id
		private long id;

		@Column(nullable = false)
		private String name;

		@Embedded
		private Address address;

		@OneToMany(mappedBy = "company")
		private List<Employee> employees;

		public Company() {
		}

		public Company(long id, String name, Address address) {
			this.id = id;
			this.name = name;
			this.address = address;
			this.employees = new ArrayList<>();
		}

		public long getId() {
			return id;
		}

		public void setId(long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Address getAddress() {
			return address;
		}

		public void setAddress(Address address) {
			this.address = address;
		}
	}

	@Embeddable
	static class Address implements Serializable {
		private String city;

		private String street;

		public Address() {
		}

		public Address(String city, String street) {
			this.city = city;
			this.street = street;
		}

		public String getCity() {
			return city;
		}

		public void setCity(String city) {
			this.city = city;
		}

		public String getStreet() {
			return street;
		}

		public void setStreet(String street) {
			this.street = street;
		}
	}


	@Entity(name = "Employee")
	static class Employee {
		private UUID uniqueIdentifier;

		private String firstName;

		private String lastName;

		private float salary;

		private Company company;

		public Employee() {
		}

		public Employee(UUID uniqueIdentifier, String firstName, String lastName, float salary, Company company) {
			this.uniqueIdentifier = uniqueIdentifier;
			this.firstName = firstName;
			this.lastName = lastName;
			this.salary = salary;
			this.company = company;
		}

		@Id
		public UUID getUniqueIdentifier() {
			return uniqueIdentifier;
		}

		public void setUniqueIdentifier(UUID uniqueIdentifier) {
			this.uniqueIdentifier = uniqueIdentifier;
		}

		public String getFirstName() {
			return firstName;
		}

		public void setFirstName(String firstName) {
			this.firstName = firstName;
		}

		public String getLastName() {
			return lastName;
		}

		public void setLastName(String lastName) {
			this.lastName = lastName;
		}

		public float getSalary() {
			return salary;
		}

		public void setSalary(float salary) {
			this.salary = salary;
		}

		@ManyToOne
		public Company getCompany() {
			return company;
		}

		public void setCompany(Company company) {
			this.company = company;
		}
	}

	@Entity(name = "EntityOfArrays")
	static class EntityOfArrays {
		@Id
		private Long id;

		@JdbcTypeCode(SqlTypes.ARRAY)
		private Integer[] integerArray;

		@JdbcTypeCode(SqlTypes.ARRAY)
		private Collection<String> stringCollectionArray;

		@JdbcTypeCode(SqlTypes.JSON_ARRAY)
		private long[] longArray;

		@JdbcTypeCode(SqlTypes.JSON_ARRAY)
		private List<Boolean> booleanListArray;
	}
}
