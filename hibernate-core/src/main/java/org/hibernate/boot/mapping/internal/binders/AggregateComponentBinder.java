/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.binders;

import org.hibernate.annotations.Struct;
import org.hibernate.boot.model.relational.QualifiedName;
import org.hibernate.boot.model.relational.QualifiedNameImpl;
import org.hibernate.boot.mapping.internal.sources.ColumnSource;
import org.hibernate.boot.mapping.internal.sources.ComponentSource;
import org.hibernate.boot.mapping.internal.context.BindingState;
import org.hibernate.mapping.AggregateColumn;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.java.spi.EmbeddableAggregateJavaType;

/**
 * New-pipeline support for aggregate embeddable mappings such as {@link Struct}.
 */
final class AggregateComponentBinder {
	private AggregateComponentBinder() {
	}

	static void processAggregate(
			PersistentClass ownerBinding,
			ComponentSource source,
			Component component,
			Table table,
			BindingState state) {
		if ( source.kind() != ComponentSource.Kind.EMBEDDED_ATTRIBUTE || !isAggregate( source ) ) {
			return;
		}

		final QualifiedName structName = determineStructName( source, state );
		final String structNameText = structName == null ? null : structName.render();
		state.addJavaTypeRegistration(
				component.getComponentClass(),
				new EmbeddableAggregateJavaType<>( component.getComponentClass(), structNameText )
		);
		component.setStructName( structName );
		component.setStructColumnNames( determineStructAttributeNames( source ) );

		final Column column = ColumnBinder.bindColumn(
				aggregateColumnSource( source ),
				() -> source.sourceMember().resolveAttributeName()
		);
		final BasicValue aggregateValue = new BasicValue( state.getMetadataBuildingContext(), table );
		aggregateValue.setTable( table );
		aggregateValue.setTypeUsingReflection( ownerBinding.getClassName(), source.sourceMember().resolveAttributeName() );
		final AggregateColumn aggregateColumn = new AggregateColumn( column, component );
		aggregateColumn.setValue( aggregateValue );
		if ( structNameText != null && aggregateColumn.getSqlType() == null ) {
			aggregateColumn.setSqlTypeCode( SqlTypes.STRUCT );
			aggregateColumn.setSqlType( structNameText );
		}
		aggregateValue.addColumn( aggregateColumn );
		table.addColumn( aggregateColumn );
		component.setAggregateColumn( aggregateColumn );

		state.getMetadataBuildingContext().getMetadataCollector().addSecondPass(
				new org.hibernate.boot.model.internal.AggregateComponentSecondPass(
						table,
						ownerBinding.getEntityName(),
						component,
						source.componentType(),
						source.sourceMember().resolveAttributeName(),
						state.getMetadataBuildingContext()
				)
		);
	}

	private static boolean isAggregate(ComponentSource source) {
		return source.sourceMember() != null
				&& ( source.sourceMember().hasDirectAnnotationUsage( Struct.class )
					|| source.componentType().hasDirectAnnotationUsage( Struct.class ) );
	}

	private static ColumnSource aggregateColumnSource(ComponentSource source) {
		return ColumnSource.from( source.sourceMember().getDirectAnnotationUsage( jakarta.persistence.Column.class ) );
	}

	private static QualifiedName determineStructName(ComponentSource source, BindingState state) {
		final Struct memberStruct = source.sourceMember().getDirectAnnotationUsage( Struct.class );
		if ( memberStruct != null ) {
			return toQualifiedName( memberStruct, state );
		}

		final Struct typeStruct = source.componentType().getDirectAnnotationUsage( Struct.class );
		return typeStruct == null ? null : toQualifiedName( typeStruct, state );
	}

	private static QualifiedName toQualifiedName(Struct struct, BindingState state) {
		final var database = state.getDatabase();
		return new QualifiedNameImpl(
				database.toIdentifier( struct.catalog() ),
				database.toIdentifier( struct.schema() ),
				database.toIdentifier( struct.name() )
		);
	}

	private static String[] determineStructAttributeNames(ComponentSource source) {
		final Struct memberStruct = source.sourceMember().getDirectAnnotationUsage( Struct.class );
		if ( memberStruct != null ) {
			return memberStruct.attributes();
		}

		final Struct typeStruct = source.componentType().getDirectAnnotationUsage( Struct.class );
		return typeStruct == null ? null : typeStruct.attributes();
	}
}
