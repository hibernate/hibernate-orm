/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.materialize;

import java.util.List;

import org.hibernate.boot.model.internal.GeneratorBinder;
import org.hibernate.boot.model.internal.GeneratorStrategies;
import org.hibernate.boot.models.bind.internal.binders.BasicValueBinder;
import org.hibernate.boot.models.bind.internal.binders.ColumnBinder;
import org.hibernate.boot.models.bind.internal.binders.ComponentBinder;
import org.hibernate.boot.models.bind.internal.binders.CustomMappingBinder;
import org.hibernate.boot.models.bind.internal.binders.IdentifierBinding;
import org.hibernate.boot.models.bind.internal.binders.ModelBinders;
import org.hibernate.boot.models.bind.internal.model.IdentifierAttributeBinding;
import org.hibernate.boot.models.bind.internal.model.IdentifierContribution;
import org.hibernate.boot.models.bind.internal.sources.BasicValueSource;
import org.hibernate.boot.models.bind.internal.sources.ColumnSource;
import org.hibernate.boot.models.bind.internal.sources.ComponentSource;
import org.hibernate.boot.models.bind.spi.BindingContext;
import org.hibernate.boot.models.bind.spi.BindingOptions;
import org.hibernate.boot.models.bind.spi.BindingState;
import org.hibernate.boot.models.categorize.spi.AggregatedKeyMapping;
import org.hibernate.boot.models.categorize.spi.AttributeMetadata;
import org.hibernate.boot.models.categorize.spi.BasicKeyMapping;
import org.hibernate.boot.models.bind.internal.view.IdentifierContributionView;
import org.hibernate.boot.models.categorize.spi.EntityTypeMetadata;
import org.hibernate.boot.models.categorize.spi.KeyMapping;
import org.hibernate.boot.models.categorize.spi.NonAggregatedKeyMapping;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.SyntheticProperty;
import org.hibernate.mapping.Table;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;

import jakarta.persistence.GeneratedValue;
import jakarta.annotation.Nullable;

import static jakarta.persistence.GenerationType.AUTO;
import static org.hibernate.boot.model.internal.ClassPropertyHolder.handleGenericComponentProperty;
import static org.hibernate.boot.models.bind.internal.binders.AttributeBinder.bindPropertyAccessor;
import static org.hibernate.property.access.spi.BuiltInPropertyAccessStrategies.EMBEDDED;
import static org.hibernate.spi.NavigablePath.IDENTIFIER_MAPPER_PROPERTY;

/// Transitional materialization boundary for entity identifiers.
///
/// The horizontal binding model owns identifier semantics through
/// [IdentifierContribution] and [IdentifierContributionView].  This class is the
/// named bridge that turns those facts, plus the mapping objects created by the
/// existing worker binder, into the compatibility [IdentifierBinding] consumed by
/// later boot phases.
///
/// In the next slices, mapping object creation should move behind this boundary
/// instead of being interleaved with semantic contribution collection in
/// [IdentifierBinder].
///
/// @since 9.0
/// @author Steve Ebersole
public class IdentifierMappingMaterializer {
	private final ModelBinders modelBinders;
	private final BindingState state;
	private final BindingOptions options;
	private final BindingContext context;

	public IdentifierMappingMaterializer(
			ModelBinders modelBinders,
			BindingState state,
			BindingOptions options,
			BindingContext context) {
		this.modelBinders = modelBinders;
		this.state = state;
		this.options = options;
		this.context = context;
	}

	public IdentifierBinding materializeBasicIdentifier(
			EntityTypeMetadata typeMetadata,
			RootClass typeBinding,
			BasicKeyMapping basicKeyMapping,
			Table table,
			IdentifierContribution contribution) {
		final AttributeMetadata idAttribute = basicKeyMapping.getAttribute();
		final MemberDetails idAttributeMember = idAttribute.getMember();

		final BasicValue idValue = createBasicIdValue( table, idAttributeMember );
		typeBinding.setIdentifier( idValue );

		final Property idProperty = createProperty( idAttribute.getName(), idValue, idAttributeMember );
		typeBinding.setIdentifierProperty( idProperty );
		typeBinding.setDeclaredIdentifierProperty( idProperty );

		final Column column = bindIdColumn( idAttributeMember, idAttribute::getName, table, idValue );
		CustomMappingBinder.callAttributeBinders( idAttributeMember, typeBinding, idProperty, state, context );
		addSelectableName( contribution, idAttribute.getName(), column.getName() );

		return materializeIdentifierBinding(
				typeMetadata,
				typeBinding,
				basicKeyMapping,
				idValue,
				idProperty,
				table,
				List.of( column ),
				contribution
		);
	}

