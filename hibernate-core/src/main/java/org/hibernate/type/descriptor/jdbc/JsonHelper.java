/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.jdbc;


import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Array;
import java.sql.SQLException;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.Internal;
import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.hibernate.collection.spi.CollectionSemantics;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.collection.spi.PersistentMap;
import org.hibernate.internal.build.AllowReflection;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.internal.util.collections.StandardStack;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.CompositeIdentifierMapping;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.ValuedModelPart;
import org.hibernate.metamodel.mapping.internal.BasicValuedCollectionPart;
import org.hibernate.metamodel.mapping.internal.EmbeddedAttributeMapping;
import org.hibernate.metamodel.mapping.internal.SingleAttributeIdentifierMapping;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.type.BasicPluralType;
import org.hibernate.type.BasicType;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.BasicPluralJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.format.JsonDocumentItemType;
import org.hibernate.type.format.JsonDocumentReader;
import org.hibernate.type.format.JsonDocumentWriter;

import static org.hibernate.Hibernate.isInitialized;
import static org.hibernate.type.descriptor.jdbc.StructHelper.getSubPart;
import static org.hibernate.type.descriptor.jdbc.StructHelper.instantiate;
import org.hibernate.type.format.JsonValueJDBCTypeAdapter;
import org.hibernate.type.format.JsonValueJDBCTypeAdapterFactory;
import org.hibernate.type.format.StringJsonDocumentReader;

/**
 * A Helper for serializing and deserializing JSON, based on an {@link org.hibernate.metamodel.mapping.EmbeddableMappingType}.
 *
 * @author Christian Beikov
 * @author Emmanuel Jannetti
 */
@Internal
public class JsonHelper {

	/**
	 * Serializes an array of values into JSON object/array
	 * @param elementMappingType the type definitions
	 * @param values the values to be serialized
	 * @param options wrapping options
	 * @param writer the document writer used for serialization
	 */
	public static void serializeArray(MappingType elementMappingType, Object[] values, WrapperOptions options, JsonDocumentWriter writer) {
		writer.startArray();
		if ( values.length == 0 ) {
			writer.endArray();
			return;
		}
		for ( Object value : values ) {
			try {
				serializeValue(elementMappingType, value, options, writer);
			}
			catch (IOException e) {
				throw new IllegalArgumentException( "Could not serialize JSON array value" , e );
			}
		}
		writer.endArray();
	}

	/**
	 * Serializes an array of values into JSON object/array
	 * @param elementJavaType the array element type
	 * @param elementJdbcType the JDBC type
	 * @param values values to be serialized
	 * @param options wrapping options
	 * @param writer the document writer used for serialization
	 */
	public static void serializeArray(JavaType<?> elementJavaType, JdbcType elementJdbcType, Object[] values, WrapperOptions options, JsonDocumentWriter writer) {
		writer.startArray();
		if ( values.length == 0 ) {
			writer.endArray();
			return;
		}
		for ( Object value : values ) {
			if (value == null) {
				writer.nullValue();
			}
			else {
				writer.serializeJsonValue( value ,(JavaType<?>) elementJavaType,elementJdbcType,options);
			}
		}
		writer.endArray();
	}

	/**
	 * Checks that a <code>JDBCType</code> is assignable to an array
	 * @param type the jdbc type
	 * @return <code>true</code> if types is of array kind <code>false</code> otherwise.
	 */
	private static boolean isArrayType(JdbcType type) {
		return (type.getDefaultSqlTypeCode() == SqlTypes.ARRAY ||
				type.getDefaultSqlTypeCode() == SqlTypes.JSON_ARRAY);
	}

