/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.expression.instantiation;

import org.hibernate.sql.results.internal.instantiation.ArgumentReader;
import org.hibernate.sql.results.spi.QueryResult;
import org.hibernate.sql.results.spi.QueryResultCreationContext;
import org.hibernate.sql.results.spi.QueryResultProducer;

/**
 * @author Steve Ebersole
 */
public class DynamicInstantiationArgument {
	private final QueryResultProducer argumentResultProducer;
	private final String alias;

	@SuppressWarnings("WeakerAccess")
	public DynamicInstantiationArgument(QueryResultProducer argumentResultProducer, String alias) {
		this.argumentResultProducer = argumentResultProducer;
		this.alias = alias;
	}

	public String getAlias() {
		return alias;
	}

	@SuppressWarnings({"unchecked", "WeakerAccess"})
	public ArgumentReader buildArgumentReader(QueryResultCreationContext context) {
		final QueryResult queryResult = argumentResultProducer.createQueryResult( alias, context );

		return new ArgumentReader( queryResult.getResultAssembler(), alias );
	}
}
