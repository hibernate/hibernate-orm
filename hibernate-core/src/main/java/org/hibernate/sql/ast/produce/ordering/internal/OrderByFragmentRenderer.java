/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.ordering.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.hibernate.internal.CoreLogging;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.metamodel.model.relational.spi.PhysicalColumn;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.spi.QueryParameterBinding;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.sql.ast.consume.SyntaxException;
import org.hibernate.sql.ast.consume.spi.AbstractSqlAstWalker;
import org.hibernate.sql.ast.consume.spi.ConversionContext;
import org.hibernate.sql.ast.tree.spi.assign.Assignment;
import org.hibernate.sql.ast.tree.spi.expression.ColumnReference;
import org.hibernate.sql.ast.tree.spi.sort.SortSpecification;

import org.jboss.logging.Logger;

/**
 * Final step in the boot-time aspect of interpreting order-by fragment into
 * the intermediate SQL fragment (with embedded templates for later replacement).
 *
 * @author Steve Ebersole
 */
public class OrderByFragmentRenderer extends AbstractSqlAstWalker {
	private static final Logger LOG = CoreLogging.logger( OrderByFragmentRenderer.class );

	public static OrderByTranslation renderOrderByFragment(
			TranslationContext translationContext,
			PersistentCollectionDescriptor collectionDescriptor,
			List<SortSpecification> sortSpecifications) {
		final OrderByFragmentRenderer renderer = new OrderByFragmentRenderer(
				translationContext,
				collectionDescriptor
		);

		for ( SortSpecification sortSpecification : sortSpecifications ) {
			renderer.visitSortSpecification( sortSpecification );
		}

		return new OrderByTranslationImpl(
				renderer.getSql(),
				renderer.referencedColumns
		);
	}

	private final TranslationContext context;
	private final PersistentCollectionDescriptor collectionDescriptor;

	private List<PhysicalColumn> referencedColumns;

	public OrderByFragmentRenderer(
			TranslationContext context,
			PersistentCollectionDescriptor collectionDescriptor) {
		this.context = context;
		this.collectionDescriptor = collectionDescriptor;
	}

	@Override
	protected ConversionContext getConversionContext() {
		return context;
	}


	@Override
	public void visitColumnReference(ColumnReference columnReference) {
		final String sqlFragment;

		final Column column = columnReference.getColumn();
		if ( !PhysicalColumn.class.isInstance( column ) ) {
			LOG.debugf( "@OrderBy fragment referred to formula [%s]; passing through", column.getExpression() );
			sqlFragment = column.getExpression();
		}
		else {
			final PhysicalColumn physicalColumn = (PhysicalColumn) column;

			if ( referencedColumns == null ) {
				referencedColumns = new ArrayList<>();
			}
			referencedColumns.add( physicalColumn );

			sqlFragment = OrderByTranslationImpl.determinePlaceholderText( physicalColumn );
		}

		appendSql( sqlFragment );
	}


	@Override
	public void visitAssignment(Assignment assignment) {
		throw new SyntaxException( "Encountered unexpected assignment clause" );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// ParameterBindingResolutionContext - parameters not allowed

	@Override
	public <T> Collection<T> getLoadIdentifiers() {
		return Collections.emptyList();
	}

	@Override
	public QueryParameterBindings getQueryParameterBindings() {
		return NO_PARAM_BINDINGS;
	}

	private static QueryParameterBindings NO_PARAM_BINDINGS = new QueryParameterBindings() {
		@Override
		public boolean isBound(QueryParameter parameter) {
			return false;
		}

		@Override
		public <T> QueryParameterBinding<T> getBinding(QueryParameter<T> parameter) {
			return null;
		}

		@Override
		public <T> QueryParameterBinding<T> getBinding(String name) {
			return null;
		}

		@Override
		public <T> QueryParameterBinding getBinding(int position) {
			return null;
		}

		@Override
		public void validate() {

		}
	};

}
