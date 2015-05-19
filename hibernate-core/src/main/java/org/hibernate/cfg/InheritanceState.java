/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.persistence.Access;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.MappedSuperclass;

import org.hibernate.AnnotationException;
import org.hibernate.annotations.common.reflection.XAnnotatedElement;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.cfg.annotations.EntityBinder;
import org.hibernate.mapping.PersistentClass;

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
	private Map<XClass, InheritanceState> inheritanceStatePerClass;
	private List<XClass> classesToProcessForMappedSuperclass = new ArrayList<XClass>();
	private MetadataBuildingContext buildingContext;
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
			setType( inhAnn == null ? InheritanceType.SINGLE_TABLE : inhAnn.strategy() );
		}
	}

	boolean hasTable() {
		return !hasParents() || !InheritanceType.SINGLE_TABLE.equals( getType() );
	}

	boolean hasDenormalizedTable() {
		return hasParents() && InheritanceType.TABLE_PER_CLASS.equals( getType() );
	}

	public static InheritanceState getInheritanceStateOfSuperEntity(
			XClass clazz, Map<XClass, InheritanceState> states
	) {
		XClass superclass = clazz;
		do {
			superclass = superclass.getSuperclass();
			InheritanceState currentState = states.get( superclass );
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

	void postProcess(PersistentClass persistenceClass, EntityBinder entityBinder) {
		//make sure we run elements to process
		getElementsToProcess();
		addMappedSuperClassInMetadata( persistenceClass );
		entityBinder.setPropertyAccessType( accessType );
	}

	public XClass getClassWithIdClass(boolean evenIfSubclass) {
		if ( !evenIfSubclass && hasParents() ) {
			return null;
		}
		if ( clazz.isAnnotationPresent( IdClass.class ) ) {
			return clazz;
		}
		else {
			InheritanceState state = InheritanceState.getSuperclassInheritanceState( clazz, inheritanceStatePerClass );
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
     * Get the annotated elements and determine access type from hierarchy, guessing from @Id or @EmbeddedId presence if not
     * specified.
     * Change EntityBinder by side effect
     */

	public ElementsToProcess getElementsToProcess() {
		if ( elementsToProcess == null ) {
			InheritanceState inheritanceState = inheritanceStatePerClass.get( clazz );
			assert !inheritanceState.isEmbeddableSuperclass();


			getMappedSuperclassesTillNextEntityOrdered();

			accessType = determineDefaultAccessType();

			ArrayList<PropertyData> elements = new ArrayList<PropertyData>();
			int idPropertyCount = 0;

			for ( XClass classToProcessForMappedSuperclass : classesToProcessForMappedSuperclass ) {
				PropertyContainer propertyContainer = new PropertyContainer(
						classToProcessForMappedSuperclass,
						clazz
				);
				int currentIdPropertyCount = AnnotationBinder.addElementsOfClass(
						elements,
						accessType,
						propertyContainer,
						buildingContext
				);
				idPropertyCount += currentIdPropertyCount;
			}

			if ( idPropertyCount == 0 && !inheritanceState.hasParents() ) {
				throw new AnnotationException( "No identifier specified for entity: " + clazz.getName() );
			}
			elements.trimToSize();
			elementsToProcess = new ElementsToProcess( elements, idPropertyCount );
		}
		return elementsToProcess;
	}

	private AccessType determineDefaultAccessType() {
        for (XClass xclass = clazz; xclass != null; xclass = xclass.getSuperclass()) {
            if ((xclass.getSuperclass() == null || Object.class.getName().equals(xclass.getSuperclass().getName()))
                && (xclass.isAnnotationPresent(Entity.class) || xclass.isAnnotationPresent(MappedSuperclass.class))
                && xclass.isAnnotationPresent(Access.class))
                return AccessType.getAccessStrategy(xclass.getAnnotation(Access.class).value());
        }
        // Guess from identifier.
        // FIX: Shouldn't this be determined by the first attribute (i.e., field or property) with annotations, but without an
        //      explicit Access annotation, according to JPA 2.0 spec 2.3.1: Default Access Type?
		for (XClass xclass = clazz; xclass != null && !Object.class.getName().equals(xclass.getName()); xclass = xclass.getSuperclass()) {
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
			}
		}
        throw new AnnotationException( "No identifier specified for entity: " + clazz );
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
					&& !buildingContext.getBuildingOptions().getReflectionManager().equals( superClass, Object.class )
					&& superclassState == null );

			currentClassInHierarchy = superClass;
		}
		while ( superclassState != null && superclassState.isEmbeddableSuperclass() );
	}

	private void addMappedSuperClassInMetadata(PersistentClass persistentClass) {
		//add @MappedSuperclass in the metadata
		// classes from 0 to n-1 are @MappedSuperclass and should be linked
		org.hibernate.mapping.MappedSuperclass mappedSuperclass = null;
		final InheritanceState superEntityState =
				InheritanceState.getInheritanceStateOfSuperEntity( clazz, inheritanceStatePerClass );
		PersistentClass superEntity =
				superEntityState != null ?
						buildingContext.getMetadataCollector().getEntityBinding( superEntityState.getClazz().getName() ) :
						null;
		final int lastMappedSuperclass = classesToProcessForMappedSuperclass.size() - 1;
		for ( int index = 0; index < lastMappedSuperclass; index++ ) {
			org.hibernate.mapping.MappedSuperclass parentSuperclass = mappedSuperclass;
			final Class<?> type = buildingContext.getBuildingOptions().getReflectionManager()
					.toClass( classesToProcessForMappedSuperclass.get( index ) );
			//add MAppedSuperclass if not already there
			mappedSuperclass = buildingContext.getMetadataCollector().getMappedSuperclass( type );
			if ( mappedSuperclass == null ) {
				mappedSuperclass = new org.hibernate.mapping.MappedSuperclass( parentSuperclass, superEntity );
				mappedSuperclass.setMappedClass( type );
				buildingContext.getMetadataCollector().addMappedSuperclass( type, mappedSuperclass );
			}
		}
		if ( mappedSuperclass != null ) {
			persistentClass.setSuperMappedSuperclass( mappedSuperclass );
		}
	}

	static final class ElementsToProcess {
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
