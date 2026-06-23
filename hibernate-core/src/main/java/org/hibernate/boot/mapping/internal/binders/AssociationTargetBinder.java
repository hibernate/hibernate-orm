/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.binders;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.SecondaryTable;

import org.hibernate.MappingException;
import org.hibernate.boot.mapping.internal.materialize.ResolvedUniqueKey;
import org.hibernate.boot.mapping.internal.materialize.UniqueKeyMappingMaterializer;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.JoinedSubclass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.SyntheticProperty;
import org.hibernate.mapping.Value;

import static org.hibernate.property.access.spi.BuiltInPropertyAccessStrategies.EMBEDDED;

/// Resolves association targets that do not point at the target primary key.
///
/// An owning to-one can reference a unique property instead of the target
/// identifier by naming non-id `referencedColumnName` values.  Member binding can
/// create the association value, but it cannot reliably identify the referenced
/// property until the target entity's basic properties have been bound.  This
/// phase matches the referenced target columns to a target property and registers
/// the unique property reference for later foreign-key creation.
///
/// @since 9.0
/// @author Steve Ebersole
class AssociationTargetBinder {
	private final EntityTypeBinder entityBinder;
	private final UniqueKeyMappingMaterializer uniqueKeyMappingMaterializer = new UniqueKeyMappingMaterializer();

	AssociationTargetBinder(EntityTypeBinder entityBinder) {
		this.entityBinder = entityBinder;
	}

	void bindAssociationTargets() {
		entityBinder.getBindingState().forEachAssociationTargetBinding( (associationTargetBinding) -> {
			if ( associationTargetBinding.ownerBinding() == entityBinder.getTypeBinding() ) {
				bindAssociationTarget( associationTargetBinding );
			}
		} );
	}

	private void bindAssociationTarget(AssociationTargetBinding associationTargetBinding) {
		final Property property = resolveReferencedProperty( associationTargetBinding );
		UniquePropertyReferenceBinder.bindUniquePropertyReference(
				entityBinder.getBindingState(),
				associationTargetBinding.value(),
				property.getName()
		);
	}

	private Property resolveReferencedProperty(AssociationTargetBinding associationTargetBinding) {
		final List<Identifier> referencedColumnNames = referencedColumnNames( associationTargetBinding );
		final PersistentClass targetBinding = associationTargetBinding.targetTypeBinder().getTypeBinding();
		for ( Property property : referenceableProperties( targetBinding ) ) {
			if ( property.getValue() instanceof SimpleValue simpleValue
					&& columnNamesMatch( simpleValue.getColumns(), referencedColumnNames ) ) {
				materializeUniqueKey( simpleValue, associationTargetBinding.role() );
				return property;
			}
		}
		final Property secondaryTableKeyProperty = resolveSecondaryTableKeyProperty(
				associationTargetBinding,
				targetBinding,
				referencedColumnNames
		);
		if ( secondaryTableKeyProperty != null ) {
			return secondaryTableKeyProperty;
		}
		final List<Property> properties = resolveReferencedProperties(
				targetBinding,
				referencedColumnNames,
				associationTargetBinding
		);
		if ( !properties.isEmpty() ) {
			return createSyntheticProperty(
					targetBinding,
					properties,
					syntheticPropertyName( associationTargetBinding ),
					associationTargetBinding.role()
			);
		}
		throw new MappingException(
				"Could not resolve non-primary-key association target columns "
						+ referencedColumnNames + " - " + associationTargetBinding.role()
		);
	}

	private List<Property> referenceableProperties(PersistentClass targetBinding) {
		final ArrayList<Property> properties = new ArrayList<>();
		if ( targetBinding.getIdentifierProperty() != null ) {
			properties.add( targetBinding.getIdentifierProperty() );
		}
		properties.addAll( targetBinding.getReferenceableProperties() );
		return properties;
	}

	private Property resolveSecondaryTableKeyProperty(
			AssociationTargetBinding associationTargetBinding,
			PersistentClass targetBinding,
			List<Identifier> referencedColumnNames) {
		final Property identifierProperty = targetBinding.getIdentifierProperty();
		if ( identifierProperty == null ) {
			return null;
		}
		if ( sourceSecondaryTableKeyMatches( associationTargetBinding, referencedColumnNames ) ) {
			return identifierProperty;
		}
		if ( targetBinding instanceof JoinedSubclass joinedSubclass
				&& joinedSubclass.getKey() != null
				&& columnNamesMatch( joinedSubclass.getKey().getColumns(), referencedColumnNames ) ) {
			return identifierProperty;
		}
		for ( Join join : targetBinding.getJoins() ) {
			if ( join.getKey() != null && columnNamesMatch( join.getKey().getColumns(), referencedColumnNames ) ) {
				return identifierProperty;
			}
		}
		return null;
	}

