/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.hibernate.AnnotationException;
import org.hibernate.boot.spi.AccessType;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.PropertyData;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.FieldDetails;
import org.hibernate.models.spi.MethodDetails;

import jakarta.persistence.Access;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.MappedSuperclass;

import static jakarta.persistence.InheritanceType.SINGLE_TABLE;
import static jakarta.persistence.InheritanceType.TABLE_PER_CLASS;
import static org.hibernate.boot.model.internal.PropertyBinder.addElementsOfClass;
import static org.hibernate.boot.model.internal.PropertyBinder.hasIdAnnotation;
import static org.hibernate.internal.util.ReflectHelper.OBJECT_CLASS_NAME;

/**
 * Some extra data to the inheritance position of a class.
 *
 * @author Emmanuel Bernard
 */
public class InheritanceState {
	private ClassDetails classDetails;

	/**
	 * Has sibling (either mappedsuperclass entity)
	 */
	private boolean hasSiblings = false;

	/**
	 * a mother entity is available
	 */
	private boolean hasParents = false;
	private InheritanceType type;
	private boolean isEmbeddableSuperclass = false;
	private final Map<ClassDetails, InheritanceState> inheritanceStatePerClass;
	private final List<ClassDetails> classesToProcessForMappedSuperclass = new ArrayList<>();
	private final MetadataBuildingContext buildingContext;
	private AccessType accessType;
	private ElementsToProcess elementsToProcess;
	private Boolean hasIdClassOrEmbeddedId;

	public InheritanceState(
			ClassDetails classDetails,
			Map<ClassDetails, InheritanceState> inheritanceStatePerClass,
			MetadataBuildingContext buildingContext) {
		this.setClassDetails( classDetails );
		this.buildingContext = buildingContext;
		this.inheritanceStatePerClass = inheritanceStatePerClass;
		extractInheritanceType( classDetails );
	}

	private void extractInheritanceType(ClassDetails classDetails) {
		final var inheritance = classDetails.getDirectAnnotationUsage( Inheritance.class );
		final var mappedSuperclass = classDetails.getDirectAnnotationUsage( MappedSuperclass.class );
		if ( mappedSuperclass != null ) {
			setEmbeddableSuperclass( true );
			setType( inheritance == null ? null : inheritance.strategy() );
		}
		else {
			setType( inheritance == null ? SINGLE_TABLE : inheritance.strategy() );
		}
	}

	public boolean hasTable() {
		return !hasParents() || SINGLE_TABLE != getType();
	}

	public boolean hasDenormalizedTable() {
		return hasParents() && TABLE_PER_CLASS == getType();
	}

	public static InheritanceState getInheritanceStateOfSuperEntity(
			ClassDetails classDetails,
			Map<ClassDetails, InheritanceState> states) {
		ClassDetails candidate = classDetails;
		do {
			candidate = candidate.getSuperClass();
			final var currentState = states.get( candidate );
			if ( currentState != null && !currentState.isEmbeddableSuperclass() ) {
				return currentState;
			}
		}
		while ( candidate != null && !OBJECT_CLASS_NAME.equals( candidate.getName() ) );
		return null;
	}

	public static InheritanceState getSuperclassInheritanceState(
			ClassDetails classDetails,
			Map<ClassDetails, InheritanceState> states) {
		ClassDetails superclass = classDetails;
		do {
			superclass = superclass.getSuperClass();
			final var currentState = states.get( superclass );
			if ( currentState != null ) {
				return currentState;
			}
		}
		while ( superclass != null && !OBJECT_CLASS_NAME.equals( superclass.getName() ) );
		return null;
	}

	public ClassDetails getClassDetails() {
		return classDetails;
	}

	public void setClassDetails(ClassDetails classDetails) {
		this.classDetails = classDetails;
	}

	public boolean hasSiblings() {
		return hasSiblings;
	}

	public void setHasSiblings(boolean hasSiblings) {
		this.hasSiblings = hasSiblings;
	}

	public boolean hasParents() {
		return hasParents;
	}

	public void setHasParents(boolean hasParents) {
		this.hasParents = hasParents;
	}

	public InheritanceType getType() {
		return type;
	}

	public void setType(InheritanceType type) {
		this.type = type;
	}

	public boolean isEmbeddableSuperclass() {
		return isEmbeddableSuperclass;
	}

