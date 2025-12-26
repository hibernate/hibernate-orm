/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.language;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.SelectionQuery;
import org.hibernate.tool.language.domain.Address;
import org.hibernate.tool.language.domain.Company;
import org.hibernate.tool.language.domain.Employee;
import org.hibernate.tool.language.internal.ResultsJsonSerializerImpl;

import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.domain.animal.Cat;
import org.hibernate.testing.orm.domain.animal.Human;
import org.hibernate.testing.orm.domain.animal.Name;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.Tuple;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@DomainModel(annotatedClasses = {
		Address.class, Company.class, Employee.class,
}, standardModels = {
		StandardDomainModel.ANIMAL
})
@SessionFactory
public class ResultsJsonSerializerTest {
	private final ObjectMapper mapper = new ObjectMapper();

	@Test
	public void testEmbedded(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final SelectionQuery<Address> q = query(
					"select address from Company where id = 1",
					Address.class,
					session
			);

			try {
				final String result = toString( q.getResultList(), q, scope.getSessionFactory() );

				final JsonNode jsonNode = getSingleValue( mapper.readTree( result ) );
				assertThat( jsonNode.get( "city" ).textValue() ).isEqualTo( "Milan" );
				assertThat( jsonNode.get( "street" ).textValue() ).isEqualTo( "Via Gustavo Fara" );
			}
			catch (JsonProcessingException e) {
				fail( "Serialization failed with exception", e );
			}
		} );
	}

	@Test
	public void testEmbeddedSubPart(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final SelectionQuery<String> q = query(
					"select address.city from Company where id = 1",
					String.class,
					session
			);

			try {
				final String result = toString( q.getResultList(), q, scope.getSessionFactory() );

				final JsonNode jsonNode = getSingleValue( mapper.readTree( result ) );
				assertThat( jsonNode.textValue() ).isEqualTo( "Milan" );
			}
			catch (JsonProcessingException e) {
				fail( "Serialization failed with exception", e );
			}
		} );
	}

	@Test
	public void testNumericFunction(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final SelectionQuery<Long> q = query( "select max(id) from Company", Long.class, session );

			try {
				final String result = toString( q.getResultList(), q, scope.getSessionFactory() );

				final JsonNode jsonNode = getSingleValue( mapper.readTree( result ) );
				assertThat( jsonNode.intValue() ).isEqualTo( 4 );
			}
			catch (JsonProcessingException e) {
				fail( "Serialization failed with exception", e );
			}
		} );
	}

	@Test
	public void testStringyFunction(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final SelectionQuery<String> q = query(
					"select upper(name) from Company where id = 1",
					String.class,
					session
			);

			try {
				final String result = toString( q.getResultList(), q, scope.getSessionFactory() );

				final JsonNode jsonNode = getSingleValue( mapper.readTree( result ) );
				assertThat( jsonNode.textValue() ).isEqualTo( "RED HAT" );
			}
			catch (JsonProcessingException e) {
				fail( "Serialization failed with exception", e );
			}
		} );
	}

	@Test
	public void testNullFunction(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final SelectionQuery<String> q = query(
					"select lower(address.street) from Company where id = 4",
					String.class,
					session
			);

			try {
				final String result = toString( q.getResultList(), q, scope.getSessionFactory() );

				final JsonNode jsonNode = getSingleValue( mapper.readTree( result ) );
				assertThat( jsonNode.isNull() ).isTrue();
			}
			catch (JsonProcessingException e) {
				fail( "Serialization failed with exception", e );
			}
		} );
	}

	@Test
	public void testCompany(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final SelectionQuery<Company> q = query( "from Company where id = 1", Company.class, session );

			try {
				final String result = toString( q.getResultList(), q, scope.getSessionFactory() );

				final JsonNode jsonNode = getSingleValue( mapper.readTree( result ) );
				assertThat( jsonNode.get( "id" ).intValue() ).isEqualTo( 1 );
				assertThat( jsonNode.get( "name" ).textValue() ).isEqualTo( "Red Hat" );
				assertThat( jsonNode.get( "employees" ).textValue() ).isEqualTo( "<uninitialized>" );

				final JsonNode address = jsonNode.get( "address" );
				assertThat( address.get( "city" ).textValue() ).isEqualTo( "Milan" );
				assertThat( address.get( "street" ).textValue() ).isEqualTo( "Via Gustavo Fara" );
			}
			catch (JsonProcessingException e) {
				fail( "Serialization failed with exception", e );
			}
		} );
	}

	@Test
	public void testMultipleSelectionsArray(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final SelectionQuery<Object[]> q = query(
					"SELECT e.firstName, e.lastName FROM Employee e JOIN e.company c WHERE c.name = 'IBM'",
					Object[].class,
					session
			);

			try {
				final String result = toString( q.getResultList(), q, scope.getSessionFactory() );

				System.out.println(result);

				final JsonNode jsonNode = getSingleValue( mapper.readTree( result ) );
				assertThat( jsonNode.isArray() ).isTrue();
				assertThat( jsonNode.get( 0 ).asText() ).isEqualTo( "Andrea" );
				assertThat( jsonNode.get( 1 ).asText() ).isEqualTo( "Boriero" );
			}
			catch (JsonProcessingException e) {
				fail( "Serialization failed with exception", e );
			}
		} );
	}

	@Test
	public void testMultipleSelectionsTuple(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final SelectionQuery<Tuple> q = query(
					"SELECT e.firstName, e.lastName FROM Employee e where e.company.id = 1 ORDER BY e.lastName, e.firstName",
					Tuple.class,
					session
			);

			try {
				final String result = toString( q.getResultList(), q, scope.getSessionFactory() );

				System.out.println(result);

				final JsonNode jsonNode = mapper.readTree( result );
				assertThat( jsonNode.isArray() ).isTrue();
				assertThat( jsonNode.size() ).isEqualTo( 2 );

				final JsonNode first = jsonNode.get( 0 );
				assertThat( first.isArray() ).isTrue();
				assertThat( first.size() ).isEqualTo( 2 );
				assertThat( first.get( 0 ).asText() ).isEqualTo( "Marco" );
				assertThat( first.get( 1 ).asText() ).isEqualTo( "Belladelli" );

				final JsonNode second = jsonNode.get( 1 );
				assertThat( second.isArray() ).isTrue();
				assertThat( second.size() ).isEqualTo( 2 );
				assertThat( second.get( 0 ).asText() ).isEqualTo( "Matteo" );
				assertThat( second.get( 1 ).asText() ).isEqualTo( "Cauzzi" );
			}
			catch (JsonProcessingException e) {
				fail( "Serialization failed with exception", e );
			}
		} );
	}

	@Test
	public void testCompanyFetchEmployees(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final SelectionQuery<Company> q = query(
					"from Company c join fetch c.employees where c.id = 1",
					Company.class,
					session
			);

			try {
				final String result = toString( q.getResultList(), q, scope.getSessionFactory() );

				final JsonNode jsonNode = getSingleValue( mapper.readTree( result ) );
				assertThat( jsonNode.get( "id" ).intValue() ).isEqualTo( 1 );
				assertThat( jsonNode.get( "name" ).textValue() ).isEqualTo( "Red Hat" );

				final JsonNode employees = jsonNode.get( "employees" );
				assertThat( employees.isArray() ).isTrue();
				employees.forEach( employee -> {
					assertDoesNotThrow( () -> UUID.fromString( employee.get( "uniqueIdentifier" ).asText() ) );
					assertThat( employee.get( "firstName" ).textValue() ).startsWith( "Ma" );
					final JsonNode company = employee.get( "company" );
					assertThat( company.get( "id" ).intValue() ).isEqualTo( 1 );
					assertThat( company.properties().stream().map( Map.Entry::getKey ) )
							.containsOnly( "id" ); // circular relationship
				} );
			}
			catch (JsonProcessingException e) {
				fail( "Serialization failed with exception", e );
			}
		} );
	}

	@Test
	public void testSelectCollection(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final SelectionQuery<Employee> q = query(
					"select c.employees from Company c where c.id = 1",
					Employee.class,
					session
			);

			try {
				final String result = toString( q.getResultList(), q, scope.getSessionFactory() );
				System.out.println( result );

				final JsonNode jsonNode = mapper.readTree( result );
				assertThat( jsonNode.isArray() ).isTrue();
				assertThat( jsonNode.size() ).isEqualTo( 2 );

				final JsonNode first = jsonNode.get( 0 );
				assertThat( first.isObject() ).isTrue();
				assertDoesNotThrow( () -> UUID.fromString( first.get( "uniqueIdentifier" ).asText() ) );
				assertThat( first.get( "company" ).get( "name" ).textValue() ).isEqualTo( "Red Hat" );

				final JsonNode second = jsonNode.get( 1 );
				assertThat( second.isObject() ).isTrue();
				assertDoesNotThrow( () -> UUID.fromString( second.get( "uniqueIdentifier" ).asText() ) );
				assertThat( second.get( "company" ).get( "name" ).textValue() ).isEqualTo( "Red Hat" );
			}
			catch (JsonProcessingException e) {
				fail( "Serialization failed with exception", e );
			}
		} );
	}

	@Test
	public void testSelectCollectionProperty(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final SelectionQuery<String> q = query(
					"select element(c.employees).firstName from Company c where c.id = 1",
					String.class,
					session
			);

			try {
				final String result = toString( q.getResultList(), q, scope.getSessionFactory() );
				System.out.println( result );

				final JsonNode jsonNode = mapper.readTree( result );
				assertThat( jsonNode.isArray() ).isTrue();
				assertThat( jsonNode.size() ).isEqualTo( 2 );
				assertThat( Set.of( jsonNode.get( 0 ).textValue(), jsonNode.get( 1 ).textValue() ) ).containsOnly(
						"Marco",
						"Matteo"
				);
			}
			catch (JsonProcessingException e) {
				fail( "Serialization failed with exception", e );
			}
		} );
	}

	@Test
	public void testComplexInheritance(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final SelectionQuery<Human> q = query( "from Human h where h.id = 1", Human.class, session );

			try {
				final Human human = q.getSingleResult();

				Hibernate.initialize( human.getFamily() );
				assertThat( human.getFamily() ).hasSize( 1 );
				Hibernate.initialize( human.getPets() );
				assertThat( human.getPets() ).hasSize( 1 );
				Hibernate.initialize( human.getNickNames() );
				assertThat( human.getNickNames() ).hasSize( 2 );

				final String result = toString( List.of( human ), q, scope.getSessionFactory() );

				final JsonNode jsonNode = getSingleValue( mapper.readTree( result ) );
				assertThat( jsonNode.get( "id" ).intValue() ).isEqualTo( 1 );

				final JsonNode family = jsonNode.get( "family" );
				assertThat( family.isArray() ).isTrue();
				final JsonNode mapNode = getSingleValue( family );
				assertThat( mapNode.isObject() ).isTrue();
				assertThat( mapNode.get( "key" ).textValue() ).isEqualTo( "sister" );
				assertThat( mapNode.get( "value" ).get( "description" ).textValue() ).isEqualTo( "Marco's sister" );

				final JsonNode pets = jsonNode.get( "pets" );
				assertThat( pets.isArray() ).isTrue();
				assertThat( pets.size() ).isEqualTo( 1 );
				final JsonNode cat = pets.get( 0 );
				assertThat( cat.isObject() ).isTrue();
				assertThat( cat.get( "id" ).intValue() ).isEqualTo( 2 );
				assertThat( cat.get( "description" ).textValue() ).isEqualTo( "Gatta" );
				final JsonNode owner = cat.get( "owner" );
				assertThat( owner.get( "id" ).intValue() ).isEqualTo( 1 );
				assertThat( owner.properties().stream().map( Map.Entry::getKey ) )
						.containsOnly( "id" ); // circular relationship

				final JsonNode nickNames = jsonNode.get( "nickNames" );
				assertThat( nickNames.isArray() ).isTrue();
				assertThat( nickNames.size() ).isEqualTo( 2 );
				assertThat( Set.of( nickNames.get( 0 ).textValue(), nickNames.get( 1 ).textValue() ) ).containsOnly(
						"Bella",
						"Eskimo Joe"
				);
			}
			catch (JsonProcessingException e) {
				fail( "Serialization failed with exception", e );
			}
		} );
	}

	@BeforeAll
	public void beforeAll(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Company rh = new Company( 1L, "Red Hat", new Address( "Milan", "Via Gustavo Fara" ) );
			session.persist( rh );
			final Company ibm = new Company( 2L, "IBM", new Address( "Segrate", "Circonvallazione Idroscalo" ) );
			session.persist( ibm );
			session.persist( new Company( 3L, "Belladelli Giovanni", new Address( "Pegognaga", "Via Roma" ) ) );
			session.persist( new Company( 4L, "Another Company", null ) );

			session.persist( new Employee( UUID.randomUUID(), "Marco", "Belladelli", 100_000, rh ) );
			session.persist( new Employee( UUID.randomUUID(), "Matteo", "Cauzzi", 50_000, rh ) );
			session.persist( new Employee( UUID.randomUUID(), "Andrea", "Boriero", 200_000, ibm ) );

			final Human human = human( 1L, session );
			cat( 2L, human, session );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	static <T> SelectionQuery<T> query(String hql, Class<T> resultType, SharedSessionContractImplementor session) {
		return session.createSelectionQuery( hql, resultType );
	}

	static <T> String toString(
			List<? extends T> values,
			SelectionQuery<T> query,
			SessionFactoryImplementor sessionFactory) {
		try {
			return new ResultsJsonSerializerImpl( sessionFactory ).toString( values, query );
		}
		catch (IOException e) {
			throw new UncheckedIOException( "Error during result serialization", e );
		}
	}

	static JsonNode getSingleValue(JsonNode jsonNode) {
		assertThat( jsonNode.isArray() ).isTrue();
		assertThat( jsonNode.size() ).isEqualTo( 1 );
		return jsonNode.get( 0 );
	}

	private static Human human(Long id, Session session) {
		final Human human = new Human();
		human.setId( id );
		human.setName( new Name( "Marco", 'M', "Belladelli" ) );
		human.setBirthdate( new Date() );
		human.setNickNames( new TreeSet<>( Set.of( "Bella", "Eskimo Joe" ) ) );
		final Human sister = new Human();
		sister.setId( 99L );
		sister.setName( new Name( "Sister", 'S', "Belladelli" ) );
		sister.setDescription( "Marco's sister" );
		human.setFamily( Map.of( "sister", sister ) );
		session.persist( sister );
		session.persist( human );
		return human;
	}

	private static Cat cat(Long id, Human owner, Session session) {
		final Cat cat = new Cat();
		cat.setId( id );
		cat.setDescription( "Gatta" );
		cat.setOwner( owner );
		session.persist( cat );
		return cat;
	}
}