	private boolean sourceSecondaryTableKeyMatches(
			AssociationTargetBinding associationTargetBinding,
			List<Identifier> referencedColumnNames) {
		final SecondaryTable[] secondaryTables = associationTargetBinding.targetTypeBinder()
				.getManagedType()
				.getClassDetails()
				.getRepeatedAnnotationUsages(
						SecondaryTable.class,
						entityBinder.getBindingContext().getBootstrapContext().getModelsContext()
				);
		for ( SecondaryTable secondaryTable : secondaryTables ) {
			if ( primaryKeyJoinColumnsMatch( secondaryTable.pkJoinColumns(), referencedColumnNames ) ) {
				return true;
			}
		}
		return false;
	}

	private boolean primaryKeyJoinColumnsMatch(
			PrimaryKeyJoinColumn[] primaryKeyJoinColumns,
			List<Identifier> referencedColumnNames) {
		if ( primaryKeyJoinColumns.length != referencedColumnNames.size() ) {
			return false;
		}
		final Database database = entityBinder.getBindingState().getDatabase();
		for ( int i = 0; i < primaryKeyJoinColumns.length; i++ ) {
			if ( !database.toIdentifier( primaryKeyJoinColumns[i].name() ).matches( referencedColumnNames.get( i ) ) ) {
				return false;
			}
		}
		return true;
	}

	private List<Property> resolveReferencedProperties(
			PersistentClass targetBinding,
			List<Identifier> referencedColumnNames,
			AssociationTargetBinding associationTargetBinding) {
		final LinkedHashSet<Property> result = new LinkedHashSet<>();
		for ( Identifier referencedColumnName : referencedColumnNames ) {
			final Property property = findPropertyContainingColumn( targetBinding, referencedColumnName );
			if ( property == null ) {
				return List.of();
			}
			result.add( property );
		}
		final List<Property> properties = new ArrayList<>( result );
		final List<Property> coalescedProperties = coalesceCompleteComponents(
				targetBinding,
				properties,
				referencedColumnNames
		);
		if ( !columnNamesMatch( collectColumns( coalescedProperties ), referencedColumnNames ) ) {
			throw new MappingException(
					"Referenced columns span properties that do not match the requested order "
							+ referencedColumnNames + " - " + associationTargetBinding.role()
			);
		}
		return coalescedProperties;
	}

	private List<Property> coalesceCompleteComponents(
			PersistentClass targetBinding,
			List<Property> properties,
			List<Identifier> referencedColumnNames) {
		final ArrayList<Property> result = new ArrayList<>( properties.size() );
		for ( Property property : properties ) {
			final Property componentProperty = findCompleteComponentProperty(
					targetBinding,
					property,
					referencedColumnNames
			);
			if ( componentProperty != null ) {
				if ( !result.contains( componentProperty ) ) {
					result.add( componentProperty );
				}
			}
			else {
				result.add( property );
			}
		}
		return result;
	}

	private Property findCompleteComponentProperty(
			PersistentClass targetBinding,
			Property property,
			List<Identifier> referencedColumnNames) {
		for ( Property referenceableProperty : referenceableProperties( targetBinding ) ) {
			final Property match = findCompleteComponentProperty(
					referenceableProperty,
					property,
					referencedColumnNames
			);
			if ( match != null ) {
				return match;
			}
		}
		return null;
	}

	private Property findCompleteComponentProperty(
			Property componentProperty,
			Property property,
			List<Identifier> referencedColumnNames) {
		if ( !( componentProperty.getValue() instanceof Component component ) ) {
			return null;
		}
		if ( component.getProperties().contains( property )
				&& referencedColumnNamesContainAll( component.getColumns(), referencedColumnNames ) ) {
			return componentProperty;
		}
		for ( Property subProperty : component.getProperties() ) {
			final Property match = findCompleteComponentProperty( subProperty, property, referencedColumnNames );
			if ( match != null ) {
				return match;
			}
		}
		return null;
	}

	private boolean referencedColumnNamesContainAll(List<Column> columns, List<Identifier> referencedColumnNames) {
		for ( Column column : columns ) {
			if ( !referencedColumnNamesContains( referencedColumnNames, column ) ) {
				return false;
			}
		}
		return true;
	}

	private boolean referencedColumnNamesContains(List<Identifier> referencedColumnNames, Column column) {
		final Identifier columnName = column.getNameIdentifier( entityBinder.getBindingState().getDatabase() );
		for ( Identifier referencedColumnName : referencedColumnNames ) {
			if ( columnName.matches( referencedColumnName ) ) {
				return true;
			}
		}
		return false;
	}

	private Property findPropertyContainingColumn(PersistentClass targetBinding, Identifier referencedColumnName) {
		for ( Property property : referenceableProperties( targetBinding ) ) {
			final Property match = findPropertyContainingColumn( property, referencedColumnName );
			if ( match != null ) {
				return match;
			}
		}
		return null;
	}

