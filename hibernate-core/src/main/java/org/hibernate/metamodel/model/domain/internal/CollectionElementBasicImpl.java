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
import org.hibernate.mapping.Collection;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.spi.AbstractCollectionElement;
import org.hibernate.metamodel.model.domain.spi.CollectionElementBasic;
import org.hibernate.metamodel.model.domain.spi.ConvertibleNavigable;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.sql.results.internal.ScalarQueryResultImpl;
import org.hibernate.sql.results.spi.QueryResult;
import org.hibernate.sql.results.spi.QueryResultCreationContext;
import org.hibernate.sql.results.spi.SqlSelectionResolver;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;
import org.hibernate.type.converter.spi.AttributeConverterDefinition;
import org.hibernate.type.descriptor.spi.ValueBinder;
import org.hibernate.type.descriptor.spi.ValueExtractor;
import org.hibernate.type.spi.BasicType;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class CollectionElementBasicImpl<J>
		extends AbstractCollectionElement<J>
		implements CollectionElementBasic<J>, ConvertibleNavigable<J> {
	private static final Logger log = Logger.getLogger( CollectionElementBasicImpl.class );

	private final Column column;
	private final BasicType<J> basicType;
	private final AttributeConverterDefinition attributeConverter;

	public CollectionElementBasicImpl(
			PersistentCollectionDescriptor persister,
			Collection mappingBinding,
			RuntimeModelCreationContext creationContext) {
		super( persister );

		final BasicValueMapping simpleElementValueMapping = (BasicValueMapping) mappingBinding.getElement();

		this.column = creationContext.getDatabaseObjectResolver().resolveColumn( simpleElementValueMapping.getMappedColumn() );

		// todo (6.0) : resolve SimpleValue -> BasicType
		this.basicType = null;

		this.attributeConverter = simpleElementValueMapping.getAttributeConverterDefinition();

		log.debugf(
				"AttributeConverter [%s] being injected for elements of the '%s' collection; was : %s",
				attributeConverter.getAttributeConverter(),
				getContainer().getNavigableRole(),
				this.attributeConverter
		);
	}

	@Override
	public AttributeConverterDefinition getAttributeConverter() {
		return attributeConverter;
	}

	@Override
	public ElementClassification getClassification() {
		return ElementClassification.BASIC;
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
		return new ScalarQueryResultImpl(
				resultVariable,
				sqlSelectionResolver.resolveSqlSelection(
						creationContext.currentColumnReferenceSource().resolveColumnReference( getBoundColumn() )
				),
				getBasicType()
		);
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
	public ValueBinder getValueBinder() {
		return basicType.getValueBinder();
	}

	@Override
	public ValueExtractor getValueExtractor() {
		return basicType.getValueExtractor();
	}
}
