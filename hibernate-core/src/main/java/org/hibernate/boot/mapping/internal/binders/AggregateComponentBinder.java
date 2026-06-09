/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.binders;

import org.hibernate.annotations.Struct;
import org.hibernate.boot.mapping.internal.model.AggregateMappingIntent;
import org.hibernate.boot.mapping.internal.model.AggregateValuePlan;
import org.hibernate.boot.mapping.internal.sources.ComponentSource;
import org.hibernate.boot.mapping.internal.context.BindingState;
import org.hibernate.mapping.AggregateColumn;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
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
			ComponentMemberTarget memberTarget,
			BindingState state) {
		final AggregateMappingIntent intent = source.aggregateMappingIntent();
		if ( !intent.isAggregate() ) {
			return;
		}
		if ( !memberTarget.isAggregateMemberTarget() ) {
			throw new org.hibernate.AssertionFailure( "Aggregate component members must use an aggregate member target" );
		}

		final AggregateValuePlan plan = AggregateValuePlan.from( intent, state, memberTarget.aggregateMemberContainer() );
		state.addJavaTypeRegistration(
				component.getComponentClass(),
				new EmbeddableAggregateJavaType<>( component.getComponentClass(), plan.structNameText() )
		);
		component.setStructName( plan.structName() );
		component.setStructColumnNames( intent.structAttributeNames() );

		final Column column = ColumnBinder.bindColumn(
				intent.aggregateColumnSource(),
				() -> source.sourceMember().resolveAttributeName()
		);
		final BasicValue aggregateValue = new BasicValue( state.getMetadataBuildingContext(), memberTarget.table() );
		aggregateValue.setTable( memberTarget.table() );
		aggregateValue.setTypeUsingReflection(
				source.sourceMember().getDeclaringType().getName(),
				source.sourceMember().resolveAttributeName()
		);
		if ( plan.explicitAggregateJavaType() ) {
			aggregateValue.setExplicitJavaTypeAccess(
					(typeConfiguration) -> new EmbeddableAggregateJavaType<>( component.getComponentClass(), plan.structNameText() )
			);
		}
		if ( plan.aggregateValueJdbcTypeCode() != null ) {
			final int aggregateValueJdbcTypeCode = plan.aggregateValueJdbcTypeCode();
			aggregateValue.setExplicitJdbcTypeCode( plan.aggregateValueJdbcTypeCode() );
			aggregateValue.setExplicitJdbcTypeAccess( (typeConfiguration) -> {
				final var jdbcTypeRegistry = typeConfiguration.getJdbcTypeRegistry();
				return jdbcTypeRegistry.getConstructor( aggregateValueJdbcTypeCode ) == null
						? jdbcTypeRegistry.getDescriptor( aggregateValueJdbcTypeCode )
						: null;
			} );
		}
		final AggregateColumn aggregateColumn = new AggregateColumn( column, component );
		aggregateColumn.setValue( aggregateValue );
		if ( plan.structNameText() != null && aggregateColumn.getSqlType() == null ) {
			aggregateColumn.setSqlTypeCode( plan.aggregateColumnSqlTypeCode() );
			aggregateColumn.setSqlType( plan.aggregateColumnSqlType() );
		}
		else if ( plan.aggregateColumnSqlTypeCode() != null ) {
			aggregateColumn.setSqlTypeCode( plan.aggregateColumnSqlTypeCode() );
		}
		aggregateValue.addColumn( aggregateColumn );
		if ( !source.isNested() ) {
			memberTarget.table().addColumn( aggregateColumn );
		}
		component.setAggregateColumn( aggregateColumn );

		state.getMetadataBuildingContext().getMetadataCollector().addSecondPass(
				new org.hibernate.boot.model.internal.AggregateComponentSecondPass(
						memberTarget.table(),
						ownerBinding.getEntityName(),
						component,
						source.componentType(),
						source.sourceMember().resolveAttributeName(),
						state.getMetadataBuildingContext(),
						plan.memberContainer()
				)
		);
	}

}
