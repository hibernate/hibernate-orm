/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.format;

import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.BooleanJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.JdbcDateJavaType;
import org.hibernate.type.descriptor.java.JdbcTimeJavaType;
import org.hibernate.type.descriptor.java.JdbcTimestampJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;

import java.io.OutputStream;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;


/**
 * Implementation of <code>JsonDocumentWriter</code> for String-based OSON document.
 * This implementation will receive a {@link JsonAppender } to a serialze JSON object to it
 *
 * @author Emmanuel Jannetti
 */
public class StringJsonDocumentWriter extends StringJsonDocument implements JsonDocumentWriter {

	private final JsonAppender appender;

	/**
	 * Creates a new StringJsonDocumentWriter.
	 */
	public StringJsonDocumentWriter() {
		this( new StringBuilder() );
	}

	/**
	 * Creates a new StringJsonDocumentWriter.
	 *
	 * @param sb the StringBuilder to receive the serialized JSON
	 */
	public StringJsonDocumentWriter(StringBuilder sb) {
		this.processingStates.push( JsonProcessingState.NONE );
		this.appender = new JsonAppender( sb );
	}

	/**
	 * Callback to be called when the start of an JSON object is encountered.
	 */
	@Override
	public JsonDocumentWriter startObject() {
		// Note: startArray and startObject must not call moveProcessingStateMachine()
		if ( this.processingStates.getCurrent() == JsonProcessingState.STARTING_ARRAY ) {
			// are we building an array of objects?
			// i.e, [{},...]
			// move to JsonProcessingState.ARRAY first
			this.processingStates.push( JsonProcessingState.ARRAY );
		}
		else if ( this.processingStates.getCurrent() == JsonProcessingState.ARRAY ) {
			// That means that we ae building an array of object ([{},...])
			// JSON object hee are treat as array item.
			// -> add the marker first
			this.appender.append( StringJsonDocumentMarker.SEPARATOR.getMarkerCharacter() );
		}
		this.appender.append( StringJsonDocumentMarker.OBJECT_START.getMarkerCharacter() );
		this.processingStates.push( JsonProcessingState.STARTING_OBJECT );
		return this;
	}

	/**
	 * Callback to be called when the end of an JSON object is encountered.
	 */
	@Override
	public JsonDocumentWriter endObject() {
		this.appender.append( StringJsonDocumentMarker.OBJECT_END.getMarkerCharacter() );
		this.processingStates.push( JsonProcessingState.ENDING_OBJECT );
		moveProcessingStateMachine();
		return this;
	}

	/**
	 * Callback to be called when the start of an array is encountered.
	 */
	@Override
	public JsonDocumentWriter startArray() {
		this.processingStates.push( JsonProcessingState.STARTING_ARRAY );
		// Note: startArray and startObject do not call moveProcessingStateMachine()
		this.appender.append( StringJsonDocumentMarker.ARRAY_START.getMarkerCharacter() );
		return this;
	}

	/**
	 * Callback to be called when the end of an array is encountered.
	 */
	@Override
	public JsonDocumentWriter endArray() {
		this.appender.append( StringJsonDocumentMarker.ARRAY_END.getMarkerCharacter() );
		this.processingStates.push( JsonProcessingState.ENDING_ARRAY );
		moveProcessingStateMachine();
		return this;
	}

	@Override
	public JsonDocumentWriter objectKey(String key) {
		if ( key == null || key.isEmpty() ) {
			throw new IllegalArgumentException( "key cannot be null or empty" );
		}

		if ( JsonProcessingState.OBJECT.equals( this.processingStates.getCurrent() ) ) {
			// we have started an object, and we are adding an item key: we do add a separator.
			this.appender.append( StringJsonDocumentMarker.SEPARATOR.getMarkerCharacter() );
		}
		this.appender.append( StringJsonDocumentMarker.QUOTE.getMarkerCharacter() );
		this.appender.append( key );
		this.appender.append( "\":" );
		moveProcessingStateMachine();
		return this;
	}

