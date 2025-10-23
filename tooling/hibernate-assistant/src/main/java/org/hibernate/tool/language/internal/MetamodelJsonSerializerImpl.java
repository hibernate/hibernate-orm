/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.language.internal;

import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.tool.language.spi.MetamodelSerializer;
import org.hibernate.type.format.StringJsonDocumentWriter;

import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.EmbeddableType;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.IdentifiableType;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.MapAttribute;
import jakarta.persistence.metamodel.MappedSuperclassType;
import jakarta.persistence.metamodel.Metamodel;
import jakarta.persistence.metamodel.PluralAttribute;
import jakarta.persistence.metamodel.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of {@link MetamodelSerializer} that represents the {@link Metamodel} as a JSON array of mapped objects.
 */
public class MetamodelJsonSerializerImpl implements MetamodelSerializer {
	public static MetamodelJsonSerializerImpl INSTANCE = new MetamodelJsonSerializerImpl();

	/**
	 * Utility method that generates a JSON string representation of the mapping information
	 * contained in the provided {@link Metamodel metamodel} instance. The representation
	 * does not follow a strict scheme, and is more akin to natural language, as it's
	 * mainly meant for consumption by a LLM.
	 *
	 * @param metamodel the metamodel instance containing information on the persistence structures
	 *
	 * @return the JSON representation of the provided {@link Metamodel metamodel}
	 */
	@Override
	public String toString(Metamodel metamodel) {
		final List<Map<String, Object>> entities = new ArrayList<>();
		final List<Map<String, Object>> embeddables = new ArrayList<>();
		final List<Map<String, Object>> mappedSupers = new ArrayList<>();
		for ( ManagedType<?> managedType : metamodel.getManagedTypes() ) {
			switch ( managedType.getPersistenceType() ) {
				case ENTITY -> entities.add( getEntityTypeDescription( (EntityType<?>) managedType ) );
				case EMBEDDABLE -> embeddables.add( getEmbeddableTypeDescription( (EmbeddableType<?>) managedType ) );
				case MAPPED_SUPERCLASS -> mappedSupers.add( getMappedSuperclassTypeDescription( (MappedSuperclassType<?>) managedType ) );
				default ->
						throw new IllegalStateException( "Unexpected persistence type for managed type [" + managedType + "]" );
			}
		}
		return toJson( Map.of(
				"entities", entities,
				"mappedSuperclasses", mappedSupers,
				"embeddables", embeddables
		) );
	}

	private static String toJson(Map<String, Object> map) {
		if ( map.isEmpty() ) {
			return "{}";
		}

		final StringJsonDocumentWriter writer = new StringJsonDocumentWriter( new StringBuilder() );
		toJson( map, writer );
		return writer.toString();
	}

	private static void toJson(Object value, StringJsonDocumentWriter writer) {
		if ( value instanceof String strValue ) {
			writer.stringValue( strValue );
		}
		else if ( value instanceof Boolean boolValue ) {
			writer.booleanValue( boolValue );
		}
		else if ( value instanceof Number numValue ) {
			writer.numericValue( numValue );
		}
		else if ( value instanceof Map<?, ?> map ) {
			writer.startObject();
			for ( final var entry : map.entrySet() ) {
				writer.objectKey( entry.getKey().toString() );
				toJson( entry.getValue(), writer );
			}
			writer.endObject();
		}
		else if ( value instanceof Collection<?> collection ) {
			writer.startArray();
			for ( final var item : collection ) {
				toJson( item, writer );
			}
			writer.endArray();
		}
		else if ( value == null ) {
			writer.nullValue();
		}
		else {
			throw new IllegalArgumentException( "Unsupported value type: " + value.getClass().getName() );
		}
	}

	private static void putIfNotNull(Map<String, Object> map, String key, Object value) {
		if ( value != null ) {
			map.put( key, value );
		}
	}

	private static <T> Map<String, Object> getEntityTypeDescription(EntityType<T> entityType) {
		final Map<String, Object> map = new HashMap<>( 5 );
		map.put( "name", entityType.getName() );
		map.put( "class", entityType.getJavaType().getTypeName() );
		putIfNotNull( map, "superType", superTypeDescriptor( (ManagedDomainType<?>) entityType ) );
		putIfNotNull( map, "identifierAttribute", identifierDescriptor( entityType ) );
		map.put( "attributes", attributeArray( entityType.getAttributes() ) );
		return map;
	}

	private static String superTypeDescriptor(ManagedDomainType<?> managedType) {
		final var superType = managedType.getSuperType();
		return superType != null ? superType.getJavaType().getTypeName() : null;
	}

	private static <T> Map<String, Object> getMappedSuperclassTypeDescription(MappedSuperclassType<T> mappedSuperclass) {
		final Class<T> javaType = mappedSuperclass.getJavaType();
		final Map<String, Object> map = new HashMap<>( 5 );
		map.put( "name", javaType.getSimpleName() );
		map.put( "class", javaType.getTypeName() );
		putIfNotNull( map, "superType", superTypeDescriptor( (ManagedDomainType<?>) mappedSuperclass ) );
		putIfNotNull( map, "identifierAttribute", identifierDescriptor( mappedSuperclass ) );
		map.put( "attributes", attributeArray( mappedSuperclass.getAttributes() ) );
		return map;
	}

	private static <T> String identifierDescriptor(IdentifiableType<T> identifiableType) {
		final Type<?> idType = identifiableType.getIdType();
		if ( idType != null ) {
			final var id = identifiableType.getId( idType.getJavaType() );
			return id.getName();
		}
		else {
			return null;
		}
	}

	private static <T> Map<String, Object> getEmbeddableTypeDescription(EmbeddableType<T> embeddableType) {
		final Class<T> javaType = embeddableType.getJavaType();
		final Map<String, Object> map = new HashMap<>( 4 );
		map.put( "name", javaType.getSimpleName() );
		map.put( "class", javaType.getTypeName() );
		putIfNotNull( map, "superType", superTypeDescriptor( (ManagedDomainType<?>) embeddableType ) );
		map.put( "attributes", attributeArray( embeddableType.getAttributes() ) );
		return map;
	}

	private static <T> List<Map<String, String>> attributeArray(Set<Attribute<? super T, ?>> attributes) {
		if ( attributes.isEmpty() ) {
			return List.of();
		}

		return attributes.stream().map( attribute -> {
			final String name = attribute.getName();
			String type = attribute.getJavaType().getTypeName();
			// add key and element types for plural attributes
			if ( attribute instanceof PluralAttribute<?, ?, ?> pluralAttribute ) {
				type += "<";
				final var collectionType = pluralAttribute.getCollectionType();
				if ( collectionType == PluralAttribute.CollectionType.MAP ) {
					type += ( (MapAttribute<?, ?, ?>) pluralAttribute ).getKeyJavaType().getTypeName() + ",";
				}
				type += pluralAttribute.getElementType().getJavaType().getTypeName() + ">";
			}
			return Map.of(
					"type", type,
					"name", name
			);
		} ).toList();
	}
}
