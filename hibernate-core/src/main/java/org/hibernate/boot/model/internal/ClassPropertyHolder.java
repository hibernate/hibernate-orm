/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.AssertionFailure;
import org.hibernate.PropertyNotFoundException;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.SecondPass;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.IndexedCollection;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.ToOne;
import org.hibernate.mapping.Value;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;

import jakarta.persistence.Convert;
import jakarta.persistence.JoinTable;

import static org.hibernate.internal.util.StringHelper.isEmpty;
import static org.hibernate.internal.util.collections.CollectionHelper.mapOfSize;

/**
 * @author Emmanuel Bernard

 */
public class ClassPropertyHolder extends AbstractPropertyHolder {
	private final PersistentClass persistentClass;
	private Map<String, Join> joins;
	private transient Map<String, Join> joinsPerRealTableName;
	private EntityBinder entityBinder;
	private final Map<ClassDetails, InheritanceState> inheritanceStatePerClass;

	private final Map<String,AttributeConversionInfo> attributeConversionInfoMap;

	public ClassPropertyHolder(
			PersistentClass persistentClass,
			ClassDetails entityXClass,
			Map<String, Join> joins,
			MetadataBuildingContext context,
			Map<ClassDetails, InheritanceState> inheritanceStatePerClass) {
		super( persistentClass.getEntityName(), null, entityXClass, context );
		this.persistentClass = persistentClass;
		this.joins = joins;
		this.inheritanceStatePerClass = inheritanceStatePerClass;

		this.attributeConversionInfoMap = buildAttributeConversionInfoMap( entityXClass );
	}

	public ClassPropertyHolder(
			PersistentClass persistentClass,
			ClassDetails entityXClass,
			EntityBinder entityBinder,
			MetadataBuildingContext context,
			Map<ClassDetails, InheritanceState> inheritanceStatePerClass) {
		this( persistentClass, entityXClass, entityBinder.getSecondaryTables(), context, inheritanceStatePerClass );
		this.entityBinder = entityBinder;
	}

	@Override
	protected String normalizeCompositePath(String attributeName) {
		return attributeName;
	}

	@Override
	protected String normalizeCompositePathForLogging(String attributeName) {
		return getEntityName() + '.' + attributeName;
	}

	protected Map<String, AttributeConversionInfo> buildAttributeConversionInfoMap(ClassDetails entityClassDetails) {
		final HashMap<String, AttributeConversionInfo> map = new HashMap<>();
		collectAttributeConversionInfo( map, entityClassDetails );
		return map;
	}

	private void collectAttributeConversionInfo(Map<String, AttributeConversionInfo> infoMap, ClassDetails entityClassDetails) {
		if ( entityClassDetails == null ) {
			// typically indicates we have reached the end of the inheritance hierarchy
			return;
		}

		// collect superclass info first
		collectAttributeConversionInfo( infoMap, entityClassDetails.getSuperClass() );

		final var modelContext = getSourceModelContext();
		final boolean canContainConvert =
				entityClassDetails.hasAnnotationUsage( jakarta.persistence.Entity.class, modelContext )
				|| entityClassDetails.hasAnnotationUsage( jakarta.persistence.MappedSuperclass.class, modelContext )
				|| entityClassDetails.hasAnnotationUsage( jakarta.persistence.Embeddable.class, modelContext );
		if ( ! canContainConvert ) {
			return;
		}

		entityClassDetails.forEachAnnotationUsage( Convert.class, modelContext, (usage) -> {
			final var info = new AttributeConversionInfo( usage, entityClassDetails );
			if ( isEmpty( info.getAttributeName() ) ) {
				throw new IllegalStateException( "@Convert placed on @Entity/@MappedSuperclass must define attributeName" );
			}
			infoMap.put( info.getAttributeName(), info );
		} );
	}

