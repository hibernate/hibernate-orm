/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.materialize;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.hibernate.boot.model.internal.GeneratorBinder;
import org.hibernate.boot.model.internal.GeneratorStrategies;
import org.hibernate.boot.mapping.internal.binders.AssociationIdentifierBinding;
import org.hibernate.boot.mapping.internal.binders.AssociationTableBinding;
import org.hibernate.boot.mapping.internal.binders.BasicValueBinder;
import org.hibernate.boot.mapping.internal.binders.ColumnBinder;
import org.hibernate.boot.mapping.internal.binders.ComponentBinder;
import org.hibernate.boot.mapping.internal.binders.CustomMappingBinder;
import org.hibernate.boot.mapping.internal.binders.EntityTypeBinder;
import org.hibernate.boot.mapping.internal.binders.IdentifierBinding;
import org.hibernate.boot.mapping.internal.binders.ModelBinders;
import org.hibernate.boot.mapping.internal.model.IdentifierAttributeBinding;
import org.hibernate.boot.mapping.internal.model.EntityIdentifierBinding;
import org.hibernate.boot.mapping.internal.sources.BasicValueSource;
import org.hibernate.boot.mapping.internal.sources.ColumnSource;
import org.hibernate.boot.mapping.internal.sources.ComponentSource;
import org.hibernate.boot.mapping.internal.sources.ForeignKeySource;
import org.hibernate.boot.mapping.internal.sources.ToOneSource;
import org.hibernate.boot.mapping.internal.context.BindingContext;
import org.hibernate.boot.mapping.internal.context.BindingOptions;
import org.hibernate.boot.mapping.internal.context.BindingState;
import org.hibernate.boot.models.AttributeNature;
import org.hibernate.boot.mapping.internal.categorize.AggregatedKeyMapping;
import org.hibernate.boot.mapping.internal.categorize.AttributeMetadata;
import org.hibernate.boot.mapping.internal.categorize.BasicKeyMapping;
import org.hibernate.boot.mapping.internal.view.EntityIdentifierBindingView;
import org.hibernate.boot.mapping.internal.categorize.EntityTypeMetadata;
import org.hibernate.boot.mapping.internal.categorize.KeyMapping;
import org.hibernate.boot.mapping.internal.categorize.NonAggregatedKeyMapping;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.generator.EventTypeSets;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.OneToOne;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.ToOne;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;

import jakarta.persistence.Embedded;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.Transient;
import jakarta.annotation.Nullable;

import static jakarta.persistence.GenerationType.AUTO;
import static org.hibernate.boot.model.internal.ClassPropertyHolder.handleGenericComponentProperty;
import static org.hibernate.id.IdentifierGeneratorHelper.getForeignId;

/// Transitional materialization boundary for entity identifiers.
///
/// The horizontal binding model owns identifier semantics through
/// [EntityIdentifierBinding] and [EntityIdentifierBindingView].  This class is the
/// named bridge that turns those facts, plus the mapping objects created by the
/// existing worker binder, into the compatibility [IdentifierBinding] consumed by
/// later boot phases.
///
/// In the next slices, mapping object creation should move behind this boundary
/// instead of being interleaved with semantic binding collection in
/// [IdentifierBinder].
///
/// @since 9.0
/// @author Steve Ebersole
public class IdentifierMappingMaterializer {
	private final ModelBinders modelBinders;
	private final BindingState state;
	private final BindingOptions options;
	private final BindingContext context;
	private final PropertyMappingMaterializer propertyMappingMaterializer = new PropertyMappingMaterializer();
	private final PrimaryTableKeyMappingMaterializer primaryTableKeyMappingMaterializer;

	public IdentifierMappingMaterializer(
			ModelBinders modelBinders,
			BindingState state,
			BindingOptions options,
			BindingContext context) {
		this.modelBinders = modelBinders;
		this.state = state;
		this.options = options;
		this.context = context;
		this.primaryTableKeyMappingMaterializer = new PrimaryTableKeyMappingMaterializer(
				state.getMetadataBuildingContext()
		);
	}

	public IdentifierBinding materializeBasicIdentifier(
			EntityTypeMetadata typeMetadata,
			RootClass typeBinding,
			BasicKeyMapping basicKeyMapping,
			Table table,
			EntityIdentifierBinding binding) {
		final AttributeMetadata idAttribute = basicKeyMapping.getAttribute();
		final MemberDetails idAttributeMember = idAttribute.getMember();

		final BasicValue idValue = createBasicIdValue( table, idAttributeMember );
		typeBinding.setIdentifier( idValue );

		final Property idProperty = createProperty( idAttribute.getName(), idValue, idAttributeMember );
		typeBinding.setIdentifierProperty( idProperty );
		typeBinding.setDeclaredIdentifierProperty( idProperty );

		final Column column = bindIdColumn( idAttributeMember, idAttribute::getName, table, idValue );
		CustomMappingBinder.callAttributeBinders( idAttributeMember, typeBinding, idProperty, state, context );
		addSelectableName( binding, idAttribute.getName(), column.getName() );

		return materializeEntityIdentifierBinding(
				typeMetadata,
				typeBinding,
				basicKeyMapping,
				idValue,
				idProperty,
				table,
				List.of( column ),
				binding
		);
	}