	private Property findPropertyContainingColumn(Property property, Identifier referencedColumnName) {
		if ( property.getValue() instanceof Component component ) {
			for ( Property subProperty : component.getProperties() ) {
				final Property match = findPropertyContainingColumn( subProperty, referencedColumnName );
				if ( match != null ) {
					return match;
				}
			}
		}
		else if ( containsColumn( property.getValue(), referencedColumnName ) ) {
			return property;
		}
		return null;
	}

	private boolean containsColumn(Value value, Identifier referencedColumnName) {
		final Database database = entityBinder.getBindingState().getDatabase();
		for ( Column column : value.getColumns() ) {
			if ( column.getNameIdentifier( database ).matches( referencedColumnName ) ) {
				return true;
			}
		}
		return false;
	}

	private Property createSyntheticProperty(
			PersistentClass targetBinding,
			List<Property> properties,
			String syntheticPropertyName,
			String sourceRole) {
		final Component component = new Component(
				metadataBuildingContext(),
				targetBinding
		);
		component.setComponentClassName( targetBinding.getClassName() );
		component.setEmbedded( true );
		for ( Property property : properties ) {
			component.addProperty( cloneProperty( targetBinding, property ) );
		}
		component.sortProperties();

		final SyntheticProperty syntheticProperty = new SyntheticProperty();
		syntheticProperty.setName( syntheticPropertyName );
		syntheticProperty.setPersistentClass( targetBinding );
		syntheticProperty.setUpdatable( false );
		syntheticProperty.setInsertable( false );
		syntheticProperty.setValue( component );
		syntheticProperty.setPropertyAccessorName( EMBEDDED.getExternalName() );
		targetBinding.addProperty( syntheticProperty );
		materializeUniqueKey( component, sourceRole );
		return syntheticProperty;
	}

	private Property cloneProperty(PersistentClass ownerBinding, Property property) {
		if ( property.isComposite() ) {
			final Component component = (Component) property.getValue();
			final Component copy = new Component( metadataBuildingContext(), component );
			copy.setComponentClassName( component.getComponentClassName() );
			copy.setEmbedded( component.isEmbedded() );
			for ( Property subProperty : component.getProperties() ) {
				copy.addProperty( cloneProperty( ownerBinding, subProperty ) );
			}
			copy.sortProperties();
			final SyntheticProperty result = new SyntheticProperty();
			result.setName( property.getName() );
			result.setPersistentClass( ownerBinding );
			result.setUpdatable( false );
			result.setInsertable( false );
			result.setValue( copy );
			result.setPropertyAccessorName( property.getPropertyAccessorName() );
			return result;
		}
		final Property clone = property.copy();
		clone.setInsertable( false );
		clone.setUpdatable( false );
		clone.setNaturalIdentifier( false );
		clone.setPersistentClass( ownerBinding );
		return clone;
	}

	private String syntheticPropertyName(AssociationTargetBinding associationTargetBinding) {
		return ( "_" + associationTargetBinding.ownerBinding().getEntityName() + "_" + associationTargetBinding.role() )
				.replace( '.', '_' );
	}

	private List<Column> collectColumns(List<Property> properties) {
		final ArrayList<Column> columns = new ArrayList<>();
		for ( Property property : properties ) {
			columns.addAll( property.getValue().getColumns() );
		}
		return columns;
	}

	private void materializeUniqueKey(SimpleValue value, String sourceRole) {
		uniqueKeyMappingMaterializer.materializeUniqueKey(
				ResolvedUniqueKey.from( value, metadataBuildingContext(), sourceRole )
		);
	}

	private MetadataBuildingContext metadataBuildingContext() {
		return entityBinder.getBindingState().getMetadataBuildingContext();
	}

	private List<Identifier> referencedColumnNames(AssociationTargetBinding associationTargetBinding) {
		final List<Identifier> result = new ArrayList<>( associationTargetBinding.referencedColumnNames().size() );
		final Database database = entityBinder.getBindingState().getDatabase();
		for ( String referencedColumnName : associationTargetBinding.referencedColumnNames() ) {
			if ( StringHelper.isEmpty( referencedColumnName ) ) {
				throw new MappingException(
						"Non-primary-key association join columns must name referenced columns - "
								+ associationTargetBinding.role()
				);
			}
			result.add( database.toIdentifier( referencedColumnName ) );
		}
		return result;
	}

	private boolean columnNamesMatch(List<Column> columns, List<Identifier> referencedColumnNames) {
		if ( columns.size() != referencedColumnNames.size() ) {
			return false;
		}
		final Database database = entityBinder.getBindingState().getDatabase();
		for ( int i = 0; i < columns.size(); i++ ) {
			if ( !columns.get( i ).getNameIdentifier( database ).matches( referencedColumnNames.get( i ) ) ) {
				return false;
			}
		}
		return true;
	}
}
