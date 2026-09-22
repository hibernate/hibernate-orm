/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.binders;

import org.hibernate.annotations.Struct;
import org.hibernate.boot.mapping.internal.context.BindingOptions;
import org.hibernate.boot.mapping.internal.context.BindingState;
import org.hibernate.boot.mapping.internal.materialize.BasicValueResolutionBuilder;
import org.hibernate.boot.mapping.internal.materialize.BasicValueResolutionDetails;
import org.hibernate.boot.mapping.internal.model.AggregateMappingIntent;
import org.hibernate.boot.mapping.internal.model.AggregateValuePlan;
import org.hibernate.boot.mapping.internal.sources.BasicValueSource;
import org.hibernate.boot.mapping.internal.sources.ColumnSource;
import org.hibernate.boot.mapping.internal.sources.ComponentSource;
import org.hibernate.boot.model.naming.internal.ImplicitNamingContextImpl;
import org.hibernate.boot.model.naming.internal.ImplicitNamingHelper;
import org.hibernate.boot.model.naming.spi.AggregateColumnNamingInput;
import org.hibernate.boot.model.naming.spi.EntityNamingInput;
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
			ComponentMemberTarget enclosingTarget,
			ColumnSource columnSource,
			BindingOptions options,
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

		final var buildingContext = state.getMetadataBuildingContext();
		final var implicitName = ImplicitNamingHelper.once(
				() -> buildingContext.getBuildingPlan().getImplicitNamingStrategy().determineAggregateColumnName(
						namingInput( ownerBinding, source, intent, enclosingTarget ),
						ImplicitNamingContextImpl.forPhysicalNaming( buildingContext ) ),
				"aggregate column" );
		final var logicalName = ColumnBinder.logicalColumnName( columnSource, implicitName );
		final Column column = ColumnBinder.bindColumn( columnSource,
				ColumnBinder.finalizeColumnName( logicalName.toString(), logicalName.isExplicit(), options, state ),
				false, true, 255, 0, 0 );
		final BasicValue aggregateValue = BasicValue.unregistered( state.getMetadataBuildingContext(), memberTarget.table() );
		aggregateValue.setTable( memberTarget.table() );
		aggregateValue.setTypeUsingReflection(
				source.sourceMember().getDeclaringType().getName(),
				source.sourceMember().resolveAttributeName(),
				state.getClassLoaderService()
		);
		final var resolutionInput = BasicValueResolutionDetails.create(
				aggregateValue,
				BasicValueSource.attribute( source.sourceMember() )
		);
		if ( plan.explicitAggregateJavaType() ) {
			resolutionInput.setExplicitJavaType(
					new EmbeddableAggregateJavaType<>( component.getComponentClass(), plan.structNameText() )
			);
		}
		if ( plan.aggregateValueJdbcTypeCode() != null ) {
			aggregateValue.setExplicitJdbcTypeCode( plan.aggregateValueJdbcTypeCode() );
			resolutionInput.setConfiguredJdbcTypeCode( plan.aggregateValueJdbcTypeCode() );
			final var jdbcTypeRegistry = state.getMetadataBuildingContext().getTypeConfiguration().getJdbcTypeRegistry();
			if ( jdbcTypeRegistry.getConstructor( plan.aggregateValueJdbcTypeCode() ) == null ) {
				resolutionInput.setExplicitJdbcType( jdbcTypeRegistry.getDescriptor( plan.aggregateValueJdbcTypeCode() ) );
			}
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
		BasicValueResolutionBuilder.applyResolution(
				resolutionInput,
				state.getMetadataBuildingContext().getServiceComponents(),
				state.getMappingResolutionState()
		);
		if ( enclosingTarget.isAggregateMemberTarget() ) {
			enclosingTarget.registerMemberColumn( aggregateColumn );
		}
		else {
			enclosingTarget.table().addColumn( aggregateColumn );
			ColumnBinder.registerColumnNameBinding( enclosingTarget.table(), logicalName, aggregateColumn, options, state );
		}
		component.setAggregateColumn( aggregateColumn );

		state.addAggregateComponentBinding( new AggregateComponentBinding(
				ownerBinding.getEntityName(),
				component,
				source.componentType(),
				source.sourceMember().resolveAttributeName(),
				state.getMetadataBuildingContext(),
				plan.memberContainer()
		) );
	}

	private static AggregateColumnNamingInput namingInput(
			PersistentClass ownerBinding,
			ComponentSource source,
			AggregateMappingIntent intent,
			ComponentMemberTarget enclosingTarget) {
		return new AggregateColumnNamingInput(
				new EntityNamingInput( ownerBinding.getClassName(), ownerBinding.getEntityName(), ownerBinding.getJpaEntityName() ),
				source.componentType().getName(),
				attributePath( source ),
				source.sourceMember().resolveAttributeName(),
				namingUsage( source ),
				switch ( intent.aggregateKind() ) {
					case STRUCT -> AggregateColumnNamingInput.StorageKind.STRUCT;
					case JSON -> AggregateColumnNamingInput.StorageKind.JSON;
					case XML -> AggregateColumnNamingInput.StorageKind.XML;
				},
				intent.plural(),
				enclosingTarget.isAggregateMemberTarget()
						? AggregateColumnNamingInput.Scope.AGGREGATE_MEMBER
						: AggregateColumnNamingInput.Scope.TABLE_COLUMN
		);
	}

	private static AggregateColumnNamingInput.Usage namingUsage(ComponentSource source) {
		if ( source.isNested() ) {
			return AggregateColumnNamingInput.Usage.ATTRIBUTE;
		}
		return switch ( source.kind() ) {
			case COLLECTION_ELEMENT -> AggregateColumnNamingInput.Usage.COLLECTION_ELEMENT;
			case MAP_KEY -> AggregateColumnNamingInput.Usage.MAP_KEY;
			default -> AggregateColumnNamingInput.Usage.ATTRIBUTE;
		};
	}

	private static String attributePath(ComponentSource source) {
		final String path = source.namingPathPrefix().substring( 0, source.namingPathPrefix().length() - 1 );
		// Map-key member naming has a synthetic key segment; usage carries that
		// distinction separately in the aggregate naming input.
		return source.kind() == ComponentSource.Kind.MAP_KEY
				? path.replaceFirst( "\\.key(?=\\.|$)", "" )
				: path;
	}

}
