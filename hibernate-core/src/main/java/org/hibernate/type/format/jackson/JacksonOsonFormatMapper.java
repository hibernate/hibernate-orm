/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.format.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import oracle.sql.json.OracleJsonFactory;
import oracle.sql.json.OracleJsonParser;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.type.BasicPluralType;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.UUIDJavaType;
import org.hibernate.type.descriptor.jdbc.AggregateJdbcType;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Emmanuel Jannetti
 * @author Bidyadhar Mohanty
 */
public class JacksonOsonFormatMapper extends JacksonJsonFormatMapper {

	public static final String SHORT_NAME = "jackson";

	private final ObjectMapper objectMapper;
	private final EmbeddableMappingType embeddableMappingType;

	/**
	 * Creates a new JacksonOsonFormatMapper
	 * @param objectMapper the Jackson object mapper
	 * same as JacksonOsonFormatMapper(objectMapper, null)
	 */
	public JacksonOsonFormatMapper(ObjectMapper objectMapper) {
		this(objectMapper, null);
	}

	/**
	 * Creates a new JacksonOsonFormatMapper
	 * @param objectMapper the Jackson object mapper
	 * @param embeddableMappingType the embeddable mapping definitions
	 */
	public JacksonOsonFormatMapper(ObjectMapper objectMapper, EmbeddableMappingType embeddableMappingType) {
		super(objectMapper);
		this.objectMapper = objectMapper;
		this.embeddableMappingType = embeddableMappingType;
		if (this.embeddableMappingType != null) {
			this.objectMapper.setAnnotationIntrospector(
					new JacksonJakartaAnnotationIntrospector( this.embeddableMappingType ) );
		}
	}


