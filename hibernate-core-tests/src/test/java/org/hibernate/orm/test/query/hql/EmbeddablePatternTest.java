package org.hibernate.orm.test.query.hql;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.query.SemanticException;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DomainModel(annotatedClasses = {EmbeddablePatternTest.Person.class, EmbeddablePatternTest.Address.class})
@SessionFactory
@Jira("https://hibernate.atlassian.net/browse/HHH-20592")
public class EmbeddablePatternTest {

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new Person( 1L, "Gavin",
					new Address( "Main St", "Springfield", "12345", 1999 ) ) );
			session.persist( new Person( 2L, "Steve",
					new Address( "High St", "Shelbyville", "54321", 2005 ) ) );
			session.persist( new Person( 3L, "Christian",
					new Address( "Main St", "Springfield", "12345", 2012 ) ) );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testUnqualified(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			assertThat( session.createSelectionQuery(
							"select id from Person where address(street, zip) = (:street, :zip) order by id",
							Long.class )
					.setParameter( "street", "Main St" )
					.setParameter( "zip", "12345" )
					.getResultList() )
					.containsExactly( 1L, 3L );
		} );
	}

	@Test
	public void testQualified(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			// the order of the attributes is determined by the pattern,
			// and not by the order of the fields of the embeddable class
			assertThat( session.createSelectionQuery(
							"select p.id from Person p where p.address(zip, city, street) = (:zip, :city, :street)",
							Long.class )
					.setParameter( "street", "High St" )
					.setParameter( "city", "Shelbyville" )
					.setParameter( "zip", "54321" )
					.getResultList() )
					.containsExactly( 2L );
			assertThat( session.createSelectionQuery(
							"select p.id from Person p where ('54321', 'High St') = p.address(zip, street)",
							Long.class )
					.getResultList() )
					.containsExactly( 2L );
		} );
	}

	@Test
	public void testAttributeNamedLikeDatetimeField(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			assertThat( session.createSelectionQuery(
							"select p.id from Person p where p.address(year, zip) = (:year, :zip)",
							Long.class )
					.setParameter( "year", 2012 )
					.setParameter( "zip", "12345" )
					.getResultList() )
					.containsExactly( 3L );
			assertThat( session.createSelectionQuery(
							"select p.id from Person p where p.address(zip, year) = (:zip, :year)",
							Long.class )
					.setParameter( "year", 1999 )
					.setParameter( "zip", "12345" )
					.getResultList() )
					.containsExactly( 1L );
		} );
	}

	@Test
	public void testSingleAttribute(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			// just like (p.address.zip), a pattern with one attribute is not a tuple
			assertThat( session.createSelectionQuery(
							"select p.id from Person p where p.address(zip) = ('54321')",
							Long.class )
					.getResultList() )
					.containsExactly( 2L );
			assertThat( session.createSelectionQuery(
							"select p.id from Person p where p.address(zip) in ('54321', '00000')",
							Long.class )
					.getResultList() )
					.containsExactly( 2L );
			assertThat( session.createSelectionQuery(
							"select p.address(year) from Person p where p.id = 2",
							Integer.class )
					.getSingleResult() )
					.isEqualTo( 2005 );
		} );
	}

	@Test
	public void testAmbiguousAttribute(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			// an ambiguous attribute which is not of embeddable type
			// doesn't get in the way of an unregistered function
			assertThat( session.createSelectionQuery(
							"select name(p.id) from Person p, Person q",
							Object.class )
					.getQueryString() )
					.isNotNull();
			assertThat( session.createSelectionQuery(
							"select name.fun(p.id) from Person p, Person q",
							Object.class )
					.getQueryString() )
					.isNotNull();
			// but an ambiguous pattern is an error
			assertThatThrownBy( () -> session.createSelectionQuery(
					"select p.id from Person p, Person q where address(zip, city) = ('12345', 'Springfield')",
					Long.class ) )
					.isInstanceOf( SemanticException.class )
					.hasMessageContaining( "Ambiguous unqualified attribute reference 'address'" );
		} );
	}

	@Test
	public void testJoinAlias(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			assertThat( session.createSelectionQuery(
							"select p.id from Person p join p.address a where a(city, zip) = ('Shelbyville', '54321')",
							Long.class )
					.getResultList() )
					.containsExactly( 2L );
		} );
	}

	@Test
	public void testInPredicate(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			assertThat( session.createSelectionQuery(
							"select p.id from Person p where p.address(year, city) in ((1999, 'Springfield'), (2005, 'Shelbyville')) order by p.id",
							Long.class )
					.getResultList() )
					.containsExactly( 1L, 2L );
		} );
	}

	@Test
	public void testCorrelatedSubquery(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			assertThat( session.createSelectionQuery(
							"select p.id from Person p where exists (select 1 from Person q where q.id <> p.id and q.address(street, city, zip) = p.address(street, city, zip)) order by p.id",
							Long.class )
					.getResultList() )
					.containsExactly( 1L, 3L );
		} );
	}

	@Test
	public void testSelect(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Object[] result =
					session.createSelectionQuery(
									"select p.name, p.address(zip, year) from Person p where p.id = 2",
									Object[].class )
							.getSingleResult();
			assertThat( result[0] ).isEqualTo( "Steve" );
			assertThat( (Object[]) result[1] ).containsExactly( "54321", 2005 );
		} );
	}

	@Test
	public void testUnknownAttribute(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			assertThatThrownBy( () -> session.createSelectionQuery(
					"from Person p where p.address(street, postcode) = ('Main St', '12345')",
					Person.class ) )
					.hasMessageContaining( "postcode" );
		} );
	}

	@Test
	public void testIllegalPattern(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			assertThatThrownBy( () -> session.createSelectionQuery(
					"from Person p where p.address(street, 'zip') = ('Main St', '12345')",
					Person.class ) )
					.isInstanceOf( SemanticException.class )
					.hasMessageContaining( "p.address" );
			assertThatThrownBy( () -> session.createSelectionQuery(
					"from Person p where p.address(street, p.name) = ('Main St', '12345')",
					Person.class ) )
					.isInstanceOf( SemanticException.class )
					.hasMessageContaining( "p.address" );
			assertThatThrownBy( () -> session.createSelectionQuery(
					"from Person p where p.address() = ('Main St', '12345')",
					Person.class ) )
					.isInstanceOf( SemanticException.class )
					.hasMessageContaining( "p.address" );
		} );
	}

	@Test
	public void testNotEmbeddable(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			// a path which is not of embeddable type is still treated as a function name
			assertThat( session.createSelectionQuery(
							"select upper(p.name) from Person p where p.id = 1",
							String.class )
					.getSingleResult() )
					.isEqualTo( "GAVIN" );
			assertThat( session.createSelectionQuery(
							"select p.name(p.id) from Person p",
							Object.class )
					.getQueryString() )
					.isNotNull();
			assertThatThrownBy( () -> session.createSelectionQuery(
					"select p.name(street) from Person p",
					Object.class ) )
					.isInstanceOf( SemanticException.class )
					.hasMessageContaining( "'street'" );
		} );
	}

	@Entity(name = "Person")
	@Table(name = "pattern_person")
	public static class Person {
		@Id
		Long id;
		String name;
		Address address;

		Person() {
		}

		Person(Long id, String name, Address address) {
			this.id = id;
			this.name = name;
			this.address = address;
		}
	}

	@Embeddable
	public static class Address {
		String street;
		String city;
		String zip;
		@Column(name = "since_year")
		int year;

		Address() {
		}

		Address(String street, String city, String zip, int year) {
			this.street = street;
			this.city = city;
			this.zip = zip;
			this.year = year;
		}
	}
}