	public IdentifierBinding materializeAggregatedIdentifier(
			EntityTypeMetadata type,
			RootClass typeBinding,
			AggregatedKeyMapping aggregatedKeyMapping,
			Table table,
			ClassDetails keyType,
			ComponentSource componentSource,
			EntityIdentifierBinding binding) {
		final Component idValue = new Component( state.getMetadataBuildingContext(), typeBinding );
		idValue.setKey( true );
		idValue.setFlattened( false );
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
		addAggregatedSelectableNames( binding, idValue, componentSource );

		return materializeEntityIdentifierBinding(
				type,
				typeBinding,
				aggregatedKeyMapping,
				idValue,
				idProperty,
				table,
				columns,
				binding
		);
	}

	public IdentifierBinding materializeScalarIdClassIdentifier(
			EntityTypeMetadata type,
			RootClass typeBinding,
			NonAggregatedKeyMapping idMapping,
			Table table,
			EntityIdentifierBinding binding) {
		final Component idValue = new Component( state.getMetadataBuildingContext(), typeBinding );
		idValue.setKey( true );
		idValue.setFlattened( true );
		idValue.setComponentClassName( idMapping.getIdClassType().getClassName() );
		idValue.setTable( table );
		typeBinding.setIdentifier( idValue );
		typeBinding.setEmbeddedIdentifier( false );

		final Component identifierMapper = new Component( state.getMetadataBuildingContext(), typeBinding );
		identifierMapper.setFlattened( true );
		identifierMapper.setComponentClassName( typeBinding.getClassName() );
		identifierMapper.setTable( table );
		typeBinding.setIdentifierMapper( identifierMapper );
		typeBinding.setDeclaredIdentifierMapper( identifierMapper );

			typeBinding.addProperty( propertyMappingMaterializer.createIdentifierMapperProperty( identifierMapper ) );

		final List<Column> columns = new java.util.ArrayList<>( idMapping.getIdAttributes().size() );
		for ( AttributeMetadata idAttribute : idMapping.getIdAttributes() ) {
			final MemberDetails member = idAttribute.getMember();
			final IdentifierAttributeBinding attribute = binding.getAttribute( idAttribute.getName() );
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
			addSelectableName( binding, idAttribute.getName(), column.getName() );
		}

		return materializeEntityIdentifierBinding(
				type,
				typeBinding,
				idMapping,
				idValue,
				null,
				table,
				columns,
				binding
		);
	}

	public IdentifierBinding materializeEmbeddedIdClassIdentifier(
			EntityTypeMetadata type,
			RootClass typeBinding,
			NonAggregatedKeyMapping idMapping,
			Table table,
			EntityIdentifierBinding binding) {
		final IdClassMappingParts mappingParts = createIdClassMappingParts( typeBinding, idMapping, table );
		final List<Column> columns = new java.util.ArrayList<>( idMapping.getIdAttributes().size() );

		for ( AttributeMetadata idAttribute : idMapping.getIdAttributes() ) {
			final MemberDetails member = idAttribute.getMember();
			final IdentifierAttributeBinding attribute = binding.getAttribute( idAttribute.getName() );
			final MemberDetails idClassMember = attribute.idRepresentationMember();
			if ( idAttribute.getNature() == AttributeNature.BASIC ) {
				final BasicValue basicValue = createBasicIdValue( table, member );
				final Property rootProperty = createProperty( idAttribute.getName(), basicValue, member );
				CustomMappingBinder.callAttributeBinders( member, typeBinding, rootProperty, state, context );

				final BasicValue idClassValue = createBasicIdValue( table, idClassMember );
				applyGeneratedValue( idClassValue, member );

				final Property mapperProperty = createProperty( idAttribute.getName(), basicValue, member );
				mapperProperty.setInsertable( false );
				mapperProperty.setUpdatable( false );
				mappingParts.identifierMapper().addProperty( mapperProperty );

				final Property idClassProperty = createProperty( idAttribute.getName(), idClassValue, idClassMember );
				mappingParts.idValue().addProperty( idClassProperty );

				final Column column = bindIdColumn( member, idAttribute::getName, table, basicValue, idClassValue );
				columns.add( column );
				addSelectableName( binding, idAttribute.getName(), column.getName() );
			}
			else if ( idAttribute.getNature() == AttributeNature.EMBEDDED ) {
				final Component idClassComponent = createEmbeddedIdClassComponent(
						idAttribute,
						idClassMember,
						table,
						type,
						typeBinding
				);
				final Property idClassProperty = createProperty( idAttribute.getName(), idClassComponent, idClassMember );
				mappingParts.idValue().addProperty( idClassProperty );

				final Component identifierMapperComponent = idClassComponent.copy();
				final Property mapperProperty = createProperty( idAttribute.getName(), identifierMapperComponent, member );
				mapperProperty.setInsertable( false );
				mapperProperty.setUpdatable( false );
				mappingParts.identifierMapper().addProperty( mapperProperty );
				handleGenericComponentProperty(
						mapperProperty,
						member,
						state.getMetadataBuildingContext()
				);

				for ( Column column : idClassComponent.getColumns() ) {
					columns.add( column );
					addSelectableName( binding, idAttribute.getName(), column.getName() );
				}
			}
		}

		return materializeEntityIdentifierBinding(
				type,
				typeBinding,
				idMapping,
				mappingParts.idValue(),
				null,
				table,
				columns,
				binding
		);
	}

