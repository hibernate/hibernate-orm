/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.serial.internal;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.mapping.AppliedMappingPart;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.IdentifierCollection;
import org.hibernate.mapping.IndexedCollection;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.MappedSuperclass;
import org.hibernate.mapping.MappingRole;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.SyntheticProperty;
import org.hibernate.mapping.Value;

import static org.hibernate.mapping.MappingRole.PartKind.COLLECTION_IDENTIFIER;
import static org.hibernate.mapping.MappingRole.PartKind.DISCRIMINATOR;
import static org.hibernate.mapping.MappingRole.PartKind.ELEMENT;
import static org.hibernate.mapping.MappingRole.PartKind.IDENTIFIER;
import static org.hibernate.mapping.MappingRole.PartKind.IDENTIFIER_MAPPER;
import static org.hibernate.mapping.MappingRole.PartKind.INDEX;
import static org.hibernate.mapping.MappingRole.PartKind.JOIN;
import static org.hibernate.mapping.MappingRole.PartKind.KEY;
import static org.hibernate.mapping.MappingRole.PartKind.VERSION;

/// Index of the durable parts of a finalized mapping graph by intrinsic mapping role.
///
/// Traversal-derived roles are retained only as an explicit compatibility
/// fallback for roleless declaration-side projections.  Applied properties and
/// values are validated against their intrinsic [MappingRole]; archive identity
/// is never invented for an already-applied mapping part.
///
/// An intrinsic role identifies one applied value state, although multiple
/// property aliases may expose that value and same-application component
/// wrappers may share its child properties.  A declaration fallback role may
/// identify multiple equivalent compatibility projections; restoration applies
/// its single declarative recipe to every such projection.
///
/// @since 9.0
/// @author Steve Ebersole
public final class MappingModelGraphIndex {
	private final Map<MappingRole, List<BasicValue>> basicValuesByRole = new LinkedHashMap<>();
	private final Map<BasicValue, MappingRole> basicValueRoles = new IdentityHashMap<>();
	private final Map<MappingRole, Property> propertiesByRole = new LinkedHashMap<>();
	private final Map<MappingRole, Component> componentsByRole = new LinkedHashMap<>();
	private final Map<Component, MappingRole> componentRoles = new IdentityHashMap<>();
	private final Map<Value, MappingRole> visited = new IdentityHashMap<>();
	private final Set<MappedSuperclass> visitedMappedSuperclasses =
			java.util.Collections.newSetFromMap( new IdentityHashMap<>() );

	private MappingModelGraphIndex(MetadataImplementor metadata) {
		for ( MappedSuperclass mappedSuperclass : metadata.getMappedSuperclassMappingsCopy() ) {
			indexMappedSuperclass( mappedSuperclass );
		}
		for ( PersistentClass entity : metadata.getEntityBindings() ) {
			indexMappedSuperclass( entity.getSuperMappedSuperclass() );
			final MappingRole entityRole = MappingRole.entity( entity.getEntityName() );
			if ( entity.getSuperclass() == null ) {
				indexProperty( entity.getIdentifierProperty(), entityRole.append( IDENTIFIER ), true );
				if ( entity.getIdentifierProperty() == null ) {
					indexValue( entity.getIdentifier(), entityRole.append( IDENTIFIER ), true );
				}
				indexValue( entity.getIdentifierMapper(), entityRole.append( IDENTIFIER_MAPPER ), true );
				indexValue( entity.getDiscriminator(), entityRole.append( DISCRIMINATOR ), true );
				indexProperty( entity.getVersion(), entityRole.append( VERSION ), true );
			}
			// Include declaration-side generic properties. They are the sole
			// compatibility case in this traversal where a missing role is valid.
			indexProperties( entity.getUnjoinedProperties(), entityRole, false );
			for ( Join join : entity.getJoins() ) {
				final MappingRole joinRole = entityRole.append( JOIN, join.getTable().getName() );
				indexValue( join.getKey(), joinRole.append( KEY ), true );
				indexProperties( join.getProperties(), entityRole, true );
			}
		}
		for ( org.hibernate.mapping.Collection collection : metadata.getCollectionBindings() ) {
			final MappingRole expectedRole = MappingRole.collection( collection.getRole() );
			final MappingRole collectionRole = intrinsicRole( collection );
			if ( collectionRole == null ) {
				throw missingRole( "collection", expectedRole );
			}
			if ( !collectionRole.equals( expectedRole ) ) {
				throw inconsistentRole( "collection", collectionRole, expectedRole );
			}
			indexValue( collection.getKey(), collectionRole.append( KEY ), true );
			indexValue( collection.getElement(), collectionRole.append( ELEMENT ), true );
			if ( collection instanceof IndexedCollection indexed ) {
				indexValue( indexed.getIndex(), collectionRole.append( INDEX ), true );
			}
			if ( collection instanceof IdentifierCollection identified ) {
				indexValue( identified.getIdentifier(), collectionRole.append( COLLECTION_IDENTIFIER ), true );
			}
		}
	}

