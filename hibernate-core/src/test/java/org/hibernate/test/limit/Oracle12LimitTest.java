package org.hibernate.test.limit;

import java.util.List;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Root;

import org.hibernate.dialect.Oracle12cDialect;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

@RequiresDialect(Oracle12cDialect.class)
@TestForIssue(jiraKey = "HHH-14819")
public class Oracle12LimitTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Person.class,
				Travel.class
		};
	}

	@Test
	public void testLimit() {
		inTransaction(
				session -> {
					final CriteriaBuilder criteriabuilder = session.getCriteriaBuilder();
					final CriteriaQuery criteriaquery = criteriabuilder.createQuery();
					final Root<Person> personRoot = criteriaquery.from( Person.class );
					final Join<Person, Travel> travels = personRoot.join( "travels", JoinType.LEFT );

					criteriaquery.select( personRoot ).
							where( criteriabuilder.or( criteriabuilder.equal( personRoot.get( "name" ), "A" ) ) )
							.distinct( true );

					criteriaquery.orderBy( criteriabuilder.desc( criteriabuilder.upper( travels.get( "destination" ) ) ) );

					final TypedQuery<Person> createQuery = session.createQuery( criteriaquery );

					createQuery.setFirstResult( 3 ).setMaxResults( 10 ).getResultList();
				}
		);
	}

	@Entity(name = "Person")
	public class Person {
		@Id
		private Long id;

		@OneToMany
		private List<Travel> travels;

		public Person() {
		}

		private String name;
	}

	@Entity(name = "Travel")
	public class Travel {
		@Id
		private Integer id;

		private String destination;

		public Travel() {
		}
	}
}