	/**
	 * Process OSON parser tokens
	 * @param osonParser the OSON parser
	 * @param currentEvent the current of the parser
	 * @param finalResult the populated object array
	 * @param embeddableMappingType the embeddable mapping definitions
	 * @param options the wrapping options
	 * @throws IOException error while reading from underlying parser
	 */
	private void consumeOsonTokens(OracleJsonParser osonParser, OracleJsonParser.Event currentEvent, Object [] finalResult, EmbeddableMappingType embeddableMappingType, WrapperOptions options)
			throws IOException {

		OracleJsonParser.Event event = currentEvent;

		int selectableIndex = -1;
		SelectableMapping mapping = null;
		String currentKeyName = null;
		List<Object> subArrayList = null;
		BasicPluralType<?, ?> pluralType = null;
		while ( event != null ) {
			switch ( event ) {
				case OracleJsonParser.Event.KEY_NAME:
					currentKeyName = osonParser.getString();
					selectableIndex = embeddableMappingType.getSelectableIndex( currentKeyName );
					if ( selectableIndex >= 0 ) {
						// we may not have a selectable mapping for that key
						mapping = embeddableMappingType.getJdbcValueSelectable( selectableIndex );
					}
					break;
				case OracleJsonParser.Event.START_ARRAY:
					// initialize array to gather values
					subArrayList = new ArrayList<>();
					assert (mapping.getJdbcMapping() instanceof BasicPluralType<?, ?>)
							: "Array event received for non plural type";
					// initialize array's element type
					pluralType = (BasicPluralType<?, ?>) mapping.getJdbcMapping();
					break;
				case OracleJsonParser.Event.END_ARRAY:
					assert (subArrayList != null && pluralType != null) : "Wrong event ordering";
					// flush array values
					finalResult[selectableIndex] = pluralType.getJdbcJavaType().wrap( subArrayList, options );
					// reset until we encounter next array elem
					subArrayList = null;
					pluralType = null;
					break;
				case OracleJsonParser.Event.VALUE_DATE:
					LocalDateTime localDate = osonParser.getLocalDateTime();
					if ( pluralType != null ) {
						// dealing with arrays
						subArrayList.add( Date.valueOf( localDate.toLocalDate() ) );
					}
					else {
						finalResult[selectableIndex] = Date.valueOf( localDate.toLocalDate() );
					}
					break;
				case OracleJsonParser.Event.VALUE_TIMESTAMP:
					LocalDateTime local = osonParser.getLocalDateTime();
					Object theOne;
					if ( "java.sql.Timestamp".equals(
							embeddableMappingType.getJdbcValueSelectable( selectableIndex )
									.getJdbcMapping().getJdbcJavaType().getJavaType().getTypeName() ) ) {
						theOne = Timestamp.valueOf( local );
					}
					else {
						theOne = local;
					}
					if ( pluralType != null ) {
						// dealing with arrays
						subArrayList.add( theOne );
					}
					else {
						finalResult[selectableIndex] = theOne;
					}
					break;
				case OracleJsonParser.Event.VALUE_TIMESTAMPTZ:
					if ( pluralType != null ) {
						// dealing with arrays
						subArrayList.add( osonParser.getOffsetDateTime() );
					}
					else {
						finalResult[selectableIndex] = mapping.getJdbcMapping().convertToDomainValue(
								mapping.getJdbcMapping().getJdbcJavaType()
										.wrap( osonParser.getOffsetDateTime(), options ) );
					}
					break;
				case OracleJsonParser.Event.VALUE_INTERVALDS:
				case OracleJsonParser.Event.VALUE_INTERVALYM:
					if ( pluralType != null ) {
						// dealing with arrays
						subArrayList.add( osonParser.getDuration() );
					}
					else {
						finalResult[selectableIndex] = osonParser.getDuration();
					}
					break;
				case OracleJsonParser.Event.VALUE_STRING:
					if ( pluralType != null ) {
						// dealing with arrays
						subArrayList.add(
								pluralType.getElementType().getJdbcJavaType().fromString( osonParser.getString() ) );
					}
					else {
						finalResult[selectableIndex] = mapping.getJdbcMapping().getJdbcJavaType()
								.fromString( osonParser.getString() );
					}
					break;
				case OracleJsonParser.Event.VALUE_TRUE:
					if ( pluralType != null ) {
						// dealing with arrays
						subArrayList.add( Boolean.TRUE );
					}
					else {
						finalResult[selectableIndex] = Boolean.TRUE;
					}
					break;
				case OracleJsonParser.Event.VALUE_FALSE:
					if ( pluralType != null ) {
						// dealing with arrays
						subArrayList.add( Boolean.FALSE );
					}
					else {
						finalResult[selectableIndex] = Boolean.FALSE;
					}
					break;
				case OracleJsonParser.Event.VALUE_NULL:
					if ( pluralType != null ) {
						// dealing with arrays
						subArrayList.add( null );
					}
					else {
						finalResult[selectableIndex] = null;
					}
					break;
				case OracleJsonParser.Event.VALUE_DECIMAL:
					if ( pluralType != null ) {
						// dealing with arrays
						subArrayList.add( osonParser.isIntegralNumber() ? osonParser.getInt() : osonParser.getFloat() );
					}
					else {
						// not array case: wrap value directly
						if ( osonParser.isIntegralNumber() ) {
							theOne = mapping.getJdbcMapping().convertToDomainValue(
									mapping.getJdbcMapping().getJdbcJavaType().wrap( osonParser.getInt(), options ) );
						}
						else {
							theOne = mapping.getJdbcMapping().convertToDomainValue(
									mapping.getJdbcMapping().getJdbcJavaType().wrap( osonParser.getFloat(), options ) );
						}
					}
					break;
				case OracleJsonParser.Event.VALUE_DOUBLE:
					if ( pluralType != null ) {
						// dealing with arrays
						subArrayList.add( osonParser.getDouble() );
					}
					else {
						finalResult[selectableIndex] = mapping.getJdbcMapping().convertToDomainValue(
								mapping.getJdbcMapping().getJdbcJavaType().wrap( osonParser.getDouble(), options ) );
					}
					break;
				case OracleJsonParser.Event.VALUE_FLOAT:
					if ( pluralType != null ) {
						// dealing with arrays
						subArrayList.add( osonParser.getFloat() );
					}
					else {
						finalResult[selectableIndex] = mapping.getJdbcMapping().convertToDomainValue(
								mapping.getJdbcMapping().getJdbcJavaType().wrap( osonParser.getFloat(), options ) );
					}
					break;
				case OracleJsonParser.Event.VALUE_BINARY:
					byte[] bytes = osonParser.getBytes();
					if ( "java.util.UUID".equals(
							mapping.getJdbcMapping().getJdbcJavaType().getJavaType().getTypeName() ) ) {
						theOne = UUIDJavaType.INSTANCE.wrap( osonParser.getBytes(), options );
					}
					else {
						theOne = bytes;
					}
					if ( pluralType != null ) {
						// dealing with arrays
						subArrayList.add( theOne );
					}
					else {
						finalResult[selectableIndex] = theOne;
					}
					break;
				case OracleJsonParser.Event.START_OBJECT:
					if ( currentKeyName == null ) {
						// that's the root
						consumeOsonTokens( osonParser, osonParser.next(), finalResult,
								embeddableMappingType,
								options );
					}
					else {
						selectableIndex = embeddableMappingType.getSelectableIndex( currentKeyName );
						if ( selectableIndex != -1 ) {
							final SelectableMapping selectable = embeddableMappingType.getJdbcValueSelectable(
									selectableIndex );
							final AggregateJdbcType aggregateJdbcType = (AggregateJdbcType) selectable.getJdbcMapping()
									.getJdbcType();
							final EmbeddableMappingType subMappingType = aggregateJdbcType.getEmbeddableMappingType();
							finalResult[selectableIndex] = new Object[subMappingType.getJdbcValueCount()];
							consumeOsonTokens( osonParser, osonParser.next(),
									(Object[]) finalResult[selectableIndex],
									subMappingType,
									options );
						}
					}
					break;
				case OracleJsonParser.Event.END_OBJECT:
					return;
				default:
					throw new IOException( "Unknown OSON event " + event );

			}
			event = osonParser.hasNext() ? osonParser.next() : null;
		}

	}


