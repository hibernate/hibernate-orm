/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.sql.ast;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.query.hql.spi.HqlQueryImplementor;
import org.hibernate.query.spi.QueryImplementor;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.internal.QuerySqmImpl;
import org.hibernate.query.sqm.sql.internal.SqmSelectInterpretation;
import org.hibernate.query.sqm.sql.internal.SqmSelectToSqlAstConverter;
import org.hibernate.query.sqm.tree.SqmStatement;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;

import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("WeakerAccess")
@DomainModel(
		standardModels = {
				StandardDomainModel.GAMBIT,
				StandardDomainModel.CONTACTS
		}
)
@ServiceRegistry(
		settings = @ServiceRegistry.Setting(
				name = AvailableSettings.HBM2DDL_AUTO,
				value = "create-drop"
		)
)
@SessionFactory
public class SmokeTests {
	@Test
	public void testSimpleHql(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final QueryImplementor query = session.createQuery( "select c.name.first from Contact c" );
					final HqlQueryImplementor<?> hqlQuery = (HqlQueryImplementor<?>) query;
					final SqmSelectStatement sqmStatement = (SqmSelectStatement) hqlQuery.getSqmStatement();

					final SqmSelectToSqlAstConverter sqmConverter = new SqmSelectToSqlAstConverter(
							hqlQuery.getQueryOptions(),
							( (QuerySqmImpl) hqlQuery ).getDomainParameterXref(),
							query.getParameterBindings(),
							session.getLoadQueryInfluencers(),
							scope.getSessionFactory()
					);

					final SqmSelectInterpretation interpretation = sqmConverter.interpret( sqmStatement );
				}
		);
	}
}