	/**
	 * Adds a separator if needed.
	 * The logic here is know if we have to prepend a separator
	 * as such, it must be called at the beginning of all methods
	 * Separator is to separate array items or key/value pairs in an object.
	 */
	private void addItemsSeparator() {
		if ( this.processingStates.getCurrent().equals( JsonProcessingState.ARRAY ) ) {
			// We started to serialize an array and already added item to it:add a separator anytime.
			this.appender.append( StringJsonDocumentMarker.SEPARATOR.getMarkerCharacter() );
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
	 */
	private void moveProcessingStateMachine() {
		switch ( this.processingStates.getCurrent() ) {
			case STARTING_OBJECT:
				//after starting an object, we start adding key/value pairs
				this.processingStates.push( JsonProcessingState.OBJECT );
				break;
			case STARTING_ARRAY:
				//after starting an array, we start adding value to it
				this.processingStates.push( JsonProcessingState.ARRAY );
				break;
			case ENDING_ARRAY:
				// when ending an array, we have one or two states.
				//   ARRAY (unless this is an empty array)
				//   STARTING_ARRAY
				// first pop ENDING_ARRAY
				this.processingStates.pop();
				// if we have ARRAY, so that's not an empty array. pop that state
				if ( this.processingStates.getCurrent().equals( JsonProcessingState.ARRAY ) ) {
					this.processingStates.pop();
				}
				final JsonProcessingState arrayStart = this.processingStates.pop();
				assert arrayStart.equals( JsonProcessingState.STARTING_ARRAY );
				break;
			case ENDING_OBJECT:
				// when ending an object, we have one or two states.
				//   OBJECT (unless this is an empty object)
				//   STARTING_OBJECT
				// first pop ENDING_OBJECT
				this.processingStates.pop();
				// if we have OBJECT, so that's not an empty object. pop that state
				if ( this.processingStates.getCurrent().equals( JsonProcessingState.OBJECT ) ) {
					this.processingStates.pop();
				}
				final JsonProcessingState objectStart = this.processingStates.pop();
				assert objectStart.equals( JsonProcessingState.STARTING_OBJECT );
				break;
			default:
				//nothing to do for the other ones.
		}
	}

	@Override
	public JsonDocumentWriter nullValue() {
		addItemsSeparator();
		this.appender.append( "null" );
		moveProcessingStateMachine();
		return this;
	}

	@Override
	public JsonDocumentWriter numericValue(Number value) {
		addItemsSeparator();
		appender.append( value.toString() );
		moveProcessingStateMachine();
		return this;
	}

	@Override
	public JsonDocumentWriter booleanValue(boolean value) {
		addItemsSeparator();
		BooleanJavaType.INSTANCE.appendEncodedString( this.appender, value );
		moveProcessingStateMachine();
		return this;
	}

	@Override
	public JsonDocumentWriter stringValue(String value) {
		addItemsSeparator();

		appender.append( StringJsonDocumentMarker.QUOTE.getMarkerCharacter() );
		appender.startEscaping();
		appender.append( value );
		appender.endEscaping();
		appender.append( StringJsonDocumentMarker.QUOTE.getMarkerCharacter() );

		moveProcessingStateMachine();
		return this;
	}

	@Override
	public <T> JsonDocumentWriter serializeJsonValue(Object value, JavaType<T> javaType, JdbcType jdbcType, WrapperOptions options) {
		addItemsSeparator();
		convertedBasicValueToString( value, options, this.appender, javaType, jdbcType );
		moveProcessingStateMachine();
		return this;
	}

	private <T> void convertedCastBasicValueToString(Object value, WrapperOptions options, JsonAppender appender, JavaType<T> javaType, JdbcType jdbcType) {
		assert javaType.isInstance( value );
		//noinspection unchecked
		convertedBasicValueToString( (T) value, options, appender, javaType, jdbcType );
	}

	/**
	 * Converts a value to String according to its mapping type.
	 * This method serializes the value and writes it into the underlying appender
	 *
	 * @param value the value
	 * @param javaType the Java type of the value
	 * @param jdbcType the JDBC SQL type of the value
	 * @param options the wapping options.
	 */
	private <T> void convertedBasicValueToString(
			Object value,
			WrapperOptions options,
			JsonAppender appender,
			JavaType<T> javaType,
			JdbcType jdbcType) {
		assert javaType.isInstance( value );

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
					appender.appendSql( ((Enum<?>) value).ordinal() );
					break;
				}
			case SqlTypes.BOOLEAN:
			case SqlTypes.BIT:
			case SqlTypes.BIGINT:
			case SqlTypes.FLOAT:
			case SqlTypes.REAL:
			case SqlTypes.DOUBLE:
				// These types fit into the native representation of JSON, so let's use that
				javaType.appendEncodedString( appender, (T) value );
				break;
			case SqlTypes.CHAR:
			case SqlTypes.NCHAR:
			case SqlTypes.VARCHAR:
			case SqlTypes.NVARCHAR:
				if ( value instanceof Boolean ) {
					// BooleanJavaType has this as an implicit conversion
					appender.append( StringJsonDocumentMarker.QUOTE.getMarkerCharacter() );
					appender.append( (Boolean) value ? 'Y' : 'N' );
					appender.append( StringJsonDocumentMarker.QUOTE.getMarkerCharacter() );
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
				appender.append( StringJsonDocumentMarker.QUOTE.getMarkerCharacter() );
				appender.startEscaping();
				javaType.appendEncodedString( appender, (T) value );
				appender.endEscaping();
				appender.append( StringJsonDocumentMarker.QUOTE.getMarkerCharacter() );
				break;
			case SqlTypes.DATE:
				appender.append( StringJsonDocumentMarker.QUOTE.getMarkerCharacter() );
				JdbcDateJavaType.INSTANCE.appendEncodedString(
						appender,
						javaType.unwrap( (T) value, java.sql.Date.class, options )
				);
				appender.append( StringJsonDocumentMarker.QUOTE.getMarkerCharacter() );
				break;
			case SqlTypes.TIME:
			case SqlTypes.TIME_WITH_TIMEZONE:
			case SqlTypes.TIME_UTC:
				appender.append( StringJsonDocumentMarker.QUOTE.getMarkerCharacter() );
				JdbcTimeJavaType.INSTANCE.appendEncodedString(
						appender,
						javaType.unwrap( (T) value, java.sql.Time.class, options )
				);
				appender.append( StringJsonDocumentMarker.QUOTE.getMarkerCharacter() );
				break;
			case SqlTypes.TIMESTAMP:
				appender.append( StringJsonDocumentMarker.QUOTE.getMarkerCharacter() );
				JdbcTimestampJavaType.INSTANCE.appendEncodedString(
						appender,
						javaType.unwrap( (T) value, java.sql.Timestamp.class, options )
				);
				appender.append( StringJsonDocumentMarker.QUOTE.getMarkerCharacter() );
				break;
			case SqlTypes.TIMESTAMP_WITH_TIMEZONE:
			case SqlTypes.TIMESTAMP_UTC:
				appender.append( StringJsonDocumentMarker.QUOTE.getMarkerCharacter() );
				DateTimeFormatter.ISO_OFFSET_DATE_TIME.formatTo(
						javaType.unwrap( (T) value, OffsetDateTime.class, options ),
						appender
				);
				appender.append( StringJsonDocumentMarker.QUOTE.getMarkerCharacter() );
				break;
			case SqlTypes.DECIMAL:
			case SqlTypes.NUMERIC:
			case SqlTypes.DURATION:
			case SqlTypes.UUID:
				// These types need to be serialized as JSON string, but don't have a need for escaping
				appender.append( StringJsonDocumentMarker.QUOTE.getMarkerCharacter() );
				javaType.appendEncodedString( appender, (T) value );
				appender.append( StringJsonDocumentMarker.QUOTE.getMarkerCharacter() );
				break;
			case SqlTypes.BINARY:
			case SqlTypes.VARBINARY:
			case SqlTypes.LONGVARBINARY:
			case SqlTypes.LONG32VARBINARY:
			case SqlTypes.BLOB:
			case SqlTypes.MATERIALIZED_BLOB:
				// These types need to be serialized as JSON string, and for efficiency uses appendString directly
				appender.append( StringJsonDocumentMarker.QUOTE.getMarkerCharacter() );
				appender.write( javaType.unwrap( (T) value, byte[].class, options ) );
				appender.append( StringJsonDocumentMarker.QUOTE.getMarkerCharacter() );
				break;
			case SqlTypes.ARRAY:
			case SqlTypes.JSON_ARRAY:
				// Caller handles this. We should never end up here actually.
				throw new IllegalStateException( "unexpected JSON array type" );
			default:
				throw new UnsupportedOperationException( "Unsupported JdbcType nested in JSON: " + jdbcType );
		}
	}

	public String getJson() {
		return appender.toString();
	}

	@Override
	public String toString() {
		return appender.toString();
	}

	private static class JsonAppender extends OutputStream implements SqlAppender {

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
		public void appendSql(double value) {
			sb.append( value );
		}

		@Override
		public void appendSql(float value) {
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
			if ( (hex.length() & 1) == 1 ) {
				sb.append( '0' );
			}
			sb.append( hex );
		}

		@Override
		public void write(byte[] bytes) {
			write( bytes, 0, bytes.length );
		}

		@Override
		public void write(byte[] bytes, int off, int len) {
			sb.ensureCapacity( sb.length() + (len << 1) );
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
					sb.append( "\\b" );
					break;
				case '\t':
					sb.append( "\\t" );
					break;
				case '\n':
					sb.append( "\\n" );
					break;
				case '\f':
					sb.append( "\\f" );
					break;
				case '\r':
					sb.append( "\\r" );
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
}
