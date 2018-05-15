/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sql.spi;

import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.produce.spi.SqlExpressable;
import org.hibernate.sql.results.internal.domain.basic.BasicResultImpl;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultCreationContext;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class QueryResultBuilderScalar
		implements WrappableQueryResultBuilder, SqlExpressable {

	// todo (6.0) : as far as potentially building a (Sql)Expression keep in mind that this is a scalar return at the root level
	//		meaning that the instantiator itself could act as the qualifier for
	// todo (6.0) : the handling of this as a (Sql)Expression is still a design question
	//		ultimately there is no real purpose for handling this as a SQL AST - the
	//		SQL to execute is already

	private final String columnName;
	private final SqlExpressableType type;

	public QueryResultBuilderScalar(
			String columnName,
			SqlExpressableType type) {
		this.columnName = columnName;
		this.type = type;
	}

	@Override
	public JavaTypeDescriptor getResultType() {
		return type.getJavaTypeDescriptor();
	}

	@Override
	public SqlExpressableType getExpressableType() {
		return type;
	}

	@Override
	public DomainResult buildReturn(
			DomainResultCreationState creationState,
			DomainResultCreationContext creationContext) {
		return new BasicResultImpl(
				columnName,
				new ResolvingSqlSelectionImpl( columnName ),
				type
		);
	}
}