	public IdentifierBinding materializeAssociationIdClassIdentifier(
			EntityTypeMetadata type,
			RootClass typeBinding,
			NonAggregatedKeyMapping idMapping,
			Table table,
			EntityIdentifierBinding binding) {
		final IdClassMappingParts mappingParts = createIdClassMappingParts( typeBinding, idMapping, table );
		final List<Column> columns = new ArrayList<>( idMapping.getIdAttributes().size() );

		for ( AttributeMetadata idAttribute : idMapping.getIdAttributes() ) {
			final IdentifierAttributeBinding attribute = binding.getAttribute( idAttribute.getName() );
			if ( idAttribute.getNature() == AttributeNature.BASIC && !isToOneMember( attribute.idRepresentationMember() ) ) {
				materializeBasicIdClassAttribute(
						typeBinding,
						table,
						mappingParts,
						idAttribute,
						attribute.idRepresentationMember(),
						columns,
						binding
				);
			}
			else if ( idAttribute.getNature() == AttributeNature.EMBEDDED ) {
				materializeEmbeddedIdClassAttribute(
						type,
						typeBinding,
						table,
						mappingParts,
						idAttribute,
						attribute.idRepresentationMember(),
						columns,
						binding
				);
			}
			else {
				materializeAssociationIdClassAttribute(
						type,
						typeBinding,
						table,
						mappingParts,
						idAttribute,
						attribute,
						columns
				);
			}
		}

		return materializeEntityIdentifierBinding(
				type,
				typeBinding,
				idMapping,
				mappingParts.idValue(),
				null,
				table,
				columns,
				binding
		);
	}

