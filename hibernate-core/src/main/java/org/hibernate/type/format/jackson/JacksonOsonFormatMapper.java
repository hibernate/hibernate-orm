/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.format.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import oracle.sql.json.OracleJsonParser;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.AggregateJdbcType;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * @author Emmanuel Jannetti
 * @auther Bidyadhar Mohanty
 */
public class JacksonOsonFormatMapper extends JacksonJsonFormatMapper {

	public static final String SHORT_NAME = "jackson";

	private final ObjectMapper objectMapper;
	private final EmbeddableMappingType embeddableMappingType;
	private static final Class OsonParserKlass;
	private static Method OsonParserKlassCurrentEventMethod = null;
	private static Method OsonParserKlassGetLocalDateTimeMethod = null;
	private static Method OsonParserKlassReadDurationMethod = null;
	private static Method OsonParserKlassReadOffsetDateTimeMethod = null;
		static {
	try {

		OsonParserKlass = JacksonOsonFormatMapper.class.getClassLoader()
				.loadClass( "oracle.jdbc.provider.oson.OsonParser" );
		OsonParserKlassCurrentEventMethod = OsonParserKlass.getMethod( "currentOsonEvent" );
		OsonParserKlassGetLocalDateTimeMethod = OsonParserKlass.getMethod( "getLocalDateTime" );
		OsonParserKlassReadDurationMethod = OsonParserKlass.getMethod( "readDuration" );
		OsonParserKlassReadOffsetDateTimeMethod = OsonParserKlass.getMethod("readOffsetDateTime" );
	}
	catch (ClassNotFoundException | LinkageError | NoSuchMethodException e) {
		// should not happen as OracleOsonJacksonJdbcType is loaded
		// only when Oracle OSON JDBC extension is present
		// see OracleDialect class.
		throw new ExceptionInInitializerError(
				"JacksonOsonFormatMapper class loaded without OSON extension: " + e.getClass() + " " + e.getMessage() );
	}
}
	public JacksonOsonFormatMapper(ObjectMapper objectMapper, EmbeddableMappingType embeddableMappingType) {
		super(objectMapper);
		this.objectMapper = objectMapper;
		this.embeddableMappingType = embeddableMappingType;
		this.objectMapper.setAnnotationIntrospector( new JacksonJakartaAnnotationIntrospector( this.embeddableMappingType ) );

	}

	public <T> JacksonOsonFormatMapper(ObjectMapper objectMapper, EmbeddableMappingType embeddableMappingType, JavaType<T> javaType) {
		super(objectMapper);
		this.objectMapper = objectMapper;
		this.embeddableMappingType = embeddableMappingType;
		this.objectMapper.setAnnotationIntrospector( new JacksonJakartaAnnotationIntrospector( this.embeddableMappingType ) );

	}



