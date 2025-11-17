/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.jdbc.spi;

import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.hibernate.collection.spi.CollectionSemantics;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.collection.spi.PersistentMap;
import org.hibernate.internal.util.collections.IdentitySet;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.ValuedModelPart;
import org.hibernate.metamodel.mapping.internal.BasicValuedCollectionPart;
import org.hibernate.metamodel.mapping.internal.EmbeddedAttributeMapping;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.jdbc.JsonHelper;
import org.hibernate.type.format.JsonDocumentWriter;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;

import static org.hibernate.Hibernate.isInitialized;

/**
 * Extension of {@link JsonHelper} that can be used to generate
 * descriptive JSON output, that is more appropriate for representing
 * complex data structures for displaying purposes.
 */
public class DescriptiveJsonGeneratingVisitor extends JsonGeneratingVisitor {

	private Map<String, IdentitySet<Object>> circularityTracker;

	@Override
	protected void serializeEntity(Object value, EntityMappingType entityType, WrapperOptions options, JsonDocumentWriter writer) {
		final EntityIdentifierMapping identifierMapping = entityType.getIdentifierMapping();
		trackingEntity( value, entityType, shouldProcessEntity -> {
			try {
				writer.startObject();
				writer.objectKey( identifierMapping.getAttributeName() );
				serializeEntityIdentifier( value, identifierMapping, options, writer );
				if ( shouldProcessEntity ) {
					// if it wasn't already encountered, append all properties
					serializeObjectValues( entityType, value, options, writer );
				}
				writer.endObject();
			}
			catch (IOException e) {
				throw new UncheckedIOException( "Error serializing entity", e );
			}
		} );
	}

	private void trackingEntity(Object entity, EntityMappingType entityType, Consumer<Boolean> action) {
		if ( circularityTracker == null ) {
			circularityTracker = new HashMap<>();
		}
		final IdentitySet<Object> entities = circularityTracker.computeIfAbsent(
				entityType.getEntityName(),
				k -> new IdentitySet<>()
		);
		final boolean added = entities.add( entity );
		action.accept( added );
		if ( added ) {
			entities.remove( entity );
		}
	}

	@Override
	protected boolean handleNullOrLazy(Object value, JsonDocumentWriter writer) {
		if ( value == null ) {
			writer.nullValue();
			return true;
		}
		else if ( value == LazyPropertyInitializer.UNFETCHED_PROPERTY ) {
			writer.stringValue( value.toString() );
			return true;
		}
		else if ( !isInitialized( value ) ) {
			writer.stringValue( "<uninitialized>" );
			return true;
		}
		else {
			return false;
		}
	}

	@Override
	protected void serializeModelPart(
			ValuedModelPart modelPart,
			Object value,
			WrapperOptions options,
			JsonDocumentWriter writer) throws IOException {
		// Extended version of default method that always expands embeddable
		// objects and can handle entities and plural attributes
		if ( modelPart instanceof SelectableMapping ) {
			writer.objectKey( modelPart.getPartName() );
			visit( modelPart.getMappedType(), value, options, writer );
		}
		else if ( modelPart instanceof EmbeddedAttributeMapping embeddedAttribute ) {
			writer.objectKey( embeddedAttribute.getAttributeName() );
			visit( embeddedAttribute.getMappedType(), value, options, writer );
		}
		else if ( modelPart instanceof EntityValuedModelPart entityPart ) {
			writer.objectKey( entityPart.getPartName() );
			visit( entityPart.getEntityMappingType(), value, options, writer );
		}
		else if ( modelPart instanceof PluralAttributeMapping plural ) {
			writer.objectKey( plural.getPartName() );
			serializePluralAttribute( value, plural, options, writer );
		}
		else {
			// could not handle model part, throw exception
			throw new UnsupportedOperationException(
					"Support for model part type not yet implemented: "
					+ (modelPart != null ? modelPart.getClass().getName() : "null")
			);
		}
	}

	private void serializePluralAttribute(
			Object value,
			PluralAttributeMapping plural,
			WrapperOptions options,
			JsonDocumentWriter writer) throws IOException {
		if ( handleNullOrLazy( value, writer ) ) {
			// nothing left to do
			return;
		}

		final CollectionPart element = plural.getElementDescriptor();
		final CollectionSemantics<?, ?> collectionSemantics = plural.getMappedType().getCollectionSemantics();
		switch ( collectionSemantics.getCollectionClassification() ) {
			case MAP:
			case SORTED_MAP:
			case ORDERED_MAP:
				serializePersistentMap(
						(PersistentMap<?, ?>) value,
						plural.getIndexDescriptor(),
						element,
						options,
						writer
				);
				break;
			default:
				serializePersistentCollection(
						(PersistentCollection<?>) value,
						plural.getCollectionDescriptor(),
						element,
						options,
						writer
				);
		}
	}

	/**
	 * Serializes a persistent map to JSON [{key: ..., value: ...}, ...]
	 */
	private <K, E> void serializePersistentMap(
			PersistentMap<K, E> map,
			CollectionPart key,
			CollectionPart value,
			WrapperOptions options,
			JsonDocumentWriter writer) throws IOException {
		writer.startArray();
		for ( final Map.Entry<K, E> entry : map.entrySet() ) {
			writer.startObject();
			writer.objectKey( "key" );
			serializeCollectionPart( entry.getKey(), key, options, writer );
			writer.objectKey( "value" );
			serializeCollectionPart( entry.getValue(), value, options, writer );
			writer.endObject();
		}
		writer.endArray();
	}

	/**
	 * Serializes a persistent collection to a JSON array
	 */
	private <E> void serializePersistentCollection(
			PersistentCollection<E> collection,
			CollectionPersister persister,
			CollectionPart element,
			WrapperOptions options,
			JsonDocumentWriter appender) throws IOException {
		appender.startArray();
		final Iterator<?> entries = collection.entries( persister );
		while ( entries.hasNext() ) {
			serializeCollectionPart( entries.next(), element, options, appender );
		}
		appender.endArray();
	}

	private void serializeCollectionPart(
			Object value,
			CollectionPart collectionPart,
			WrapperOptions options,
			JsonDocumentWriter appender) throws IOException {
		if ( collectionPart instanceof BasicValuedCollectionPart basic ) {
			appender.serializeJsonValue( value, basic.getJavaType(), basic.getJdbcMapping().getJdbcType(), options );
		}
		else {
			visit( collectionPart.getMappedType(), value, options, appender );
		}
	}
}
