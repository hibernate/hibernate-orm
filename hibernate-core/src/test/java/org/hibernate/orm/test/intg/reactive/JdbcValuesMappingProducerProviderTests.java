/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.intg.reactive;

import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.results.ResultSetMapping;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.results.jdbc.internal.JdbcValuesMappingProducerProviderStandard;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMappingProducer;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMappingProducerProvider;

import org.hibernate.testing.orm.domain.gambit.EntityOfBasics;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.ExpectedException;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.junit.jupiter.api.Test;

/**
 * @author Steve Ebersole
 */
@ServiceRegistry( services = @ServiceRegistry.Service(
		role = JdbcValuesMappingProducerProvider.class,
		impl = JdbcValuesMappingProducerProviderTests.CustomJdbcValuesMappingProducerProvider.class
) )
@DomainModel( annotatedClasses = EntityOfBasics.class )
public class JdbcValuesMappingProducerProviderTests {
	@Test
	@ExpectedException( GoodIfBadException.class )
	public void testIt(DomainModelScope scope) {
		try ( SessionFactory sessionFactory = scope.getDomainModel().buildSessionFactory() ) {
		}
	}

	public static class CustomJdbcValuesMappingProducerProvider extends JdbcValuesMappingProducerProviderStandard {
		@Override
		public JdbcValuesMappingProducer buildMappingProducer(SelectStatement sqlAst, SessionFactoryImplementor sessionFactory) {
			throw new GoodIfBadException();
		}

		@Override
		public ResultSetMapping buildResultSetMapping(String name, boolean isDynamic, SessionFactoryImplementor sessionFactory) {
			throw new GoodIfBadException();
		}
	}

	public static class GoodIfBadException extends RuntimeException {
	}
}