	public IdentifierBinding materializeAggregatedIdentifier(
			EntityTypeMetadata type,
			RootClass typeBinding,
			AggregatedKeyMapping aggregatedKeyMapping,
			Table table,
			ClassDetails keyType,
			ComponentSource componentSource,
			IdentifierContribution contribution) {
		final Component idValue = new Component( state.getMetadataBuildingContext(), typeBinding );
		idValue.setKey( true );
		idValue.setEmbedded( false );
		idValue.setComponentClassName( keyType.getClassName() );
		idValue.setTable( table );
		idValue.setTypeUsingReflection( type.getClassDetails().getClassName(), aggregatedKeyMapping.getAttributeName() );
		typeBinding.setIdentifier( idValue );
		typeBinding.setEmbeddedIdentifier( true );

		final Property idProperty = createProperty(
				aggregatedKeyMapping.getAttributeName(),
				idValue,
				aggregatedKeyMapping.getAttribute().getMember()
		);
		typeBinding.setIdentifierProperty( idProperty );
		typeBinding.setDeclaredIdentifierProperty( idProperty );
		CustomMappingBinder.callAttributeBinders(
				aggregatedKeyMapping.getAttribute().getMember(),
				typeBinding,
				idProperty,
				state,
				context
		);

		final List<Column> columns = bindComponentIdentifierProperties(
				type,
				typeBinding,
				componentSource,
				idValue,
				table
		);
		handleGenericComponentProperty(
				idProperty,
				aggregatedKeyMapping.getAttribute().getMember(),
				state.getMetadataBuildingContext()
		);
		addAggregatedSelectableNames( contribution, idValue, componentSource );

		return materializeIdentifierBinding(
				type,
				typeBinding,
				aggregatedKeyMapping,
				idValue,
				idProperty,
				table,
				columns,
				contribution
		);
	}

	public IdentifierBinding materializeScalarIdClassIdentifier(
			EntityTypeMetadata type,
			RootClass typeBinding,
			NonAggregatedKeyMapping idMapping,
			Table table,
			IdentifierContribution contribution) {
		final Component idValue = new Component( state.getMetadataBuildingContext(), typeBinding );
		idValue.setKey( true );
		idValue.setEmbedded( true );
		idValue.setComponentClassName( idMapping.getIdClassType().getClassName() );
		idValue.setTable( table );
		typeBinding.setIdentifier( idValue );
		typeBinding.setEmbeddedIdentifier( false );

		final Component identifierMapper = new Component( state.getMetadataBuildingContext(), typeBinding );
		identifierMapper.setEmbedded( true );
		identifierMapper.setComponentClassName( typeBinding.getClassName() );
		identifierMapper.setTable( table );
		typeBinding.setIdentifierMapper( identifierMapper );
		typeBinding.setDeclaredIdentifierMapper( identifierMapper );

		final SyntheticProperty syntheticMapperProperty = new SyntheticProperty();
		syntheticMapperProperty.setName( IDENTIFIER_MAPPER_PROPERTY );
		syntheticMapperProperty.setUpdatable( false );
		syntheticMapperProperty.setInsertable( false );
		syntheticMapperProperty.setPropertyAccessorName( EMBEDDED.getExternalName() );
		syntheticMapperProperty.setValue( identifierMapper );
		typeBinding.addProperty( syntheticMapperProperty );

		final List<Column> columns = new java.util.ArrayList<>( idMapping.getIdAttributes().size() );
		for ( AttributeMetadata idAttribute : idMapping.getIdAttributes() ) {
			final MemberDetails member = idAttribute.getMember();
			final IdentifierAttributeBinding attribute = contribution.getAttribute( idAttribute.getName() );
			final MemberDetails idClassMember = attribute.idRepresentationMember();

			final BasicValue basicValue = createBasicIdValue( table, member );
			final Property rootProperty = createProperty( idAttribute.getName(), basicValue, member );
			CustomMappingBinder.callAttributeBinders( member, typeBinding, rootProperty, state, context );

			final BasicValue idClassValue = createBasicIdValue( table, idClassMember );
			applyGeneratedValue( idClassValue, member );

			final Property mapperProperty = createProperty( idAttribute.getName(), basicValue, member );
			mapperProperty.setInsertable( false );
			mapperProperty.setUpdatable( false );
			identifierMapper.addProperty( mapperProperty );

			final Property idClassProperty = createProperty( idAttribute.getName(), idClassValue, idClassMember );
			idValue.addProperty( idClassProperty );

			final Column column = bindIdColumn( member, idAttribute::getName, table, basicValue, idClassValue );
			columns.add( column );
			addSelectableName( contribution, idAttribute.getName(), column.getName() );
		}

		return materializeIdentifierBinding(
				type,
				typeBinding,
				idMapping,
				idValue,
				null,
				table,
				columns,
				contribution
		);
	}