	public static void serializeValue(MappingType mappedType, Object value, WrapperOptions options, JsonDocumentWriter writer)
			throws IOException {
		if ( handleNullOrLazy( value, writer ) ) {
			// nothing left to do
			return;
		}

		if ( mappedType instanceof EntityMappingType entityType ) {
			serializeEntity( value, entityType, options, writer );
		}
		else if ( mappedType instanceof ManagedMappingType managedMappingType ) {
			serialize( managedMappingType, value, options, writer );
		}
		else if ( mappedType instanceof BasicType<?> basicType ) {
			if ( isArrayType( basicType.getJdbcType() ) ) {
				final int length = Array.getLength( value );
				writer.startArray();
				if ( length != 0 ) {
					//noinspection unchecked
					final JavaType<Object> elementJavaType = ((BasicPluralJavaType<Object>) basicType.getJdbcJavaType()).getElementJavaType();
					final JdbcType elementJdbcType = ((ArrayJdbcType) basicType.getJdbcType()).getElementJdbcType();
					final Object domainArray = basicType.convertToRelationalValue( value );
					for ( int j = 0; j < length; j++ ) {
						writer.serializeJsonValue( Array.get( domainArray, j ), elementJavaType, elementJdbcType, options );
					}
				}
				writer.endArray();
			}
			else {
				writer.serializeJsonValue(
						basicType.convertToRelationalValue( value ),
						basicType.getJdbcJavaType(),
						basicType.getJdbcType(),
						options
				);
			}
		}
		else {
			throw new UnsupportedOperationException(
					"Support for mapping type not yet implemented: " + mappedType.getClass().getName()
			);
		}
	}

	/**
	 * Checks the provided {@code value} is either null or a lazy property.
	 *
	 * @param value the value to check
	 * @param writer the current {@link JsonDocumentWriter}
	 *
	 * @return {@code true} if it was, indicating no further processing of the value is needed, {@code false otherwise}.
	 */
	private static boolean handleNullOrLazy(Object value, JsonDocumentWriter writer) {
		if ( value == null ) {
			writer.nullValue();
			return true;
		}
		else if ( writer.expandProperties() ) {
			// avoid force-initialization when serializing all properties
			if ( value == LazyPropertyInitializer.UNFETCHED_PROPERTY ) {
				writer.stringValue( value.toString() );
				return true;
			}
			else if ( !isInitialized( value ) ) {
				writer.stringValue( "<uninitialized>" );
				return true;
			}
		}
		return false;
	}

	/**
	 * Serialized an Object value to JSON object using a document writer.
	 *
	 * @param managedMappingType the managed mapping type of the given value
	 * @param value the value to be serialized
	 * @param options wrapping options
	 * @param writer the document writer
	 * @throws IOException if the underlying writer failed to serialize a mpped value or failed to perform need I/O.
	 */
	public static void serialize(ManagedMappingType managedMappingType, Object value, WrapperOptions options, JsonDocumentWriter writer)
			throws IOException {
		writer.startObject();
		serializeObject( managedMappingType, value, options, writer );
		writer.endObject();
	}

	/**
	 * JSON object managed type serialization.
	 *
	 * @param managedMappingType the managed mapping type of the given object
	 * @param object the object to be serialized
	 * @param options wrapping options
	 * @param writer the document writer
	 * @throws IOException if an error occurred while writing to an underlying writer
	 * @see #serialize(ManagedMappingType, Object, WrapperOptions, JsonDocumentWriter)
	 */
	private static void serializeObject(ManagedMappingType managedMappingType, Object object, WrapperOptions options, JsonDocumentWriter writer)
			throws IOException {
		final Object[] values = managedMappingType.getValues( object );
		for ( int i = 0; i < values.length; i++ ) {
			final ValuedModelPart subPart = getSubPart( managedMappingType, i );
			final Object value = values[i];
			serializeModelPart( subPart, value, options, writer );
		}
	}

	private static void serializeModelPart(
			ValuedModelPart modelPart,
			Object value,
			WrapperOptions options,
			JsonDocumentWriter writer) throws IOException {
		if ( modelPart instanceof SelectableMapping selectableMapping ) {
			writer.objectKey( writer.expandProperties() ? modelPart.getPartName() : selectableMapping.getSelectableName() );
			serializeValue( modelPart.getMappedType(), value, options, writer );
		}
		else if ( modelPart instanceof EmbeddedAttributeMapping embeddedAttribute ) {
			if ( writer.expandProperties() ) {
				writer.objectKey( embeddedAttribute.getAttributeName() );
				serializeValue( embeddedAttribute.getMappedType(), value, options, writer );
			}
			else {
				if ( value == null ) {
					return;
				}

				final EmbeddableMappingType mappingType = embeddedAttribute.getMappedType();
				final SelectableMapping aggregateMapping = mappingType.getAggregateMapping();
				if ( aggregateMapping == null ) {
					serializeObject( mappingType, value, options, writer );
				}
				else {
					final String name = aggregateMapping.getSelectableName();
					writer.objectKey( name );
					serializeValue( mappingType, value, options, writer );
				}
			}
		}
		else if ( writer.expandProperties() ) {
			// Entity and plural attribute serialization is only supported for expanded properties
			if ( modelPart instanceof EntityValuedModelPart entityPart ) {
				writer.objectKey( entityPart.getPartName() );
				serializeValue( entityPart.getEntityMappingType(), value, options, writer );
			}
			else if ( modelPart instanceof PluralAttributeMapping plural ) {
				writer.objectKey( plural.getPartName() );
				serializePluralAttribute( value, plural, options, writer );
			}
		}
		else {
			// could not handle model part, throw exception
			throw new UnsupportedOperationException(
					"Support for model part type not yet implemented: "
					+ (modelPart != null ? modelPart.getClass().getName() : "null")
			);
		}
	}