	public void setEmbeddableSuperclass(boolean embeddableSuperclass) {
		isEmbeddableSuperclass = embeddableSuperclass;
	}

	public ElementsToProcess postProcess(PersistentClass persistenceClass, EntityBinder entityBinder) {
		//make sure we run elements to process
		getElementsToProcess();
		addMappedSuperClassInMetadata( persistenceClass );
		entityBinder.setPropertyAccessType( accessType );
		return elementsToProcess;
	}

	public void postProcess(Component component) {
		if ( classesToProcessForMappedSuperclass.isEmpty() ) {
			// Component classes might be processed more than once,
			// so only do this the first time we encounter them
			getMappedSuperclassesTillNextEntityOrdered();
		}
		addMappedSuperClassInMetadata( component );
	}

	public ClassDetails getClassWithIdClass(boolean evenIfSubclass) {
		if ( !evenIfSubclass && hasParents() ) {
			return null;
		}
		else if ( classDetails.hasDirectAnnotationUsage( IdClass.class ) ) {
			return classDetails;
		}
		else {
			final long count =
					Stream.concat( classDetails.getFields().stream(), classDetails.getMethods().stream() )
							.filter( member -> member.hasDirectAnnotationUsage( Id.class ) )
							.count();
			if ( count > 1 ) {
				return classDetails;
			}
			else {
				final var state = getSuperclassInheritanceState( classDetails, inheritanceStatePerClass );
				return state == null ? null : state.getClassWithIdClass( true );
			}
		}
	}

	public Boolean hasIdClassOrEmbeddedId() {
		if ( hasIdClassOrEmbeddedId == null ) {
			hasIdClassOrEmbeddedId = false;
			if ( getClassWithIdClass( true ) != null ) {
				hasIdClassOrEmbeddedId = true;
			}
			else {
				for ( PropertyData property : getElementsToProcess().getElements() ) {
					if ( property.getAttributeMember().hasDirectAnnotationUsage( EmbeddedId.class ) ) {
						hasIdClassOrEmbeddedId = true;
						break;
					}
				}
			}
		}
		return hasIdClassOrEmbeddedId;
	}

	/*
	 * Get the annotated elements and determine access type from hierarchy,
	 * guessing from @Id or @EmbeddedId presence if not specified.
	 * Change EntityBinder by side effect
	 */
	private ElementsToProcess getElementsToProcess() {
		if ( elementsToProcess == null ) {
			final var inheritanceState = inheritanceStatePerClass.get( classDetails );
			assert !inheritanceState.isEmbeddableSuperclass();

			getMappedSuperclassesTillNextEntityOrdered();

			accessType = determineDefaultAccessType();

			final ArrayList<PropertyData> elements = new ArrayList<>();
			int idPropertyCount = 0;
			for ( ClassDetails classToProcessForMappedSuperclass : classesToProcessForMappedSuperclass ) {
				final var container =
						new PropertyContainer( classToProcessForMappedSuperclass, classDetails, accessType );
				idPropertyCount = addElementsOfClass( elements, container, buildingContext, idPropertyCount );
			}
			if ( idPropertyCount == 0 && !inheritanceState.hasParents() ) {
				throw new AnnotationException( "Entity '" + classDetails.getName() + "' has no identifier"
						+ " (every '@Entity' class must declare or inherit at least one '@Id' or '@EmbeddedId' property)" );
			}
			elements.trimToSize();
			elementsToProcess = new ElementsToProcess( elements, idPropertyCount );
		}
		return elementsToProcess;
	}

	private AccessType determineDefaultAccessType() {
		for ( ClassDetails candidate = classDetails; candidate != null; candidate = candidate.getSuperClass() ) {
			if ( ( candidate.getSuperClass() == null || OBJECT_CLASS_NAME.equals( candidate.getSuperClass().getName() ) )
					&& ( candidate.hasDirectAnnotationUsage( Entity.class ) || candidate.hasDirectAnnotationUsage( MappedSuperclass.class ) )
					&& candidate.hasDirectAnnotationUsage( Access.class ) ) {
				return AccessType.getAccessStrategy( candidate.getDirectAnnotationUsage( Access.class ).value() );
			}
		}
		// Guess from identifier.
		// FIX: Shouldn't this be determined by the first attribute (i.e., field or property) with annotations,
		// but without an explicit Access annotation, according to JPA 2.0 spec 2.3.1: Default Access Type?
		for ( ClassDetails candidate = classDetails;
				candidate != null && !OBJECT_CLASS_NAME.equals( candidate.getName() );
				candidate = candidate.getSuperClass() ) {
			if ( candidate.hasDirectAnnotationUsage( Entity.class )
				|| candidate.hasDirectAnnotationUsage( MappedSuperclass.class ) ) {
				for ( MethodDetails method : candidate.getMethods() ) {
					if ( method.getMethodKind() == MethodDetails.MethodKind.GETTER
							&& hasIdAnnotation( method ) ) {
						return AccessType.PROPERTY;
					}
				}
				for ( FieldDetails field : candidate.getFields() ) {
					if ( hasIdAnnotation( field ) ) {
						return AccessType.FIELD;
					}
				}
			}
		}
		throw new AnnotationException(
				"Entity '" + classDetails.getName() + "' has no identifier"
						+ " (every '@Entity' class must declare or inherit at least one '@Id' or '@EmbeddedId' property)"
		);
	}

