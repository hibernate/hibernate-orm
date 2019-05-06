/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.sqm.sql;

import org.hibernate.metamodel.spi.MetamodelImplementor;
import org.hibernate.orm.test.query.sqm.BaseSqmUnitTest;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.ast.consume.spi.SqlAstSelectToJdbcSelectConverter;
import org.hibernate.sql.ast.produce.spi.SqlAstSelectDescriptor;
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

		final SqmSelectToSqlAstConverter sqmConverter = new SqmSelectToSqlAstConverter(
				executionContext.getQueryOptions(),
				DomainParameterXref.from( sqm ),
				executionContext.getDomainParameterBindingContext().getQueryParameterBindings(),
				executionContext.getSession().getLoadQueryInfluencers(),
				executionContext.getCallback(),
				this
		);

		final SqlAstSelectDescriptor interpretation = sqmConverter.interpret( sqm );

		return SqlAstSelectToJdbcSelectConverter.interpret(
				interpretation,
				executionContext.getSession().getSessionFactory()
		);
	}
}
