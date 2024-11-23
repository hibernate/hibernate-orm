package org.hibernate.userguide.associations;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.criterion.ForeignKeyExpression;
import org.hibernate.criterion.ForeingKeyProjection;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.PropertyProjection;
import org.hibernate.criterion.Restrictions;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.jdbc.SQLStatementInterceptor;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.hibernate.testing.transaction.TransactionUtil2.inTransaction;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author Fábio Ueno
 */
public class NotFoundTest extends BaseEntityManagerFunctionalTestCase {

	private SQLStatementInterceptor sqlStatementInterceptor;

	@Override
	protected void addConfigOptions(Map options) {
		sqlStatementInterceptor = new SQLStatementInterceptor( options );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Person.class,
				City.class
		};
	}

	@Before
	public void createTestData() {
		inTransaction( entityManagerFactory(), (entityManager) -> {
			//tag::associations-not-found-persist-example[]
			City newYork = new City( 1, "New York" );
			entityManager.persist( newYork );

			Person person = new Person( 1, "John Doe", newYork );
			entityManager.persist( person );
			//end::associations-not-found-persist-example[]
		} );
	}

	@After
	public void dropTestData() {
		inTransaction( entityManagerFactory(), (em) -> {
			em.createQuery( "delete Person" ).executeUpdate();
			em.createQuery( "delete City" ).executeUpdate();
		} );
	}

	@Test
	public void test() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::associations-not-found-find-baseline[]
			Person person = entityManager.find(Person.class, 1);
			assertEquals("New York", person.getCity().getName());
			//end::associations-not-found-find-baseline[]
		});

		breakForeignKey();

		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::associations-not-found-non-existing-find-example[]
			Person person = entityManager.find(Person.class, 1);

			assertNull(null, person.getCity());
			//end::associations-not-found-non-existing-find-example[]
		});
	}

	private void breakForeignKey() {
		inTransaction( entityManagerFactory(), (em) -> {
			//tag::associations-not-found-break-fk[]
			// the database allows this because there is no physical foreign-key
			em.createQuery( "delete City" ).executeUpdate();
			//end::associations-not-found-break-fk[]
		} );
	}

	@Test
	public void queryTest() {
		breakForeignKey();

		inTransaction( entityManagerFactory(), (entityManager) -> {
			//tag::associations-not-found-implicit-join-example[]
			final List<Person> nullResults = entityManager
					.createQuery( "from Person p where p.city.id is null", Person.class )
					.list();
			assertThat( nullResults ).isEmpty();

			final List<Person> nonNullResults = entityManager
					.createQuery( "from Person p where p.city.id is not null", Person.class )
					.list();
			assertThat( nonNullResults ).isEmpty();
			//end::associations-not-found-implicit-join-example[]
		} );
	}

	@Test
	public void queryTestFk() {
		breakForeignKey();

		inTransaction( entityManagerFactory(), (entityManager) -> {
			sqlStatementInterceptor.clear();
			//tag::associations-not-found-fk-function-example[]
			final List<String> nullResults = entityManager
					.createQuery( "select p.name from Person p where fk( p.city ) is null", String.class )
					.list();

			assertThat( nullResults ).isEmpty();

			final List<String> nonNullResults = entityManager
					.createQuery( "select p.name from Person p where fk( p.city ) is not null", String.class )
					.list();
			assertThat( nonNullResults ).hasSize( 1 );
			assertThat( nonNullResults.get( 0 ) ).isEqualTo( "John Doe" );
			//end::associations-not-found-fk-function-example[]

			// In addition, make sure that the two executed queries do not create a join
			assertThat( sqlStatementInterceptor.getQueryCount() ).isEqualTo( 2 );
			assertThat( sqlStatementInterceptor.getSqlQueries().get( 0 ) ).doesNotContain( " join " );
			assertThat( sqlStatementInterceptor.getSqlQueries().get( 1 ) ).doesNotContain( " join " );
		} );
	}

	@Test
	public void cirteriaTestFk() {
		breakForeignKey();

		inTransaction( entityManagerFactory(), (entityManager) -> {
			sqlStatementInterceptor.clear();
			Session session = entityManager.unwrap( Session.class );
			//tag::associations-not-found-fk-criteria-example[]
			Criteria criteria = session.createCriteria( Person.class );
			ProjectionList projList = Projections.projectionList();
			projList.add( Projections.property( "name" ) );
			criteria.setProjection( projList );
			criteria.add( Restrictions.fkIsNull( "city" ) );
			final List<Integer> nullResults = criteria.list();

			assertThat( nullResults ).isEmpty();

			criteria = session.createCriteria( Person.class );
			projList = Projections.projectionList();
			projList.add( Projections.property( "name" ) );
			criteria.setProjection( projList );
			criteria.add( Restrictions.fkIsNotNull( "city" ) );
			final List<String> nonNullResults = criteria.list();

			assertThat( nonNullResults ).hasSize( 1 );
			assertThat( nonNullResults.get( 0 ) ).isEqualTo( "John Doe" );

			// selecting Person -> city Foreign key
			criteria = session.createCriteria( Person.class );
			projList = Projections.projectionList();
			projList.add( Projections.fk( "city" ) );
			criteria.setProjection( projList );
			criteria.add( Restrictions.fkIsNotNull( "city" ) );

			final List<Integer> foreigKeyResults = criteria.list();
			assertThat( foreigKeyResults ).hasSize( 1 );
			assertThat( foreigKeyResults.get( 0 ) ).isEqualTo( 1 );
			//end::associations-not-found-fk-criteria-example[]

			// In addition, make sure that the two executed queries do not create a join
			assertThat( sqlStatementInterceptor.getQueryCount() ).isEqualTo( 3 );
			assertThat( sqlStatementInterceptor.getSqlQueries().get( 0 ) ).doesNotContain( " join " );
			assertThat( sqlStatementInterceptor.getSqlQueries().get( 1 ) ).doesNotContain( " join " );
			assertThat( sqlStatementInterceptor.getSqlQueries().get( 2 ) ).doesNotContain( " join " );
		} );
	}

	//tag::associations-not-found-domain-model-example[]
	@Entity(name = "Person")
	@Table(name = "Person")
	public static class Person {

		@Id
		private Integer id;
		private String name;

		@ManyToOne
		@NotFound(action = NotFoundAction.IGNORE)
		@JoinColumn(name = "city_fk", referencedColumnName = "id")
		private City city;

		//Getters and setters are omitted for brevity

		//end::associations-not-found-domain-model-example[]


		public Person() {
		}

		public Person(Integer id, String name, City city) {
			this.id = id;
			this.name = name;
			this.city = city;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public City getCity() {
			return city;
		}
		//tag::associations-not-found-domain-model-example[]
	}

	@Entity(name = "City")
	@Table(name = "City")
	public static class City implements Serializable {

		@Id
		private Integer id;

		private String name;

		//Getters and setters are omitted for brevity

		//end::associations-not-found-domain-model-example[]


		public City() {
		}

		public City(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
		//tag::associations-not-found-domain-model-example[]
	}
	//end::associations-not-found-domain-model-example[]
}
