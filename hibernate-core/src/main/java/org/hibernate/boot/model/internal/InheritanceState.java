/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.model.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.AnnotationException;
import org.hibernate.annotations.common.reflection.XAnnotatedElement;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.boot.spi.AccessType;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.PropertyData;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;

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

/**
 * Some extra data to the inheritance position of a class.
 *
 * @author Emmanuel Bernard
 */
public class InheritanceState {
	private XClass clazz;

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
	private final Map<XClass, InheritanceState> inheritanceStatePerClass;
	private final List<XClass> classesToProcessForMappedSuperclass = new ArrayList<>();
	private final MetadataBuildingContext buildingContext;
	private AccessType accessType;
	private ElementsToProcess elementsToProcess;
	private Boolean hasIdClassOrEmbeddedId;

	public InheritanceState(
			XClass clazz,
			Map<XClass, InheritanceState> inheritanceStatePerClass,
			MetadataBuildingContext buildingContext) {
		this.setClazz( clazz );
		this.buildingContext = buildingContext;
		this.inheritanceStatePerClass = inheritanceStatePerClass;
		extractInheritanceType();
	}

	private void extractInheritanceType() {
		XAnnotatedElement element = getClazz();
		Inheritance inhAnn = element.getAnnotation( Inheritance.class );
		MappedSuperclass mappedSuperClass = element.getAnnotation( MappedSuperclass.class );
		if ( mappedSuperClass != null ) {
			setEmbeddableSuperclass( true );
			setType( inhAnn == null ? null : inhAnn.strategy() );
		}
		else {
			setType( inhAnn == null ? SINGLE_TABLE : inhAnn.strategy() );
		}
	}

	public boolean hasTable() {
		return !hasParents() || SINGLE_TABLE != getType();
	}

	public boolean hasDenormalizedTable() {
		return hasParents() && TABLE_PER_CLASS == getType();
	}

	public static InheritanceState getInheritanceStateOfSuperEntity(XClass clazz, Map<XClass, InheritanceState> states) {
		XClass superclass = clazz;
		do {
			superclass = superclass.getSuperclass();
			final InheritanceState currentState = states.get( superclass );
			if ( currentState != null && !currentState.isEmbeddableSuperclass() ) {
				return currentState;
			}
		}
		while ( superclass != null && !Object.class.getName().equals( superclass.getName() ) );
		return null;
	}

	public static InheritanceState getSuperclassInheritanceState(XClass clazz, Map<XClass, InheritanceState> states) {
		XClass superclass = clazz;
		do {
			superclass = superclass.getSuperclass();
			InheritanceState currentState = states.get( superclass );
			if ( currentState != null ) {
				return currentState;
			}
		}
		while ( superclass != null && !Object.class.getName().equals( superclass.getName() ) );
		return null;
	}

	public XClass getClazz() {
		return clazz;
	}

