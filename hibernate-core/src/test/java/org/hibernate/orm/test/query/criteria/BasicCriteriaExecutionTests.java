/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.criteria;

import org.hibernate.boot.MetadataSources;
import org.hibernate.orm.test.support.domains.AvailableDomainModel;
import org.hibernate.orm.test.support.domains.gambit.BasicEntity;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaRoot;
import org.hibernate.query.criteria.spi.CriteriaNodeBuilder;

import org.hibernate.testing.junit5.SessionFactoryBasedFunctionalTest;
import org.junit.jupiter.api.Test;

/**
 * @author Steve Ebersole
 */
public class BasicCriteriaExecutionTests extends SessionFactoryBasedFunctionalTest {
	@Override
	protected void applyMetadataSources(MetadataSources metadataSources) {
		super.applyMetadataSources( metadataSources );
		AvailableDomainModel.GAMBIT.getDomainModel().applyDomainModel( metadataSources );
	}

	@Override
	protected boolean exportSchema() {
		return true;
	}

	@Test
	public void testExecutingBasicCriteriaQuery() {
		final CriteriaNodeBuilder criteriaBuilder = sessionFactory().getQueryEngine().getCriteriaBuilder();

		final JpaCriteriaQuery<Object> criteria = criteriaBuilder.createQuery();

		final JpaRoot<BasicEntity> root = criteria.from( BasicEntity.class );

		criteria.select( root );

		sessionFactoryScope().inSession(
				session -> session.createQuery( criteria ).list()
		);
	}
}
