/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.instantiation.internal;

import org.hibernate.query.sqm.sql.BaseSqmToSqlAstConverter;
import org.hibernate.query.sqm.sql.internal.DomainResultProducer;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.results.graph.DomainResultCreationState;

/**
 * @author Steve Ebersole
 */
public class DynamicInstantiationArgument<T> {
	private final DomainResultProducer<T> argumentResultProducer;
	private final String alias;

	public DynamicInstantiationArgument(DomainResultProducer<T> argumentResultProducer, String alias) {
		this.argumentResultProducer = argumentResultProducer;
		this.alias = alias;
	}

	public String getAlias() {
		return alias;
	}

	public ArgumentDomainResult<T> buildArgumentDomainResult(DomainResultCreationState creationState) {
		final SqlExpressionResolver sqlExpressionResolver = creationState.getSqlAstCreationState()
				.getCurrentProcessingState()
				.getSqlExpressionResolver();
		if ( sqlExpressionResolver instanceof BaseSqmToSqlAstConverter.SqmAliasedNodeCollector ) {
			if ( !( argumentResultProducer instanceof DynamicInstantiation<?> ) ) {
				( (BaseSqmToSqlAstConverter.SqmAliasedNodeCollector) sqlExpressionResolver ).next();
			}
		}
		return new ArgumentDomainResult<>( argumentResultProducer.createDomainResult( alias, creationState ) );
	}
}
