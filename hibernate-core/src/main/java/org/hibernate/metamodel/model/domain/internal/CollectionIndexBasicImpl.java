/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import java.util.Collections;
import java.util.List;

import org.hibernate.boot.model.domain.BasicValueMapping;
import org.hibernate.mapping.IndexedCollection;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.spi.AbstractCollectionIndex;
import org.hibernate.metamodel.model.domain.spi.CollectionIndexBasic;
import org.hibernate.metamodel.model.domain.spi.ConvertibleNavigable;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.sql.ast.produce.result.internal.QueryResultScalarImpl;
import org.hibernate.sql.ast.produce.result.spi.QueryResult;
import org.hibernate.sql.ast.produce.result.spi.QueryResultCreationContext;
import org.hibernate.sql.ast.produce.result.spi.SqlSelectionResolver;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;
import org.hibernate.type.converter.spi.AttributeConverterDefinition;
import org.hibernate.type.spi.BasicType;

/**
 * @author Steve Ebersole
 */
public class CollectionIndexBasicImpl<J>
		extends AbstractCollectionIndex<J>
		implements CollectionIndexBasic<J>, ConvertibleNavigable<J> {

	private final BasicType<J> basicType;
	private final Column column;
	private final AttributeConverterDefinition attributeConverter;

	public CollectionIndexBasicImpl(
			PersistentCollectionDescriptor persister,
			IndexedCollection mappingBinding,
			RuntimeModelCreationContext creationContext) {
		super( persister );

		final BasicValueMapping valueMapping = (BasicValueMapping) mappingBinding.getIndex();
		this.column  = creationContext.getDatabaseObjectResolver().resolveColumn( valueMapping.getMappedColumn() );

		this.attributeConverter = valueMapping.getAttributeConverterDefinition();

		// todo (6.0) : SimpleValue -> BasicType
		this.basicType = null;
	}

	@Override
	public AttributeConverterDefinition getAttributeConverter() {
		return attributeConverter;
	}

	@Override
	public Column getBoundColumn() {
		return column;
	}

	@Override
	public List<Column> getColumns() {
		return Collections.singletonList( getBoundColumn() );
	}

	@Override
	public BasicType<J> getBasicType() {
		return basicType;
	}

	@Override
	public QueryResult generateQueryResult(
			NavigableReference selectedExpression,
			String resultVariable,
			SqlSelectionResolver sqlSelectionResolver,
			QueryResultCreationContext creationContext) {
		return new QueryResultScalarImpl(
				selectedExpression,
				sqlSelectionResolver.resolveSqlSelection(
						creationContext.currentColumnReferenceSource().resolveColumnReference( column )
				),
				resultVariable,
				this
	  	);
	}
}
