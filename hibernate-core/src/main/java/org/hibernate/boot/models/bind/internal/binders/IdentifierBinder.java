/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.binders;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.boot.models.bind.internal.sources.BasicValueSource;
import org.hibernate.boot.models.bind.internal.sources.ColumnSource;
import org.hibernate.boot.models.bind.internal.sources.ComponentSource;
import org.hibernate.boot.models.bind.internal.sources.ForeignKeySource;
import org.hibernate.boot.models.bind.internal.sources.ToOneSource;
import org.hibernate.boot.models.bind.spi.BindingContext;
import org.hibernate.boot.models.bind.spi.BindingOptions;
import org.hibernate.boot.models.bind.spi.BindingState;
import org.hibernate.boot.models.AttributeNature;
import org.hibernate.boot.model.internal.GeneratorBinder;
import org.hibernate.boot.model.internal.GeneratorStrategies;
import org.hibernate.boot.models.categorize.spi.AggregatedKeyMapping;
import org.hibernate.boot.models.categorize.spi.AttributeMetadata;
import org.hibernate.boot.models.categorize.spi.BasicKeyMapping;
import org.hibernate.boot.models.categorize.spi.EntityHierarchy;
import org.hibernate.boot.models.categorize.spi.EntityTypeMetadata;
import org.hibernate.boot.models.categorize.spi.KeyMapping;
import org.hibernate.boot.models.categorize.spi.NonAggregatedKeyMapping;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.OneToOne;
import org.hibernate.mapping.PrimaryKey;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.ToOne;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;

import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;

import static jakarta.persistence.GenerationType.AUTO;
import static org.hibernate.boot.models.bind.internal.binders.AttributeBinder.bindPropertyAccessor;

/// Binds the root identifier shape for an entity hierarchy.
///
/// The identifier phase creates the mapping model's primary identifier value,
/// identifier property, primary-key columns, and an [IdentifierBinding] snapshot
/// consumed by later phases.  It supports basic ids, aggregated component ids,
/// and non-aggregated `IdClass` ids.
///
/// Association-valued `IdClass` attributes are only partially bound here.  Their
/// `ManyToOne` value is created so the identifier component has the right shape,
/// but the actual join columns are deferred through [AssociationIdentifierBinding]
/// until target identifier bindings are available.
///
/// @since 9.0
/// @author Steve Ebersole
public class IdentifierBinder {
	private final ModelBinders modelBinders;

	private final BindingState state;
	private final BindingOptions options;
	private final BindingContext context;

	public IdentifierBinder(
			ModelBinders modelBinders,
			BindingState state,
			BindingOptions options,
			BindingContext context) {
		this.modelBinders = modelBinders;
		this.state = state;
		this.options = options;
		this.context = context;
	}

	public static IdentifierBinding bindIdentifier(
			EntityTypeMetadata type,
			RootClass typeBinding,
			ModelBinders modelBinders,
			BindingState state,
			BindingOptions options,
			BindingContext context) {
		final IdentifierBinder identifierBinder = new IdentifierBinder( modelBinders, state, options, context );
		return identifierBinder.bindIdentifier( type, typeBinding );
	}

	private IdentifierBinding bindIdentifier(EntityTypeMetadata type, RootClass typeBinding) {
		final EntityHierarchy hierarchy = type.getHierarchy();
		final KeyMapping idMapping = hierarchy.getIdMapping();
		final Table table = typeBinding.getTable();

		final PrimaryKey primaryKey = new PrimaryKey( table );
		table.setPrimaryKey( primaryKey );

		if ( idMapping instanceof BasicKeyMapping basicKeyMapping ) {
			return bindBasicIdentifier( basicKeyMapping, table, type, typeBinding );
		}
		else if ( idMapping instanceof AggregatedKeyMapping aggregatedKeyMapping ) {
			return bindAggregatedIdentifier( aggregatedKeyMapping, table, type, typeBinding );
		}
		else {
			return bindNonAggregatedIdentifier( (NonAggregatedKeyMapping) idMapping, table, type, typeBinding );
		}
	}

	private IdentifierBinding bindBasicIdentifier(
			BasicKeyMapping basicKeyMapping,
			Table table,
			EntityTypeMetadata typeMetadata,
			RootClass typeBinding) {
		final AttributeMetadata idAttribute = basicKeyMapping.getAttribute();
		final MemberDetails idAttributeMember = idAttribute.getMember();

		final BasicValue idValue = createBasicIdValue( table, idAttributeMember );
		typeBinding.setIdentifier( idValue );

		final Property idProperty = createProperty( idAttribute.getName(), idValue, idAttributeMember );
		typeBinding.setIdentifierProperty( idProperty );
		typeBinding.setDeclaredIdentifierProperty( idProperty );

		final org.hibernate.mapping.Column column = bindIdColumn( idAttributeMember, idAttribute::getName, idValue, table );
		CustomMappingBinder.callAttributeBinders( idAttributeMember, typeBinding, idProperty, state, context );

		return new IdentifierBinding(
				typeMetadata,
				typeBinding,
				basicKeyMapping,
				idValue,
				idProperty,
				table,
				List.of( column )
		);
	}

