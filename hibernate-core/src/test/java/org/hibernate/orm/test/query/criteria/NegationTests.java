/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.criteria;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaPredicate;

import org.hibernate.testing.orm.domain.gambit.BasicEntity;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@ServiceRegistry(
		settings = @Setting( name = AvailableSettings.JPA_QUERY_COMPLIANCE, value = "true" )
)
@DomainModel( annotatedClasses = BasicEntity.class )
@SessionFactory
public class NegationTests {
	@Test
	public void	simpleTest(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final HibernateCriteriaBuilder nodeBuilder = session.getFactory().getCriteriaBuilder();
			final JpaPredicate equality = nodeBuilder.equal( nodeBuilder.literal( 1 ), nodeBuilder.literal( 1 ) );
			final JpaPredicate inequality = equality.not();
			assertThat( equality ).isNotSameAs( inequality );
			assertThat( equality.isNegated() ).isFalse();
			assertThat( inequality.isNegated() ).isTrue();
		} );
	}

	@BeforeEach
	public void createTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> session.persist( new BasicEntity( 1, "abc" ) ) );
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}
}
