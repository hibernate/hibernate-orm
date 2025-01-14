/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.format;

import org.hibernate.dialect.JsonHelper;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.BooleanJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.JdbcDateJavaType;
import org.hibernate.type.descriptor.java.JdbcTimeJavaType;
import org.hibernate.type.descriptor.java.JdbcTimestampJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Stack;

/**
 * JsonDocument String writer implementation
 * @author Emmanuel Jannetti
 */
public class StringJsonDocumentWriter implements JsonDocumentWriter{

	private static final char ARRAY_END_MARKER = ']';
	private static final char ARRAY_START_MARKER = '[';
	private static final char OBJECT_END_MARKER = '}';
	private static final char OBJECT_START_MARKER = '{';
	private static final char SEPARATOR_MARKER = ',';
	private static final char TOKEN_QUOTE = '"';

	private JsonHelper.JsonAppender appender;

	/**
	 * Processing states. This can be (nested)Object or Arrays.
	 * When processing objects, values are stored as [,]"key":"value"[,]. we add separator when adding new key
	 * When processing arrays, values are stored as [,]"value"[,]. we add separator when adding new value
	 */
	private enum PROCESSING_STATE {
		NONE,
		STARTING_OBJECT, // object started but no value added
		OBJECT, // object started, and we've started adding key/value pairs
		ENDING_OBJECT, // we are ending an object
		STARTING_ARRAY,  // array started but no value added
		ENDING_ARRAY,  // we are ending an array
		ARRAY // we are piling array values
	}
	private Stack<PROCESSING_STATE> processingStates = new Stack<>();


	public StringJsonDocumentWriter(JsonHelper.JsonAppender appender) {
		this.processingStates.push( PROCESSING_STATE.NONE );
		this.appender = appender;
	}

	/**
	 * Callback to be called when the start of an JSON object is encountered.
	 */
	@Override
	public void startObject() throws IOException {
		// Note: startArray and startObject must not call moveProcessingStateMachine()
		if (this.processingStates.peek() == PROCESSING_STATE.STARTING_ARRAY) {
			// are we building an array of objects?
			// i.e, [{},...]
			// move to PROCESSING_STATE.ARRAY first
			this.processingStates.push( PROCESSING_STATE.ARRAY);
		}
		else if (this.processingStates.peek() == PROCESSING_STATE.ARRAY) {
			// That means that we ae building an array of object ([{},...])
			// JSON object hee are treat as array item.
			// -> add the marker first
			this.appender.append(SEPARATOR_MARKER);
		}
		this.appender.append( OBJECT_START_MARKER);
		this.processingStates.push( PROCESSING_STATE.STARTING_OBJECT );
	}

	/**
	 * Callback to be called when the end of an JSON object is encountered.
	 */
	@Override
	public void endObject() throws IOException {
		this.appender.append( OBJECT_END_MARKER );
		this.processingStates.push( PROCESSING_STATE.ENDING_OBJECT);
		moveProcessingStateMachine();
	}

	/**
	 * Callback to be called when the start of an array is encountered.
	 */
	@Override
	public void startArray() {
		this.processingStates.push( PROCESSING_STATE.STARTING_ARRAY );
		// Note: startArray and startObject do not call moveProcessingStateMachine()
		this.appender.append( ARRAY_START_MARKER );

	}

	/**
	 * Callback to be called when the end of an array is encountered.
	 */
	@Override
	public void endArray() {
		this.appender.append( ARRAY_END_MARKER );
		this.processingStates.push( PROCESSING_STATE.ENDING_ARRAY);
		moveProcessingStateMachine();
	}


	@Override
	public void objectKey(String key) {
		if (this.processingStates.peek().equals( PROCESSING_STATE.OBJECT )) {
			// we have started an object, and we are adding an item key: we do add a separator.
			this.appender.append( SEPARATOR_MARKER );
		}
		this.appender.append( TOKEN_QUOTE );
		this.appender.append( key );
		this.appender.append( "\":" );
		moveProcessingStateMachine();
	}

	/**
	 * Adds a separator if needed.
	 * The logic here is know if we have to prepend a separator
	 * as such, it must be called at the beginning of all methods
	 * Separator is to separate array items or key/value pairs in an object.
	 */
	private void addItemsSeparator() {
		if (this.processingStates.peek().equals( PROCESSING_STATE.ARRAY )) {
			// We started to serialize an array and already added item to it:add a separator anytime.
			this.appender.append( SEPARATOR_MARKER );
		}
	}

