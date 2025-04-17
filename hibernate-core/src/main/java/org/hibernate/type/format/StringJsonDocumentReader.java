/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.format;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.BigDecimalJavaType;
import org.hibernate.type.descriptor.java.BigIntegerJavaType;
import org.hibernate.type.descriptor.java.BooleanJavaType;
import org.hibernate.type.descriptor.java.ByteJavaType;
import org.hibernate.type.descriptor.java.DoubleJavaType;
import org.hibernate.type.descriptor.java.FloatJavaType;
import org.hibernate.type.descriptor.java.IntegerJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.LongJavaType;
import org.hibernate.type.descriptor.java.ShortJavaType;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.NoSuchElementException;

/**
 * Implementation of <code>JsonDocumentReader</code> for String representation of JSON objects.
 */
public class StringJsonDocumentReader extends StringJsonDocument implements JsonDocumentReader {

	private static final char ESCAPE_CHAR = '\\';

	private final String jsonString;
	private final int limit;
	private int position;
	private int jsonValueStart;
	private int jsonValueEnd;

	/**
	 * Creates a new <code>StringJsonDocumentReader</code>
	 * @param json the JSON String. of the object to be parsed.
	 */
	public StringJsonDocumentReader(String json) {
		if (json == null) {
			throw new IllegalArgumentException( "json cannot be null" );
		}
		this.jsonString = json;
		this.position = 0;
		this.limit = jsonString.length();
		this.jsonValueStart = 0;
		this.jsonValueEnd = 0;
	}

	@Override
	public boolean hasNext() {
		return this.position < this.limit;
	}

	private void skipWhiteSpace() {
		for (;this.position  < this.limit; this.position++ ) {
			if (!Character.isWhitespace( this.jsonString.charAt(this.position))) {
				return;
			}
		}
	}

	private void resetValueWindow() {
		this.jsonValueStart = 0;
		this.jsonValueEnd = 0;
	}

	/**
	 * Moves the state machine according to the current state and the given marker
	 *
	 * @param marker the marker we just read
	 */
	private void moveStateMachine(StringJsonDocumentMarker marker) {
		JsonProcessingState currentState = this.processingStates.getCurrent();
		switch (marker) {
			case OBJECT_START:
				if ( currentState == JsonProcessingState.STARTING_ARRAY ) {
					// move the state machine to ARRAY as we are adding something to it
					this.processingStates.push( JsonProcessingState.ARRAY);
				}
				this.processingStates.push( JsonProcessingState.STARTING_OBJECT );
				break;
			case OBJECT_END:
				assert this.processingStates.getCurrent() == JsonProcessingState.OBJECT ||
					this.processingStates.getCurrent() == JsonProcessingState.STARTING_OBJECT;
				if ( this.processingStates.pop() == JsonProcessingState.OBJECT) {
					assert this.processingStates.getCurrent() == JsonProcessingState.STARTING_OBJECT;
					this.processingStates.pop();
				}
				break;
			case ARRAY_START:
				this.processingStates.push( JsonProcessingState.STARTING_ARRAY );
				break;
			case ARRAY_END:
				assert this.processingStates.getCurrent() == JsonProcessingState.ARRAY ||
					this.processingStates.getCurrent() == JsonProcessingState.STARTING_ARRAY;
				if ( this.processingStates.pop() == JsonProcessingState.ARRAY) {
					assert this.processingStates.getCurrent() == JsonProcessingState.STARTING_ARRAY;
					this.processingStates.pop();
				}
				break;
			case SEPARATOR:
				// While processing an object, following SEPARATOR that will a key
				if ( currentState == JsonProcessingState.OBJECT) {
					this.processingStates.push( JsonProcessingState.OBJECT_KEY_NAME );
				}
				break;
			case KEY_VALUE_SEPARATOR:
				// that's the start of an attribute value
				assert this.processingStates.getCurrent() == JsonProcessingState.OBJECT_KEY_NAME;
				// flush the OBJECT_KEY_NAME
				this.processingStates.pop();
				assert this.processingStates.getCurrent() == JsonProcessingState.OBJECT;
				break;
			case QUOTE:
				switch ( currentState ) {
					case STARTING_ARRAY:
						this.processingStates.push( JsonProcessingState.ARRAY );
						break;
					case STARTING_OBJECT:
						this.processingStates.push( JsonProcessingState.OBJECT );
						this.processingStates.push( JsonProcessingState.OBJECT_KEY_NAME );
						break;
				}
				break;
			case OTHER:
				if ( currentState == JsonProcessingState.STARTING_ARRAY) {
					this.processingStates.push( JsonProcessingState.ARRAY );
				}
				break;
		}
	}

