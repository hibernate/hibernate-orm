/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import jakarta.persistence.FetchType;
import jakarta.persistence.CascadeType;
import org.hibernate.MappingException;
import org.hibernate.boot.mapping.internal.materialize.UniqueKeyMappingMaterializer;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.engine.FetchStyle;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.AttributeContainer;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.TypeDetails;

import java.lang.annotation.Annotation;
import java.util.EnumSet;
import java.util.Set;
import java.util.StringTokenizer;

import static org.hibernate.internal.util.StringHelper.isEmpty;
import static org.hibernate.internal.util.StringHelper.qualifier;
import static org.hibernate.models.spi.TypeDetailsHelper.resolveRawClass;

/**
 * @author Emmanuel Bernard
 */
public class BinderHelper {
	private static final UniqueKeyMappingMaterializer UNIQUE_KEY_MAPPING_MATERIALIZER =
			new UniqueKeyMappingMaterializer();


	private BinderHelper() {
	}

	public static final Set<String> PRIMITIVE_NAMES = Set.of(
			byte.class.getName(),
			short.class.getName(),
			int.class.getName(),
			long.class.getName(),
			float.class.getName(),
			double.class.getName(),
			char.class.getName(),
			boolean.class.getName()
	);

	public static boolean isPrimitive(String elementTypeName) {
		return PRIMITIVE_NAMES.contains( elementTypeName );
	}

	/**
	 * Retrieve the property by path in a recursive way, including IdentifierProperty in the loop
	 * If propertyName is null or empty, the IdentifierProperty is returned
	 */
	public static Property findPropertyByName(PersistentClass associatedClass, String propertyName) {
		final var idProperty = associatedClass.getIdentifierProperty();
		final String idName = idProperty == null ? null : idProperty.getName();
		try {
			return isEmpty( propertyName ) || propertyName.equals( idName )
					? idProperty // Default to id
					: findProperty( associatedClass, propertyName, idProperty, idName );
		}
		catch ( MappingException e ) {
			try {
				// if we do not find it, try to check the identifier mapper
				return findPropertyUsingIdMapper( associatedClass, propertyName );
			}
			catch ( MappingException ee ) {
				return null;
			}
		}
	}

	/**
	 * Retrieve the property by path in a recursive way
	 */
	public static Property findPropertyByName(Component component, String propertyName) {
		try {
			return isEmpty( propertyName )
					? null // Do not expect to use a primary key for this case
					: findProperty( component, propertyName, null );
		}
		catch (MappingException e) {
			try {
				// if we do not find it, try to check the identifier mapper
				return findPropertyUsingIdMapper( component.getOwner(), propertyName );
			}
			catch (MappingException ee) {
				return null;
			}
		}
	}

	private static Property findProperty(
			PersistentClass associatedClass, String propertyName,
			Property idProperty, String idName) {
		Property property;
		// Handle id property
		final String name;
		if ( propertyName.indexOf( idName + "." ) == 0 ) {
			property = idProperty;
			name = propertyName.substring( idName.length() + 1 );
		}
		else {
			property = null;
			name = propertyName;
		}
		return findProperty( associatedClass, name, property );
	}

	private static Property findProperty(AttributeContainer root, String name, Property property) {
		final var tokens = new StringTokenizer( name, ".", false );
		while ( tokens.hasMoreTokens() ) {
			final String element = tokens.nextToken();
			if ( property == null ) {
				property = root.getProperty( element );
			}
			else if ( property.isComposite() ) {
				final var value = (Component) property.getValue();
				property = value.getProperty( element );
			}
			else {
				return null;
			}
		}
		return property;
	}

	private static Property findPropertyUsingIdMapper(PersistentClass associatedClass, String propertyName) {
		final var identifierMapper = associatedClass.getIdentifierMapper();
		return identifierMapper == null ? null : findProperty( identifierMapper, propertyName, null );
	}

	/**
	 * Find the column owner (ie PersistentClass or Join) of columnName.
	 * If columnName is null or empty, persistentClass is returned
	 */
	public static AttributeContainer findColumnOwner(
			PersistentClass persistentClass,
			String columnName,
			MetadataBuildingContext context) {
		final var metadataCollector = context.getMetadataCollector();
		PersistentClass current = persistentClass;
		while ( current != null ) {
			try {
				metadataCollector.getPhysicalColumnName( current.getTable(), columnName );
				return current;
			}
			catch (MappingException me) {
				//swallow it
			}
			for ( Join join : current.getJoins() ) {
				try {
					metadataCollector.getPhysicalColumnName( join.getTable(), columnName );
					return join;
				}
				catch (MappingException me) {
					//swallow it
				}
			}
			current = current.getSuperclass();
		}
		return null;
	}

	public static FetchStyle getFetchStyle(FetchType fetch) {
		return switch ( fetch ) {
			case EAGER -> FetchStyle.JOIN;
			case LAZY -> FetchStyle.SELECT;
			case DEFAULT -> throw new UnsupportedOperationException( "Not implemented yet - decide how to handle" );
		};
	}

	public static String renderCascadeTypeList(EnumSet<CascadeType> cascadeTypes) {
		return renderCascadeTypeList( cascadeTypes, false );
	}

	public static String renderCascadeTypeList(EnumSet<CascadeType> cascadeTypes, boolean orphanRemoval) {
		final var cascade = new StringBuilder();
		for ( var cascadeType : cascadeTypes ) {
			cascade.append( "," );
			cascade.append( switch ( cascadeType ) {
				case ALL -> "all";
				case PERSIST -> "persist";
				case MERGE -> "merge";
				case REFRESH -> "refresh";
				case DETACH -> "evict";
				case REMOVE -> "delete";
			} );
		}
		if ( orphanRemoval ) {
			cascade.append( ",delete-orphan" );
		}
		return cascade.isEmpty() ? "none" : cascade.substring(1);
	}

	static boolean isGlobalGeneratorNameGlobal(MetadataBuildingContext context) {
		return context.getJpaCompliance().isGlobalGeneratorScopeEnabled();
	}

	public static boolean isDefault(ClassDetails clazz) {
		return clazz == ClassDetails.VOID_CLASS_DETAILS;
	}

	public static boolean isDefault(TypeDetails clazz) {
		return resolveRawClass( clazz ) == ClassDetails.VOID_CLASS_DETAILS;
	}

	/**
	 * Extract an annotation from the package-info for the package the given class is defined in
	 *
	 * @param annotationType The type of annotation to return
	 * @param classDetails The class in the package
	 * @param context The processing context
	 *
	 * @return The annotation or {@code null}
	 */
	public static <A extends Annotation> A extractFromPackage(
			Class<A> annotationType,
			ClassDetails classDetails,
			MetadataBuildingContext context) {

// todo (soft-delete) : or if we want caching of this per package
//  +
//				final SoftDelete fromPackage = context.getMetadataCollector().resolvePackageAnnotation( packageName, SoftDelete.class );
//  +
//		where context.getMetadataCollector() can cache some of this - either the annotations themselves
//		or even just the XPackage resolutions

		final String packageName = qualifier( classDetails.getName() );
		if ( isEmpty( packageName ) ) {
			return null;
		}
		else {
			final var modelsContext = context.getModelsContext();
			try {
				return modelsContext.getClassDetailsRegistry()
						.resolveClassDetails( packageName + ".package-info" )
						.getAnnotationUsage( annotationType, modelsContext );
			}
			catch (ClassLoadingException ignore) {
				return null;
			}
		}
	}
}