	private IdentifierBinding bindAggregatedIdentifier(
			AggregatedKeyMapping aggregatedKeyMapping,
			Table table,
			EntityTypeMetadata type,
			RootClass typeBinding) {
		final Component idValue = new Component( state.getMetadataBuildingContext(), typeBinding );
		idValue.setKey( true );
		idValue.setEmbedded( true );
		idValue.setComponentClassName( aggregatedKeyMapping.getKeyType().getClassName() );
		idValue.setTable( table );
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

		final List<org.hibernate.mapping.Column> columns = bindComponentIdentifierProperties(
				type,
				typeBinding,
				aggregatedKeyMapping.getKeyType(),
				idValue,
				table
		);

		return new IdentifierBinding(
				type,
				typeBinding,
				aggregatedKeyMapping,
				idValue,
				idProperty,
				table,
				columns
		);
	}

	private IdentifierBinding bindNonAggregatedIdentifier(
			NonAggregatedKeyMapping idMapping,
			Table table,
			EntityTypeMetadata type,
			RootClass typeBinding) {
		final Component idValue = new Component( state.getMetadataBuildingContext(), typeBinding );
		idValue.setKey( true );
		idValue.setEmbedded( false );
		idValue.setComponentClassName( idMapping.getIdClassType().getClassName() );
		idValue.setTable( table );
		typeBinding.setIdentifier( idValue );
		typeBinding.setEmbeddedIdentifier( false );

		final List<org.hibernate.mapping.Column> columns = new ArrayList<>( idMapping.getIdAttributes().size() );
		for ( AttributeMetadata idAttribute : idMapping.getIdAttributes() ) {
			final MemberDetails member = idAttribute.getMember();
			if ( idAttribute.getNature() == AttributeNature.BASIC ) {
				final BasicValue basicValue = createBasicIdValue( table, member );
				final Property rootProperty = createProperty( idAttribute.getName(), basicValue, member );
				typeBinding.addProperty( rootProperty );
				CustomMappingBinder.callAttributeBinders( member, typeBinding, rootProperty, state, context );

				final Property componentProperty = createProperty( idAttribute.getName(), basicValue, member );
				componentProperty.setInsertable( false );
				componentProperty.setUpdatable( false );
				idValue.addProperty( componentProperty );

				final org.hibernate.mapping.Column column = bindIdColumn( member, idAttribute::getName, basicValue, table );
				columns.add( column );
			}
			else if ( idAttribute.getNature() == AttributeNature.TO_ONE ) {
				final ToOne toOne = bindToOneIdentifier( idAttribute, table, type, typeBinding, columns );
				final Property rootProperty = createProperty( idAttribute.getName(), toOne, member );
				typeBinding.addProperty( rootProperty );
				CustomMappingBinder.callAttributeBinders( member, typeBinding, rootProperty, state, context );

				final Property componentProperty = createProperty( idAttribute.getName(), toOne, member );
				componentProperty.setInsertable( false );
				componentProperty.setUpdatable( false );
				idValue.addProperty( componentProperty );
			}
			else {
				throw new UnsupportedOperationException(
						"IdClass identifier attributes are only implemented for basic and to-one attributes - "
								+ typeBinding.getEntityName() + "." + idAttribute.getName()
				);
			}
		}

		return new IdentifierBinding(
				type,
				typeBinding,
				idMapping,
				idValue,
				null,
				table,
				columns
		);
	}

	private ToOne bindToOneIdentifier(
			AttributeMetadata idAttribute,
			Table table,
			EntityTypeMetadata type,
			RootClass typeBinding,
			List<org.hibernate.mapping.Column> identifierColumns) {
		final ToOneSource source = ToOneSource.create(
				idAttribute.getMember(),
				type.getClassDetails().getClassName(),
				idAttribute.getName(),
				null
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
		if ( source.isLogicalOneToOne() ) {
			manyToOne.markAsLogicalOneToOne();
		}

		state.addAssociationIdentifierBinding( new AssociationIdentifierBinding(
				type,
				typeBinding,
				createProperty( idAttribute.getName(), manyToOne, idAttribute.getMember() ),
				manyToOne,
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
			Table table,
			EntityTypeMetadata type,
			RootClass typeBinding,
			EntityTypeBinder targetTypeBinder,
			List<org.hibernate.mapping.Column> identifierColumns) {
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
				targetTypeBinder,
				List.of(),
				source.valueForeignKeySource( null ),
				identifierColumns
		) );
		return oneToOne;
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

	private List<org.hibernate.mapping.Column> bindComponentIdentifierProperties(
			EntityTypeMetadata type,
			RootClass typeBinding,
			ClassDetails embeddableType,
			Component idValue,
			Table table) {
		return new ComponentBinder( modelBinders, state, options, context ).bindBasicProperties(
				type,
				typeBinding,
				ComponentSource.embeddedIdentifier( embeddableType ),
				idValue,
				table,
				(member, column) -> table.getPrimaryKey().addColumn( column ),
				true,
				false,
				false
		);
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

	private org.hibernate.mapping.Column bindIdColumn(
			MemberDetails member,
			java.util.function.Supplier<String> implicitName,
			BasicValue basicValue,
			Table table) {
		final Column columnAnn = member.getDirectAnnotationUsage( Column.class );
		final org.hibernate.mapping.Column column = ColumnBinder.bindColumn(
				ColumnSource.from( columnAnn ),
				implicitName,
				true,
				false
		);
		basicValue.addColumn( column, true, false );
		table.addColumn( column );
		table.getPrimaryKey().addColumn( column );
		return column;
	}

	private FetchType effectiveFetchType(ToOneSource source) {
		return source.fetchType() == FetchType.LAZY ? FetchType.LAZY : options.getDefaultToOneFetchType();
	}
}