	@Override
	public void startingProperty(MemberDetails property) {
		if ( property != null ) {
			final String propertyName = property.resolveAttributeName();
			if ( !attributeConversionInfoMap.containsKey( propertyName ) ) {
				property.forEachAnnotationUsage( Convert.class, getSourceModelContext(), (usage) -> {
					final var info = new AttributeConversionInfo( usage, property );
					final String infoAttributeName = info.getAttributeName();
					final String path =
							isEmpty( infoAttributeName )
									? propertyName
									: propertyName + '.' + infoAttributeName;
					attributeConversionInfoMap.put( path, info );
				} );
			}
		}
	}

	@Override
	protected AttributeConversionInfo locateAttributeConversionInfo(MemberDetails attributeMember) {
		return locateAttributeConversionInfo( attributeMember.resolveAttributeName() );
	}

	@Override
	protected AttributeConversionInfo locateAttributeConversionInfo(String path) {
		return attributeConversionInfoMap.get( path );
	}

	@Override
	public String getEntityName() {
		return persistentClass.getEntityName();
	}

	@Override
	public void addProperty(Property prop, MemberDetails memberDetails, @Nullable AnnotatedColumns columns, ClassDetails declaringClass) {
		//AnnotatedColumn.checkPropertyConsistency( ); //already called earlier
		if ( columns != null ) {
			if ( columns.isSecondary() ) {
				addPropertyToJoin( prop, memberDetails, declaringClass, columns.getJoin() );
			}
			else {
				addProperty( prop, memberDetails, declaringClass );
			}
		}
		else {
			addProperty( prop, memberDetails,  declaringClass );
		}
	}

	@Override
	public void addProperty(Property prop, MemberDetails memberDetails, ClassDetails declaringClass) {
		if ( prop.getValue() instanceof Component ) {
			//TODO handle quote and non quote table comparison
			final String tableName = prop.getValue().getTable().getName();
			if ( getJoinsPerRealTableName().containsKey( tableName ) ) {
				final var join = getJoinsPerRealTableName().get( tableName );
				addPropertyToJoin( prop, memberDetails, declaringClass, join );
			}
			else {
				addPropertyToPersistentClass( prop, memberDetails, declaringClass );
			}
		}
		else {
			addPropertyToPersistentClass( prop, memberDetails, declaringClass );
		}
	}

	@Override
	public Join addJoin(JoinTable joinTableAnn, boolean noDelayInPkColumnCreation) {
		final var join = entityBinder.addJoinTable( joinTableAnn, this, noDelayInPkColumnCreation );
		joins = entityBinder.getSecondaryTables();
		return join;
	}

	@Override
	public Join addJoin(JoinTable joinTable, Table table, boolean noDelayInPkColumnCreation) {
		final var join = entityBinder.createJoin(
				this,
				noDelayInPkColumnCreation,
				false,
				joinTable.joinColumns(),
				table.getQualifiedTableName(),
				table
		);
		joins = entityBinder.getSecondaryTables();
		return join;
	}

	/**
	 * Embeddable classes can be defined using generics. For this reason, we must check
	 * every property value and specially handle generic components by setting the property
	 * as generic, to later be able to resolve its concrete type, and creating a new component
	 * with correctly typed sub-properties for the metamodel.
	 */
	public static void handleGenericComponentProperty(Property property, MemberDetails memberDetails, MetadataBuildingContext context) {
		if ( property.getValue() instanceof Component component ) {
			final var collector = context.getMetadataCollector();
			if ( component.isGeneric() && component.getPropertySpan() > 0
					&& collector.getGenericComponent( component.getComponentClass() ) == null ) {
				// If we didn't already, register the generic component to use it later
				// as the metamodel type for generic embeddable attributes
				final Component copy = component.copy();
				copy.setGeneric( false );
				copy.getProperties().clear();
				final var declaredMembers = getDeclaredAttributeMembers(
						memberDetails.getType().determineRawClass(),
						component.getProperty( 0 ).getPropertyAccessorName()
				);
				for ( var prop : component.getProperties() ) {
					final var declaredMember = declaredMembers.get( prop.getName() );
					if ( declaredMember == null ) {
						// This can happen for generic custom composite user types
						copy.addProperty( prop );
					}
					else {
						prepareActualProperty(
								prop,
								declaredMember,
								true,
								context,
								copy::addProperty
						);
					}
				}
				collector.registerGenericComponent( copy );
			}
		}
	}

