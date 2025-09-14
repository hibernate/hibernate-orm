/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.AssertionFailure;
import org.hibernate.annotations.CollectionType;
import org.hibernate.annotations.ManyToAny;
import org.hibernate.annotations.MapKeyType;
import org.hibernate.boot.model.convert.spi.ConverterAutoApplyHandler;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Table;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.TypeDetails;

import jakarta.persistence.Convert;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.MapKeyClass;
import jakarta.persistence.MapKeyEnumerated;
import jakarta.persistence.MapKeyTemporal;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Temporal;

import static org.hibernate.internal.CoreMessageLogger.CORE_LOGGER;
import static org.hibernate.internal.util.StringHelper.isEmpty;
import static org.hibernate.internal.util.StringHelper.isNotEmpty;

/**
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
public class CollectionPropertyHolder extends AbstractPropertyHolder {

	private final Collection collection;

	// assume true, the constructor will opt out where appropriate
	private boolean canElementBeConverted = true;
	private boolean canKeyBeConverted = true;

	private final Map<String,AttributeConversionInfo> elementAttributeConversionInfoMap;
	private final Map<String,AttributeConversionInfo> keyAttributeConversionInfoMap;

	public CollectionPropertyHolder(
			Collection collection,
			String path,
			ClassDetails clazzToProcess,
			MemberDetails property,
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
			MemberDetails collectionProperty,
			boolean isComposite,
			Map<String,AttributeConversionInfo> elementAttributeConversionInfoMap,
			Map<String,AttributeConversionInfo> keyAttributeConversionInfoMap) {
		collectionProperty.forEachAnnotationUsage( Convert.class, getSourceModelContext(),
				usage -> applyLocalConvert(
						usage,
						collectionProperty,
						isComposite,
						elementAttributeConversionInfoMap,
						keyAttributeConversionInfoMap
				) );
	}

	private void applyLocalConvert(
			Convert convertAnnotation,
			MemberDetails collectionProperty,
			boolean isComposite,
			Map<String,AttributeConversionInfo> elementAttributeConversionInfoMap,
			Map<String,AttributeConversionInfo> keyAttributeConversionInfoMap) {

		// IMPL NOTE: the rules here are quite more lenient than what JPA says. For example, JPA says that @Convert
		// on a Map of basic types should default to "value" but it should explicitly specify 'attributeName' of "key"
		// (or prefixed with "key." for embedded paths) to be applied on the key. However, we try to see if conversion
		// of either is disabled for whatever reason. For example, if the Map is annotated with @Enumerated, the
		// elements cannot be converted, and so any @Convert likely meant the key, so we apply it to the key

		final var info = new AttributeConversionInfo( convertAnnotation, collectionProperty );
		final String attributeName = info.getAttributeName();
		if ( collection.isMap() ) {
			logSpecNoncompliance( attributeName, collection.getRole() );
		}

		if ( isEmpty( attributeName ) ) {
			// the @Convert did not name an attribute...
			if ( canElementBeConverted && canKeyBeConverted ) {
				if ( !isComposite ) {
					// if element is of basic type default to "value"
					elementAttributeConversionInfoMap.put( "", info );
				}
				else {
					throwMissingAttributeName();
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
				keyPath = removePrefix( attributeName, "key" );
				elementPath = removePrefix( attributeName, "value" );

				if ( keyPath == null && elementPath == null ) {
					// specified attributeName needs to have 'key.' or 'value.' prefix
					throwMissingAttributeName();
				}
			}
			else if ( canKeyBeConverted ) {
				keyPath = removePrefix( attributeName, "key", attributeName );
				elementPath = null;
			}
			else {
				keyPath = null;
				elementPath = removePrefix( attributeName, "value", attributeName );
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
								attributeName,
								collection.getRole()
						)
				);
			}
		}
	}

	private void throwMissingAttributeName() {
		throw new IllegalStateException( "'@Convert' annotation for map [" + collection.getRole()
									+ "] must specify 'attributeName=\"key\"' or 'attributeName=\"value\"'" );
	}

	private static void logSpecNoncompliance(String attributeName, String role) {
		final boolean specCompliant = isNotEmpty( attributeName )
				&& (attributeName.startsWith( "key" ) || attributeName.startsWith( "value" ) );
		if ( !specCompliant ) {
			CORE_LOGGER.nonCompliantMapConversion( role );
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
		else if ( path.startsWith(prefix + ".") ) {
			return path.substring( prefix.length() + 1 );
		}
		else {
			return defaultValue;
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
	public void startingProperty(MemberDetails property) {
		// for now, nothing to do...
	}

	@Override
	protected AttributeConversionInfo locateAttributeConversionInfo(MemberDetails attributeMember) {
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
	public void addProperty(Property prop, MemberDetails memberDetails, ClassDetails declaringClass) {
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
	public void addProperty(Property prop, MemberDetails memberDetails, @Nullable AnnotatedColumns columns, ClassDetails declaringClass) {
		//Ejb3Column.checkPropertyConsistency( ); //already called earlier
		throw new AssertionFailure( "addProperty to a join table of a collection: does it make sense?" );
	}

	@Override
	public Join addJoin(JoinTable joinTableAnn, boolean noDelayInPkColumnCreation) {
		throw new AssertionFailure( "Add join in a second pass" );
	}

	@Override
	public Join addJoin(JoinTable joinTableAnn, Table table, boolean noDelayInPkColumnCreation) {
		throw new AssertionFailure( "Add join in a second pass" );
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "(" + collection.getRole() + ")";
	}

	boolean prepared;

	public void prepare(MemberDetails collectionProperty, boolean isComposite) {
		// fugly
		if ( prepared ) {
			return;
		}

		if ( collectionProperty == null ) {
			return;
		}

		prepared = true;

		if ( collection.isMap() ) {
			if ( collectionProperty.hasDirectAnnotationUsage( MapKeyEnumerated.class ) ) {
				canKeyBeConverted = false;
			}
			else if ( collectionProperty.hasDirectAnnotationUsage( MapKeyTemporal.class ) ) {
				canKeyBeConverted = false;
			}
			else if ( collectionProperty.hasDirectAnnotationUsage( MapKeyClass.class ) ) {
				canKeyBeConverted = false;
			}
			else if ( collectionProperty.hasDirectAnnotationUsage( MapKeyType.class ) ) {
				canKeyBeConverted = false;
			}
		}
		else {
			canKeyBeConverted = false;
		}

		if ( collectionProperty.hasDirectAnnotationUsage( ManyToAny.class ) ) {
			canElementBeConverted = false;
		}
		else if ( collectionProperty.hasDirectAnnotationUsage( OneToMany.class ) ) {
			canElementBeConverted = false;
		}
		else if ( collectionProperty.hasDirectAnnotationUsage( ManyToMany.class ) ) {
			canElementBeConverted = false;
		}
		else if ( collectionProperty.hasDirectAnnotationUsage( Enumerated.class ) ) {
			canElementBeConverted = false;
		}
		else if ( collectionProperty.hasDirectAnnotationUsage( Temporal.class ) ) {
			canElementBeConverted = false;
		}
		else if ( collectionProperty.hasDirectAnnotationUsage( CollectionType.class ) ) {
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

	public ConverterDescriptor<?,?> resolveElementAttributeConverterDescriptor(
			MemberDetails memberDetails,
			ClassDetails classDetails) {
		final AttributeConversionInfo info = locateAttributeConversionInfo( "element" );
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
		return getAttributeConverterAutoApplyHandler()
				.findAutoApplyConverterForCollectionElement( memberDetails, getContext() );
	}

	public ConverterDescriptor<?,?> mapKeyAttributeConverterDescriptor(
			MemberDetails memberDetails,
			TypeDetails keyTypeDetails) {
		final var info = locateAttributeConversionInfo( "key" );
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
		return getAttributeConverterAutoApplyHandler()
				.findAutoApplyConverterForMapKey( memberDetails, getContext() );
	}

	private ConverterAutoApplyHandler getAttributeConverterAutoApplyHandler() {
		return getContext().getMetadataCollector()
				.getConverterRegistry()
				.getAttributeConverterAutoApplyHandler();
	}
}
