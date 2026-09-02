/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.query.hql.spi.HqlTranslator;
import org.hibernate.query.sqm.tree.spi.SqmStatement;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Verifies that explicit global HQL translator configuration remains the
/// canonical integration path after removing translator selection from Dialect.
///
/// @author Steve Ebersole
@DomainModel
@ServiceRegistry(settings = @Setting(
		name = AvailableSettings.SEMANTIC_QUERY_PRODUCER,
		value = "org.hibernate.orm.test.query.hql.GlobalHqlTranslatorIntegrationTest$ConfiguredHqlTranslator"
))
@SessionFactory
public class GlobalHqlTranslatorIntegrationTest {
	@Test
	void usesExplicitlyConfiguredTranslator(SessionFactoryScope scope) {
		assertThat( scope.getSessionFactory().getQueryEngine().getHqlTranslator() )
				.isInstanceOf( ConfiguredHqlTranslator.class );
	}

	public static class ConfiguredHqlTranslator implements HqlTranslator {
		@Override
		public <R> SqmStatement<R> translate(String hql, Class<R> expectedResultType) {
			throw new AssertionError( "The selection test must not translate HQL" );
		}
	}
}
