package org.hibernate.orm.test.limit;

import java.util.List;

import org.hibernate.dialect.OracleDialect;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;

@RequiresDialect(value = OracleDialect.class, majorVersion = 12)
@TestForIssue(jiraKey = "HHH-14819")
@DomainModel(
		annotatedClasses = {
				Oracle12LimitTest.Person.class,
				Oracle12LimitTest.Travel.class
		}
)
@SessionFactory
public class Oracle12LimitTest {


	@Test
	public void testLimit(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final CriteriaBuilder criteriabuilder = session.getCriteriaBuilder();
					final CriteriaQuery criteriaquery = criteriabuilder.createQuery();
					final Root<Person> personRoot = criteriaquery.from( Person.class );
					final Join<Person, Travel> travels = personRoot.join( "travels", JoinType.LEFT );

					final Path<String> destination = travels.get( "destination" );
					criteriaquery.multiselect( personRoot, destination ).
							where( criteriabuilder.or( criteriabuilder.equal( personRoot.get( "name" ), "A" ) ) )
							.distinct( true );

					criteriaquery.orderBy( criteriabuilder.desc( criteriabuilder.upper( destination ) ) );

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