	private static void serializeEntity(
			Object value,
			EntityMappingType entityType,
			WrapperOptions options,
			JsonDocumentWriter writer) throws IOException {
		final EntityIdentifierMapping identifierMapping = entityType.getIdentifierMapping();
		writer.trackingEntity( value, entityType, shouldProcessEntity -> {
			try {
				writer.startObject();
				writer.objectKey( identifierMapping.getAttributeName() );
				serializeEntityIdentifier( value, identifierMapping, options, writer );
				if ( shouldProcessEntity ) {
					// if it wasn't already encountered, append all properties
					serializeObject( entityType, value, options, writer );
				}
				writer.endObject();
			}
			catch (IOException e) {
				throw new UncheckedIOException( "Error serializing entity", e );
			}
		} );
	}

	private static void serializeEntityIdentifier(
			Object value,
			EntityIdentifierMapping identifierMapping,
			WrapperOptions options,
			JsonDocumentWriter writer) throws IOException {
		final Object identifier = identifierMapping.getIdentifier( value );
		if ( identifierMapping instanceof SingleAttributeIdentifierMapping singleAttribute ) {
			writer.serializeJsonValue(
					identifier,
					singleAttribute.getJavaType(),
					singleAttribute.getSingleJdbcMapping().getJdbcType(),
					options
			);
		}
		else if ( identifier instanceof CompositeIdentifierMapping composite ) {
			serializeValue( composite.getMappedType(), identifier, options, writer );
		}
		else {
			throw new UnsupportedOperationException( "Unsupported identifier type: " + identifier.getClass().getName() );
		}
	}

