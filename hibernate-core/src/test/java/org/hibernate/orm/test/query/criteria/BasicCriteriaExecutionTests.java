/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.criteria;

import org.hibernate.boot.MetadataSources;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaParameterExpression;
import org.hibernate.query.criteria.JpaRoot;

import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.domain.gambit.BasicEntity;
import org.hibernate.testing.orm.junit.BaseSessionFactoryFunctionalTest;
import org.junit.jupiter.api.Test;

/**
 * @author Steve Ebersole
 */
public class BasicCriteriaExecutionTests extends BaseSessionFactoryFunctionalTest  {
	@Override
	protected void applyMetadataSources(MetadataSources metadataSources) {
		super.applyMetadataSources( metadataSources );
		StandardDomainModel.GAMBIT.getDescriptor().applyDomainModel( metadataSources );
	}

	@Test
	public void testExecutingBasicCriteriaQuery() {
		final HibernateCriteriaBuilder criteriaBuilder = sessionFactory().getQueryEngine().getCriteriaBuilder();

		final JpaCriteriaQuery<Object> criteria = criteriaBuilder.createQuery();

		final JpaRoot<BasicEntity> root = criteria.from( BasicEntity.class );

		criteria.select( root );

		sessionFactoryScope().inSession(
				session -> session.createQuery( criteria ).list()
		);
	}

	@Test
	public void testExecutingBasicCriteriaQueryLiteralPredicate() {
		final HibernateCriteriaBuilder criteriaBuilder = sessionFactory().getQueryEngine().getCriteriaBuilder();

		final JpaCriteriaQuery<Object> criteria = criteriaBuilder.createQuery();

		final JpaRoot<BasicEntity> root = criteria.from( BasicEntity.class );

		criteria.select( root );

		criteria.where( criteriaBuilder.equal( criteriaBuilder.literal( 1 ), criteriaBuilder.literal( 1 ) ) );

		sessionFactoryScope().inSession(
				session -> session.createQuery( criteria ).list()
		);
	}

	@Test
	public void testExecutingBasicCriteriaQueryParameterPredicate() {
		final HibernateCriteriaBuilder criteriaBuilder = sessionFactory().getQueryEngine().getCriteriaBuilder();

		final JpaCriteriaQuery<Object> criteria = criteriaBuilder.createQuery();

		final JpaRoot<BasicEntity> root = criteria.from( BasicEntity.class );

		criteria.select( root );

		final JpaParameterExpression<Integer> param = criteriaBuilder.parameter( Integer.class );

		criteria.where( criteriaBuilder.equal( param, param ) );

		sessionFactoryScope().inSession(
				session -> session.createQuery( criteria ).setParameter( param, 1 ).list()
		);
	}

	@Test
	public void testExecutingBasicCriteriaQueryOrderBy() {
		final HibernateCriteriaBuilder criteriaBuilder = sessionFactory().getQueryEngine().getCriteriaBuilder();

		final JpaCriteriaQuery<Object> criteria = criteriaBuilder.createQuery();

		final JpaRoot<BasicEntity> root = criteria.from( BasicEntity.class );

		criteria.select( root );

		criteria.orderBy( criteriaBuilder.asc( root.get("id") ) );

		sessionFactoryScope().inSession(
				session -> session.createQuery( criteria ).list()
		);
	}
}