	private void addPropertyToPersistentClass(Property property, MemberDetails memberDetails, ClassDetails declaringClass) {
		handleGenericComponentProperty( property, memberDetails, getContext() );
		if ( declaringClass != null ) {
			final var inheritanceState = inheritanceStatePerClass.get( declaringClass );
			if ( inheritanceState == null ) {
				throw new AssertionFailure(
						"Declaring class is not found in the inheritance state hierarchy: " + declaringClass
				);
			}
			if ( inheritanceState.isEmbeddableSuperclass() ) {
				persistentClass.addMappedSuperclassProperty( property );
				addPropertyToMappedSuperclass( property, memberDetails, declaringClass, getContext() );
			}
			else {
				persistentClass.addProperty( property );
			}
		}
		else {
			persistentClass.addProperty( property );
		}
	}

	public static void addPropertyToMappedSuperclass(
			Property prop,
			MemberDetails memberDetails,
			ClassDetails declaringClass,
			MetadataBuildingContext context) {
		final var superclass = context.getMetadataCollector().getMappedSuperclass( declaringClass.toJavaClass() );
		prepareActualProperty( prop, memberDetails, true, context, superclass::addDeclaredProperty );
	}

	static void prepareActualProperty(
			Property property,
			MemberDetails memberDetails,
			boolean allowCollections,
			MetadataBuildingContext context,
			Consumer<Property> propertyConsumer) {
		if ( memberDetails.getDeclaringType().getGenericSuperType() == null ) {
			propertyConsumer.accept( property );
			return;
		}

		if ( memberDetails.getType().isResolved() ) {
			// Avoid copying when the property doesn't depend on a type variable
			propertyConsumer.accept( property );
			return;
		}

		// If the property depends on a type variable, we have to copy it and the Value
		final var actualProperty = property.copy();
		actualProperty.setGeneric( true );
		actualProperty.setReturnedClassName( memberDetails.getType().getName() );
		final var value = actualProperty.getValue().copy();
		if ( value instanceof Collection collection ) {
			if ( !allowCollections ) {
				throw new AssertionFailure( "Collections are not allowed as identifier properties" );
			}
			// The owner is a MappedSuperclass which is not a PersistentClass, so set it to null
//						collection.setOwner( null );
			collection.setRole( memberDetails.getDeclaringType().getName() + "." + property.getName() );
			// To copy the element and key values, we need to defer setting the type name until the CollectionBinder ran
			final var originalValue = property.getValue();
			context.getMetadataCollector().addSecondPass(
					new SecondPass() {
						@Override
						public void doSecondPass(Map<String, PersistentClass> persistentClasses) {
							final var initializedCollection = (Collection) originalValue;
							final var element = initializedCollection.getElement().copy();
							setTypeName( element, memberDetails.getElementType().getName() );
							if ( initializedCollection instanceof IndexedCollection indexedCollection ) {
								final var index = indexedCollection.getIndex().copy();
								if ( memberDetails.getMapKeyType() != null ) {
									setTypeName( index, memberDetails.getMapKeyType().getName() );
								}
								( (IndexedCollection) collection ).setIndex( index );
							}
							collection.setElement( element );
						}
					}
			);
		}
		else {
			setTypeName( value, memberDetails.getType().getName() );
		}

		if ( value instanceof Component component ) {
			final var componentClass = component.getComponentClass();
			if ( component.isGeneric() ) {
				actualProperty.setValue( context.getMetadataCollector().getGenericComponent( componentClass ) );
			}
			else {
				if ( componentClass == Object.class ) {
					// Object is not a valid component class, but that is what we get when using a type variable
					component.clearProperties();
				}
				else {
					final var propertyIterator = component.getProperties().iterator();
					while ( propertyIterator.hasNext() ) {
						try {
							propertyIterator.next().getGetter( componentClass );
						}
						catch (PropertyNotFoundException e) {
							propertyIterator.remove();
						}
					}
				}
			}
		}
		actualProperty.setValue( value );
		propertyConsumer.accept( actualProperty );
	}