	public void setClazz(XClass clazz) {
		this.clazz = clazz;
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

	public XClass getClassWithIdClass(boolean evenIfSubclass) {
		if ( !evenIfSubclass && hasParents() ) {
			return null;
		}
		else if ( clazz.isAnnotationPresent( IdClass.class ) ) {
			return clazz;
		}
		else {
			final InheritanceState state = getSuperclassInheritanceState( clazz, inheritanceStatePerClass );
			if ( state != null ) {
				return state.getClassWithIdClass( true );
			}
			else {
				return null;
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
				final ElementsToProcess process = getElementsToProcess();
				for ( PropertyData property : process.getElements() ) {
					if ( property.getProperty().isAnnotationPresent( EmbeddedId.class ) ) {
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
			InheritanceState inheritanceState = inheritanceStatePerClass.get( clazz );
			assert !inheritanceState.isEmbeddableSuperclass();

			getMappedSuperclassesTillNextEntityOrdered();

			accessType = determineDefaultAccessType();

			final ArrayList<PropertyData> elements = new ArrayList<>();
			int idPropertyCount = 0;

			for ( XClass classToProcessForMappedSuperclass : classesToProcessForMappedSuperclass ) {
				PropertyContainer propertyContainer = new PropertyContainer(
						classToProcessForMappedSuperclass,
						clazz,
						accessType
				);
				idPropertyCount = addElementsOfClass(
						elements,
						propertyContainer,
						buildingContext,
						idPropertyCount );
			}

			if ( idPropertyCount == 0 && !inheritanceState.hasParents() ) {
				throw new AnnotationException( "Entity '" + clazz.getName() + "' has no identifier"
						+ " (every '@Entity' class must declare or inherit at least one '@Id' or '@EmbeddedId' property)" );
			}
			elements.trimToSize();
			elementsToProcess = new ElementsToProcess( elements, idPropertyCount );
		}
		return elementsToProcess;
	}

	private AccessType determineDefaultAccessType() {
		for ( XClass xclass = clazz; xclass != null; xclass = xclass.getSuperclass() ) {
			if ( ( xclass.getSuperclass() == null || Object.class.getName().equals( xclass.getSuperclass().getName() ) )
					&& ( xclass.isAnnotationPresent( Entity.class ) || xclass.isAnnotationPresent( MappedSuperclass.class ) )
					&& xclass.isAnnotationPresent( Access.class ) ) {
				return AccessType.getAccessStrategy( xclass.getAnnotation( Access.class ).value() );
			}
		}
		// Guess from identifier.
		// FIX: Shouldn't this be determined by the first attribute (i.e., field or property) with annotations,
		// but without an explicit Access annotation, according to JPA 2.0 spec 2.3.1: Default Access Type?
		for ( XClass xclass = clazz;
				xclass != null && !Object.class.getName().equals( xclass.getName() );
				xclass = xclass.getSuperclass() ) {
			if ( xclass.isAnnotationPresent( Entity.class ) || xclass.isAnnotationPresent( MappedSuperclass.class ) ) {
				for ( XProperty prop : xclass.getDeclaredProperties( AccessType.PROPERTY.getType() ) ) {
					final boolean isEmbeddedId = prop.isAnnotationPresent( EmbeddedId.class );
					if ( prop.isAnnotationPresent( Id.class ) || isEmbeddedId ) {
						return AccessType.PROPERTY;
					}
				}
				for ( XProperty prop : xclass.getDeclaredProperties( AccessType.FIELD.getType() ) ) {
					final boolean isEmbeddedId = prop.isAnnotationPresent( EmbeddedId.class );
					if ( prop.isAnnotationPresent( Id.class ) || isEmbeddedId ) {
						return AccessType.FIELD;
					}
				}
				for ( XProperty prop : xclass.getDeclaredProperties( AccessType.RECORD.getType() ) ) {
					final boolean isEmbeddedId = prop.isAnnotationPresent( EmbeddedId.class );
					if ( prop.isAnnotationPresent( Id.class ) || isEmbeddedId ) {
						return AccessType.RECORD;
					}
				}
			}
		}
		throw new AnnotationException( "Entity '" + clazz.getName() + "' has no identifier"
				+ " (every '@Entity' class must declare or inherit at least one '@Id' or '@EmbeddedId' property)" );
	}

	private void getMappedSuperclassesTillNextEntityOrdered() {
		//ordered to allow proper messages on properties subclassing
		XClass currentClassInHierarchy = clazz;
		InheritanceState superclassState;
		do {
			classesToProcessForMappedSuperclass.add( 0, currentClassInHierarchy );
			XClass superClass = currentClassInHierarchy;
			do {
				superClass = superClass.getSuperclass();
				superclassState = inheritanceStatePerClass.get( superClass );
			}
			while ( superClass != null
					&& !buildingContext.getBootstrapContext().getReflectionManager().equals( superClass, Object.class )
					&& superclassState == null );

			currentClassInHierarchy = superClass;
		}
		while ( superclassState != null && superclassState.isEmbeddableSuperclass() );
	}

	private void addMappedSuperClassInMetadata(Component component) {
		org.hibernate.mapping.MappedSuperclass mappedSuperclass = processMappedSuperclass( component.getTable() );
		if ( mappedSuperclass != null ) {
			component.setMappedSuperclass( mappedSuperclass );
		}
	}

	private void addMappedSuperClassInMetadata(PersistentClass persistentClass) {
		org.hibernate.mapping.MappedSuperclass mappedSuperclass = processMappedSuperclass( persistentClass.getImplicitTable() );
		if ( mappedSuperclass != null ) {
			persistentClass.setSuperMappedSuperclass( mappedSuperclass );
		}
	}

	private org.hibernate.mapping.MappedSuperclass processMappedSuperclass(Table implicitTable) {
		//add @MappedSuperclass in the metadata
		// classes from 0 to n-1 are @MappedSuperclass and should be linked
		final InheritanceState superEntityState = getInheritanceStateOfSuperEntity( clazz, inheritanceStatePerClass );
		final PersistentClass superEntity =
				superEntityState != null ?
						buildingContext.getMetadataCollector().getEntityBinding( superEntityState.getClazz().getName() ) :
						null;
		final int lastMappedSuperclass = classesToProcessForMappedSuperclass.size() - 1;
		org.hibernate.mapping.MappedSuperclass mappedSuperclass = null;
		for ( int index = 0; index < lastMappedSuperclass; index++ ) {
			org.hibernate.mapping.MappedSuperclass parentSuperclass = mappedSuperclass;
			final Class<?> type = buildingContext.getBootstrapContext().getReflectionManager()
					.toClass( classesToProcessForMappedSuperclass.get( index ) );
			//add MappedSuperclass if not already there
			mappedSuperclass = buildingContext.getMetadataCollector().getMappedSuperclass( type );
			if ( mappedSuperclass == null ) {
				mappedSuperclass = new org.hibernate.mapping.MappedSuperclass( parentSuperclass, superEntity, implicitTable );
				mappedSuperclass.setMappedClass( type );
				buildingContext.getMetadataCollector().addMappedSuperclass( type, mappedSuperclass );
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
