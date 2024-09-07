package org.hibernate.orm.test.query.criteria;

import java.util.Set;

import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaRoot;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;


@DomainModel(
		annotatedClasses = {
				CriteriaGetJoinsTest.MyEntity.class,
				CriteriaGetJoinsTest.MySecondEntity.class
		}
)
@SessionFactory
@JiraKey("HHH-16767")
public class CriteriaGetJoinsTest {

	@Test
	public void testGetJoinsFiltersEntityJoin(SessionFactoryScope scope) {

		scope.inTransaction(
				session -> {
					HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
					JpaCriteriaQuery<?> cq = cb.createQuery();
					JpaRoot<MyEntity> root = cq.from( MyEntity.class );
					root.join( "secondEntity", JoinType.LEFT );
					root.join( MyEntity.class, org.hibernate.query.common.JoinType.LEFT );
					Set<Join<MyEntity, ?>> joins = root.getJoins();
					/*
					 	SqmEntityJoin does not implement jakarta.persistence.criteria.Join, iterating through the
					 	result would cause a ClassCastException

					 */
					assertThat( joins.size() ).isEqualTo( 1 );
					joins.forEach( Join::getJoinType );
				}
		);
	}


	@Entity(name = "MyEntity")
	public static class MyEntity {
		@Id
		private Integer id;

		private String name;

		@ManyToOne
		private MySecondEntity secondEntity;

	}

	@Entity(name = "MySecondEntity")
	public static class MySecondEntity {
		@Id
		private Integer id;

		private String name;
	}
}