	private void getMappedSuperclassesTillNextEntityOrdered() {
		//ordered to allow proper messages on properties subclassing
		ClassDetails currentClassInHierarchy = classDetails;
		InheritanceState superclassState;
		do {
			classesToProcessForMappedSuperclass.add( 0, currentClassInHierarchy );
			ClassDetails superClass = currentClassInHierarchy;
			do {
				superClass = superClass.getSuperClass();
				superclassState = inheritanceStatePerClass.get( superClass );
			}
			while ( superClass != null
					&& !OBJECT_CLASS_NAME.equals( superClass.getClassName() )
					&& superclassState == null );
			currentClassInHierarchy = superClass;
		}
		while ( superclassState != null && superclassState.isEmbeddableSuperclass() );
	}

	private void addMappedSuperClassInMetadata(Component component) {
		final var mappedSuperclass = processMappedSuperclass( component.getTable() );
		if ( mappedSuperclass != null ) {
			component.setMappedSuperclass( mappedSuperclass );
		}
	}

	private void addMappedSuperClassInMetadata(PersistentClass persistentClass) {
		final var mappedSuperclass = processMappedSuperclass( persistentClass.getImplicitTable() );
		if ( mappedSuperclass != null ) {
			persistentClass.setSuperMappedSuperclass( mappedSuperclass );
		}
	}

	private org.hibernate.mapping.MappedSuperclass processMappedSuperclass(Table implicitTable) {
		//add @MappedSuperclass in the metadata
		// classes from 0 to n-1 are @MappedSuperclass and should be linked
		final var metadataCollector = buildingContext.getMetadataCollector();
		final var superEntityState = getInheritanceStateOfSuperEntity( classDetails, inheritanceStatePerClass );
		final PersistentClass superEntity =
				superEntityState != null
						? metadataCollector.getEntityBinding( superEntityState.getClassDetails().getName() )
						: null;
		final int lastMappedSuperclass = classesToProcessForMappedSuperclass.size() - 1;
		org.hibernate.mapping.MappedSuperclass mappedSuperclass = null;
		for ( int index = 0; index < lastMappedSuperclass; index++ ) {
			final var parentSuperclass = mappedSuperclass;
			// todo (jpa32) : causes the mapped-superclass Class reference to be loaded...
			//		- but this is how it's always worked, so...
			final var mappedSuperclassDetails = classesToProcessForMappedSuperclass.get( index );
			final var mappedSuperclassJavaType = mappedSuperclassDetails.toJavaClass();
			//add MappedSuperclass if not already there
			mappedSuperclass = metadataCollector.getMappedSuperclass( mappedSuperclassJavaType );
			if ( mappedSuperclass == null ) {
				mappedSuperclass = new org.hibernate.mapping.MappedSuperclass( parentSuperclass, superEntity, implicitTable );
				mappedSuperclass.setMappedClass( mappedSuperclassJavaType );
				metadataCollector.addMappedSuperclass( mappedSuperclassJavaType, mappedSuperclass );
			}
		}
		return mappedSuperclass;
	}

	public static final class ElementsToProcess {
		private final List<PropertyData> properties;
		private final int idPropertyCount;

		public List<PropertyData> getElements() {
			return properties;
		}

		public int getIdPropertyCount() {
			return idPropertyCount;
		}

		private ElementsToProcess(List<PropertyData> properties, int idPropertyCount) {
			this.properties = properties;
			this.idPropertyCount = idPropertyCount;
		}
	}
}