	private void indexMappedSuperclass(MappedSuperclass mappedSuperclass) {
		if ( mappedSuperclass == null || !visitedMappedSuperclasses.add( mappedSuperclass ) ) {
			return;
		}
		indexMappedSuperclass( mappedSuperclass.getSuperMappedSuperclass() );
		final MappingRole role = MappingRole.mappedSuperclass( mappedSuperclass.getClassName() );
		final Property identifier = mappedSuperclass.getDeclaredIdentifierProperty();
		indexValue( identifier == null ? null : identifier.getValue(), role.append( IDENTIFIER ), false );
		final Property version = mappedSuperclass.getDeclaredVersion();
		indexValue( version == null ? null : version.getValue(), role.append( VERSION ), false );
		indexProperties( mappedSuperclass.getDeclaredProperties(), role, false );
	}

	public static MappingModelGraphIndex from(MetadataImplementor metadata) {
		return new MappingModelGraphIndex( metadata );
	}

	public Map<MappingRole, List<BasicValue>> basicValuesByRole() {
		final Map<MappingRole, List<BasicValue>> result = new LinkedHashMap<>();
		basicValuesByRole.forEach( (role, values) -> result.put( role, List.copyOf( values ) ) );
		return Map.copyOf( result );
	}

	public MappingRole role(BasicValue value) {
		return basicValueRoles.get( value );
	}

	public Property property(MappingRole role) {
		return propertiesByRole.get( role );
	}

	public MappingRole role(Component component) {
		return componentRoles.get( component );
	}

	public Component component(MappingRole role) {
		return componentsByRole.get( role );
	}

	private void indexProperties(
			Iterable<Property> properties,
			MappingRole containerRole,
			boolean requireIntrinsicRole) {
		indexProperties( properties, containerRole, requireIntrinsicRole, false );
	}

	private void indexProperties(
			Iterable<Property> properties,
			MappingRole containerRole,
			boolean requireIntrinsicRole,
			boolean allowApplicationAliases) {
		for ( Property property : properties ) {
			indexProperty(
					property,
					containerRole.appendAttribute( property.getName() ),
					requireIntrinsicRole,
					allowApplicationAliases
			);
		}
	}

	private void indexProperty(
			Property property,
			MappingRole expectedRole,
			boolean requireIntrinsicRole) {
		indexProperty( property, expectedRole, requireIntrinsicRole, false );
	}