	public IdentifierBinding materializeNonAggregatedIdentifier(
			EntityTypeMetadata type,
			RootClass typeBinding,
			NonAggregatedKeyMapping idMapping,
			Table table,
			EntityIdentifierBinding binding,
			boolean hasIdClass,
			boolean wholeDerivedIdClass,
			boolean noIdClassMapsId) {
		final Component idValue = new Component( state.getMetadataBuildingContext(), typeBinding );
		idValue.setKey( true );
		idValue.setFlattened( true );
		final boolean separateIdentifierMapper = hasIdClass && !wholeDerivedIdClass || noIdClassMapsId;
		if ( !hasIdClass || wholeDerivedIdClass ) {
			idValue.setPreservePropertyOrder( true );
		}
		if ( hasIdClass && !wholeDerivedIdClass ) {
			idValue.setComponentClassName( idMapping.getIdClassType().getClassName() );
		}
		else {
			idValue.setComponentClassName( typeBinding.getClassName() );
		}
		idValue.setTable( table );
		typeBinding.setIdentifier( idValue );
		typeBinding.setEmbeddedIdentifier( false );

		final Component identifierMapper = separateIdentifierMapper
				? new Component( state.getMetadataBuildingContext(), typeBinding )
				: idValue;
		if ( separateIdentifierMapper ) {
			identifierMapper.setFlattened( true );
			identifierMapper.setComponentClassName( typeBinding.getClassName() );
			identifierMapper.setTable( table );
			if ( noIdClassMapsId ) {
				identifierMapper.setPreservePropertyOrder( true );
			}
		}
		else {
			identifierMapper.setPreservePropertyOrder( true );
		}
		typeBinding.setIdentifierMapper( identifierMapper );
		if ( hasIdClass || noIdClassMapsId ) {
			typeBinding.setDeclaredIdentifierMapper( identifierMapper );
		}
			if ( separateIdentifierMapper ) {
				typeBinding.addProperty( propertyMappingMaterializer.createIdentifierMapperProperty( identifierMapper ) );
			}

		final List<Column> columns = new ArrayList<>( idMapping.getIdAttributes().size() );
		for ( AttributeMetadata idAttribute : idMapping.getIdAttributes() ) {
			final IdentifierAttributeBinding attribute = binding.getAttribute( idAttribute.getName() );
			final MemberDetails member = idAttribute.getMember();
			final MemberDetails idClassMember = attribute.idRepresentationMember();
			if ( idAttribute.getNature() == AttributeNature.BASIC && !isToOneMember( idClassMember ) ) {
				final BasicValue basicValue = createBasicIdValue( table, member );
				final Property rootProperty = createProperty( idAttribute.getName(), basicValue, member );
				if ( !hasIdClass ) {
					typeBinding.addProperty( rootProperty );
				}
				CustomMappingBinder.callAttributeBinders( member, typeBinding, rootProperty, state, context );

				final BasicValue idClassValue = hasIdClass ? createBasicIdValue( table, idClassMember ) : basicValue;
				if ( hasIdClass ) {
					applyGeneratedValue( idClassValue, member );
				}
				if ( separateIdentifierMapper ) {
					final Property mapperProperty = createProperty( idAttribute.getName(), basicValue, member );
					mapperProperty.setInsertable( false );
					mapperProperty.setUpdatable( false );
					identifierMapper.addProperty( mapperProperty );
				}

				final Property idClassProperty = createProperty( idAttribute.getName(), idClassValue, idClassMember );
				idValue.addProperty( idClassProperty );

				final Column column = hasIdClass
						? bindIdColumn( member, idAttribute::getName, table, basicValue, idClassValue )
						: bindIdColumn( member, idAttribute::getName, table, basicValue );
				columns.add( column );
				addSelectableName( binding, idAttribute.getName(), column.getName() );
			}
			else if ( idAttribute.getNature() == AttributeNature.EMBEDDED ) {
				if ( !hasIdClass || wholeDerivedIdClass ) {
					throw new UnsupportedOperationException(
							"Embedded non-aggregated identifier attributes are only implemented for IdClass mappings - "
									+ typeBinding.getEntityName() + "." + idAttribute.getName()
					);
				}
				materializeEmbeddedIdClassAttribute(
						type,
						typeBinding,
						table,
						new IdClassMappingParts( idValue, identifierMapper ),
						idAttribute,
						idClassMember,
						columns,
						binding
				);
			}
			else if ( idAttribute.getNature() == AttributeNature.TO_ONE || isToOneMember( idClassMember ) ) {
				final org.hibernate.mapping.Value idClassValue = hasIdClass && !wholeDerivedIdClass
						? createIdClassAssociationIdentifierValue( idAttribute, idClassMember, table, type, typeBinding )
						: null;
				final MemberDetails associationMember = isToOneMember( idClassMember ) ? idClassMember : member;
				final AtomicReference<org.hibernate.mapping.Value> identifierMapperValue = new AtomicReference<>();
				final ToOne toOne = bindToOneIdentifier(
						idAttribute,
						idClassValue,
						attribute,
						identifierMapperValue,
						table,
						type,
						typeBinding,
						columns,
						associationMember
				);
				final Property rootProperty = createProperty( idAttribute.getName(), toOne, member );
				applyToOneIdentifierPropertyOptions( idAttribute, type, rootProperty, associationMember );
				if ( !hasIdClass ) {
					typeBinding.addProperty( rootProperty );
				}
				CustomMappingBinder.callAttributeBinders( member, typeBinding, rootProperty, state, context );

				if ( hasIdClass ) {
					final ToOne identifierMapperToOne = (ToOne) toOne.copy();
					identifierMapperValue.set( identifierMapperToOne );
					final Property mapperProperty = createProperty( idAttribute.getName(), identifierMapperToOne, member );
					mapperProperty.setInsertable( false );
					mapperProperty.setUpdatable( false );
					applyToOneIdentifierPropertyOptions( idAttribute, type, mapperProperty, associationMember );
					identifierMapper.addProperty( mapperProperty );
				}

				if ( !wholeDerivedIdClass ) {
					final Property idClassProperty = createProperty(
							idAttribute.getName(),
							idClassValue == null ? toOne : idClassValue,
							idClassMember
					);
					idValue.addProperty( idClassProperty );
				}
			}
			else {
				throw new UnsupportedOperationException(
						"IdClass identifier attributes are only implemented for basic and to-one attributes - "
								+ typeBinding.getEntityName() + "." + idAttribute.getName()
				);
			}
		}

		return materializeEntityIdentifierBinding(
				type,
				typeBinding,
				idMapping,
				idValue,
				null,
				table,
				columns,
				binding
		);
	}