	/**
	 * Returns the next item.
	 * @return the item
	 * @throws NoSuchElementException no more item available
	 * @throws IllegalStateException not a well-formed JSON string.
	 */
	@Override
	public JsonDocumentItemType next() {

		if ( !hasNext()) throw new NoSuchElementException("no more elements");

		while (hasNext()) {
			skipWhiteSpace();
			StringJsonDocumentMarker marker = StringJsonDocumentMarker.markerOf( this.jsonString.charAt( this.position++ ) );
			moveStateMachine( marker );
			switch ( marker) {
				case OBJECT_START:
					resetValueWindow();
					return JsonDocumentItemType.OBJECT_START;
				case OBJECT_END:
					resetValueWindow();
					//this.processingStates.pop(); // closing an object or a nested one.
					return JsonDocumentItemType.OBJECT_END;
				case ARRAY_START:
					resetValueWindow();
					//this.processingStates.push( JsonProcessingState.STARTING_ARRAY );
					return JsonDocumentItemType.ARRAY_START;
				case ARRAY_END:
					resetValueWindow();
					//this.processingStates.pop();
					return JsonDocumentItemType.ARRAY_END;
				case QUOTE:  // that's the start of an attribute key or a quoted value
					// put back the quote
					moveBufferPosition(-1);
					consumeQuotedString();
					// That's a quote:
					//   - if we are at the beginning of an array that's a quoted value
					//   - if we are in the middle of an array, that's a quoted value
					//   - if we are at the beginning of an object that's a quoted key
					//   - if we are in the middle of an object :
					//        - if we just hit ':' that's a quoted value
					//        - if we just hit ',' that's a quoted key
					switch ( this.processingStates.getCurrent() ) {
						case STARTING_ARRAY:
							//this.processingStates.push( JsonProcessingState.ARRAY );
							return JsonDocumentItemType.VALUE;
						case ARRAY:
							return JsonDocumentItemType.VALUE;
						case STARTING_OBJECT:
							//this.processingStates.push( JsonProcessingState.OBJECT );
							//this.processingStates.push( JsonProcessingState.OBJECT_KEY_NAME );
							return JsonDocumentItemType.VALUE_KEY;
						case OBJECT: // we are processing object attribute value elements
							return JsonDocumentItemType.VALUE;
						case OBJECT_KEY_NAME: // we are processing object elements key
							return JsonDocumentItemType.VALUE_KEY;
						default:
							throw new IllegalStateException( "unexpected quote read in current processing state " +
															this.processingStates.getCurrent() );
					}
				case KEY_VALUE_SEPARATOR:  // that's the start of an attribute value
					//assert this.processingStates.getCurrent() == JsonProcessingState.OBJECT_KEY_NAME;
					// flush the OBJECT_KEY_NAME
					//this.processingStates.pop();
					break;
				case SEPARATOR:
					// unless we are processing an array, following SEPARATOR that will a key
					break;
				case OTHER:
					// here we are in front of a boolean, a null or a numeric value.
					// if none of these cases we're going to raise IllegalStateException
					// put back what we've read
					moveBufferPosition(-1);
					final int valueSize = consumeNonStringValue();
					if (valueSize == -1) {
						throw new IllegalStateException( "Unrecognized marker: " + StringJsonDocumentMarker.markerOf(
								this.jsonString.charAt( this.position )));
					}
					switch ( this.processingStates.getCurrent() ) {
						case ARRAY:
						case OBJECT:
							return getUnquotedValueType(this.jsonString.charAt( this.jsonValueStart));
						default:
							throw new IllegalStateException( "unexpected read ["+
															this.jsonString.substring( this.jsonValueStart,this.jsonValueEnd )+
															"] in current processing state " +
															this.processingStates.getCurrent() );
					}
			}
		}
		throw new IllegalStateException( "unexpected end of JSON ["+
										this.jsonString.substring( this.jsonValueStart,this.jsonValueEnd )+
										"] in current processing state " +
										this.processingStates.getCurrent() );
	}