	private void indexProperty(
			Property property,
			MappingRole expectedRole,
			boolean requireIntrinsicRole,
			boolean allowApplicationAliases) {
		if ( property == null ) {
			return;
		}
		final MappingRole propertyRole = property.getMappingRole();
		final MappingRole valueRole = intrinsicRole( property.getValue() );
		if ( propertyRole == null ) {
			if ( valueRole != null ) {
				if ( allowApplicationAliases || !requireIntrinsicRole || property.isBackRef() ) {
					indexValue( property.getValue(), valueRole, true );
					return;
				}
				else {
					throw new IllegalStateException(
							"Roleless property '" + property.getName()
									+ "' contains applied value role '" + valueRole + "'"
					);
				}
			}
			if ( requireIntrinsicRole ) {
				throw missingRole( "property '" + property.getName() + "'", expectedRole );
			}
			indexValue( property.getValue(), expectedRole, false );
			return;
		}
		validatePropertyRole( property, propertyRole );
		if ( !matchesExpectedPropertyRole( propertyRole, expectedRole )
				&& !( ( allowApplicationAliases || !requireIntrinsicRole )
						&& propertyRole.equals( valueRole ) ) ) {
			throw inconsistentRole( "property '" + property.getName() + "'", propertyRole, expectedRole );
		}
		indexPropertyRole( propertyRole, property );
		if ( valueRole == null ) {
			throw missingRole( "value of property '" + property.getName() + "'", propertyRole );
		}
		if ( !( property.getValue() instanceof org.hibernate.mapping.Collection )
				&& !propertyRole.equals( valueRole )
				&& !areIdentifierAliases( propertyRole, valueRole ) ) {
			throw inconsistentRole( "value of property '" + property.getName() + "'", valueRole, propertyRole );
		}
		indexValue(
				property.getValue(),
				valueRole,
				true,
				property instanceof SyntheticProperty
		);
	}

	private static boolean matchesExpectedPropertyRole(MappingRole propertyRole, MappingRole expectedRole) {
		if ( expectedRole.getLocalPart().kind() != MappingRole.PartKind.ATTRIBUTE ) {
			return propertyRole.equals( expectedRole );
		}
		return propertyRole.getLocalPart().kind() == MappingRole.PartKind.ATTRIBUTE
				? propertyRole.equals( expectedRole )
				: propertyRole.getParent().equals( expectedRole.getParent() );
	}

	private static boolean areIdentifierAliases(MappingRole first, MappingRole second) {
		if ( first.getRootKind() != second.getRootKind()
				|| !first.getRootName().equals( second.getRootName() )
				|| first.getParts().size() != second.getParts().size() ) {
			return false;
		}
		boolean identifierAlias = false;
		for ( int i = 0; i < first.getParts().size(); i++ ) {
			final MappingRole.Part firstPart = first.getParts().get( i );
			final MappingRole.Part secondPart = second.getParts().get( i );
			if ( firstPart.equals( secondPart ) ) {
				continue;
			}
			if ( identifierAlias
					|| !( firstPart.kind() == IDENTIFIER && secondPart.kind() == IDENTIFIER_MAPPER
							|| firstPart.kind() == IDENTIFIER_MAPPER && secondPart.kind() == IDENTIFIER ) ) {
				return false;
			}
			identifierAlias = true;
		}
		return identifierAlias;
	}

	private void indexPropertyRole(MappingRole role, Property property) {
		final Property previous = propertiesByRole.putIfAbsent( role, property );
		if ( previous != null && previous.getValue() != property.getValue() ) {
			throw new IllegalStateException( "Duplicate property mapping role '" + role + "'" );
		}
	}

	private void indexValue(Value value, MappingRole fallbackRole, boolean requireIntrinsicRole) {
		indexValue( value, fallbackRole, requireIntrinsicRole, false );
	}