	private static void serializePluralAttribute(
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
	private static <K, E> void serializePersistentMap(
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
	private static <E> void serializePersistentCollection(
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

	private static void serializeCollectionPart(
			Object value,
			CollectionPart collectionPart,
			WrapperOptions options,
			JsonDocumentWriter appender) throws IOException {
		if ( collectionPart instanceof BasicValuedCollectionPart basic ) {
			appender.serializeJsonValue( value, basic.getJavaType(), basic.getJdbcMapping().getJdbcType(), options );
		}
		else {
			serializeValue( collectionPart.getMappedType(), value, options, appender );
		}
	}

	/**
	 * Consumes Json document items from a document reader and return the serialized Objects
	 * @param reader the document reader
	 * @param embeddableMappingType the type definitions
	 * @param returnEmbeddable do we return an Embeddable object or array of Objects ?
	 * @param options wrapping options
	 * @return serialized values
	 * @param <X> the type of the returned value
	 * @throws SQLException if error occured during mapping of types
	 */
	private static <X> X consumeJsonDocumentItems(JsonDocumentReader reader, EmbeddableMappingType embeddableMappingType, boolean returnEmbeddable, WrapperOptions options)
			throws SQLException {
		record SelectableData(String selectableName, int selectableIndex, SelectableMapping selectableMapping){}
		record ParseLevel(
				@Nullable SelectableData selectableData,
				@Nullable EmbeddableMappingType embeddableMappingType,
				@Nullable BasicPluralType<?, ?> arrayType,
				@Nullable List<Object> subArrayObjectList,
				@Nullable Object [] objectArray
		) {
			ParseLevel(EmbeddableMappingType embeddableMappingType) {
				this(null, embeddableMappingType);
			}
			ParseLevel(@Nullable SelectableData selectableData, EmbeddableMappingType embeddableMappingType) {
				this(
						selectableData,
						embeddableMappingType,
						null,
						null,
						new Object[embeddableMappingType.getJdbcValueCount()+ ( embeddableMappingType.isPolymorphic() ? 1 : 0 )]
				);
			}
			ParseLevel(@Nullable SelectableData selectableData, BasicPluralType<?, ?> arrayType) {
				this( selectableData, null, arrayType, new ArrayList<>(), null );
			}

			public void addValue(@Nullable SelectableData selectableData, @Nullable Object value) {
				if ( embeddableMappingType != null ) {
					assert selectableData != null;
					objectArray[selectableData.selectableIndex] = value;
				}
				else {
					assert subArrayObjectList != null;
					subArrayObjectList.add(value);
				}
			}

			public JdbcMapping determineJdbcMapping(@Nullable SelectableData currentSelectableData) {
				if ( currentSelectableData != null ) {
					return currentSelectableData.selectableMapping.getJdbcMapping();
				}
				else if ( arrayType != null ) {
					return arrayType.getElementType();
				}
				else {
					assert selectableData != null;
					return selectableData.selectableMapping.getJdbcMapping();
				}
			}

			public static String determineSelectablePath(StandardStack<ParseLevel> parseLevel, @Nullable SelectableData currentSelectableData) {
				if ( currentSelectableData != null ) {
					return currentSelectableData.selectableName;
				}
				else {
					return determineSelectablePath( parseLevel, 0 );
				}
			}

			private static String determineSelectablePath(StandardStack<ParseLevel> stack, int level) {
				final ParseLevel parseLevel = stack.peek( level );
				assert parseLevel != null;
				if ( parseLevel.selectableData != null ) {
					return parseLevel.selectableData.selectableName;
				}
				else {
					assert parseLevel.arrayType != null;
					return determineSelectablePath( stack, level + 1 ) + ".{element}";
				}
			}
		}
		final StandardStack<ParseLevel> parseLevel = new StandardStack<>();
		final JsonValueJDBCTypeAdapter adapter = JsonValueJDBCTypeAdapterFactory.getAdapter(reader,returnEmbeddable);

		parseLevel.push(new ParseLevel( embeddableMappingType ));
		SelectableData currentSelectableData = null;

		// We loop on two conditions:
		//   - the parser still has tokens left
		//   - the type stack is not empty
		// Even if the reader has some tokens left, if the type stack is empty,
		// that means that we have to stop parsing. That may be the case while parsing an object of object array,
		// the array is not empty, but we ae done parsing that specific object.
		// When we encounter OBJECT_END the current type is popped out of the stack. When parsing one object of an array we may end up
		// having an empty stack. Next Objects are parsed in the next round.
		while(reader.hasNext() && !parseLevel.isEmpty()) {
			final ParseLevel currentLevel = parseLevel.getCurrent();
			assert currentLevel != null;
			switch (reader.next()) {
				case VALUE_KEY -> {
					final EmbeddableMappingType currentEmbeddableMappingType = currentLevel.embeddableMappingType;
					assert currentEmbeddableMappingType != null
							: "Value keys are only valid for objects";

					assert currentSelectableData == null;

					final String selectableName = reader.getObjectKeyName();
					final int selectableIndex = currentEmbeddableMappingType.getSelectableIndex( selectableName );
					if ( selectableIndex < 0 ) {
						throw new IllegalArgumentException(
								String.format(
										"Could not find selectable [%s] in embeddable type [%s] for JSON processing.",
										selectableName,
										currentEmbeddableMappingType.getMappedJavaType().getJavaTypeClass().getName()
								)
						);
					}
					final SelectableMapping selectableMapping =
							currentEmbeddableMappingType.getJdbcValueSelectable( selectableIndex );
					currentSelectableData = new SelectableData( selectableName, selectableIndex, selectableMapping );
				}
				case ARRAY_START -> {
					assert currentSelectableData != null;

					if ( !(currentSelectableData.selectableMapping.getJdbcMapping() instanceof BasicPluralType<?, ?> pluralType) ) {
						throw new IllegalArgumentException(
								String.format(
										"Can't parse JSON array for selectable [%s] which is not of type BasicPluralType.",
										ParseLevel.determineSelectablePath( parseLevel, currentSelectableData )
								)
						);
					}
					parseLevel.push( new ParseLevel( currentSelectableData, pluralType ) );
					currentSelectableData = null;
				}
				case ARRAY_END -> {
					assert currentLevel.arrayType != null;
					assert currentLevel.selectableData != null;

					parseLevel.pop();
					final ParseLevel parentLevel = parseLevel.getCurrent();

					assert parentLevel.embeddableMappingType != null;
					// flush array values
					parentLevel.addValue(
							currentLevel.selectableData,
							currentLevel.arrayType.getJdbcJavaType().wrap( currentLevel.subArrayObjectList, options )
					);
				}
				case OBJECT_START -> {
					final JdbcMapping jdbcMapping = currentLevel.determineJdbcMapping( currentSelectableData );

					if ( !(jdbcMapping.getJdbcType() instanceof AggregateJdbcType aggregateJdbcType) ) {
						throw new IllegalArgumentException(
								String.format(
										"Can't parse JSON object for selectable [%s] which is not of type AggregateJdbcType.",
										ParseLevel.determineSelectablePath( parseLevel, currentSelectableData )
								)
						);
					}
					parseLevel.push(
							new ParseLevel( currentSelectableData, aggregateJdbcType.getEmbeddableMappingType() ) );
					currentSelectableData = null;
				}
				case OBJECT_END -> {
					final EmbeddableMappingType currentEmbeddableMappingType = currentLevel.embeddableMappingType;
					assert currentEmbeddableMappingType != null;

					// go back in the mapping definition tree
					parseLevel.pop();
					final Object objectValue;
					if ( returnEmbeddable ) {
						final StructAttributeValues attributeValues = StructHelper.getAttributeValues(
								embeddableMappingType,
								currentLevel.objectArray,
								options
						);
						objectValue = instantiate( embeddableMappingType, attributeValues );
					}
					else {
						objectValue = currentLevel.objectArray;
					}
					if ( parseLevel.isEmpty() ) {
						//noinspection unchecked
						return (X) objectValue;
					}
					else {
						parseLevel.getCurrent().addValue( currentLevel.selectableData, objectValue );
					}
				}
				case NULL_VALUE -> {
					currentLevel.addValue( currentSelectableData, null );
					currentSelectableData = null;
				}
				case NUMERIC_VALUE -> {
					final JdbcMapping jdbcMapping = currentLevel.determineJdbcMapping( currentSelectableData );
					currentLevel.addValue(
							currentSelectableData,
							adapter.fromNumericValue(
									jdbcMapping.getJdbcJavaType(),
									jdbcMapping.getJdbcType(),
									reader,
									options
							)
					);
					currentSelectableData = null;
				}
				case BOOLEAN_VALUE -> {
					currentLevel.addValue(
							currentSelectableData,
							reader.getBooleanValue() ? Boolean.TRUE : Boolean.FALSE
					);
					currentSelectableData = null;
				}
				case VALUE -> {
					final JdbcMapping jdbcMapping = currentLevel.determineJdbcMapping( currentSelectableData );
					currentLevel.addValue(
							currentSelectableData,
							adapter.fromValue(
									jdbcMapping.getJdbcJavaType(),
									jdbcMapping.getJdbcType(),
									reader,
									options
							)
					);
					currentSelectableData = null;
				}
			}
		}
		throw new IllegalArgumentException( "Expected JSON object end, but none found." );
	}

	/**
	 * Deserialize a JSON value to Java Object
	 * @param embeddableMappingType the mapping type
	 * @param reader the JSON reader
	 * @param returnEmbeddable do we return an Embeddable object or array of Objects
	 * @param options wrappping options
	 * @return the deserialized value
	 * @param <X>
	 * @throws SQLException
	 */
	public static <X> X deserialize(
			EmbeddableMappingType embeddableMappingType,
			JsonDocumentReader reader,
			boolean returnEmbeddable,
			WrapperOptions options) throws SQLException {
		final JsonDocumentItemType event;
		if ( !reader.hasNext() || ( event = reader.next() ) == JsonDocumentItemType.NULL_VALUE ) {
			return null;
		}
		if ( event != JsonDocumentItemType.OBJECT_START ) {
			throw new IllegalArgumentException("Malformed JSON. Expected object but got: " + event);
		}
		final X result = consumeJsonDocumentItems( reader, embeddableMappingType, returnEmbeddable, options );
		assert !reader.hasNext();
		return result;
	}


	// This is also used by Hibernate Reactive
	public static <X> X arrayFromString(
			JavaType<X> javaType,
			JdbcType elementJdbcType,
			String string,
			WrapperOptions options) throws SQLException {
		if ( string == null ) {
			return null;
		}
		return deserializeArray( javaType, elementJdbcType, new StringJsonDocumentReader( string ), options );
	}

	public static <X> X deserializeArray(
			JavaType<X> javaType,
			JdbcType elementJdbcType,
			JsonDocumentReader reader,
			WrapperOptions options) throws SQLException {
		final JsonDocumentItemType event;
		if ( !reader.hasNext() || ( event = reader.next() ) == JsonDocumentItemType.NULL_VALUE ) {
			return null;
		}
		if ( event != JsonDocumentItemType.ARRAY_START ) {
			throw new IllegalArgumentException("Malformed JSON. Expected array but got: " + event);
		}

		final CustomArrayList arrayList = new CustomArrayList();
		final JavaType<?> elementJavaType = ((BasicPluralJavaType<?>) javaType).getElementJavaType();
		final Class<?> preferredJavaTypeClass = elementJdbcType.getPreferredJavaTypeClass( options );
		final JavaType<?> jdbcJavaType;
		if ( preferredJavaTypeClass == null || preferredJavaTypeClass == elementJavaType.getJavaTypeClass() ) {
			jdbcJavaType = elementJavaType;
		}
		else {
			jdbcJavaType = options.getTypeConfiguration().getJavaTypeRegistry().resolveDescriptor( preferredJavaTypeClass );
		}

		final JsonValueJDBCTypeAdapter adapter = JsonValueJDBCTypeAdapterFactory.getAdapter(reader,false);
		while(reader.hasNext()) {
			JsonDocumentItemType type = reader.next();
			switch ( type ) {
				case ARRAY_END:
					assert !reader.hasNext();
					return javaType.wrap( arrayList, options );
				case NULL_VALUE:
					arrayList.add( null );
					break;
				case NUMERIC_VALUE:
					arrayList.add( adapter.fromNumericValue(jdbcJavaType, elementJdbcType ,reader, options)  );
					break;
				case BOOLEAN_VALUE:
					arrayList.add( reader.getBooleanValue() ? Boolean.TRUE : Boolean.FALSE );
					break;
				case VALUE:
					arrayList.add( adapter.fromValue(jdbcJavaType, elementJdbcType ,reader, options) );
					break;
				case OBJECT_START:
					assert elementJdbcType instanceof JsonJdbcType;
					final EmbeddableMappingType embeddableMappingType = ((JsonJdbcType) elementJdbcType).getEmbeddableMappingType();
					arrayList.add( consumeJsonDocumentItems(reader, embeddableMappingType, true, options) );
					break;
				default:
					throw new UnsupportedOperationException( "Unexpected JSON type " + type );
			}
		}

		throw new IllegalArgumentException( "Expected JSON array end, but none found." );
	}


	private static class CustomArrayList extends AbstractCollection<Object> implements Collection<Object> {
		Object[] array = ArrayHelper.EMPTY_OBJECT_ARRAY;
		int size;

		public void ensureCapacity(int minCapacity) {
			int oldCapacity = array.length;
			if ( minCapacity > oldCapacity ) {
				int newCapacity = oldCapacity + ( oldCapacity >> 1 );
				newCapacity = Math.max( Math.max( newCapacity, minCapacity ), 10 );
				array = Arrays.copyOf( array, newCapacity );
			}
		}

		public Object[] getUnderlyingArray() {
			return array;
		}

		@Override
		public int size() {
			return size;
		}

		@Override
		public boolean add(Object o) {
			if ( size == array.length ) {
				ensureCapacity( size + 1 );
			}
			array[size++] = o;
			return true;
		}

		@Override
		public boolean isEmpty() {
			return size == 0;
		}

		@Override
		public boolean contains(Object o) {
			for ( int i = 0; i < size; i++ ) {
				if ( Objects.equals(o, array[i] ) ) {
					return true;
				}
			}
			return false;
		}

		@Override
		public Iterator<Object> iterator() {
			return new Iterator<>() {
				int index;
				@Override
				public boolean hasNext() {
					return index != size;
				}

				@Override
				public Object next() {
					if ( index == size ) {
						throw new NoSuchElementException();
					}
					return array[index++];
				}
			};
		}

		@Override
		public Object[] toArray() {
			return Arrays.copyOf( array, size );
		}

		@Override
		@AllowReflection // We need the ability to create arrays of requested types dynamically.
		public <T> T[] toArray(T[] a) {
			//noinspection unchecked
			final T[] r = a.length >= size
					? a
					: (T[]) java.lang.reflect.Array.newInstance( a.getClass().getComponentType(), size );
			for (int i = 0; i < size; i++) {
				//noinspection unchecked
				r[i] = (T) array[i];
			}
			return null;
		}
	}

}