	private void consumeValuedToken(JsonParser osonParser, JsonToken currentToken, Object [] finalResult, EmbeddableMappingType embeddableMappingType, WrapperOptions options)
			throws IOException {

		JsonToken token = currentToken;

		int selectableIndex = -1;
		SelectableMapping mapping = null;
		while ( true ) {
			if ( token == null ) {
				break;
			}
			switch ( token ) {
				case JsonToken.START_ARRAY:
					int i = 0;
					break;
				case JsonToken.VALUE_STRING:
					selectableIndex = embeddableMappingType.getSelectableIndex( osonParser.currentName() );
					//selectableIndex = embeddableMappingType.getSelectableIndex(currentFieldName );
					//AttributeMapping am = embeddableMappingType.findAttributeMapping( currentFieldName );
					assert selectableIndex >= 0;
					mapping = embeddableMappingType.getJdbcValueSelectable( selectableIndex );
					//result.add(osonParser.getText());
					//					result.add(mapping.getJdbcMapping().convertToDomainValue(
					//							mapping.getJdbcMapping().getJdbcJavaType().wrap( osonParser.getText(), options ) ));

					try {
						OracleJsonParser.Event event =
								(OracleJsonParser.Event)OsonParserKlassCurrentEventMethod.invoke( osonParser );
						switch(event) {
							case OracleJsonParser.Event.VALUE_DATE :
									LocalDateTime localDate =
										(LocalDateTime)OsonParserKlassGetLocalDateTimeMethod.invoke( osonParser );
									finalResult[selectableIndex] =  java.sql.Date.valueOf(localDate.toLocalDate());
								break;
								case OracleJsonParser.Event.VALUE_TIMESTAMP:
									LocalDateTime local =
											(LocalDateTime)OsonParserKlassGetLocalDateTimeMethod.invoke( osonParser );
									if ("java.sql.Timestamp".equals(
											embeddableMappingType.getJdbcValueSelectable( selectableIndex )
													.getJdbcMapping().getJdbcJavaType().getJavaType().getTypeName())) {
										finalResult[selectableIndex] = Timestamp.valueOf( local );
									} else {
										finalResult[selectableIndex] = local;
									}
									break;
								case OracleJsonParser.Event.VALUE_TIMESTAMPTZ:
									OffsetDateTime offsetDT =
											(OffsetDateTime)OsonParserKlassReadOffsetDateTimeMethod.invoke( osonParser );
									finalResult[selectableIndex] = offsetDT;
									break;
								case OracleJsonParser.Event.VALUE_INTERVALDS:
									Duration duration = (Duration) OsonParserKlassReadDurationMethod.invoke(osonParser);
									finalResult[selectableIndex] = duration;
									break;
								case OracleJsonParser.Event.VALUE_INTERVALYM:
									//break;  TODO should not be like that
								default :
//									finalResult[selectableIndex] = mapping.getJdbcMapping().convertToDomainValue(
//											mapping.getJdbcMapping().getJdbcJavaType().fromString( osonParser.getText() ) );
//									Object f1 = mapping.getJdbcMapping().getJdbcJavaType().fromString( osonParser.getText() );
//									Object f2 = mapping.getJdbcMapping().convertToDomainValue(mapping.getJdbcMapping().getJdbcJavaType().fromString( osonParser.getText() ));
//									String s1 = osonParser.getText();
//									Object o1 = mapping.getJdbcMapping().convertToDomainValue( osonParser.getText()  );
//									Object o2 = mapping.getJdbcMapping().convertToRelationalValue( osonParser.getText()  );
//									finalResult[selectableIndex] = mapping.getJdbcMapping().convertToDomainValue( osonParser.getText()  );
									finalResult[selectableIndex] = mapping.getJdbcMapping().getJdbcJavaType().fromString( osonParser.getText() );
							}
					}
					catch (Exception e) {
						throw new IOException( e );
					}
					break;
				case JsonToken.VALUE_TRUE:
					selectableIndex = embeddableMappingType.getSelectableIndex( osonParser.currentName());
					finalResult[selectableIndex] = Boolean.TRUE;
					break;
				case JsonToken.VALUE_FALSE:
					selectableIndex = embeddableMappingType.getSelectableIndex( osonParser.currentName());
					finalResult[selectableIndex] = Boolean.FALSE;
					break;
				case JsonToken.VALUE_NULL:
					selectableIndex = embeddableMappingType.getSelectableIndex( osonParser.currentName());
					finalResult[selectableIndex] = null;
					break;
				case JsonToken.VALUE_NUMBER_INT:
					selectableIndex = embeddableMappingType.getSelectableIndex( osonParser.currentName() );
					//selectableIndex = embeddableMappingType.getSelectableIndex(currentFieldName );
					assert selectableIndex >= 0;
					mapping = embeddableMappingType.getJdbcValueSelectable( selectableIndex );
					finalResult[selectableIndex] = mapping.getJdbcMapping().convertToDomainValue(
						mapping.getJdbcMapping().getJdbcJavaType().wrap( osonParser.getIntValue(), options ) );
					break;
				case JsonToken.VALUE_NUMBER_FLOAT:
					selectableIndex = embeddableMappingType.getSelectableIndex( osonParser.currentName() );
					//selectableIndex = embeddableMappingType.getSelectableIndex(currentFieldName );
					assert selectableIndex >= 0;
					mapping = embeddableMappingType.getJdbcValueSelectable( selectableIndex );
					finalResult[selectableIndex] = mapping.getJdbcMapping().convertToDomainValue(
							mapping.getJdbcMapping().getJdbcJavaType().wrap( osonParser.getFloatValue(), options ) );
					break;
				case JsonToken.VALUE_EMBEDDED_OBJECT:
					selectableIndex = embeddableMappingType.getSelectableIndex( osonParser.currentName() );
					byte[] bytes = osonParser.getBinaryValue();
					if ("java.util.UUID".equals(
							embeddableMappingType.getJdbcValueSelectable( selectableIndex )
									.getJdbcMapping().getJdbcJavaType().getJavaType().getTypeName())) {
						ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
						long mostSignificantBits = byteBuffer.getLong();
						long leastSignificantBits = byteBuffer.getLong();
						finalResult[selectableIndex] = new UUID(mostSignificantBits, leastSignificantBits);
					} else {
						finalResult[selectableIndex] = bytes;
					}
					break;
				case JsonToken.START_OBJECT:
					if ( osonParser.currentName() == null ) {
						// that's the root
						consumeValuedToken( osonParser, osonParser.nextToken(), finalResult,
								embeddableMappingType,
								options  );
					}
					else {
						selectableIndex = embeddableMappingType.getSelectableIndex( osonParser.currentName() );
						if ( selectableIndex != -1 ) {
							final SelectableMapping selectable = embeddableMappingType.getJdbcValueSelectable(
									selectableIndex );
							final AggregateJdbcType aggregateJdbcType = (AggregateJdbcType) selectable.getJdbcMapping()
									.getJdbcType();
							final EmbeddableMappingType subMappingType = aggregateJdbcType.getEmbeddableMappingType();
							finalResult[selectableIndex] = new Object[subMappingType.getJdbcValueCount()];
							consumeValuedToken( osonParser, osonParser.nextToken(),
									(Object[]) finalResult[selectableIndex],
									subMappingType,
									options );
						}
					}
					break;
				case JsonToken.END_OBJECT:
					return;
			}
			token = osonParser.nextToken();
		}

	}

	public <T> T readToArray(EmbeddableMappingType embeddableMappingType, Object source, WrapperOptions options) throws IOException {
		JsonParser osonParser = objectMapper.getFactory().createParser( (byte[]) source );
		Object []finalResult = new Object[embeddableMappingType.getJdbcValueCount()];
		consumeValuedToken(osonParser, osonParser.nextToken(), finalResult, embeddableMappingType, options);
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

		T t = objectMapper.readValue( osonParser, objectMapper.constructType( javaType.getJavaType()) );

		return t;
	}
}
