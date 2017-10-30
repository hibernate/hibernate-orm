/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.metamodel.Type;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.sql.ast.produce.metamodel.spi.EmbeddedValueExpressableType;
import org.hibernate.sql.ast.produce.spi.ColumnReferenceQualifier;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;
import org.hibernate.sql.results.internal.CompositeQueryResultImpl;
import org.hibernate.sql.results.internal.SqlSelectionGroupImpl;
import org.hibernate.sql.results.spi.QueryResult;
import org.hibernate.sql.results.spi.QueryResultCreationContext;
import org.hibernate.sql.results.spi.SqlSelection;
import org.hibernate.sql.results.spi.SqlSelectionGroup;
import org.hibernate.sql.results.spi.SqlSelectionGroupResolutionContext;
import org.hibernate.type.descriptor.java.spi.EmbeddableJavaDescriptor;
import org.hibernate.type.descriptor.spi.ValueBinder;
import org.hibernate.type.descriptor.spi.ValueExtractor;

/**
 * Describes parts of the domain model that can be composite values.
 *
 * @author Steve Ebersole
 */
public interface EmbeddedValuedNavigable<J> extends EmbeddedValueExpressableType<J>, NavigableContainer<J> {
	@Override
	EmbeddedContainer getContainer();

	EmbeddedTypeDescriptor<J> getEmbeddedDescriptor();

	@Override
	EmbeddableJavaDescriptor<J> getJavaTypeDescriptor();

	@Override
	default Type.PersistenceType getPersistenceType() {
		return Type.PersistenceType.EMBEDDABLE;
	}

	@Override
	default QueryResult createQueryResult(
			NavigableReference navigableReference,
			String resultVariable,
			QueryResultCreationContext creationContext) {
		return new CompositeQueryResultImpl(
				resultVariable,
				getEmbeddedDescriptor(),
				resolveSqlSelectionGroup(
						navigableReference.getSqlExpressionQualifier(),
						creationContext
				)
		);
	}

	@Override
	default List<SqlSelection> resolveSqlSelectionGroup(
			ColumnReferenceQualifier qualifier,
			SqlSelectionGroupResolutionContext resolutionContext) {
		final List<SqlSelection> group = new ArrayList<>();
		for ( Column column : getEmbeddedDescriptor().collectColumns() ) {
			group.add(
					resolutionContext.getSqlSelectionResolver().resolveSqlSelection(
							resolutionContext.getSqlSelectionResolver().resolveSqlExpression(
									qualifier,
									column
							)
					)
			);
		}

		return group;
	}

	@Override
	default ValueBinder getValueBinder() {
		throw new NotYetImplementedFor6Exception(  );
	}

	@Override
	default ValueExtractor getValueExtractor() {
		throw new NotYetImplementedFor6Exception(  );
	}

	@Override
	default int getNumberOfJdbcParametersToBind() {
		throw new NotYetImplementedFor6Exception(  );
	}
}
