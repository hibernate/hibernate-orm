/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.sql.ast;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.sql.ast.spi.JdbcParameterRenderer;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.type.descriptor.jdbc.JdbcType;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.domain.gambit.EntityOfBasics;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @implNote Restricted to H2 as there is nothing intrinsically Dialect specific here,
 * though each database has specific syntax for labelled parameters
 *
 * @author Steve Ebersole
 */
@ServiceRegistry( services = @ServiceRegistry.Service(
		role = JdbcParameterRenderer.class,
		impl = JdbcParameterRendererTests.JdbcParameterRendererImpl.class
) )
@DomainModel( annotatedClasses = EntityOfBasics.class )
@SessionFactory( useCollectingStatementInspector = true )
@RequiresDialect( H2Dialect.class )
public class JdbcParameterRendererTests {
	@Test
	public void basicTest(SessionFactoryScope scope) {
		final String queryString = "select e from EntityOfBasics e where e.id = :id";

		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		scope.inTransaction( (session) -> {
			session.createSelectionQuery( queryString, EntityOfBasics.class ).setParameter( "id", 1 ).list();
		} );

		assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
		final String sql = statementInspector.getSqlQueries().get( 0 );
		assertThat( sql ).contains( "?1" );
	}

	public static class JdbcParameterRendererImpl implements JdbcParameterRenderer {
		@Override
		public void renderJdbcParameter(int position, JdbcType jdbcType, SqlAppender appender, Dialect dialect) {
			jdbcType.appendWriteExpression( "?" + position, appender, dialect );
		}
	}
}
