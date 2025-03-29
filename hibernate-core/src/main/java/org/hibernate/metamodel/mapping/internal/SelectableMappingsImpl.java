/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.dialect.Dialect;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.Value;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.SelectableMappings;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.type.CompositeType;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;
import org.hibernate.type.MappingContext;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Christian Beikov
 */
public class SelectableMappingsImpl implements SelectableMappings {

	private final SelectableMapping[] selectableMappings;

	public SelectableMappingsImpl(SelectableMapping[] selectableMappings) {
		this.selectableMappings = selectableMappings;
	}

	private static void resolveJdbcMappings(List<JdbcMapping> jdbcMappings, MappingContext mapping, Type valueType) {
		final Type keyType;
		if ( valueType instanceof EntityType ) {
			keyType = ( (EntityType) valueType ).getIdentifierOrUniqueKeyType( mapping );
		}
		else {
			keyType = valueType;
		}
		if ( keyType instanceof CompositeType ) {
			Type[] subtypes = ( (CompositeType) keyType ).getSubtypes();
			for ( Type subtype : subtypes ) {
				resolveJdbcMappings( jdbcMappings, mapping, subtype );
			}
		}
		else {
			jdbcMappings.add( (JdbcMapping) keyType );
		}
	}

	public static SelectableMappings from(
			String containingTableExpression,
			Value value,
			int[] propertyOrder,
			MappingContext mappingContext,
			TypeConfiguration typeConfiguration,
			boolean[] insertable,
			boolean[] updateable,
			Dialect dialect,
			SqmFunctionRegistry sqmFunctionRegistry,
			RuntimeModelCreationContext creationContext) {
		if ( insertable.length == 0 ) {
			return from(
					containingTableExpression,
					value,
					propertyOrder,
					mappingContext,
					typeConfiguration,
					dialect,
					sqmFunctionRegistry,
					creationContext
			);
		}
		final List<JdbcMapping> jdbcMappings = new ArrayList<>();
		resolveJdbcMappings( jdbcMappings, mappingContext, value.getType() );

		final List<Selectable> selectables = value.getVirtualSelectables();

		final SelectableMapping[] selectableMappings = new SelectableMapping[jdbcMappings.size()];
		for ( int i = 0; i < selectables.size(); i++ ) {
			selectableMappings[propertyOrder[i]] = SelectableMappingImpl.from(
					containingTableExpression,
					selectables.get( i ),
					jdbcMappings.get( propertyOrder[i] ),
					typeConfiguration,
					insertable[i],
					updateable[i],
					false,
					dialect,
					sqmFunctionRegistry,
					creationContext
			);
		}

		return new SelectableMappingsImpl( selectableMappings );
	}

	private static SelectableMappings from(
			String containingTableExpression,
			Value value,
			int[] propertyOrder,
			MappingContext mapping,
			TypeConfiguration typeConfiguration,
			Dialect dialect,
			SqmFunctionRegistry sqmFunctionRegistry,
			RuntimeModelCreationContext creationContext) {
		final List<JdbcMapping> jdbcMappings = new ArrayList<>();
		resolveJdbcMappings( jdbcMappings, mapping, value.getType() );

		final List<Selectable> selectables = value.getVirtualSelectables();

		final SelectableMapping[] selectableMappings = new SelectableMapping[jdbcMappings.size()];
		for ( int i = 0; i < selectables.size(); i++ ) {
			selectableMappings[propertyOrder[i]] = SelectableMappingImpl.from(
					containingTableExpression,
					selectables.get( i ),
					jdbcMappings.get( propertyOrder[i] ),
					typeConfiguration,
					false,
					false,
					false,
					dialect,
					sqmFunctionRegistry,
					creationContext
			);
		}

		return new SelectableMappingsImpl( selectableMappings );
	}

	public static SelectableMappings from(EmbeddableMappingType embeddableMappingType) {
		final int propertySpan = embeddableMappingType.getNumberOfAttributeMappings();
		final List<SelectableMapping> selectableMappings = CollectionHelper.arrayList( propertySpan );

		embeddableMappingType.forEachAttributeMapping(
				(index, attributeMapping) -> attributeMapping.forEachSelectable(
						(columnIndex, selection) -> selectableMappings.add( selection )
				)
		);

		return new SelectableMappingsImpl( selectableMappings.toArray( new SelectableMapping[0] ) );
	}

	@Override
	public SelectableMapping getSelectable(int columnIndex) {
		return selectableMappings[columnIndex];
	}

	@Override
	public int getJdbcTypeCount() {
		return selectableMappings.length;
	}

	@Override
	public int forEachSelectable(final int offset, final SelectableConsumer consumer) {
		for ( int i = 0; i < selectableMappings.length; i++ ) {
			consumer.accept( offset + i, selectableMappings[i] );
		}
		return selectableMappings.length;
	}

}
