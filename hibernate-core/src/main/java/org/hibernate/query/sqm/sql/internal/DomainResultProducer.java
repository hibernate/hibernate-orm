/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.sql.internal;

import java.util.function.Consumer;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.metamodel.mapping.SqlExpressableType;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Something that can produce a DomainResult as part of a SQM interpretation
 *
 * @author Steve Ebersole
 */
public interface DomainResultProducer<T> {

	// this has to be designed as a bridge, but more geared toward the SQL

	/*
	 * select p.name, p2.name from Person p, Person p2
	 *
	 * SqmPathSource (SqmExpressable) (unmapped)
	 *
	 * DomainType
	 * SimpleDomainType
	 * ...
	 *
	 * MappingType
	 *
	 *
	 *
	 * ValueMapping (mapped)
	 *
	 *
	 * ModelPartContainer personMapping = ...;
	 * personMapping.getValueMapping( "name" );
	 */

	/**
	 * Visit all of the SqlExpressableTypes associated with this this Readable.
	 *
	 * Used during cacheable SQL AST creation.
	 */
	default void visitJdbcTypes(Consumer<SqlExpressableType> action, TypeConfiguration typeConfiguration) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	/**
	 * Produce the domain query
	 */
	default DomainResult<T> createDomainResult(
			String resultVariable,
			DomainResultCreationState creationState) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	/**
	 * Used when this producer is a selection in a sub-query.  The
	 * DomainResult is only needed for root query of a SELECT statement.
	 *
	 * This default impl assumes this producer is a true (Sql)Expression
	 */
	default void applySqlSelections(DomainResultCreationState creationState) {
		throw new NotYetImplementedFor6Exception( getClass() );
//		// this form works for basic-valued nodes
//		creationState.getSqlExpressionResolver().resolveSqlSelection(
//				(Expression) this,
//				( (Expression) this ).getType().getJavaTypeDescriptor(),
//				creationState.getSqlAstCreationState().getCreationContext().getDomainModel().getTypeConfiguration()
//		);
	}
}