	/**
	 * Gets the type of unquoted value.
	 * We assume that the String value follows JSON specification. I.e unquoted value that starts with 't' can't be anything else
	 * than <code>true</code>
	 * @param jsonValueChar the value
	 * @return the type of the value
	 */
	private JsonDocumentItemType getUnquotedValueType(char jsonValueChar) {
		switch(jsonValueChar) {
			case 't': {
				//true
				return JsonDocumentItemType.BOOLEAN_VALUE;
			}
			case 'f': {
				//false
				return JsonDocumentItemType.BOOLEAN_VALUE;
			}
			case 'n' : {
					// null
					return JsonDocumentItemType.NULL_VALUE;
				}
			case '-':
			case '0':
			case '1':
			case '2':
			case '3':
			case '4':
			case '5':
			case '6':
			case '7':
			case '8':
			case '9': {
				return JsonDocumentItemType.NUMERIC_VALUE;
			}
			default :
				return JsonDocumentItemType.VALUE;
			}
	}

	private void moveBufferPosition(int shift) {
		this.position += shift;
	}

	/**
	 * Moves the current position to a given character, skipping any whitespace
	 * We expect the character to be the next non-blank character in the sequence
	 * @param character the character we should stop at.
	 * @throws IllegalStateException if we encounter an unexpected character other than white spaces before the desired one.
	 */

	private void moveTo(char character) throws IllegalStateException {
		int pointer = this.position;
		while ( pointer < this.limit) {
			char c = this.jsonString.charAt( pointer );
			if ( c == character) {
				this.position = pointer == this.position?this.position:pointer - 1;
				return;
			}
			if (!Character.isWhitespace(c)) {
				// we did find an unexpected character
				// let the exception raise
				//this.json.reset();
				break;
			}
			pointer++;
		}
		throw new IllegalStateException("character [" + character + "] is not the next non-blank character");
	}

	private int nextQuote() {
		int pointer = this.position;

		while ( pointer < this.limit) {
			final char c = this.jsonString.charAt( pointer );
			if (c == ESCAPE_CHAR) {
				pointer++;
			}
			else if (c == '"') {
				// found
				return pointer;
			}
			pointer++;
		}
		return -1;
	}

	/**
	 * Consume a non-quoted value
	 * @return the length of this value. can be 0, -1 in case of error
	 */
	private int consumeNonStringValue() {
		int newViewLimit = 0;
		boolean allGood = false;
		for (int i =  this.position; i < this.limit; i++ ) {
			char c = this.jsonString.charAt(i);
			if ((StringJsonDocumentMarker.markerOf( c ) != StringJsonDocumentMarker.OTHER) ||
				Character.isWhitespace( c )) {
				// hit a JSON marker or a space.
				allGood = true;
				// give that character back to the buffer
				newViewLimit = i;
				break;
			}
		}

		if (allGood) {
			this.jsonValueEnd = newViewLimit;
			this.jsonValueStart = position;
			this.position = newViewLimit;
		}
		return allGood?(this.jsonValueEnd-this.jsonValueStart):-1;
	}
	/**
	 * Consumes a quoted value
	 * @return the length of this value. can be 0, -1 in case of error
	 */
	private void consumeQuotedString() {

		// be sure we are at a meaningful place
		// key name are unquoted
		moveTo( StringJsonDocumentMarker.QUOTE.getMarkerCharacter() );

		// skip the quote we are positioned on.
		this.position++;

		//locate ending quote
		int endingQuote = nextQuote();
		if (endingQuote == -1) {
			throw new IllegalStateException("Can't find ending quote of key name");
		}

		this.jsonValueEnd = endingQuote;
		this.jsonValueStart = position;

		this.position =  endingQuote + 1;

	}

	/**
	 * Ensures that the current state is on value.
	 * @throws IllegalStateException if not on "value" state
	 */
	private void ensureValueState() throws IllegalStateException {
		if ( (this.processingStates.getCurrent() != JsonProcessingState.OBJECT ) &&
			this.processingStates.getCurrent() != JsonProcessingState.ARRAY)  {
			throw new IllegalStateException( "unexpected processing state: " + this.processingStates.getCurrent() );
		}
	}
	/**
	 * Ensures that we have a value ready to be exposed. i.e we just consume one.
	 * @throws IllegalStateException if no value available
	 */
	private void ensureAvailableValue() throws IllegalStateException {
		if (this.jsonValueEnd == 0 ) {
			throw new IllegalStateException( "No available value");
		}
	}