	private static Map<String, MemberDetails> getDeclaredAttributeMembers(
			ClassDetails declaringType,
			String accessType) {
		final Map<String, MemberDetails> members = new HashMap<>();
		ClassDetails superclass = declaringType;
		while ( superclass != null ) {
			applyAttributeMembers( superclass, accessType, members );
			superclass = superclass.getSuperClass();
		}
		return members;
	}

	public static final String ACCESS_PROPERTY = "property";
	public static final String ACCESS_FIELD = "field";
	public static final String ACCESS_RECORD = "record";

	private static void applyAttributeMembers(
			ClassDetails classDetails,
			String accessType,
			Map<String, MemberDetails> members) {
		final var collectedMembers = switch ( accessType ) {
			case ACCESS_FIELD -> classDetails.getFields();
			case ACCESS_PROPERTY -> classDetails.getMethods();
			case ACCESS_RECORD -> classDetails.getRecordComponents();
			default -> throw new IllegalArgumentException( "Unknown access type " + accessType );
		};
		for ( var member : collectedMembers ) {
			if ( member.isPersistable() ) {
				members.put( member.resolveAttributeName(), member );
			}
		}
	}

	private static void setTypeName(Value value, String typeName) {
		if ( value instanceof ToOne toOne ) {
			toOne.setReferencedEntityName( typeName );
			toOne.setTypeName( typeName );
		}
		else if ( value instanceof Component component ) {
			// Avoid setting type name for generic components
			if ( !component.isGeneric() ) {
				component.setComponentClassName( typeName );
			}
			if ( component.getTypeName() != null ) {
				component.setTypeName( typeName );
			}
		}
		else if ( value instanceof SimpleValue simpleValue ) {
			simpleValue.setTypeName( typeName );
		}
	}

	private void addPropertyToJoin(Property property, MemberDetails memberDetails, ClassDetails declaringClass, Join join) {
		if ( declaringClass != null ) {
			final var inheritanceState = inheritanceStatePerClass.get( declaringClass );
			if ( inheritanceState == null ) {
				throw new AssertionFailure(
						"Declaring class is not found in the inheritance state hierarchy: " + declaringClass
				);
			}
			if ( inheritanceState.isEmbeddableSuperclass() ) {
				join.addMappedSuperclassProperty( property );
				addPropertyToMappedSuperclass( property, memberDetails, declaringClass, getContext() );
			}
			else {
				join.addProperty( property );
			}
		}
		else {
			join.addProperty( property );
		}
	}

	/**
	 * Needed for proper compliance with naming strategy, the property table
	 * can be overridden if the properties are part of secondary tables
	 */
	private Map<String, Join> getJoinsPerRealTableName() {
		if ( joinsPerRealTableName == null ) {
			joinsPerRealTableName = mapOfSize( joins.size() );
			for ( Join join : joins.values() ) {
				joinsPerRealTableName.put( join.getTable().getName(), join );
			}
		}
		return joinsPerRealTableName;
	}

	@Override
	public String getClassName() {
		return persistentClass.getClassName();
	}

	@Override
	public String getEntityOwnerClassName() {
		return getClassName();
	}

	@Override
	public Table getTable() {
		return persistentClass.getTable();
	}

	@Override
	public boolean isComponent() {
		return false;
	}

	@Override
	public boolean isEntity() {
		return true;
	}

	@Override
	public PersistentClass getPersistentClass() {
		return persistentClass;
	}

	@Override
	public KeyValue getIdentifier() {
		return persistentClass.getIdentifier();
	}

	@Override
	public boolean isOrWithinEmbeddedId() {
		return false;
	}

	@Override
	public boolean isWithinElementCollection() {
		return false;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "(" + getEntityName() + ")";
	}
}
