/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.jdbc;


import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.sql.SQLException;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

import org.hibernate.Internal;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.internal.util.collections.StandardStack;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.ValuedModelPart;
import org.hibernate.metamodel.mapping.internal.EmbeddedAttributeMapping;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.type.BasicPluralType;
import org.hibernate.type.BasicType;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.BasicPluralJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.AggregateJdbcType;
import org.hibernate.type.descriptor.jdbc.ArrayJdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.format.JsonDocumentItemType;
import org.hibernate.type.format.JsonDocumentReader;
import org.hibernate.type.format.JsonDocumentReaderFactory;
import org.hibernate.type.format.JsonDocumentWriter;
import static org.hibernate.type.descriptor.jdbc.StructHelper.getEmbeddedPart;
import static org.hibernate.type.descriptor.jdbc.StructHelper.instantiate;
import org.hibernate.type.format.JsonValueJDBCTypeAdapter;
import org.hibernate.type.format.JsonValueJDBCTypeAdapterFactory;

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
				if (value == null) {
					writer.nullValue();
				}
				else if ( elementMappingType instanceof EmbeddableMappingType ) {
					JsonHelper.serialize( (EmbeddableMappingType) elementMappingType, value, options, writer );
				} else if ( elementMappingType instanceof BasicType<?> ) {
					//noinspection unchecked
					final BasicType<Object> basicType = (BasicType<Object>) elementMappingType;

					if ( isArrayType(basicType.getJdbcType())) {
						final int length = Array.getLength( value );
						if ( length != 0 ) {
							//noinspection unchecked
							final JavaType<Object> elementJavaType = ( (BasicPluralJavaType<Object>) basicType.getJdbcJavaType() ).getElementJavaType();
							final JdbcType elementJdbcType = ( (ArrayJdbcType) basicType.getJdbcType() ).getElementJdbcType();
							final Object domainArray = basicType.convertToRelationalValue( value );
							for ( int j = 0; j < length; j++ ) {
								writer.serializeJsonValue(Array.get(domainArray,j), elementJavaType, elementJdbcType, options);
							}
						}
					}
					else {
						writer.serializeJsonValue(basicType.convertToRelationalValue( value),
								(JavaType<Object>)basicType.getJdbcJavaType(),basicType.getJdbcType(), options);
					}
				}
				else {
					throw new UnsupportedOperationException( "Support for mapping type not yet implemented: " + elementMappingType.getClass().getName() );
				}
			}
			catch (IOException e) {
				// TODO : do better than this
				throw new RuntimeException( e );
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
				writer.serializeJsonValue( value ,(JavaType<Object>) elementJavaType,elementJdbcType,options);
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

	/**
	 * Serialized an Object value to JSON object using a document writer.
	 *
	 * @param embeddableMappingType the embeddable mapping definition of the given value.
	 * @param domainValue the value to be serialized.
	 * @param options wrapping options
	 * @param writer the document writer
	 * @throws IOException if the underlying writer failed to serialize a mpped value or failed to perform need I/O.
	 */
	public static void serialize(EmbeddableMappingType embeddableMappingType,
										Object domainValue, WrapperOptions options, JsonDocumentWriter writer) throws IOException {
		writer.startObject();
		serializeMapping(embeddableMappingType, domainValue, options, writer);
		writer.endObject();
	}

	/**
	 * JSON object attirbute serialization
	 * @see #serialize(EmbeddableMappingType, Object, WrapperOptions, JsonDocumentWriter)
	 * @param embeddableMappingType the embeddable mapping definition of the given value.
	 * @param domainValue the value to be serialized.
	 * @param options wrapping options
	 * @param writer the document writer
	 * @throws IOException if an error occurred while writing to an underlying writer
	 */
	private static void serializeMapping(EmbeddableMappingType embeddableMappingType,
								Object domainValue, WrapperOptions options, JsonDocumentWriter writer) throws IOException {
		final Object[] values = embeddableMappingType.getValues( domainValue );
		for ( int i = 0; i < values.length; i++ ) {
			final ValuedModelPart attributeMapping = getEmbeddedPart( embeddableMappingType, i );
			if ( attributeMapping instanceof SelectableMapping ) {
				final String name = ( (SelectableMapping) attributeMapping ).getSelectableName();
				writer.objectKey( name );
				if (values[i] == null) {
					writer.nullValue();
				}
				else if (attributeMapping.getMappedType() instanceof BasicType<?>) {
					final BasicType<Object> basicType = (BasicType<Object>) attributeMapping.getMappedType();
					if ( isArrayType(basicType.getJdbcType())) {
						final int length = Array.getLength( values[i] );
						writer.startArray();
						if ( length != 0 ) {
							//noinspection unchecked
							final JavaType<Object> elementJavaType = ( (BasicPluralJavaType<Object>) basicType.getJdbcJavaType() ).getElementJavaType();
							final JdbcType elementJdbcType = ( (ArrayJdbcType) basicType.getJdbcType() ).getElementJdbcType();
							final Object domainArray = basicType.convertToRelationalValue(   values[i] );
							for ( int j = 0; j < length; j++ ) {
								writer.serializeJsonValue(Array.get(domainArray,j), elementJavaType, elementJdbcType, options);
							}
						}
						writer.endArray();
					}
					else {
						writer.serializeJsonValue(basicType.convertToRelationalValue( values[i]),
								(JavaType<Object>)basicType.getJdbcJavaType(),basicType.getJdbcType(), options);
					}
				}
				else if ( attributeMapping.getMappedType() instanceof EmbeddableMappingType ) {
					writer.startObject();
					serializeMapping(  (EmbeddableMappingType)attributeMapping.getMappedType(), values[i], options,writer);
					writer.endObject();
				}

			}
			else if ( attributeMapping instanceof EmbeddedAttributeMapping ) {
				if ( values[i] == null ) {
					//writer.nullValue();
					continue;
				}
				final EmbeddableMappingType mappingType = (EmbeddableMappingType) attributeMapping.getMappedType();
				final SelectableMapping aggregateMapping = mappingType.getAggregateMapping();
				if (aggregateMapping == null) {
					serializeMapping(
							mappingType,
							values[i],
							options,
							writer );
				}
				else {
					final String name = aggregateMapping.getSelectableName();
					writer.objectKey( name );
					writer.startObject();
					serializeMapping(
							mappingType,
							values[i],
							options,
							writer);
					writer.endObject();

				}
			}
			else {
				throw new UnsupportedOperationException( "Support for attribute mapping type not yet implemented: " + attributeMapping.getClass().getName() );
			}

		}
	}

	/**
	 * Consumes Json document items from a document reader and return the serialized Objects
	 * @param reader the document reader
	 * @param embeddableMappingType the type definitions
	 * @param returnEmbeddable do we return an Embeddable object or array of Objects ?
	 * @param options wrapping options
	 * @return serialized values
	 * @param <X>
	 * @throws SQLException if error occured during mapping of types
	 */
	private static <X> X consumeJsonDocumentItems(JsonDocumentReader reader, EmbeddableMappingType embeddableMappingType, boolean returnEmbeddable, WrapperOptions options)
			throws SQLException {
		// final result of a mapped object array
		Object [] objectArrayResult;
		// current mapping to be used
		SelectableMapping currentSelectableMapping = null;
		String currentKeyName = null;
		List<Object> subArrayObjectList = null;
		BasicPluralType<?, ?> subArrayObjectTypes = null;

		// mapping definitions are in a tree
		// Each mapping definition may contain sub mappings (sub embeddable mapping)
		// This stack is used to keep a pointer on the current mapping to be used to assign correct types.
		// see onStartObject()/onEndObject() methods
		StandardStack<EmbeddableMappingType> embeddableMappingTypes = new StandardStack<>();
		// As for mapping definitions, when "sub embeddable" is encountered, the array
		// that needs to be filled with Objects is the one we allocate in the final result array slot.
		// We use a stack to keep track of array ref
		StandardStack<Object[]> objectArrays = new StandardStack<>();

		// index within objectArrayResult
		int currentSelectableIndexInResultArray = -1;

		JsonValueJDBCTypeAdapter adapter = JsonValueJDBCTypeAdapterFactory.getAdapter(reader,returnEmbeddable);

		embeddableMappingTypes.push(embeddableMappingType);
		objectArrayResult = new Object[embeddableMappingType.getJdbcValueCount()+ ( embeddableMappingType.isPolymorphic() ? 1 : 0 )];
		objectArrays.push( objectArrayResult );

		while(reader.hasNext()) {
			JsonDocumentItemType type = reader.next();
			switch (type) {
				case VALUE_KEY:
					currentKeyName = reader.getObjectKeyName();

					currentSelectableIndexInResultArray = embeddableMappingTypes.getCurrent().getSelectableIndex( currentKeyName );
					if ( currentSelectableIndexInResultArray >= 0 ) {
						// we may not have a selectable mapping for that key
						currentSelectableMapping = embeddableMappingTypes.getCurrent().getJdbcValueSelectable( currentSelectableIndexInResultArray );
					}
					else {
						throw new IllegalArgumentException(
								String.format(
										"Could not find selectable [%s] in embeddable type [%s] for JSON processing.",
										currentKeyName,
										embeddableMappingTypes.getCurrent().getMappedJavaType().getJavaTypeClass().getName()
								)
						);
					}
					break;
				case ARRAY_START:
					assert (subArrayObjectList == null && subArrayObjectTypes == null) : "ARRAY_START item received twice in a row";

					// initialize an array to gather values
					subArrayObjectList = new ArrayList<>();
					assert (currentSelectableMapping.getJdbcMapping() instanceof BasicPluralType<?, ?>)
							: "Array event received for non plural type";
					// initialize array's element type
					subArrayObjectTypes = (BasicPluralType<?, ?>) currentSelectableMapping.getJdbcMapping();
					break;
				case ARRAY_END:
					assert (subArrayObjectList != null && subArrayObjectTypes != null) : "ARRAY_END item received twice in a row";
					// flush array values
					objectArrays.getCurrent()[currentSelectableIndexInResultArray] = subArrayObjectTypes.getJdbcJavaType().wrap( subArrayObjectList, options );
					// reset until we encounter next array element
					subArrayObjectList = null;
					subArrayObjectTypes = null;
					break;
				case OBJECT_START:
					if (currentKeyName != null) {
						// We are dealing with a sub-object, allocate space for it then,
						// otherwise, we have nothing to do.
						// Push the new (sub)mapping definition.
						assert embeddableMappingTypes.getCurrent() != null;
						currentSelectableIndexInResultArray = embeddableMappingTypes.getCurrent().getSelectableIndex( currentKeyName );
						assert currentSelectableIndexInResultArray != -1: "Cannot get index of " + currentKeyName;

						final SelectableMapping selectable = embeddableMappingTypes.getCurrent().getJdbcValueSelectable(
								currentSelectableIndexInResultArray );
						final AggregateJdbcType aggregateJdbcType = (AggregateJdbcType) selectable.getJdbcMapping()
								.getJdbcType();
						final EmbeddableMappingType subMappingType = aggregateJdbcType.getEmbeddableMappingType();
						assert objectArrays.getCurrent() != null;
						objectArrays.getCurrent()[currentSelectableIndexInResultArray] =
								new Object[subMappingType.getJdbcValueCount()];
						embeddableMappingTypes.push( subMappingType );
						objectArrays.push( (Object[]) objectArrays.getCurrent()[currentSelectableIndexInResultArray] );
					}
					break;
				case OBJECT_END:
					// go back in the mapping definition tree
					embeddableMappingTypes.pop();
					objectArrays.pop();
					break;
				case NULL_VALUE:
					if ( subArrayObjectList != null ) {
						// dealing with arrays
						subArrayObjectList.add( null );
					}
					else {
						objectArrays.getCurrent()[currentSelectableIndexInResultArray] = null;
					}
					break;
				case NUMERIC_VALUE:
					if ( subArrayObjectList != null ) {
						// dealing with arrays
						subArrayObjectList.add( adapter.fromNumericValue( subArrayObjectTypes.getElementType().getJdbcJavaType(),
								subArrayObjectTypes.getElementType().getJdbcType(),reader,options));
					}
					else {
						objectArrays.getCurrent()[currentSelectableIndexInResultArray] = adapter.fromNumericValue( currentSelectableMapping.getJdbcMapping().getJdbcJavaType(),
								currentSelectableMapping.getJdbcMapping().getJdbcType(),reader,options);
					}
					break;
				case BOOLEAN_VALUE:
					if ( subArrayObjectList != null ) {
						// dealing with arrays
						subArrayObjectList.add( reader.getBooleanValue()?Boolean.TRUE:Boolean.FALSE);
					}
					else {
						objectArrays.getCurrent()[currentSelectableIndexInResultArray] = reader.getBooleanValue()?Boolean.TRUE:Boolean.FALSE;
					}
					break;
				case VALUE:
					if ( subArrayObjectList != null ) {
						// dealing with arrays
						subArrayObjectList.add(adapter.fromValue( subArrayObjectTypes.getElementType().getJdbcJavaType(),
								subArrayObjectTypes.getElementType().getJdbcType(),reader,options));
					}
					else {
						objectArrays.getCurrent()[currentSelectableIndexInResultArray] = adapter.fromValue( currentSelectableMapping.getJdbcMapping().getJdbcJavaType(),
								currentSelectableMapping.getJdbcMapping().getJdbcType(),reader,options);
					}

					break;
				default:
					assert false: "Unexpected type " + type;
			}
		}
		return (X) objectArrayResult;
	}

	/**
	 * Deserialize a JSON value to Java Object
	 * @param embeddableMappingType the mapping type
	 * @param source the JSON value
	 * @param returnEmbeddable do we return an Embeddable object or array of Objects
	 * @param options wrappping options
	 * @return the deserialized value
	 * @param <X>
	 * @throws SQLException
	 */
	public static <X> X deserialize(
			EmbeddableMappingType embeddableMappingType,
			Object source,
			boolean returnEmbeddable,
			WrapperOptions options) throws SQLException {

		if ( source == null ) {
			return null;
		}
		JsonDocumentReader reader = JsonDocumentReaderFactory.getJsonDocumentReader(source);

		final Object[] values = consumeJsonDocumentItems(reader, embeddableMappingType, returnEmbeddable, options);
		if ( returnEmbeddable ) {
			final StructAttributeValues attributeValues = StructHelper.getAttributeValues(
					embeddableMappingType,
					values,
					options
			);
			//noinspection unchecked
			return (X) instantiate( embeddableMappingType, attributeValues );
		}
		//noinspection unchecked
		return (X) values;
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
		JsonDocumentReader reader = JsonDocumentReaderFactory.getJsonDocumentReader(string);
		JsonValueJDBCTypeAdapter adapter = JsonValueJDBCTypeAdapterFactory.getAdapter(reader,false);

		assert reader.hasNext():"Invalid array string";
		assert reader.next() == JsonDocumentItemType.ARRAY_START:"Invalid start of array";
		boolean endArrayFound = false;
		while(reader.hasNext()) {
			JsonDocumentItemType type = reader.next();
			switch ( type ) {
				case JsonDocumentItemType.ARRAY_END:
					endArrayFound=true;
					break;
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
				default:
					assert false : "Unexpected type " + type;
			}
		}


		assert endArrayFound:"Invalid end of array";
		return javaType.wrap( arrayList, options );
	}



	public static class JsonAppender extends OutputStream implements SqlAppender {

		private final static char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

		private final StringBuilder sb;
		private boolean escape;

		public JsonAppender(StringBuilder sb) {
			this.sb = sb;
		}

		@Override
		public void appendSql(String fragment) {
			append( fragment );
		}

		@Override
		public void appendSql(char fragment) {
			append( fragment );
		}

		@Override
		public void appendSql(int value) {
			sb.append( value );
		}

		@Override
		public void appendSql(long value) {
			sb.append( value );
		}

		@Override
		public void appendSql(boolean value) {
			sb.append( value );
		}

		@Override
		public String toString() {
			return sb.toString();
		}

		public void startEscaping() {
			assert !escape;
			escape = true;
		}

		public void endEscaping() {
			assert escape;
			escape = false;
		}

		@Override
		public JsonAppender append(char fragment) {
			if ( escape ) {
				appendEscaped( fragment );
			}
			else {
				sb.append( fragment );
			}
			return this;
		}

		@Override
		public JsonAppender append(CharSequence csq) {
			return append( csq, 0, csq.length() );
		}

		@Override
		public JsonAppender append(CharSequence csq, int start, int end) {
			if ( escape ) {
				int len = end - start;
				sb.ensureCapacity( sb.length() + len );
				for ( int i = start; i < end; i++ ) {
					appendEscaped( csq.charAt( i ) );
				}
			}
			else {
				sb.append( csq, start, end );
			}
			return this;
		}

		@Override
		public void write(int v) {
			final String hex = Integer.toHexString( v );
			sb.ensureCapacity( sb.length() + hex.length() + 1 );
			if ( ( hex.length() & 1 ) == 1 ) {
				sb.append( '0' );
			}
			sb.append( hex );
		}

		@Override
		public void write(byte[] bytes) {
			write(bytes, 0, bytes.length);
		}

		@Override
		public void write(byte[] bytes, int off, int len) {
			sb.ensureCapacity( sb.length() + ( len << 1 ) );
			for ( int i = 0; i < len; i++ ) {
				final int v = bytes[off + i] & 0xFF;
				sb.append( HEX_ARRAY[v >>> 4] );
				sb.append( HEX_ARRAY[v & 0x0F] );
			}
		}

		private void appendEscaped(char fragment) {
			switch ( fragment ) {
				case 0:
				case 1:
				case 2:
				case 3:
				case 4:
				case 5:
				case 6:
				case 7:
				//   8 is '\b'
				//   9 is '\t'
				//   10 is '\n'
				case 11:
				//   12 is '\f'
				//   13 is '\r'
				case 14:
				case 15:
				case 16:
				case 17:
				case 18:
				case 19:
				case 20:
				case 21:
				case 22:
				case 23:
				case 24:
				case 25:
				case 26:
				case 27:
				case 28:
				case 29:
				case 30:
				case 31:
					sb.append( "\\u" ).append( Integer.toHexString( fragment ) );
					break;
				case '\b':
					sb.append("\\b");
					break;
				case '\t':
					sb.append("\\t");
					break;
				case '\n':
					sb.append("\\n");
					break;
				case '\f':
					sb.append("\\f");
					break;
				case '\r':
					sb.append("\\r");
					break;
				case '"':
					sb.append( "\\\"" );
					break;
				case '\\':
					sb.append( "\\\\" );
					break;
				default:
					sb.append( fragment );
					break;
			}
		}

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
