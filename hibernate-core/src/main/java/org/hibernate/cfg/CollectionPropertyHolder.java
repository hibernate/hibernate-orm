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
package org.hibernate.cfg;

import java.util.HashMap;
import java.util.Map;
import javax.persistence.Convert;
import javax.persistence.Converts;
import javax.persistence.Enumerated;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.MapKeyClass;
import javax.persistence.MapKeyEnumerated;
import javax.persistence.MapKeyTemporal;
import javax.persistence.OneToMany;
import javax.persistence.Temporal;

import org.hibernate.AssertionFailure;
import org.hibernate.annotations.CollectionType;
import org.hibernate.annotations.ManyToAny;
import org.hibernate.annotations.MapKeyType;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.IndexedCollection;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Table;

/**
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
public class CollectionPropertyHolder extends AbstractPropertyHolder {
	private static final CoreMessageLogger log = CoreLogging.messageLogger( CollectionPropertyHolder.class );

	private final Collection collection;

	// assume true, the constructor will opt out where appropriate
	private boolean canElementBeConverted = true;
	private boolean canKeyBeConverted = true;

	private Map<String,AttributeConversionInfo> elementAttributeConversionInfoMap;
	private Map<String,AttributeConversionInfo> keyAttributeConversionInfoMap;

	public CollectionPropertyHolder(
			Collection collection,
			String path,
			XClass clazzToProcess,
			XProperty property,
			PropertyHolder parentPropertyHolder,
			Mappings mappings) {
		super( path, parentPropertyHolder, clazzToProcess, mappings );
		this.collection = collection;
		setCurrentProperty( property );

		this.elementAttributeConversionInfoMap = new HashMap<String, AttributeConversionInfo>();
		this.keyAttributeConversionInfoMap = new HashMap<String, AttributeConversionInfo>();
	}

	private void buildAttributeConversionInfoMaps(
			XProperty collectionProperty,
			Map<String,AttributeConversionInfo> elementAttributeConversionInfoMap,
			Map<String,AttributeConversionInfo> keyAttributeConversionInfoMap) {
		if ( collectionProperty == null ) {
			// not sure this is valid condition
			return;
		}

		{
			final Convert convertAnnotation = collectionProperty.getAnnotation( Convert.class );
			if ( convertAnnotation != null ) {
				applyLocalConvert( convertAnnotation, collectionProperty, elementAttributeConversionInfoMap, keyAttributeConversionInfoMap );
			}
		}

		{
			final Converts convertsAnnotation = collectionProperty.getAnnotation( Converts.class );
			if ( convertsAnnotation != null ) {
				for ( Convert convertAnnotation : convertsAnnotation.value() ) {
					applyLocalConvert(
							convertAnnotation,
							collectionProperty,
							elementAttributeConversionInfoMap,
							keyAttributeConversionInfoMap
					);
				}
			}
		}
	}

	private void applyLocalConvert(
			Convert convertAnnotation,
			XProperty collectionProperty,
			Map<String,AttributeConversionInfo> elementAttributeConversionInfoMap,
			Map<String,AttributeConversionInfo> keyAttributeConversionInfoMap) {

		// IMPL NOTE : the rules here are quite more lenient than what JPA says.  For example, JPA says
		// that @Convert on a Map always needs to specify attributeName of key/value (or prefixed with
		// key./value. for embedded paths).  However, we try to see if conversion of either is disabled
		// for whatever reason.  For example, if the Map is annotated with @Enumerated the elements cannot
		// be converted so any @Convert likely meant the key, so we apply it to the key

		final AttributeConversionInfo info = new AttributeConversionInfo( convertAnnotation, collectionProperty );
		if ( collection.isMap() ) {
			boolean specCompliant = StringHelper.isNotEmpty( info.getAttributeName() )
					&& ( info.getAttributeName().startsWith( "key" )
					|| info.getAttributeName().startsWith( "value" ) );
			if ( !specCompliant ) {
				log.nonCompliantMapConversion( collection.getRole() );
			}
		}

		if ( StringHelper.isEmpty( info.getAttributeName() ) ) {
			if ( canElementBeConverted && canKeyBeConverted ) {
				throw new IllegalStateException(
						"@Convert placed on Map attribute [" + collection.getRole()
								+ "] must define attributeName of 'key' or 'value'"
				);
			}
			else if ( canKeyBeConverted ) {
				keyAttributeConversionInfoMap.put( "", info );
			}
			else if ( canElementBeConverted ) {
				elementAttributeConversionInfoMap.put( "", info );
			}
			// if neither, we should not be here...
		}
		else {
			if ( canElementBeConverted && canKeyBeConverted ) {
				// specified attributeName needs to have 'key.' or 'value.' prefix
				if ( info.getAttributeName().startsWith( "key." ) ) {
					keyAttributeConversionInfoMap.put(
							info.getAttributeName().substring( 4 ),
							info
					);
				}
				else if ( info.getAttributeName().startsWith( "value." ) ) {
					elementAttributeConversionInfoMap.put(
							info.getAttributeName().substring( 6 ),
							info
					);
				}
				else {
					throw new IllegalStateException(
							"@Convert placed on Map attribute [" + collection.getRole()
									+ "] must define attributeName of 'key' or 'value'"
					);
				}
			}
		}
	}

	@Override
	protected String normalizeCompositePath(String attributeName) {
		return attributeName;
	}

	@Override
	protected String normalizeCompositePathForLogging(String attributeName) {
		return collection.getRole() + '.' + attributeName;
	}

	@Override
	public void startingProperty(XProperty property) {
		if ( property == null ) {
			return;
		}

		// todo : implement (and make sure it gets called - for handling collections of composites)
	}

	@Override
	protected AttributeConversionInfo locateAttributeConversionInfo(XProperty property) {
		if ( canElementBeConverted && canKeyBeConverted ) {
			// need to decide whether 'property' refers to key/element
			// todo : this may not work for 'basic collections' since there is no XProperty for the element
		}
		else if ( canKeyBeConverted ) {

		}
		else {
			return null;
		}

		return null;
	}

	@Override
	protected AttributeConversionInfo locateAttributeConversionInfo(String path) {
		// todo : implement
		return null;
	}

	public String getClassName() {
		throw new AssertionFailure( "Collection property holder does not have a class name" );
	}

	public String getEntityOwnerClassName() {
		return null;
	}

	public Table getTable() {
		return collection.getCollectionTable();
	}

	public void addProperty(Property prop, XClass declaringClass) {
		throw new AssertionFailure( "Cannot add property to a collection" );
	}

	public KeyValue getIdentifier() {
		throw new AssertionFailure( "Identifier collection not yet managed" );
	}

	public boolean isOrWithinEmbeddedId() {
		return false;
	}

	public PersistentClass getPersistentClass() {
		return collection.getOwner();
	}

	public boolean isComponent() {
		return false;
	}

	public boolean isEntity() {
		return false;
	}

	public String getEntityName() {
		return collection.getOwner().getEntityName();
	}

	public void addProperty(Property prop, Ejb3Column[] columns, XClass declaringClass) {
		//Ejb3Column.checkPropertyConsistency( ); //already called earlier
		throw new AssertionFailure( "addProperty to a join table of a collection: does it make sense?" );
	}

	public Join addJoin(JoinTable joinTableAnn, boolean noDelayInPkColumnCreation) {
		throw new AssertionFailure( "Add a <join> in a second pass" );
	}

	@Override
	public String toString() {
		return super.toString() + "(" + collection.getRole() + ")";
	}

	boolean prepared;

	public void prepare(XProperty collectionProperty) {
		// fugly
		if ( prepared ) {
			return;
		}

		if ( collectionProperty == null ) {
			return;
		}

		prepared = true;

		if ( collection.isMap() ) {
			if ( collectionProperty.isAnnotationPresent( MapKeyEnumerated.class ) ) {
				canKeyBeConverted = false;
			}
			else if ( collectionProperty.isAnnotationPresent( MapKeyTemporal.class ) ) {
				canKeyBeConverted = false;
			}
			else if ( collectionProperty.isAnnotationPresent( MapKeyClass.class ) ) {
				canKeyBeConverted = false;
			}
			else if ( collectionProperty.isAnnotationPresent( MapKeyType.class ) ) {
				canKeyBeConverted = false;
			}
		}
		else {
			canKeyBeConverted = false;
		}

		if ( collectionProperty.isAnnotationPresent( ManyToAny.class ) ) {
			canElementBeConverted = false;
		}
		else if ( collectionProperty.isAnnotationPresent( OneToMany.class ) ) {
			canElementBeConverted = false;
		}
		else if ( collectionProperty.isAnnotationPresent( ManyToMany.class ) ) {
			canElementBeConverted = false;
		}
		else if ( collectionProperty.isAnnotationPresent( Enumerated.class ) ) {
			canElementBeConverted = false;
		}
		else if ( collectionProperty.isAnnotationPresent( Temporal.class ) ) {
			canElementBeConverted = false;
		}
		else if ( collectionProperty.isAnnotationPresent( CollectionType.class ) ) {
			canElementBeConverted = false;
		}

		// Is it valid to reference a collection attribute in a @Convert attached to the owner (entity) by path?
		// if so we should pass in 'clazzToProcess' also
		if ( canKeyBeConverted || canElementBeConverted ) {
			buildAttributeConversionInfoMaps( collectionProperty, elementAttributeConversionInfoMap, keyAttributeConversionInfoMap );
		}
	}

	public AttributeConverterDefinition resolveElementAttributeConverterDefinition(XClass elementXClass) {
		AttributeConversionInfo info = locateAttributeConversionInfo( "element" );
		if ( info != null ) {
			if ( info.isConversionDisabled() ) {
				return null;
			}
			else {
				try {
					return makeAttributeConverterDefinition( info );
				}
				catch (Exception e) {
					throw new IllegalStateException(
							String.format( "Unable to instantiate AttributeConverter [%s", info.getConverterClass().getName() ),
							e
					);
				}
			}
		}

		log.debugf(
				"Attempting to locate auto-apply AttributeConverter for collection element [%s]",
				collection.getRole()
		);

		final Class elementClass = determineElementClass( elementXClass );
		if ( elementClass != null ) {
			for ( AttributeConverterDefinition attributeConverterDefinition : getMappings().getAttributeConverters() ) {
				if ( ! attributeConverterDefinition.isAutoApply() ) {
					continue;
				}
				log.debugf(
						"Checking auto-apply AttributeConverter [%s] type [%s] for match [%s]",
						attributeConverterDefinition.toString(),
						attributeConverterDefinition.getEntityAttributeType().getSimpleName(),
						elementClass.getSimpleName()
				);
				if ( areTypeMatch( attributeConverterDefinition.getEntityAttributeType(), elementClass ) ) {
					return attributeConverterDefinition;
				}
			}
		}

		return null;
	}

	private Class determineElementClass(XClass elementXClass) {
		if ( elementXClass != null ) {
			try {
				return getMappings().getReflectionManager().toClass( elementXClass );
			}
			catch (Exception e) {
				log.debugf(
						"Unable to resolve XClass [%s] to Class for collection elements [%s]",
						elementXClass.getName(),
						collection.getRole()
				);
			}
		}

		if ( collection.getElement() != null ) {
			if ( collection.getElement().getType() != null ) {
				return collection.getElement().getType().getReturnedClass();
			}
		}

		// currently this is called from paths where the element type really should be known,
		// so log the fact that we could not resolve the collection element info
		log.debugf(
				"Unable to resolve element information for collection [%s]",
				collection.getRole()
		);
		return null;
	}

	public AttributeConverterDefinition keyElementAttributeConverterDefinition(XClass keyXClass) {
		AttributeConversionInfo info = locateAttributeConversionInfo( "key" );
		if ( info != null ) {
			if ( info.isConversionDisabled() ) {
				return null;
			}
			else {
				try {
					return makeAttributeConverterDefinition( info );
				}
				catch (Exception e) {
					throw new IllegalStateException(
							String.format( "Unable to instantiate AttributeConverter [%s", info.getConverterClass().getName() ),
							e
					);
				}
			}
		}

		log.debugf(
				"Attempting to locate auto-apply AttributeConverter for collection key [%s]",
				collection.getRole()
		);

		final Class elementClass = determineKeyClass( keyXClass );
		if ( elementClass != null ) {
			for ( AttributeConverterDefinition attributeConverterDefinition : getMappings().getAttributeConverters() ) {
				if ( ! attributeConverterDefinition.isAutoApply() ) {
					continue;
				}
				log.debugf(
						"Checking auto-apply AttributeConverter [%s] type [%s] for match [%s]",
						attributeConverterDefinition.toString(),
						attributeConverterDefinition.getEntityAttributeType().getSimpleName(),
						elementClass.getSimpleName()
				);
				if ( areTypeMatch( attributeConverterDefinition.getEntityAttributeType(), elementClass ) ) {
					return attributeConverterDefinition;
				}
			}
		}

		return null;
	}

	private Class determineKeyClass(XClass keyXClass) {
		if ( keyXClass != null ) {
			try {
				return getMappings().getReflectionManager().toClass( keyXClass );
			}
			catch (Exception e) {
				log.debugf(
						"Unable to resolve XClass [%s] to Class for collection key [%s]",
						keyXClass.getName(),
						collection.getRole()
				);
			}
		}

		final IndexedCollection indexedCollection = (IndexedCollection) collection;
		if ( indexedCollection.getIndex() != null ) {
			if ( indexedCollection.getIndex().getType() != null ) {
				return indexedCollection.getIndex().getType().getReturnedClass();
			}
		}

		// currently this is called from paths where the element type really should be known,
		// so log the fact that we could not resolve the collection element info
		log.debugf(
				"Unable to resolve key information for collection [%s]",
				collection.getRole()
		);
		return null;
	}
}
