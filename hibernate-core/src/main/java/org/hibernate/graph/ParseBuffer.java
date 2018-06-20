/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.graph;

/**
 * A parsing helper class used by the {@link EntityGraphParser} to traverse 
 * {@linkplain CharSequence character sequences) while tracking the parsing
 * position.
 * 
 * @author asusnjar
 *
 */
class ParseBuffer implements CharSequence {

	/**
	 * Character sequence being parsed.
	 */
	private final CharSequence text;
	
	/**
	 * Length of the character sequence.
	 */
	private final int length;
	
	/**
	 * Current parsing position.
	 */
	private int position = 0;

	/**
	 * Main constructor.
	 * 
	 * @param text Character sequence to parse.
	 */
	public ParseBuffer(CharSequence text) {
		this.text = text;
		this.length = text.length();
	}

	/**
	 * Returns the number of characters remaining in the buffer (starting from the current parsing position).
	 * 
	 * @see CharSequence#length()
	 */
	@Override
	public int length() {
		return length - position;
	}

	/**
	 * Returns the characters remaining in the buffer (starting from the current parsing position).
	 * 
	 * @see CharSequence#toString()
	 */
	@Override
	public String toString() {
		return text.subSequence( position, length ).toString();
	}

	/**
	 * Returns the character that is at the specified index <u>after</u> the current parsing position.
	 * 
	 * @see CharSequence#charAt(int)
	 */
	@Override
	public char charAt(int index) {
		return text.charAt( position + index );
	}

	/**
	 * Returns a subsequence with {@code start} and {@code end} indexes being relative to the
	 * current parsing position. 
	 * 
	 * @see CharSequence#subSequence(int, int)
	 */
	@Override
	public CharSequence subSequence(int start, int end) {
		return text.subSequence( position + start, position + end );
	}

	/**
	 * Returns {@code true} if and only if we are at the end of the buffer.
	 */
	public boolean isAtEnd() {
		return position >= length;
	}

	/**
	 * Skips all {@linkplain Character#isWhitespace(char) regular whitespace characters} in the buffer. 
	 */
	public void skipWhitespace() {
		while ( ( position < length ) && Character.isWhitespace( text.charAt( position ) ) ) {
			position++;
		}
	}

	/**
	 * Returns the character at the current parsing position, without advancing the position.
	 * 
	 * @see ParseBuffer#consumeChar()
	 */
	public char peekChar() {
		return text.charAt( position );
	}

	/**
	 * Returns the character at the current parsing position and advances to the next character.
	 * 
	 * @see #peekChar()
	 */
	public char consumeChar() {
		return text.charAt( position++ );
	}

	/**
	 * Returns the character sequence that follows the Java identifier name restrictions
	 * (see {@link Character#isJavaIdentifierStart(char)} and {@link Character#isJavaIdentifierPart(char)})
	 * and advances the parsing position accordingly.
	 * 
	 * @param withDots If {@code true}, also allows/includes dots ({@code '.'}) in identifiers.
	 * 
	 * @return A consumed identifier or {@code null} if at end of the buffer or a non-identifier
	 * character is at the current parsing position. Characters that match Java identifier name
	 * restrictions (and dots if {@code withDots==true}) are eagerly consumed.
	 */
	public CharSequence consumeIdentifierSeq(boolean withDots) {
		if ( isAtEnd() ) {
			return null;
		}

		char ch = peekChar();
		if ( ( ( ch != '.' ) || !withDots ) && !Character.isJavaIdentifierStart( text.charAt( position ) ) ) {
			return null;
		}

		int start = position;
		int terminator = start + 1;

		while ( terminator < length ) {
			ch = text.charAt( terminator );
			if ( ( !Character.isJavaIdentifierPart( ch ) ) && ( ( ch != '.' ) || !withDots ) ) {
				break;
			}
			terminator++;
		}

		position = terminator;
		return text.subSequence( start, terminator );
	}

	/**
	 * A convenience version of {@link #consumeIdentifierSeq(boolean)}.
	 */
	public String consumeIdentifier(boolean withDots) {
		CharSequence id = consumeIdentifierSeq( withDots );
		return ( id == null ) ? (String) null : id.toString();
	}

	/**
	 * Rewinds the parsing position by one character back.
	 * 
	 * @see #back(int)
	 * @see #back(CharSequence)
	 */
	public void back() {
		position--;
	}

	/**
	 * Rewinds the parsing position by the specified number of character.
	 * 
	 * @param charCount Number of characters to move.
	 */
	public void back(int charCount) {
		position -= charCount;
	}

	/**
	 * "Returns" the specified character sequence to the buffer.
	 * 
	 * Present implementation does not validate that these characters match the preceding
	 * buffer content, it only rewinds the buffer based on the sequence length.
	 * 
	 * However, this should not be abused - validation may be introduced at any time.
	 */
	public void back(CharSequence seq) {
		back( seq.length() );
	}

	/**
	 * Returns {@code true} if and only if the next character in the buffer is equal to the specified
	 * character (case sensitive).
	 * 
	 * @see #matchIgnoreCase(char)
	 * @see #match(CharSequence)
	 */
	public boolean match(char ch) {
		if ( isAtEnd() || ( peekChar() != ch ) ) {
			return false;
		}
		position++;
		return true;
	}

	/**
	 * Returns {@code true} if and only if the next character in the buffer is equal to the specified
	 * character (case insensitive).
	 * 
	 * @see #match(char)
	 * @see #matchIgnoreCase(CharSequence)
	 */
	public boolean matchIgnoreCase(char ch) {
		if ( isAtEnd() || ( Character.toLowerCase( text.charAt( position ) ) != Character.toLowerCase( ch ) ) ) {
			return false;
		}
		position++;
		return true;
	}

	/**
	 * Returns {@code true} if and only if the next sequence of characters in the buffer
	 * are equal to the specified sequence (case sensitive).
	 * 
	 * @see #matchIgnoreCase(CharSequence)
	 * @see #match(char)
	 */
	public boolean match(CharSequence seq) {
		final int len = seq.length();

		if ( len >= this.length() ) {
			return false;
		}

		for ( int i = 0; i < len; i++ ) {
			if ( seq.charAt( i ) != text.charAt( position + i ) ) {
				return false;
			}
		}

		position += len;
		return true;
	}

	/**
	 * Returns {@code true} if and only if the next sequence of characters in the buffer
	 * are equal to the specified sequence (case insensitive).
	 * 
	 * @see #match(CharSequence)
	 * @see #matchIgnoreCase(char)
	 */
	public boolean matchIgnoreCase(CharSequence seq) {
		final int len = seq.length();

		if ( len >= this.length() ) {
			return false;
		}

		for ( int i = 0; i < len; i++ ) {
			if ( Character.toLowerCase( seq.charAt( i ) ) != Character.toLowerCase( text.charAt( position + i ) ) ) {
				return false;
			}
		}

		position += len;
		return true;
	}

	/**
	 * Returns the current parsing position.
	 */
	public int getPosition() {
		return position;
	}

}