	public IdentifierBinding materializeIdentifierBinding(
			EntityTypeMetadata entityType,
			RootClass rootClass,
			KeyMapping keyMapping,
			KeyValue identifierValue,
			@Nullable Property identifierProperty,
			Table table,
			List<Column> columns,
			IdentifierContribution contribution) {
		final IdentifierContributionView view = new IdentifierContributionView( contribution );
		view.identifierSelectableNames();
		return new IdentifierBinding(
				entityType,
				rootClass,
				keyMapping,
				identifierValue,
				identifierProperty,
				table,
				columns
		);
	}

	private List<Column> bindComponentIdentifierProperties(
			EntityTypeMetadata type,
			RootClass typeBinding,
			ComponentSource componentSource,
			Component idValue,
			Table table) {
		return new ComponentBinder( modelBinders, state, options, context ).bindBasicProperties(
				type,
				typeBinding,
				componentSource,
				idValue,
				table,
				(member, column) -> table.getPrimaryKey().addColumn( column ),
				false,
				false,
				false
		);
	}

	private void addAggregatedSelectableNames(
			IdentifierContribution contribution,
			Component idValue,
			ComponentSource componentSource) {
		for ( ComponentSource.ComponentMember componentMember : componentSource.members() ) {
			final Property property = idValue.getProperty( componentMember.attributeName() );
			for ( Column column : property.getValue().getColumns() ) {
				addSelectableName( contribution, componentMember.attributeName(), column.getName() );
			}
		}
	}

	private void addSelectableName(
			IdentifierContribution contribution,
			String attributeName,
			String selectableName) {
		final IdentifierAttributeBinding attribute = contribution.getAttribute( attributeName );
		if ( attribute != null ) {
			attribute.addSelectableName( selectableName );
		}
	}

	private BasicValue createBasicIdValue(Table table, MemberDetails member) {
		final BasicValue basicValue = new BasicValue( state.getMetadataBuildingContext(), table );
		basicValue.setTable( table );
		BasicValueBinder.bindBasicValue(
				BasicValueSource.identifier( member ),
				null,
				basicValue,
				options,
				state,
				context
		);
		applyGeneratedValue( basicValue, member );
		return basicValue;
	}

	private void applyGeneratedValue(BasicValue idValue, MemberDetails member) {
		if ( GeneratorBinder.createIdGeneratorFromGeneratorAnnotation(
				idValue,
				member,
				state.getMetadataBuildingContext(),
				idValue.getTable().getName() + "." + member.getName()
		) ) {
			return;
		}

		final GeneratedValue generatedValue = member.getDirectAnnotationUsage( GeneratedValue.class );
		if ( generatedValue == null ) {
			return;
		}

		final var generationType = generatedValue.strategy() == null ? AUTO : generatedValue.strategy();
		GeneratorBinder.makeIdGenerator(
				idValue,
				member,
				GeneratorStrategies.generatorStrategy( generationType, generatedValue.generator(), member.getType() ),
				generatedValue.generator(),
				state.getMetadataBuildingContext(),
				null
		);
	}

	private Property createProperty(String name, org.hibernate.mapping.Value value, MemberDetails member) {
		final Property property = new Property();
		property.setName( name );
		property.setValue( value );
		bindPropertyAccessor( member, property );
		return property;
	}

	private Column bindIdColumn(
			MemberDetails member,
			java.util.function.Supplier<String> implicitName,
			Table table,
			BasicValue basicValue,
			BasicValue... additionalValues) {
		final jakarta.persistence.Column columnAnn = member.getDirectAnnotationUsage( jakarta.persistence.Column.class );
		final Column column = ColumnBinder.bindColumn(
				ColumnSource.from( columnAnn ),
				implicitName,
				true,
				false
		);
		basicValue.addColumn( column, columnAnn == null || columnAnn.insertable(), false );
		for ( BasicValue additionalValue : additionalValues ) {
			additionalValue.addColumn( column, true, true );
		}
		table.addColumn( column );
		table.getPrimaryKey().addColumn( column );
		return column;
	}
}