	@Override
	public String getObjectKeyName() {
		if ( this.processingStates.getCurrent() != JsonProcessingState.OBJECT_KEY_NAME ) {
			throw new IllegalStateException( "unexpected processing state: " + this.processingStates.getCurrent() );
		}
		ensureAvailableValue();
		return this.jsonString.substring( this.jsonValueStart, this.jsonValueEnd);
	}

	@Override
	public String getStringValue() {
		ensureValueState();
		ensureAvailableValue();
		if ( currentValueHasEscape()) {
			return unescape(this.jsonString, this.jsonValueStart , this.jsonValueEnd);
		}
		return this.jsonString.substring( this.jsonValueStart, this.jsonValueEnd);
	}


	@Override
	public BigDecimal getBigDecimalValue() {
		ensureValueState();
		ensureAvailableValue();
		return BigDecimalJavaType.INSTANCE.fromEncodedString( this.jsonString,this.jsonValueStart,this.jsonValueEnd );
	}

	@Override
	public BigInteger getBigIntegerValue() {
		ensureValueState();
		ensureAvailableValue();
		return BigIntegerJavaType.INSTANCE.fromEncodedString( this.jsonString,this.jsonValueStart,this.jsonValueEnd );
	}

	@Override
	public double getDoubleValue() {
		ensureValueState();
		ensureAvailableValue();
		return DoubleJavaType.INSTANCE.fromEncodedString( this.jsonString,this.jsonValueStart,this.jsonValueEnd );
	}

	@Override
	public float getFloatValue() {
		ensureValueState();
		ensureAvailableValue();
		return FloatJavaType.INSTANCE.fromEncodedString( this.jsonString,this.jsonValueStart,this.jsonValueEnd );
	}

	@Override
	public long getLongValue() {
		ensureValueState();
		ensureAvailableValue();
		return LongJavaType.INSTANCE.fromEncodedString( this.jsonString,this.jsonValueStart,this.jsonValueEnd );
	}

	@Override
	public int getIntegerValue() {
		ensureValueState();
		ensureAvailableValue();
		return IntegerJavaType.INSTANCE.fromEncodedString( this.jsonString,this.jsonValueStart,this.jsonValueEnd );
	}

	@Override
	public short getShortValue() {
		ensureValueState();
		ensureAvailableValue();
		return ShortJavaType.INSTANCE.fromEncodedString( this.jsonString,this.jsonValueStart,this.jsonValueEnd );
	}

	@Override
	public byte getByteValue() {
		ensureValueState();
		ensureAvailableValue();
		return ByteJavaType.INSTANCE.fromEncodedString( this.jsonString,this.jsonValueStart,this.jsonValueEnd );
	}

	@Override
	public boolean getBooleanValue() {
		ensureValueState();
		ensureAvailableValue();
		return BooleanJavaType.INSTANCE.fromEncodedString( this.jsonString,this.jsonValueStart,this.jsonValueEnd );
	}

	@Override
	public <T> T getValue(JavaType<T> javaType, WrapperOptions options) {
		return javaType.fromEncodedString( this.jsonString.subSequence( this.jsonValueStart,this.jsonValueEnd ));
	}

	/**
	 * Walks through JSON value currently located on JSON string and check if escape is used
	 *
	 * @return <code>true</code> if escape is found
	 */
	private boolean currentValueHasEscape() {
		for (int i = this.jsonValueStart; i<this.jsonValueEnd; i++) {
			if (this.jsonString.charAt( i ) == '\\') return true;
		}
		return false;
	}

	/**
	 * Returns unescaped string
	 * @param string the string to be unescaped
	 * @param start the begin index within the string
	 * @param end the end index within the string
	 * @return the unescaped string
	 */
	private static String unescape(String string, int start, int end) {
		final StringBuilder sb = new StringBuilder( end - start );
		for ( int i = start; i < end; i++ ) {
			final char c = string.charAt( i );
			if ( c == '\\' ) {
				i++;
				final char cNext = string.charAt( i );
				switch ( cNext ) {
					case '\\':
					case '"':
					case '/':
						sb.append( cNext );
						break;
					case 'b':
						sb.append( '\b' );
						break;
					case 'f':
						sb.append( '\f' );
						break;
					case 'n':
						sb.append( '\n' );
						break;
					case 'r':
						sb.append( '\r' );
						break;
					case 't':
						sb.append( '\t' );
						break;
					case 'u':
						sb.append( (char) Integer.parseInt( string, i + 1, i + 5, 16 ) );
						i += 4;
						break;
				}
				continue;
			}
			sb.append( c );
		}
		return sb.toString();
	}


}
