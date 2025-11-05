/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.model.internal;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.hibernate.AssertionFailure;
import org.hibernate.annotations.CollectionType;
import org.hibernate.annotations.ManyToAny;
import org.hibernate.annotations.MapKeyType;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Table;

import jakarta.persistence.Convert;
import jakarta.persistence.Converts;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.MapKeyClass;
import jakarta.persistence.MapKeyEnumerated;
import jakarta.persistence.MapKeyTemporal;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Temporal;

import static org.hibernate.internal.util.StringHelper.isEmpty;
import static org.hibernate.internal.util.StringHelper.isNotEmpty;

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

	private final Map<String,AttributeConversionInfo> elementAttributeConversionInfoMap;
	private final Map<String,AttributeConversionInfo> keyAttributeConversionInfoMap;

	public CollectionPropertyHolder(
			Collection collection,
			String path,
			XClass clazzToProcess,
			XProperty property,
			PropertyHolder parentPropertyHolder,
			MetadataBuildingContext context) {
		super( path, parentPropertyHolder, clazzToProcess, context );
		this.collection = collection;
		setCurrentProperty( property );

		this.elementAttributeConversionInfoMap = new HashMap<>();
		this.keyAttributeConversionInfoMap = new HashMap<>();
	}

	public Collection getCollectionBinding() {
		return collection;
	}

	private void buildAttributeConversionInfoMaps(
			XProperty collectionProperty,
			boolean isComposite,
			Map<String,AttributeConversionInfo> elementAttributeConversionInfoMap,
			Map<String,AttributeConversionInfo> keyAttributeConversionInfoMap) {
		if ( collectionProperty == null ) {
			// not sure this is valid condition
			return;
		}

		{
			final Convert convertAnnotation = collectionProperty.getAnnotation( Convert.class );
			if ( convertAnnotation != null ) {
				applyLocalConvert(
						convertAnnotation,
						collectionProperty,
						isComposite,
						elementAttributeConversionInfoMap,
						keyAttributeConversionInfoMap
				);
			}
		}

		{
			final Converts convertsAnnotation = collectionProperty.getAnnotation( Converts.class );
			if ( convertsAnnotation != null ) {
				for ( Convert convertAnnotation : convertsAnnotation.value() ) {
					applyLocalConvert(
							convertAnnotation,
							collectionProperty,
							isComposite,
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
			boolean isComposite,
			Map<String,AttributeConversionInfo> elementAttributeConversionInfoMap,
			Map<String,AttributeConversionInfo> keyAttributeConversionInfoMap) {

		// IMPL NOTE : the rules here are quite more lenient than what JPA says.  For example, JPA says that @Convert
		// on a Map of basic types should default to "value" but it should explicitly specify attributeName of "key"
		// (or prefixed with "key." for embedded paths) to be applied on the key.  However, we try to see if conversion
		// of either is disabled for whatever reason.  For example, if the Map is annotated with @Enumerated the
		// elements cannot be converted so any @Convert likely meant the key, so we apply it to the key

		final AttributeConversionInfo info = new AttributeConversionInfo( convertAnnotation, collectionProperty );
		if ( collection.isMap() ) {
			boolean specCompliant = isNotEmpty( info.getAttributeName() )
					&& ( info.getAttributeName().startsWith( "key" )
					|| info.getAttributeName().startsWith( "value" ) );
			if ( !specCompliant ) {
				log.nonCompliantMapConversion( collection.getRole() );
			}
		}

		if ( isEmpty( info.getAttributeName() ) ) {
			// the @Convert did not name an attribute...
			if ( canElementBeConverted && canKeyBeConverted ) {
				if ( !isComposite ) {
					// if element is of basic type default to "value"
					elementAttributeConversionInfoMap.put( "", info );
				}
				else {
					throw new IllegalStateException(
							"@Convert placed on Map attribute [" + collection.getRole()
									+ "] of non-basic types must define attributeName of 'key' or 'value'" );
				}
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
			// the @Convert named an attribute...

			// we have different "resolution rules" based on whether element and key can be converted
			final String keyPath;
			final String elementPath;

			if ( canElementBeConverted && canKeyBeConverted ) {
				keyPath = removePrefix( info.getAttributeName(), "key" );
				elementPath = removePrefix( info.getAttributeName(), "value" );

				if ( keyPath == null && elementPath == null ) {
					// specified attributeName needs to have 'key.' or 'value.' prefix
					throw new IllegalStateException(
							"@Convert placed on Map attribute [" + collection.getRole()
									+ "] must define attributeName of 'key' or 'value'"
					);
				}
			}
			else if ( canKeyBeConverted ) {
				keyPath = removePrefix( info.getAttributeName(), "key", info.getAttributeName() );
				elementPath = null;
			}
			else {
				keyPath = null;
				elementPath = removePrefix( info.getAttributeName(), "value", info.getAttributeName() );
			}

			if ( keyPath != null ) {
				keyAttributeConversionInfoMap.put( keyPath, info );
			}
			else if ( elementPath != null ) {
				elementAttributeConversionInfoMap.put( elementPath, info );
			}
			else {
				// specified attributeName needs to have 'key.' or 'value.' prefix
				throw new IllegalStateException(
						String.format(
								Locale.ROOT,
								"Could not determine how to apply @Convert(attributeName='%s') to collection [%s]",
								info.getAttributeName(),
								collection.getRole()
						)
				);
			}
		}
	}

	/**
	 * Check if path has the given prefix and remove it.
	 *
	 * @param path Path.
	 * @param prefix Prefix.
	 * @return Path without prefix, or null, if path did not have the prefix.
	 */
	private String removePrefix(String path, String prefix) {
		return removePrefix( path, prefix, null );
	}

	private String removePrefix(String path, String prefix, String defaultValue) {
		if ( path.equals(prefix) ) {
			return "";
		}

		if (path.startsWith(prefix + ".")) {
			return path.substring( prefix.length() + 1 );
		}

		return defaultValue;
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
		// for now, nothing to do...
	}

	@Override
	protected AttributeConversionInfo locateAttributeConversionInfo(XProperty property) {
		// nothing to do
		return null;
	}

	@Override
	protected AttributeConversionInfo locateAttributeConversionInfo(String path) {
		final String key = removePrefix( path, "key" );
		if ( key != null ) {
			return keyAttributeConversionInfoMap.get( key );
		}

		final String element = removePrefix( path, "element" );
		if ( element != null ) {
			return elementAttributeConversionInfoMap.get( element );
		}

		return elementAttributeConversionInfoMap.get( path );
	}

	@Override
	public String getClassName() {
		throw new AssertionFailure( "Collection property holder does not have a class name" );
	}

	@Override
	public String getEntityOwnerClassName() {
		return null;
	}

	@Override
	public Table getTable() {
		return collection.getCollectionTable();
	}

	@Override
	public void addProperty(Property prop, XClass declaringClass) {
		throw new AssertionFailure( "Cannot add property to a collection" );
	}

	@Override
	public void movePropertyToJoin(Property prop, Join join, XClass declaringClass) {
		throw new AssertionFailure( "Cannot add property to a collection" );
	}

	@Override
	public KeyValue getIdentifier() {
		throw new AssertionFailure( "Identifier collection not yet managed" );
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
	public PersistentClass getPersistentClass() {
		return collection.getOwner();
	}

	@Override
	public boolean isComponent() {
		return false;
	}

	@Override
	public boolean isEntity() {
		return false;
	}

	@Override
	public String getEntityName() {
		return collection.getOwner().getEntityName();
	}

	@Override
	public void addProperty(Property prop, AnnotatedColumns columns, XClass declaringClass) {
		//Ejb3Column.checkPropertyConsistency( ); //already called earlier
		throw new AssertionFailure( "addProperty to a join table of a collection: does it make sense?" );
	}

	@Override
	public Join addJoin(JoinTable joinTableAnn, boolean noDelayInPkColumnCreation) {
		throw new AssertionFailure( "Add a <join> in a second pass" );
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "(" + collection.getRole() + ")";
	}

	boolean prepared;

	public void prepare(XProperty collectionProperty, boolean isComposite) {
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
			buildAttributeConversionInfoMaps(
					collectionProperty,
					isComposite,
					elementAttributeConversionInfoMap,
					keyAttributeConversionInfoMap
			);
		}
	}

	public ConverterDescriptor resolveElementAttributeConverterDescriptor(XProperty collectionXProperty, XClass elementXClass) {
		AttributeConversionInfo info = locateAttributeConversionInfo( "element" );
		if ( info != null ) {
			if ( info.isConversionDisabled() ) {
				return null;
			}
			else {
				try {
					return makeAttributeConverterDescriptor( info );
				}
				catch (Exception e) {
					throw buildExceptionFromInstantiationError( info, e );
				}
			}
		}

		log.debugf(
				"Attempting to locate auto-apply AttributeConverter for collection element [%s]",
				collection.getRole()
		);

		// todo : do we need to pass along `XClass elementXClass`?

		return getContext().getMetadataCollector()
				.getConverterRegistry()
				.getAttributeConverterAutoApplyHandler()
				.findAutoApplyConverterForCollectionElement( collectionXProperty, getContext() );
	}

	public ConverterDescriptor mapKeyAttributeConverterDescriptor(XProperty mapXProperty, XClass keyXClass) {
		AttributeConversionInfo info = locateAttributeConversionInfo( "key" );
		if ( info != null ) {
			if ( info.isConversionDisabled() ) {
				return null;
			}
			else {
				try {
					return makeAttributeConverterDescriptor( info );
				}
				catch (Exception e) {
					throw buildExceptionFromInstantiationError( info, e );
				}
			}
		}

		log.debugf(
				"Attempting to locate auto-apply AttributeConverter for collection key [%s]",
				collection.getRole()
		);

		// todo : do we need to pass along `XClass keyXClass`?

		return getContext().getMetadataCollector()
				.getConverterRegistry()
				.getAttributeConverterAutoApplyHandler()
				.findAutoApplyConverterForMapKey( mapXProperty, getContext() );
	}

}
