/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.format;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.CharBuffer;
import java.util.NoSuchElementException;

/**
 * Implementation of <code>JsonDocumentReader</code> for String representation of JSON objects.
 */
public class StringJsonDocumentReader extends StringJsonDocument implements  JsonDocumentReader {

	private final CharBuffer json;
	private final CharBuffer jsonValueWindow;

	/**
	 * Creates a new <code>StringJsonDocumentReader</code>
	 * @param json the JSON String. of the object to be parsed.
	 */
	public StringJsonDocumentReader(String json) {
		if (json == null) {
			throw new IllegalArgumentException( "json cannot be null" );
		}
		this.json = CharBuffer.wrap( json.toCharArray() ).asReadOnlyBuffer();
		this.jsonValueWindow = this.json.slice();
	}

	@Override
	public boolean hasNext() {
		// enough for now.
		return this.json.hasRemaining();
	}

	private void skipWhiteSpace() {
		for (int i =  this.json.position(); i < this.json.limit(); i++ ) {
			if (!Character.isWhitespace( this.json.get(i))) {
				this.json.position(i);
				return;
			}
		}
	}

	private void resetValueWindow() {
		this.jsonValueWindow.position(0);
		this.jsonValueWindow.limit( 0);
	}

	/**
	 * Moves the state machine according to the current state and the given marker
	 *
	 * @param marker the marker we just read
	 */
	private void moveStateMachine(StringJsonDocumentMarker marker) {
		StringJsonDocument.PROCESSING_STATE currentState = this.processingStates.getCurrent();
		switch (marker) {
			case OBJECT_START:
				if (currentState == PROCESSING_STATE.STARTING_ARRAY ) {
					// move the state machine to ARRAY as we are adding something to it
					this.processingStates.push(PROCESSING_STATE.ARRAY);
				}
				this.processingStates.push( PROCESSING_STATE.STARTING_OBJECT );
				break;
			case OBJECT_END:
				assert this.processingStates.getCurrent() == PROCESSING_STATE.OBJECT ||
					this.processingStates.getCurrent() == PROCESSING_STATE.STARTING_OBJECT;
				if (this.processingStates.pop() == PROCESSING_STATE.OBJECT) {
					assert this.processingStates.getCurrent() == PROCESSING_STATE.STARTING_OBJECT;
					this.processingStates.pop();
				}
				break;
			case ARRAY_START:
				this.processingStates.push( PROCESSING_STATE.STARTING_ARRAY );
				break;
			case ARRAY_END:
				assert this.processingStates.getCurrent() == PROCESSING_STATE.ARRAY ||
					this.processingStates.getCurrent() == PROCESSING_STATE.STARTING_ARRAY;
				if (this.processingStates.pop() == PROCESSING_STATE.ARRAY) {
					assert this.processingStates.getCurrent() == PROCESSING_STATE.STARTING_ARRAY;
					this.processingStates.pop();
				}
				break;
			case SEPARATOR:
				// While processing an object, following SEPARATOR that will a key
				if (currentState == PROCESSING_STATE.OBJECT) {
					this.processingStates.push( PROCESSING_STATE.OBJECT_KEY_NAME );
				}
				break;
			case KEY_VALUE_SEPARATOR:
				// that's the start of an attribute value
				assert this.processingStates.getCurrent() == PROCESSING_STATE.OBJECT_KEY_NAME;
				// flush the OBJECT_KEY_NAME
				this.processingStates.pop();
				assert this.processingStates.getCurrent() == PROCESSING_STATE.OBJECT;
				break;
			case QUOTE:
				switch ( currentState ) {
					case PROCESSING_STATE.STARTING_ARRAY:
						this.processingStates.push( PROCESSING_STATE.ARRAY );
						break;
					case PROCESSING_STATE.STARTING_OBJECT:
						this.processingStates.push( PROCESSING_STATE.OBJECT );
						this.processingStates.push( PROCESSING_STATE.OBJECT_KEY_NAME );
						break;
				}
				break;
			case OTHER:
				if (currentState == PROCESSING_STATE.STARTING_ARRAY) {
					this.processingStates.push( PROCESSING_STATE.ARRAY );
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
			StringJsonDocumentMarker marker = StringJsonDocumentMarker.markerOf( this.json.get() );
			moveStateMachine( marker );
			switch ( marker) {
				case OBJECT_START:
					//this.processingStates.push( PROCESSING_STATE.STARTING_OBJECT );
					resetValueWindow();
					return JsonDocumentItemType.OBJECT_START;
				case OBJECT_END:
					resetValueWindow();
					//this.processingStates.pop(); // closing an object or a nested one.
					return JsonDocumentItemType.OBJECT_END;
				case ARRAY_START:
					resetValueWindow();
					//this.processingStates.push( PROCESSING_STATE.STARTING_ARRAY );
					return JsonDocumentItemType.ARRAY_START;
				case ARRAY_END:
					resetValueWindow();
					//this.processingStates.pop();
					return JsonDocumentItemType.ARRAY_END;
				case QUOTE:  // that's the start of an attribute key or a quoted value
					// put back the quote
					moveBufferPosition(-1);
					consumeQuottedString();
					// That's a quote:
					//   - if we are at the beginning of an array that's a quoted value
					//   - if we are in the middle of an array, that's a quoted value
					//   - if we are at the beginning of an object that's a quoted key
					//   - if we are in the middle of an object :
					//        - if we just hit ':' that's a quoted value
					//        - if we just hit ',' that's a quoted key
					switch ( this.processingStates.getCurrent() ) {
						case PROCESSING_STATE.STARTING_ARRAY:
							//this.processingStates.push( PROCESSING_STATE.ARRAY );
							return JsonDocumentItemType.VALUE;
						case PROCESSING_STATE.ARRAY:
							return JsonDocumentItemType.VALUE;
						case PROCESSING_STATE.STARTING_OBJECT:
							//this.processingStates.push( PROCESSING_STATE.OBJECT );
							//this.processingStates.push( PROCESSING_STATE.OBJECT_KEY_NAME );
							return JsonDocumentItemType.VALUE_KEY;
						case PROCESSING_STATE.OBJECT: // we are processing object attribute value elements
							return JsonDocumentItemType.VALUE;
						case PROCESSING_STATE.OBJECT_KEY_NAME: // we are processing object elements key
							return JsonDocumentItemType.VALUE_KEY;
						default:
							throw new IllegalStateException( "unexpected quote read in current processing state " +
															this.processingStates.getCurrent() );
					}
				case KEY_VALUE_SEPARATOR:  // that's the start of an attribute value
					//assert this.processingStates.getCurrent() == PROCESSING_STATE.OBJECT_KEY_NAME;
					// flush the OBJECT_KEY_NAME
					//this.processingStates.pop();
					break;
				case SEPARATOR:
					// unless we are processing an array, following SEPARATOR that will a key
//					if (this.processingStates.getCurrent() == PROCESSING_STATE.OBJECT) {
//						this.processingStates.push( PROCESSING_STATE.OBJECT_KEY_NAME );
//					}
					break;
				case OTHER:
					// here we are in front of a boolean, a null or a numeric value.
					// if none of these cases we going to raise IllegalStateException
					// put back what we've read
					moveBufferPosition(-1);
					final int valueSize = consumeNonStringValue();
					if (valueSize == -1) {
						throw new IllegalStateException( "Unrecognized marker: " + StringJsonDocumentMarker.markerOf(
								json.get( this.json.position() )));
					}
					switch ( this.processingStates.getCurrent() ) {
						case PROCESSING_STATE.ARRAY:
						case PROCESSING_STATE.OBJECT:
							return getUnquotedValueType(this.jsonValueWindow);
						default:
							throw new IllegalStateException( "unexpected read ["+
															this.jsonValueWindow.toString()+
															"] in current processing state " +
															this.processingStates.getCurrent() );
					}
			}
		}
		// no way we get here.
		return null;
	}

	/**
	 * Gets the type of unquoted value.
	 * We assume that the String value follows JSON specification. I.e unquoted value that starts with 't' can't be anything else
	 * than <code>true</code>
	 * @param jsonValueWindow the value
	 * @return the type of the value
	 */
	private JsonDocumentItemType getUnquotedValueType(CharBuffer jsonValueWindow) {
		final int size = jsonValueWindow.remaining();
		switch(jsonValueWindow.charAt( 0 )) {
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
		this.json.position(this.json.position() + shift);
	}

	/**
	 * Moves the current position to a given character.
	 * @param character the character we should stop at.
	 * @throws IllegalStateException if we encounter an unexpected character other than white spaces before the desired one.
	 */

	private void moveTo(char character) throws IllegalStateException {
		this.json.mark();
		while ( this.json.hasRemaining()) {
			char c = this.json.get();
			if ( c == character) {
				this.json.reset();
				return;
			}
			if (!Character.isWhitespace(c)) {
				// we did find an unexpected character
				// let the exception raise
				this.json.reset();
				break;
			}
		}
		throw new IllegalStateException("Can't find character: " + character);
	}

	private int locateCharacter(char character, char escape) {
		assert character != escape;
		this.json.mark();
		int found = -1;
		boolean escapeIsOn = false;
		while ( this.json.hasRemaining()) {
			final char c = this.json.get();
			if (c == escape) {
				escapeIsOn = true;
			}
			else {
				if ( c == character ) {
					if (escapeIsOn) {
						escapeIsOn = false;
					}
					else {
						found = this.json.position() - 1;
						break;
					}
				}
			}
		}
		this.json.reset();
		return found;
	}

	/**
	 * Consume a non-quotted value
	 * @return the length of this value. can be 0, -1 in case of error
	 */
	private int consumeNonStringValue() {
		int newViewLimit = 0;
		boolean allGood = false;
		for (int i =  this.json.position(); i < this.json.limit(); i++ ) {
			char c = this.json.get(i);
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
			this.jsonValueWindow.limit(newViewLimit);
			this.jsonValueWindow.position( this.json.position() );
			this.json.position(newViewLimit);
		}
		return allGood?(this.jsonValueWindow.remaining()):-1;
	}
	private void consumeQuottedString() {

		// be sure we are at a meaningful place
		// key name are unquoted
		moveTo( StringJsonDocumentMarker.QUOTE.getMarkerCharacter() );

		// skip the quote we are positioned on.
		this.json.get();

		//locate ending quote
		int endingQuote = locateCharacter( StringJsonDocumentMarker.QUOTE.getMarkerCharacter(), '\\');
		if (endingQuote == -1) {
			throw new IllegalStateException("Can't find ending quote of key name");
		}

		this.jsonValueWindow.limit( endingQuote );
		this.jsonValueWindow.position(this.json.position());
		this.json.position( endingQuote + 1);

	}

	private void ensureValueState() throws IllegalStateException {
		if ((this.processingStates.getCurrent() !=  PROCESSING_STATE.OBJECT ) &&
			this.processingStates.getCurrent() !=  PROCESSING_STATE.ARRAY)  {
			throw new IllegalStateException( "unexpected processing state: " + this.processingStates.getCurrent() );
		}
	}
	private void ensureAvailableValue() throws IllegalStateException {
		if (this.jsonValueWindow.limit() == 0 ) {
			throw new IllegalStateException( "No available value");
		}
	}

	@Override
	public String getObjectKeyName() {
		if (this.processingStates.getCurrent() !=  PROCESSING_STATE.OBJECT_KEY_NAME ) {
			throw new IllegalStateException( "unexpected processing state: " + this.processingStates.getCurrent() );
		}
		ensureAvailableValue();
		return this.jsonValueWindow.toString();
	}
	@Override
	public String getStringValue() {
		ensureValueState();
		ensureAvailableValue();
		if (hasEscape(this.jsonValueWindow)) {
			return unescape(this.jsonValueWindow);
		}
		return this.jsonValueWindow.toString();
	}


	@Override
	public BigDecimal getBigDecimalValue() {
		ensureValueState();
		ensureAvailableValue();
		return BigDecimal.valueOf( Long.valueOf(this.jsonValueWindow.toString()) );
	}

	@Override
	public BigInteger getBigIntegerValue() {
		ensureValueState();
		ensureAvailableValue();
		return BigInteger.valueOf( Long.valueOf(this.jsonValueWindow.toString()) );
	}

	@Override
	public double getDoubleValue() {
		ensureValueState();
		ensureAvailableValue();
		return Double.valueOf(this.jsonValueWindow.toString()).doubleValue();
	}

	@Override
	public float getFloatValue() {
		ensureValueState();
		ensureAvailableValue();
		return Float.valueOf(this.jsonValueWindow.toString()).floatValue();
	}

	@Override
	public long getLongValue() {
		ensureValueState();
		ensureAvailableValue();
		return Long.valueOf(this.jsonValueWindow.toString()).longValue();
	}

	@Override
	public int getIntegerValue() {
		ensureValueState();
		ensureAvailableValue();
		return Integer.valueOf(this.jsonValueWindow.toString()).intValue();
	}

	@Override
	public short getShortValue() {
		ensureValueState();
		ensureAvailableValue();
		return Short.valueOf(this.jsonValueWindow.toString()).shortValue();
	}

	@Override
	public byte getByteValue() {
		ensureValueState();
		ensureAvailableValue();
		return Byte.valueOf(this.jsonValueWindow.toString()).byteValue();
	}

	@Override
	public boolean getBooleanValue() {
		ensureValueState();
		ensureAvailableValue();
		return Boolean.parseBoolean( this.jsonValueWindow.toString() );
	}

	@Override
	public <T> T getValue(JavaType<T> javaType, WrapperOptions options) {
		return javaType.fromEncodedString( this.jsonValueWindow.toString() );
	}

	private boolean hasEscape(CharBuffer jsonValueWindow) {
		for (int i = 0;i<jsonValueWindow.remaining();i++) {
			if (jsonValueWindow.charAt( i ) == '\\') return true;
		}
		return false;
	}
	private String unescape(CharBuffer string) {
		final StringBuilder sb = new StringBuilder( string.remaining() );
		for ( int i = 0; i < string.length(); i++ ) {
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
