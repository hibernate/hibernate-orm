/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.sqm.sql;

import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.orm.test.query.sqm.BaseSqmUnitTest;
import org.hibernate.query.sqm.tree.SqmSelectStatement;
import org.hibernate.sql.ast.consume.spi.SqlAstSelectToJdbcSelectConverter;
import org.hibernate.sql.ast.produce.spi.SqlAstProducerContext;
import org.hibernate.sql.ast.produce.spi.SqlAstSelectDescriptor;
import org.hibernate.sql.ast.produce.sqm.spi.Callback;
import org.hibernate.sql.ast.produce.sqm.spi.SqmSelectToSqlAstConverter;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcSelect;

/**
 * @author Steve Ebersole
 */
public class BaseSqmSqlTest extends BaseSqmUnitTest {

	protected JdbcSelect buildJdbcSelect(
			String hql,
			ExecutionContext executionContext) {

		final SqmSelectStatement sqm = interpretSelect( hql );

		final SqmSelectToSqlAstConverter sqmConveter = new SqmSelectToSqlAstConverter(
				executionContext.getQueryOptions(),
				new SqlAstProducerContext() {
					@Override
					public SessionFactoryImplementor getSessionFactory() {
						return executionContext.getSession().getFactory();
					}

					@Override
					public LoadQueryInfluencers getLoadQueryInfluencers() {
						return executionContext.getSession().getLoadQueryInfluencers();
					}

					@Override
					public Callback getCallback() {
						return executionContext.getCallback();
					}
				}
		);

		final SqlAstSelectDescriptor interpretation = sqmConveter.interpret( sqm );

		return SqlAstSelectToJdbcSelectConverter.interpret(
				interpretation,
				executionContext.getSession().getSessionFactory()
		);
	}
}