	/**
	 * Changes the current processing state.
	 * we are called after an item (array item or object item) has been added,
	 * do whatever it takes to move away from the current state by picking up the next logical one.
	 * <p>
	 * We have to deal with two kinds of (possibly empty) structure
	 * <ul>
	 *     <li>array of objects and values [{},null,{},"foo", ...]</li>
	 *     <li>objects than have array as attribute value {k1:v1, k2:[v21,v22,..], k3:v3, k4:null, ...}</li>
	 * </ul>
	 *   <pre>
	 *   NONE -> SA -> (A,...) --> SO -> O -> EO -> A
	 *                         --> EA -> NONE
	 *              -> EA  -> NONE
	 *
	 *        -> SO -> (O,...) ------------------> SA -> A -> EA -> O
	 *                         --> EO -> NONE         -> EA -> O
	 *              -> EO -> NONE
	 *
	 *    </pre>
	 *
	 */
	private void moveProcessingStateMachine() {
		switch (this.processingStates.peek()) {
			case STARTING_OBJECT:
				//after starting an object, we start adding key/value pairs
				this.processingStates.push( PROCESSING_STATE.OBJECT );
				break;
			case STARTING_ARRAY:
				//after starting an object, we start adding value to it
				this.processingStates.push( PROCESSING_STATE.ARRAY );
				break;
			case ENDING_ARRAY:
				// when ending an array, we have one or two states.
				//   ARRAY (unless this is an empty array)
				//   STARTING_ARRAY
				// first pop ENDING_ARRAY
				this.processingStates.pop();
				if (this.processingStates.peek().equals( PROCESSING_STATE.ARRAY ))
					this.processingStates.pop();
				assert this.processingStates.pop().equals( PROCESSING_STATE.STARTING_ARRAY );
				break;
			case ENDING_OBJECT:
				// when ending an object, we have one or two states.
				//   OBJECT (unless this is an empty object)
				//   STARTING_OBJECT
				// first pop ENDING_OBJECT
				this.processingStates.pop();
				if (this.processingStates.peek().equals( PROCESSING_STATE.OBJECT ))
					this.processingStates.pop();
				assert this.processingStates.pop().equals( PROCESSING_STATE.STARTING_OBJECT );
				break;
			default:
				//nothing to do for the other ones.
		}
	}

	@Override
	public void nullValue() {
		addItemsSeparator();
		this.appender.append( "null" );
		moveProcessingStateMachine();
	}

	@Override
	public void booleanValue(boolean value) {
		addItemsSeparator();
		BooleanJavaType.INSTANCE.appendEncodedString( this.appender, value);
		moveProcessingStateMachine();
	}

	@Override
	public void stringValue(String value) {
		addItemsSeparator();

		appender.append( TOKEN_QUOTE);
		appender.startEscaping();
		appender.append( value );
		appender.endEscaping();
		appender.append(TOKEN_QUOTE );

		moveProcessingStateMachine();

	}

	@Override
	public void numberValue(Number value) {
		addItemsSeparator();
		this.appender.append( value.toString() );
		moveProcessingStateMachine();
	}


	@Override
	public void serializeJsonValue(Object value, JavaType<Object> javaType, JdbcType jdbcType, WrapperOptions options) {
		addItemsSeparator();
		convertedBasicValueToString(value, options,this.appender,javaType,jdbcType);
		moveProcessingStateMachine();
	}

