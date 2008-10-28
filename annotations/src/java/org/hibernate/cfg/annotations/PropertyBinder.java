//$Id$
package org.hibernate.cfg.annotations;

import javax.persistence.EmbeddedId;
import javax.persistence.Id;

import org.hibernate.AnnotationException;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.OptimisticLock;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.cfg.Ejb3Column;
import org.hibernate.cfg.ExtendedMappings;
import org.hibernate.cfg.PropertyHolder;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.PropertyGeneration;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Value;
import org.hibernate.util.StringHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Emmanuel Bernard
 */
public class PropertyBinder {
	private Logger log = LoggerFactory.getLogger( PropertyBinder.class );
	private String name;
	private String returnedClassName;
	private boolean lazy;
	private String propertyAccessorName;
	private Ejb3Column[] columns;
	private PropertyHolder holder;
	private ExtendedMappings mappings;
	private Value value;
	private boolean insertable = true;
	private boolean updatable = true;
	private String cascade;
	/*
	 * property can be null
	 * prefer propertyName to property.getName() since some are overloaded
	 */
	private XProperty property;
	private XClass returnedClass;

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

	public void setPropertyAccessorName(String propertyAccessorName) {
		this.propertyAccessorName = propertyAccessorName;
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

	public void setMappings(ExtendedMappings mappings) {
		this.mappings = mappings;
	}

	private void validateBind() {
		if (property.isAnnotationPresent(Immutable.class)) {
			throw new AnnotationException("@Immutable on property not allowed. " +
					"Only allowed on entity level or on a collection.");
		}	
	}

	private void validateMake() {
		//TODO check necessary params for a make
	}

	public Property bind() {
		validateBind();
		log.debug( "binding property {} with lazy={}", name, lazy );
		String containerClassName = holder == null ?
				null :
				holder.getClassName();
		SimpleValueBinder value = new SimpleValueBinder();
		value.setMappings( mappings );
		value.setPropertyName( name );
		value.setReturnedClassName( returnedClassName );
		value.setColumns( columns );
		value.setPersistentClassName( containerClassName );
		value.setType( property, returnedClass );
		value.setMappings( mappings );
		SimpleValue propertyValue = value.make();
		setValue( propertyValue );
		Property prop = make();
		holder.addProperty( prop, columns );
		return prop;
	}

	public Property make() {
		validateMake();
		log.debug( "Building property " + name );
		Property prop = new Property();
		prop.setName( name );
		prop.setNodeName( name );
		prop.setValue( value );
		prop.setLazy( lazy );
		prop.setCascade( cascade );
		prop.setPropertyAccessorName( propertyAccessorName );
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
					throw new AnnotationException( "@Generated(INSERT) on a @Version property not allowed, use ALWAYS: "
							+ StringHelper.qualify( holder.getPath(), name ) );
				}
				insertable = false;
				if ( GenerationTime.ALWAYS.equals( generated ) ) {
					updatable = false;
				}
				prop.setGeneration( PropertyGeneration.parse( generated.toString().toLowerCase() ) );
			}
		}
		NaturalId naturalId = property != null ?
				property.getAnnotation( NaturalId.class ) :
				null;
		if ( naturalId != null ) {
			if ( !naturalId.mutable() ) {
				updatable = false;
			}
			prop.setNaturalIdentifier( true );
		}
		prop.setInsertable( insertable );
		prop.setUpdateable( updatable );
		OptimisticLock lockAnn = property != null ?
				property.getAnnotation( OptimisticLock.class ) :
				null;
		if ( lockAnn != null ) {
			prop.setOptimisticLocked( !lockAnn.excluded() );
			//TODO this should go to the core as a mapping validation checking
			if ( lockAnn.excluded() && (
					property.isAnnotationPresent( javax.persistence.Version.class )
							|| property.isAnnotationPresent( Id.class )
							|| property.isAnnotationPresent( EmbeddedId.class ) ) ) {
				throw new AnnotationException( "@OptimisticLock.exclude=true incompatible with @Id, @EmbeddedId and @Version: "
						+ StringHelper.qualify( holder.getPath(), name ) );
			}
		}
		log.trace( "Cascading " + name + " with " + cascade );
		return prop;
	}

	public void setProperty(XProperty property) {
		this.property = property;
	}

	public void setReturnedClass(XClass returnedClass) {
		this.returnedClass = returnedClass;
	}
}
