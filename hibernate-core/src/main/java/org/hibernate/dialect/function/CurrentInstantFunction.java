package org.hibernate.dialect.function;

import java.util.List;

import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.spi.translation.SqlAstTranslator;
import org.hibernate.sql.spi.SqlAppender;
import org.hibernate.sql.ast.spi.SqlAstNode;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.jdbc.internal.TemporalPreservationGuard;

/// A "function" with no parameters that returns the current instant.
///
/// With temporal preservation enabled, the expression depends on how the
/// Dialect stores an instant: where it holds the date and time at UTC, the
/// current timestamp with its time zone would be read as a different instant
/// whenever the time zone of the session is not UTC.
///
/// @see TemporalPreservationGuard#currentInstantExpression
///
/// @since 8.1
/// @author Steve Ebersole
public class CurrentInstantFunction extends CurrentFunction {
	private final BasicType<?> type;

	public CurrentInstantFunction(String name, String sql, BasicType<?> type) {
		super( name, sql, type );
		this.type = type;
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> arguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		final var sessionFactory = walker.getSessionFactory();
		if ( sessionFactory.getSessionFactoryOptions().isTemporalPreservationEnabled() ) {
			sqlAppender.appendSql( TemporalPreservationGuard.currentInstantExpression(
					sessionFactory.getJdbcServices().getDialect(),
					type,
					true,
					"the result of the function '" + getName() + "'"
			) );
		}
		else {
			super.render( sqlAppender, arguments, returnType, walker );
		}
	}
}
