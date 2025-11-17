/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.criteria;

import org.hibernate.query.criteria.HibernateCriteriaBuilder;

import org.hibernate.testing.orm.domain.gambit.BasicEntity;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

/**
 * @author Steve Ebersole
 */
@DomainModel( annotatedClasses = BasicEntity.class )
@SessionFactory
public class SubQueryTests {

	@Test
	public void modifiedSubQueryTest(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.persist( new BasicEntity( 2368, "irrelevant" ) );
			session.persist( new BasicEntity( 2578, "irrelevant" ) );
			session.persist( new BasicEntity( 3000, "irrelevant" ) );
			session.persist( new BasicEntity( 10000, "irrelevant" ) );
			session.persist( new BasicEntity( 10050, "irrelevant" ) );
		} );

		scope.inTransaction( (session) -> {
			final HibernateCriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();

			final CriteriaQuery<Integer> criteria = criteriaBuilder.createQuery( Integer.class );
			final Root<BasicEntity> root = criteria.from(BasicEntity.class);
			final Path<Integer> rootIdPath = root.get( "id" );

			final Subquery<Integer> subCriteria = criteria.subquery(Integer.class);
			final Root<BasicEntity> subRoot = subCriteria.from(BasicEntity.class);
			final Path<Integer> subRootIdPath = subRoot.get( "id" );

			subCriteria.select( subRootIdPath );
			subCriteria.where( criteriaBuilder.lt( subRootIdPath, 10050 ) );

			criteria.select( rootIdPath );
			criteria.where(
					criteriaBuilder.gt(
							rootIdPath,
							criteriaBuilder.all( subCriteria ) )
			);

			session.createQuery( criteria ).list();
		} );
	}

	@Test
	public void modifiedSubQueryHqlTest(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.persist( new BasicEntity( 2368, "irrelevant" ) );
			session.persist( new BasicEntity( 2578, "irrelevant" ) );
			session.persist( new BasicEntity( 3000, "irrelevant" ) );
			session.persist( new BasicEntity( 10000, "irrelevant" ) );
			session.persist( new BasicEntity( 10050, "irrelevant" ) );
		} );

		scope.inTransaction( (session) -> {
			final String qry = "select e.id from BasicEntity e where e.id > all (select s.id from BasicEntity s where s.id > 1)";
			session.createQuery( qry ).list();
		} );
	}

	@AfterEach
	public void cleanUpTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}
}
