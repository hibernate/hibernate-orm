package org.hibernate.test.limit;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.hibernate.dialect.Oracle8iDialect;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

@RequiresDialect(Oracle8iDialect.class)
@TestForIssue(jiraKey = "HHH-14819")
public class Oracle12LimitTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Person.class,
				UserFunctionalArea.class
		};
	}

	@Test
	public void testLimit() {
		inTransaction(
				session -> {
					final CriteriaBuilder criteriabuilder = session.getCriteriaBuilder();
					final CriteriaQuery criteriaquery = criteriabuilder.createQuery();
					final Root<Person> personRoot = criteriaquery.from( Person.class );
					final Join<Person, UserFunctionalArea> functionalArea = personRoot.join(
							"functionalArea",
							JoinType.LEFT
					);

					List<Predicate> predicates = new ArrayList<>();
					predicates.add( criteriabuilder.or( criteriabuilder.equal( personRoot.get( "name" ), "A" ) ) );

					List<Predicate> notNullPredicate = predicates.parallelStream().filter( Objects::nonNull )
							.collect( Collectors.toList() );
					criteriaquery.select( personRoot ).where( notNullPredicate.toArray( new Predicate[] {} ) ).distinct(
							true );
					criteriaquery.orderBy( criteriabuilder.desc( criteriabuilder.upper( functionalArea.get(
							"userAreaName" ) ) ) );

					final TypedQuery<Person> createQuery = session.createQuery( criteriaquery );
					createQuery.setFirstResult( 0 ).setMaxResults( 10 ).getResultList();
				}
		);
	}

	@Entity(name = "Person")
	@Table(name = "Person")
	public static class Person {
		@Id
		private Long id;

		@OneToMany
//		@JoinColumn(name = "USER_KEY", referencedColumnName = "USER_KEY")
		private List<UserFunctionalArea> functionalArea;

		private String name;
	}

	@Entity(name = "UserFunctionalArea")
	@Table(name = "UserFunctionalArea")
	public static class UserFunctionalArea {
		@Id
		@Column(name = "USER_KEY")
		private Integer id;

		private String userAreaName;
	}
}