	private  void convertedBasicValueToString(
			Object value,
			WrapperOptions options,
			JsonHelper.JsonAppender appender,
			JavaType<Object> javaType,
			JdbcType jdbcType) {
		switch ( jdbcType.getDefaultSqlTypeCode() ) {
			case SqlTypes.TINYINT:
			case SqlTypes.SMALLINT:
			case SqlTypes.INTEGER:
				if ( value instanceof Boolean ) {
					// BooleanJavaType has this as an implicit conversion
					appender.append( (Boolean) value ? '1' : '0' );
					break;
				}
				if ( value instanceof Enum ) {
					appender.appendSql( ((Enum<?>) value ).ordinal() );
					break;
				}
			case SqlTypes.BOOLEAN:
			case SqlTypes.BIT:
			case SqlTypes.BIGINT:
			case SqlTypes.FLOAT:
			case SqlTypes.REAL:
			case SqlTypes.DOUBLE:
				// These types fit into the native representation of JSON, so let's use that
				javaType.appendEncodedString( appender, value );
				break;
			case SqlTypes.CHAR:
			case SqlTypes.NCHAR:
			case SqlTypes.VARCHAR:
			case SqlTypes.NVARCHAR:
				if ( value instanceof Boolean ) {
					// BooleanJavaType has this as an implicit conversion
					appender.append( TOKEN_QUOTE );
					appender.append( (Boolean) value ? 'Y' : 'N' );
					appender.append( TOKEN_QUOTE);
					break;
				}
			case SqlTypes.LONGVARCHAR:
			case SqlTypes.LONGNVARCHAR:
			case SqlTypes.LONG32VARCHAR:
			case SqlTypes.LONG32NVARCHAR:
			case SqlTypes.CLOB:
			case SqlTypes.MATERIALIZED_CLOB:
			case SqlTypes.NCLOB:
			case SqlTypes.MATERIALIZED_NCLOB:
			case SqlTypes.ENUM:
			case SqlTypes.NAMED_ENUM:
				// These literals can contain the '"' character, so we need to escape it
				appender.append( TOKEN_QUOTE );
				appender.startEscaping();
				javaType.appendEncodedString( appender, value );
				appender.endEscaping();
				appender.append( TOKEN_QUOTE );
				break;
			case SqlTypes.DATE:
				appender.append( TOKEN_QUOTE );
				JdbcDateJavaType.INSTANCE.appendEncodedString(
						appender,
						javaType.unwrap( value, java.sql.Date.class, options )
				);
				appender.append( TOKEN_QUOTE );
				break;
			case SqlTypes.TIME:
			case SqlTypes.TIME_WITH_TIMEZONE:
			case SqlTypes.TIME_UTC:
				appender.append( TOKEN_QUOTE );
				JdbcTimeJavaType.INSTANCE.appendEncodedString(
						appender,
						javaType.unwrap( value, java.sql.Time.class, options )
				);
				appender.append( TOKEN_QUOTE );
				break;
			case SqlTypes.TIMESTAMP:
				appender.append( TOKEN_QUOTE );
				JdbcTimestampJavaType.INSTANCE.appendEncodedString(
						appender,
						javaType.unwrap( value, java.sql.Timestamp.class, options )
				);
				appender.append( TOKEN_QUOTE );
				break;
			case SqlTypes.TIMESTAMP_WITH_TIMEZONE:
			case SqlTypes.TIMESTAMP_UTC:
				appender.append( TOKEN_QUOTE );
				DateTimeFormatter.ISO_OFFSET_DATE_TIME.formatTo(
						javaType.unwrap( value, OffsetDateTime.class, options ),
						appender
				);
				appender.append( TOKEN_QUOTE );
				break;
			case SqlTypes.DECIMAL:
			case SqlTypes.NUMERIC:
			case SqlTypes.DURATION:
			case SqlTypes.UUID:
				// These types need to be serialized as JSON string, but don't have a need for escaping
				appender.append( TOKEN_QUOTE );
				javaType.appendEncodedString( appender, value );
				appender.append( TOKEN_QUOTE );
				break;
			case SqlTypes.BINARY:
			case SqlTypes.VARBINARY:
			case SqlTypes.LONGVARBINARY:
			case SqlTypes.LONG32VARBINARY:
			case SqlTypes.BLOB:
			case SqlTypes.MATERIALIZED_BLOB:
				// These types need to be serialized as JSON string, and for efficiency uses appendString directly
				appender.append( TOKEN_QUOTE );
				appender.write( javaType.unwrap( value, byte[].class, options ) );
				appender.append( TOKEN_QUOTE );
				break;
			case SqlTypes.ARRAY:
			case SqlTypes.JSON_ARRAY:
				// Caller handles this. We should never end up here actually.
				break;
			default:
				throw new UnsupportedOperationException( "Unsupported JdbcType nested in JSON: " + jdbcType );
		}
	}

}
