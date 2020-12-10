/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.Value;
import org.hibernate.metamodel.mapping.SelectionConsumer;
import org.hibernate.metamodel.mapping.SelectionMapping;
import org.hibernate.metamodel.mapping.SelectionMappings;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.type.CompositeType;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;

/**
 * @author Christian Beikov
 */
public class SelectionMappingsImpl implements SelectionMappings {

	private final SelectionMapping[] selectionMappings;

	private SelectionMappingsImpl(SelectionMapping[] selectionMappings) {
		this.selectionMappings = selectionMappings;
	}

	private static void resolveJdbcMappings(List<JdbcMapping> jdbcMappings, Mapping mapping, Type valueType) {
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

	public static SelectionMappings from(
			String containingTableExpression,
			Value value,
			Mapping mapping,
			Dialect dialect,
			SqmFunctionRegistry sqmFunctionRegistry) {
		final List<JdbcMapping> jdbcMappings = new ArrayList<>();
		resolveJdbcMappings( jdbcMappings, mapping, value.getType() );
		final List<SelectionMapping> selectionMappings = new ArrayList<>( jdbcMappings.size() );
		final Iterator<Selectable> columnIterator = value.getColumnIterator();
		while ( columnIterator.hasNext() ) {
			final Selectable selectable = columnIterator.next();
			selectionMappings.add(
					SelectionMappingImpl.from(
							containingTableExpression,
							selectable,
							jdbcMappings.get( selectionMappings.size() ),
							dialect,
							sqmFunctionRegistry
					)
			);
		}
		return new SelectionMappingsImpl( selectionMappings.toArray( new SelectionMapping[0] ) );
	}

	public static SelectionMappings from(EmbeddableMappingType embeddableMappingType) {
		final int propertySpan = embeddableMappingType.getNumberOfAttributeMappings();
		final List<SelectionMapping> selectionMappings = CollectionHelper.arrayList( propertySpan );

		embeddableMappingType.forEachAttributeMapping(
				(index, attributeMapping) -> {
					attributeMapping.forEachSelection(
							(columnIndex, selection) -> {
								selectionMappings.add( selection );
							}
					);
				}
		);

		return new SelectionMappingsImpl( selectionMappings.toArray( new SelectionMapping[0] ) );
	}

	@Override
	public SelectionMapping getSelectionMapping(int columnIndex) {
		return selectionMappings[columnIndex];
	}

	@Override
	public int getJdbcTypeCount() {
		return selectionMappings.length;
	}

	@Override
	public int forEachSelection(final int offset, final SelectionConsumer consumer) {
		for ( int i = 0; i < selectionMappings.length; i++ ) {
			consumer.accept( offset + i, selectionMappings[i] );
		}
		return selectionMappings.length;
	}

}