	public IdentifierBinding materializeEntityIdentifierBinding(
			EntityTypeMetadata entityType,
			RootClass rootClass,
			KeyMapping keyMapping,
			KeyValue identifierValue,
			@Nullable Property identifierProperty,
			Table table,
			List<Column> columns,
			EntityIdentifierBinding binding) {
		final EntityIdentifierBindingView view = new EntityIdentifierBindingView( binding );
		view.identifierSelectableNames();
		primaryTableKeyMappingMaterializer.finalizePrimaryKey( rootClass, table );
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
				(member, column) -> primaryTableKeyMappingMaterializer.addIdentifierColumn( table, column ),
				false,
				false,
				false
		);
	}

	private IdClassMappingParts createIdClassMappingParts(
			RootClass typeBinding,
			NonAggregatedKeyMapping idMapping,
			Table table) {
		final Component idValue = new Component( state.getMetadataBuildingContext(), typeBinding );
		idValue.setKey( true );
		idValue.setFlattened( true );
		idValue.setComponentClassName( idMapping.getIdClassType().getClassName() );
		idValue.setTable( table );
		typeBinding.setIdentifier( idValue );
		typeBinding.setEmbeddedIdentifier( false );

		final Component identifierMapper = new Component( state.getMetadataBuildingContext(), typeBinding );
		identifierMapper.setFlattened( true );
		identifierMapper.setComponentClassName( typeBinding.getClassName() );
		identifierMapper.setTable( table );
		typeBinding.setIdentifierMapper( identifierMapper );
		typeBinding.setDeclaredIdentifierMapper( identifierMapper );

			typeBinding.addProperty( propertyMappingMaterializer.createIdentifierMapperProperty( identifierMapper ) );

		return new IdClassMappingParts( idValue, identifierMapper );
	}

	private void materializeBasicIdClassAttribute(
			RootClass typeBinding,
			Table table,
			IdClassMappingParts mappingParts,
			AttributeMetadata idAttribute,
			MemberDetails idClassMember,
			List<Column> columns,
			EntityIdentifierBinding binding) {
		final MemberDetails member = idAttribute.getMember();
		final BasicValue basicValue = createBasicIdValue( table, member );
		final Property rootProperty = createProperty( idAttribute.getName(), basicValue, member );
		CustomMappingBinder.callAttributeBinders( member, typeBinding, rootProperty, state, context );

		final BasicValue idClassValue = createBasicIdValue( table, idClassMember );
		applyGeneratedValue( idClassValue, member );

		final Property mapperProperty = createProperty( idAttribute.getName(), basicValue, member );
		mapperProperty.setInsertable( false );
		mapperProperty.setUpdatable( false );
		mappingParts.identifierMapper().addProperty( mapperProperty );

		final Property idClassProperty = createProperty( idAttribute.getName(), idClassValue, idClassMember );
		mappingParts.idValue().addProperty( idClassProperty );

		final Column column = bindIdColumn( member, idAttribute::getName, table, basicValue, idClassValue );
		columns.add( column );
		addSelectableName( binding, idAttribute.getName(), column.getName() );
	}

	private void materializeEmbeddedIdClassAttribute(
			EntityTypeMetadata type,
			RootClass typeBinding,
			Table table,
			IdClassMappingParts mappingParts,
			AttributeMetadata idAttribute,
			MemberDetails idClassMember,
			List<Column> columns,
			EntityIdentifierBinding binding) {
		final MemberDetails member = idAttribute.getMember();
		final Component idClassComponent = createEmbeddedIdClassComponent(
				idAttribute,
				idClassMember,
				table,
				type,
				typeBinding
		);
		final Property idClassProperty = createProperty( idAttribute.getName(), idClassComponent, idClassMember );
		mappingParts.idValue().addProperty( idClassProperty );

		final Component identifierMapperComponent = idClassComponent.copy();
		final Property mapperProperty = createProperty( idAttribute.getName(), identifierMapperComponent, member );
		mapperProperty.setInsertable( false );
		mapperProperty.setUpdatable( false );
		mappingParts.identifierMapper().addProperty( mapperProperty );
		handleGenericComponentProperty(
				mapperProperty,
				member,
				state.getMetadataBuildingContext()
		);

		for ( Column column : idClassComponent.getColumns() ) {
			columns.add( column );
			addSelectableName( binding, idAttribute.getName(), column.getName() );
		}
	}

	private void materializeAssociationIdClassAttribute(
			EntityTypeMetadata type,
			RootClass typeBinding,
			Table table,
			IdClassMappingParts mappingParts,
			AttributeMetadata idAttribute,
			IdentifierAttributeBinding identifierAttribute,
			List<Column> identifierColumns) {
		final MemberDetails member = idAttribute.getMember();
		final MemberDetails idClassMember = identifierAttribute.idRepresentationMember();
		final boolean idClassMemberIsToOne = isToOneMember( idClassMember );
		final org.hibernate.mapping.Value idClassValue = createIdClassAssociationIdentifierValue(
				idAttribute,
				idClassMember,
				table,
				type,
				typeBinding
		);
		final MemberDetails associationMember = idClassMemberIsToOne ? idClassMember : member;
		final AtomicReference<org.hibernate.mapping.Value> identifierMapperValue = new AtomicReference<>();
		final ToOne toOne = bindToOneIdentifier(
				idAttribute,
				idClassValue,
				identifierAttribute,
				identifierMapperValue,
				table,
				type,
				typeBinding,
				identifierColumns,
				associationMember
		);
		final Property rootProperty = createProperty( idAttribute.getName(), toOne, member );
		applyToOneIdentifierPropertyOptions( idAttribute, type, rootProperty, associationMember );
		CustomMappingBinder.callAttributeBinders( member, typeBinding, rootProperty, state, context );

		final ToOne identifierMapperToOne = (ToOne) toOne.copy();
		identifierMapperValue.set( identifierMapperToOne );
		final Property mapperProperty = createProperty( idAttribute.getName(), identifierMapperToOne, member );
		mapperProperty.setInsertable( false );
		mapperProperty.setUpdatable( false );
		applyToOneIdentifierPropertyOptions( idAttribute, type, mapperProperty, associationMember );
		mappingParts.identifierMapper().addProperty( mapperProperty );

		final Property idClassProperty = createProperty(
				idAttribute.getName(),
				idClassValue == null ? toOne : idClassValue,
				idClassMember
		);
		mappingParts.idValue().addProperty( idClassProperty );
	}

	private Component createEmbeddedIdClassComponent(
			AttributeMetadata idAttribute,
			MemberDetails idClassMember,
			Table table,
			EntityTypeMetadata type,
			RootClass typeBinding) {
		final ClassDetails componentType = idClassMember.getType().determineRawClass();
		final Component component = new Component( state.getMetadataBuildingContext(), typeBinding );
		component.setKey( true );
		component.setFlattened( true );
		component.setComponentClassName( componentType.getClassName() );
		component.setTable( table );
		component.setTypeUsingReflection( type.getClassDetails().getClassName(), idAttribute.getName() );
		bindComponentIdentifierProperties(
				type,
				typeBinding,
				ComponentSource.embeddedIdentifier(
						idAttribute.getMember(),
						componentType,
						idAttribute.getMember().getType(),
						type.getAccessType(),
						context
				),
				component,
				table
		);
		return component;
	}

	private org.hibernate.mapping.Value createIdClassAssociationIdentifierValue(
			AttributeMetadata idAttribute,
			MemberDetails idClassMember,
			Table table,
			EntityTypeMetadata type,
			RootClass typeBinding) {
		final ToOneSource source = ToOneSource.create(
				isToOneMember( idClassMember ) ? idClassMember : idAttribute.getMember(),
				type.getClassDetails().getClassName(),
				idAttribute.getName(),
				null,
				context.getBootstrapContext().getModelsContext()
		);
		final ClassDetails targetClassDetails = source.targetClassDetails( context );
		final ClassDetails idClassMemberType = idClassMember.getType().determineRawClass();
		if ( idClassMemberType.getClassName().equals( targetClassDetails.getClassName() ) ) {
			return null;
		}
		if ( idClassMember.hasDirectAnnotationUsage( Embedded.class ) || !isBasicIdClassAssociationType( idClassMemberType ) ) {
			final Component component = new Component( state.getMetadataBuildingContext(), typeBinding );
			component.setKey( true );
			component.setFlattened( true );
			component.setComponentClassName( idClassMemberType.getClassName() );
			component.setTable( table );
			final ComponentSource sourceComponent = ComponentSource.embeddedIdentifier(
					idClassMember,
					idClassMemberType,
					type.getAccessType(),
					context
			);
			for ( MemberDetails componentMember : idClassComponentMembers( sourceComponent, idClassMemberType ) ) {
				final BasicValue basicValue = createIdClassAssociationBasicValue(
						table,
						componentMember,
						typeBinding.getEntityName(),
						idAttribute.getName()
				);
				final Property property = createProperty(
						componentMember.resolveAttributeName(),
						basicValue,
						componentMember
				);
				property.setInsertable( false );
				property.setUpdatable( false );
				component.addProperty( property );
			}
			return component;
		}
		return createIdClassAssociationBasicValue(
				table,
				idClassMember,
				typeBinding.getEntityName(),
				idAttribute.getName()
		);
	}

	private BasicValue createIdClassAssociationBasicValue(
			Table table,
			MemberDetails member,
			String entityName,
			String propertyName) {
		final BasicValue basicValue = createBasicIdValue( table, member );
		basicValue.setCustomIdGeneratorCreator( creationContext ->
				new BeforeExecutionGenerator() {
					@Override
					public Object generate(
							SharedSessionContractImplementor session,
							Object owner,
							Object currentValue,
							EventType eventType) {
						return getForeignId( entityName, propertyName, session, owner );
					}

					@Override
					public EnumSet<EventType> getEventTypes() {
						return EventTypeSets.INSERT_ONLY;
					}

					@Override
					public boolean allowAssignedIdentifiers() {
						return true;
					}
				}
		);
		return basicValue;
	}

	private List<MemberDetails> idClassComponentMembers(ComponentSource sourceComponent, ClassDetails idClassMemberType) {
		final List<ComponentSource.ComponentMember> componentMembers = sourceComponent.members();
		if ( !componentMembers.isEmpty() ) {
			final ArrayList<MemberDetails> result = new ArrayList<>( componentMembers.size() );
			for ( ComponentSource.ComponentMember componentMember : componentMembers ) {
				result.add( componentMember.member() );
			}
			return result;
		}

		final ArrayList<MemberDetails> result = new ArrayList<>();
		for ( MemberDetails field : idClassMemberType.getFields() ) {
			if ( field.resolveAttributeName() != null
					&& field.isPersistable()
					&& !field.hasDirectAnnotationUsage( Transient.class ) ) {
				result.add( field );
			}
		}
		return result;
	}

	private boolean isBasicIdClassAssociationType(ClassDetails idClassMemberType) {
		return idClassMemberType.isPrimitive()
				|| idClassMemberType.isEnum()
				|| idClassMemberType.getClassName().startsWith( "java." );
	}

	private boolean isToOneMember(MemberDetails member) {
		return member.hasDirectAnnotationUsage( jakarta.persistence.ManyToOne.class )
				|| member.hasDirectAnnotationUsage( jakarta.persistence.OneToOne.class );
	}

	private ToOne bindToOneIdentifier(
			AttributeMetadata idAttribute,
			org.hibernate.mapping.Value identifierValue,
			IdentifierAttributeBinding identifierAttribute,
			AtomicReference<org.hibernate.mapping.Value> identifierMapperValue,
			Table table,
			EntityTypeMetadata type,
			RootClass typeBinding,
			List<Column> identifierColumns,
			MemberDetails associationMember) {
		final ToOneSource source = ToOneSource.create(
				associationMember,
				type.getClassDetails().getClassName(),
				idAttribute.getName(),
				null,
				context.getBootstrapContext().getModelsContext()
		);

		final EntityTypeBinder targetTypeBinder = (EntityTypeBinder) state.getTypeBinder(
				source.targetClassDetails( context )
		);
		if ( targetTypeBinder == null ) {
			throw new org.hibernate.MappingException(
					"Could not resolve local type binding for association identifier target entity - "
							+ source.targetClassDetails( context ).getClassName()
			);
		}
		if ( source.isInverseOneToOne() ) {
			return bindInverseOneToOneIdentifier(
					idAttribute,
					source,
					identifierValue,
					identifierAttribute,
					identifierMapperValue,
					table,
					type,
					typeBinding,
					targetTypeBinder,
					identifierColumns
			);
		}

		final JoinTable joinTable = source.joinTable();
		final Table valueTable = joinTable == null
				? table
				: bindAssociationIdentifierTable( type, typeBinding, table, idAttribute.getName(), joinTable );

		final ManyToOne manyToOne = new ManyToOne( state.getMetadataBuildingContext(), valueTable );
		manyToOne.setReferencedEntityName( targetTypeBinder.getTypeBinding().getEntityName() );
		manyToOne.setReferenceToPrimaryKey( true );
		manyToOne.setTypeName( targetTypeBinder.getTypeBinding().getEntityName() );
		manyToOne.setTypeUsingReflection( type.getClassDetails().getClassName(), idAttribute.getName() );
		manyToOne.setLazy( effectiveFetchType( source ) == FetchType.LAZY );
		ToOneMaterializationHelper.applyFetchMode( source, manyToOne );
		if ( source.isLogicalOneToOne() ) {
			manyToOne.markAsLogicalOneToOne();
		}

		state.addAssociationIdentifierBinding( new AssociationIdentifierBinding(
				type,
				typeBinding,
				createProperty( idAttribute.getName(), manyToOne, idAttribute.getMember() ),
				manyToOne,
				identifierValue,
				identifierAttribute,
				identifierMapperValue,
				targetTypeBinder,
				source.valueJoinColumns( joinTable ),
				source.valueForeignKeySource( joinTable ),
				identifierColumns
		) );
		return manyToOne;
	}

	private OneToOne bindInverseOneToOneIdentifier(
			AttributeMetadata idAttribute,
			ToOneSource source,
			org.hibernate.mapping.Value identifierValue,
			IdentifierAttributeBinding identifierAttribute,
			AtomicReference<org.hibernate.mapping.Value> identifierMapperValue,
			Table table,
			EntityTypeMetadata type,
			RootClass typeBinding,
			EntityTypeBinder targetTypeBinder,
			List<Column> identifierColumns) {
		final OneToOne oneToOne = new OneToOne(
				state.getMetadataBuildingContext(),
				table,
				typeBinding
		);
		oneToOne.setPropertyName( idAttribute.getName() );
		oneToOne.setReferencedEntityName( targetTypeBinder.getTypeBinding().getEntityName() );
		oneToOne.setReferenceToPrimaryKey( true );
		oneToOne.setTypeName( targetTypeBinder.getTypeBinding().getEntityName() );
		oneToOne.setTypeUsingReflection( type.getClassDetails().getClassName(), idAttribute.getName() );
		oneToOne.setLazy( effectiveFetchType( source ) == FetchType.LAZY );
		oneToOne.setConstrained( true );
		oneToOne.setForeignKeyType( org.hibernate.type.ForeignKeyDirection.TO_PARENT );
		oneToOne.setMappedByProperty( source.oneToOne().mappedBy() );

		final Property property = createProperty( idAttribute.getName(), oneToOne, idAttribute.getMember() );
		property.setOptional( false );
		state.addAssociationIdentifierBinding( new AssociationIdentifierBinding(
				type,
				typeBinding,
				property,
				oneToOne,
				identifierValue,
				identifierAttribute,
				identifierMapperValue,
				targetTypeBinder,
				List.of(),
				source.valueForeignKeySource( null ),
				identifierColumns
		) );
		return oneToOne;
	}

	private void applyToOneIdentifierPropertyOptions(
			AttributeMetadata idAttribute,
			EntityTypeMetadata type,
			Property property,
			MemberDetails associationMember) {
		final ToOneSource source = ToOneSource.create(
				associationMember,
				type.getClassDetails().getClassName(),
				idAttribute.getName(),
				null,
				context.getBootstrapContext().getModelsContext()
		);
		property.setOptional( false );
		property.setCascade( source.cascades( state ), source.orphanRemoval() );
	}

	private Table bindAssociationIdentifierTable(
			EntityTypeMetadata type,
			RootClass typeBinding,
			Table primaryTable,
			String propertyName,
			JoinTable joinTable) {
		final Table associationTable = modelBinders.getTableBinder()
				.bindAssociationTable(
						type,
						primaryTable,
						propertyName,
						type,
						primaryTable,
						joinTable
				)
				.binding();

		final Join join = new Join();
		join.setTable( associationTable );
		join.setPersistentClass( typeBinding );
		join.setOptional( false );
		join.setInverse( false );
		typeBinding.addJoin( join );

		state.addAssociationTableBinding( new AssociationTableBinding(
				join,
				listJoinColumns( joinTable.joinColumns() ),
				ForeignKeySource.from( joinTable )
		) );
		return associationTable;
	}

	private static List<JoinColumn> listJoinColumns(JoinColumn[] joinColumns) {
		if ( joinColumns.length == 0 ) {
			return List.of();
		}
		final ArrayList<JoinColumn> result = new ArrayList<>( joinColumns.length );
		for ( JoinColumn joinColumn : joinColumns ) {
			result.add( joinColumn );
		}
		return result;
	}

	private FetchType effectiveFetchType(ToOneSource source) {
		return source.effectiveFetchType( options.getDefaultToOneFetchType() );
	}

	private record IdClassMappingParts(Component idValue, Component identifierMapper) {
	}

	private void addAggregatedSelectableNames(
			EntityIdentifierBinding binding,
			Component idValue,
			ComponentSource componentSource) {
		for ( ComponentSource.ComponentMember componentMember : componentSource.members() ) {
			final Property property = idValue.getProperty( componentMember.attributeName() );
			for ( Column column : property.getValue().getColumns() ) {
				addSelectableName( binding, componentMember.attributeName(), column.getName() );
			}
		}
	}

	private void addSelectableName(
			EntityIdentifierBinding binding,
			String attributeName,
			String selectableName) {
		final IdentifierAttributeBinding attribute = binding.getAttribute( attributeName );
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
		return propertyMappingMaterializer.createProperty( name, value, member );
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
		primaryTableKeyMappingMaterializer.addIdentifierColumn( table, column );
		return column;
	}
}