	/**
	 * Consumes OSON bytes and populate an Object array as described in the embeddable mapping definitions.
	 * @param embeddableMappingType the embeddable mapping definitions
	 * @param source the OSON bytes as <code>byte[]</code>
	 * @param options the wrapping options
	 * @return the Object array
	 * @param <T> return type i.e object array
	 * @throws IOException OSON parsing has failed
	 */
	public <T> T toObjectArray(EmbeddableMappingType embeddableMappingType, Object source, WrapperOptions options) throws IOException {
		Object []finalResult = new Object[embeddableMappingType.getJdbcValueCount()];
		OracleJsonParser osonParser = new OracleJsonFactory().createJsonBinaryParser( ByteBuffer.wrap( (byte[])source ) );
		consumeOsonTokens(osonParser, osonParser.next(), finalResult, embeddableMappingType, options);
		return (T)finalResult;
	}

	@Override
	public <T> T fromString(CharSequence charSequence, Type type) {
		try {
			return objectMapper.readValue( charSequence.toString(), objectMapper.constructType( type ) );
		}
		catch (JsonProcessingException e) {
			throw new IllegalArgumentException( "Could not deserialize string to java type: " + type, e );
		}
	}

	@Override
	public <T> String toString(T value, Type type) {
		try {
			return objectMapper.writerFor( objectMapper.constructType( type ) ).writeValueAsString( value );
		}
		catch (JsonProcessingException e) {
			throw new IllegalArgumentException( "Could not serialize object of java type: " + type, e );
		}
	}

	@Override
	public <T> void writeToTarget(T value, JavaType<T> javaType, Object target, WrapperOptions options)
			throws IOException {

		ObjectWriter writer = objectMapper.writerFor( objectMapper.constructType( javaType.getJavaType() ) );
		writer.writeValue( (JsonGenerator) target, value);
	}

	@Override
	public <T> T readFromSource(JavaType<T> javaType, Object source, WrapperOptions options) throws IOException {
		JsonParser osonParser = objectMapper.getFactory().createParser( (byte[]) source );
		return  objectMapper.readValue( osonParser, objectMapper.constructType( javaType.getJavaType()) );
	}
}
