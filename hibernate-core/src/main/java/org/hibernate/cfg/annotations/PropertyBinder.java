/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.cfg.annotations;

import java.util.Map;

import javax.persistence.EmbeddedId;
import javax.persistence.Id;
import javax.persistence.Lob;

import org.hibernate.AnnotationException;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.OptimisticLock;
import org.hibernate.annotations.common.AssertionFailure;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.cfg.AccessType;
import org.hibernate.cfg.AnnotationBinder;
import org.hibernate.cfg.BinderHelper;
import org.hibernate.cfg.Ejb3Column;
import org.hibernate.cfg.InheritanceState;
import org.hibernate.cfg.Mappings;
import org.hibernate.cfg.PropertyHolder;
import org.hibernate.cfg.PropertyPreloadedData;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.PropertyGeneration;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.ToOne;
import org.hibernate.mapping.Value;
import org.jboss.logging.Logger;

/**
 * @author Emmanuel Bernard
 */
public class PropertyBinder {
    private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, PropertyBinder.class.getName());

	private String name;
	private String returnedClassName;
	private boolean lazy;
	private AccessType accessType;
	private Ejb3Column[] columns;
	private PropertyHolder holder;
	private Mappings mappings;
	private Value value;
	private boolean insertable = true;
	private boolean updatable = true;
	private String cascade;
	private SimpleValueBinder simpleValueBinder;
	private XClass declaringClass;
	private boolean declaringClassSet;
	private boolean embedded;
	private EntityBinder entityBinder;
	private boolean isXToMany;
	private String referencedEntityName;

	public void setReferencedEntityName(String referencedEntityName) {
		this.referencedEntityName = referencedEntityName;
	}

	public void setEmbedded(boolean embedded) {
		this.embedded = embedded;
	}

	public void setEntityBinder(EntityBinder entityBinder) {
		this.entityBinder = entityBinder;
	}

	/*
			 * property can be null
			 * prefer propertyName to property.getName() since some are overloaded
			 */
	private XProperty property;
	private XClass returnedClass;
	private boolean isId;
	private Map<XClass, InheritanceState> inheritanceStatePerClass;
	private Property mappingProperty;

	public void setInsertable(boolean insertable) {
		this.insertable = insertable;
	}

	public void setUpdatable(boolean updatable) {
		this.updatable = updatable;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setReturnedClassName(String returnedClassName) {
		this.returnedClassName = returnedClassName;
	}

	public void setLazy(boolean lazy) {
		this.lazy = lazy;
	}

	public void setAccessType(AccessType accessType) {
		this.accessType = accessType;
	}

	public void setColumns(Ejb3Column[] columns) {
		insertable = columns[0].isInsertable();
		updatable = columns[0].isUpdatable();
		//consistency is checked later when we know the property name
		this.columns = columns;
	}

	public void setHolder(PropertyHolder holder) {
		this.holder = holder;
	}

	public void setValue(Value value) {
		this.value = value;
	}

	public void setCascade(String cascadeStrategy) {
		this.cascade = cascadeStrategy;
	}

	public void setMappings(Mappings mappings) {
		this.mappings = mappings;
	}

	public void setDeclaringClass(XClass declaringClass) {
		this.declaringClass = declaringClass;
		this.declaringClassSet = true;
	}

	private void validateBind() {
		if ( property.isAnnotationPresent( Immutable.class ) ) {
			throw new AnnotationException(
					"@Immutable on property not allowed. " +
							"Only allowed on entity level or on a collection."
			);
		}
		if ( !declaringClassSet ) {
			throw new AssertionFailure( "declaringClass has not been set before a bind" );
		}
	}

	private void validateMake() {
		//TODO check necessary params for a make
	}

	private Property makePropertyAndValue() {
		validateBind();
		LOG.debugf( "MetadataSourceProcessor property %s with lazy=%s", name, lazy );
		String containerClassName = holder == null ?
				null :
				holder.getClassName();
		simpleValueBinder = new SimpleValueBinder();
		simpleValueBinder.setMappings( mappings );
		simpleValueBinder.setPropertyName( name );
		simpleValueBinder.setReturnedClassName( returnedClassName );
		simpleValueBinder.setColumns( columns );
		simpleValueBinder.setPersistentClassName( containerClassName );
		simpleValueBinder.setType( property, returnedClass, containerClassName );
		simpleValueBinder.setMappings( mappings );
		simpleValueBinder.setReferencedEntityName( referencedEntityName );
		simpleValueBinder.setAccessType( accessType );
		SimpleValue propertyValue = simpleValueBinder.make();
		setValue( propertyValue );
		return makeProperty();
	}

	//used when value is provided
	public Property makePropertyAndBind() {
		return bind( makeProperty() );
	}

	//used to build everything from scratch
	public Property makePropertyValueAndBind() {
		return bind( makePropertyAndValue() );
	}

	public void setXToMany(boolean xToMany) {
		this.isXToMany = xToMany;
	}

	private Property bind(Property prop) {
		if (isId) {
			final RootClass rootClass = ( RootClass ) holder.getPersistentClass();
			//if an xToMany, it as to be wrapped today.
			//FIXME this pose a problem as the PK is the class instead of the associated class which is not really compliant with the spec
			if ( isXToMany || entityBinder.wrapIdsInEmbeddedComponents() ) {
				Component identifier = (Component) rootClass.getIdentifier();
				if (identifier == null) {
					identifier = AnnotationBinder.createComponent( holder, new PropertyPreloadedData(null, null, null), true, false, mappings );
					rootClass.setIdentifier( identifier );
					identifier.setNullValue( "undefined" );
					rootClass.setEmbeddedIdentifier( true );
					rootClass.setIdentifierMapper( identifier );
				}
				//FIXME is it good enough?
				identifier.addProperty( prop );
			}
			else {
				rootClass.setIdentifier( ( KeyValue ) getValue() );
				if (embedded) {
					rootClass.setEmbeddedIdentifier( true );
				}
				else {
					rootClass.setIdentifierProperty( prop );
					final org.hibernate.mapping.MappedSuperclass superclass = BinderHelper.getMappedSuperclassOrNull(
							declaringClass,
							inheritanceStatePerClass,
							mappings
					);
					if (superclass != null) {
						superclass.setDeclaredIdentifierProperty(prop);
					}
					else {
						//we know the property is on the actual entity
						rootClass.setDeclaredIdentifierProperty( prop );
					}
				}
			}
		}
		else {
			holder.addProperty( prop, columns, declaringClass );
		}
		return prop;
	}

	//used when the value is provided and the binding is done elsewhere
	public Property makeProperty() {
		validateMake();
		LOG.debugf( "Building property %s", name );
		Property prop = new Property();
		prop.setName( name );
		prop.setNodeName( name );
		prop.setValue( value );
		prop.setLazy( lazy );
		prop.setCascade( cascade );
		prop.setPropertyAccessorName( accessType.getType() );
		
		Generated ann = property != null ?
				property.getAnnotation( Generated.class ) :
				null;
		GenerationTime generated = ann != null ?
				ann.value() :
				null;
		if ( generated != null ) {
			if ( !GenerationTime.NEVER.equals( generated ) ) {
				if ( property.isAnnotationPresent( javax.persistence.Version.class )
						&& GenerationTime.INSERT.equals( generated ) ) {
					throw new AnnotationException(
							"@Generated(INSERT) on a @Version property not allowed, use ALWAYS: "
									+ StringHelper.qualify( holder.getPath(), name )
					);
				}
				insertable = false;
				if ( GenerationTime.ALWAYS.equals( generated ) ) {
					updatable = false;
				}
				prop.setGeneration( PropertyGeneration.parse( generated.toString().toLowerCase() ) );
			}
		}
		
		NaturalId naturalId = property != null ? property.getAnnotation( NaturalId.class ) : null;
		if ( naturalId != null ) {
			if ( ! entityBinder.isRootEntity() ) {
				throw new AnnotationException( "@NaturalId only valid on root entity (or its @MappedSuperclasses)" );
			}
			if ( ! naturalId.mutable() ) {
				updatable = false;
			}
			prop.setNaturalIdentifier( true );
		}
		
		// HHH-4635 -- needed for dialect-specific property ordering
		Lob lob = property != null ? property.getAnnotation( Lob.class ) : null;
		prop.setLob( lob != null );
		
		prop.setInsertable( insertable );
		prop.setUpdateable( updatable );

		// this is already handled for collections in CollectionBinder...
		if ( Collection.class.isInstance( value ) ) {
			prop.setOptimisticLocked( ( (Collection) value ).isOptimisticLocked() );
		}
		else {
			final OptimisticLock lockAnn = property != null
					? property.getAnnotation( OptimisticLock.class )
					: null;
			if ( lockAnn != null ) {
				//TODO this should go to the core as a mapping validation checking
				if ( lockAnn.excluded() && (
						property.isAnnotationPresent( javax.persistence.Version.class )
								|| property.isAnnotationPresent( Id.class )
								|| property.isAnnotationPresent( EmbeddedId.class ) ) ) {
					throw new AnnotationException(
							"@OptimisticLock.exclude=true incompatible with @Id, @EmbeddedId and @Version: "
									+ StringHelper.qualify( holder.getPath(), name )
					);
				}
			}
			final boolean isOwnedValue = !isToOneValue( value ) || insertable; // && updatable as well???
			final boolean includeInOptimisticLockChecks = ( lockAnn != null )
					? ! lockAnn.excluded()
					: isOwnedValue;
			prop.setOptimisticLocked( includeInOptimisticLockChecks );
		}

		LOG.tracev( "Cascading {0} with {1}", name, cascade );
		this.mappingProperty = prop;
		return prop;
	}

	private boolean isCollection(Value value) {
		return Collection.class.isInstance( value );
	}

	private boolean isToOneValue(Value value) {
		return ToOne.class.isInstance( value );
	}

	public void setProperty(XProperty property) {
		this.property = property;
	}

	public void setReturnedClass(XClass returnedClass) {
		this.returnedClass = returnedClass;
	}

	public SimpleValueBinder getSimpleValueBinder() {
		return simpleValueBinder;
	}

	public Value getValue() {
		return value;
	}

	public void setId(boolean id) {
		this.isId = id;
	}

	public void setInheritanceStatePerClass(Map<XClass, InheritanceState> inheritanceStatePerClass) {
		this.inheritanceStatePerClass = inheritanceStatePerClass;
	}
}