	private void indexValue(
			Value value,
			MappingRole fallbackRole,
			boolean requireIntrinsicRole,
			boolean allowApplicationAliases) {
		if ( value == null ) {
			return;
		}
		final MappingRole intrinsicRole = intrinsicRole( value );
		final MappingRole effectiveRole;
		if ( intrinsicRole == null ) {
			if ( requireIntrinsicRole ) {
				throw missingRole( value.getClass().getSimpleName(), fallbackRole );
			}
			effectiveRole = fallbackRole;
		}
		else {
			effectiveRole = intrinsicRole;
		}
		final MappingRole previousRole = visited.putIfAbsent( value, effectiveRole );
		if ( previousRole != null ) {
			if ( intrinsicRole != null && !intrinsicRole.equals( previousRole ) ) {
				throw new IllegalStateException(
						"Mapping value is reachable through conflicting intrinsic roles '"
								+ previousRole + "' and '" + intrinsicRole + "'"
				);
			}
			return;
		}
		if ( value instanceof BasicValue basicValue ) {
			indexBasicValue( effectiveRole, basicValue, intrinsicRole != null );
			basicValueRoles.put( basicValue, effectiveRole );
		}
		if ( value instanceof Component component ) {
			if ( intrinsicRole != null ) {
				indexComponent( effectiveRole, component );
			}
			componentRoles.put( component, effectiveRole );
			indexProperties(
					component.getProperties(),
					effectiveRole,
					intrinsicRole != null,
					allowApplicationAliases
			);
			indexValue( component.getDiscriminator(), effectiveRole.append( DISCRIMINATOR ), intrinsicRole != null );
		}
	}

	private void indexComponent(MappingRole role, Component component) {
		final Component previous = componentsByRole.putIfAbsent( role, component );
		if ( previous != null && previous != component && !areSameApplicationAliases( previous, component ) ) {
			throw new IllegalStateException( "Duplicate component mapping role '" + role + "'" );
		}
	}

	private static boolean areSameApplicationAliases(Component first, Component second) {
		if ( first.getDiscriminator() != second.getDiscriminator()
				|| first.getProperties().size() != second.getProperties().size() ) {
			return false;
		}
		for ( int i = 0; i < first.getProperties().size(); i++ ) {
			if ( first.getProperties().get( i ) != second.getProperties().get( i ) ) {
				return false;
			}
		}
		return true;
	}

	private void indexBasicValue(MappingRole role, BasicValue value, boolean intrinsicRole) {
		final List<BasicValue> values = basicValuesByRole.computeIfAbsent( role, ignored -> new ArrayList<>() );
		for ( BasicValue indexed : values ) {
			if ( indexed == value ) {
				return;
			}
		}
		if ( intrinsicRole && !values.isEmpty() ) {
			throw new IllegalStateException( "Duplicate basic value mapping role '" + role + "'" );
		}
		values.add( value );
	}

	private static MappingRole intrinsicRole(Object mappingPart) {
		return mappingPart instanceof AppliedMappingPart appliedMappingPart
				? appliedMappingPart.getMappingRole()
				: null;
	}

	private static void validatePropertyRole(Property property, MappingRole role) {
		final MappingRole.Part localPart = role.getLocalPart();
		if ( localPart == null
				|| localPart.kind() == MappingRole.PartKind.ATTRIBUTE
						&& !property.getName().equals( localPart.name() )
				|| localPart.kind() != MappingRole.PartKind.ATTRIBUTE
						&& localPart.kind() != IDENTIFIER
						&& localPart.kind() != IDENTIFIER_MAPPER
						&& localPart.kind() != VERSION ) {
			throw new IllegalStateException(
					"Property '" + property.getName() + "' has structurally inconsistent mapping role '" + role + "'"
			);
		}
	}

	private static IllegalStateException missingRole(String kind, MappingRole expectedRole) {
		return new IllegalStateException(
				"Durable " + kind + " is missing intrinsic mapping role; expected '" + expectedRole + "'"
		);
	}

	private static IllegalStateException inconsistentRole(
			String kind,
			MappingRole actualRole,
			MappingRole expectedRole) {
		return new IllegalStateException(
				"Structurally inconsistent " + kind + " mapping role '" + actualRole
						+ "'; expected '" + expectedRole + "'"
		);
	}

	static <K, T> void putUnique(Map<K, T> index, K role, T value, String kind) {
		final T previous = index.putIfAbsent( role, value );
		if ( previous != null && previous != value ) {
			throw new IllegalStateException( "Duplicate " + kind + " mapping role '" + role + "'" );
		}
	}
}
