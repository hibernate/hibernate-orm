/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.internal.util.StringHelper;

/**
 * @author Gavin King
 */
public class Replacer {
	private final String[] chunks;
	private final String quote;
	private final String delimiter;
	private final List<Replacement> replacements = new ArrayList<>();

	static class Replacement {
		String placeholder;
		String replacement;

		Replacement(String placeholder, String replacement) {
			this.placeholder = placeholder;
			this.replacement = replacement;
		}

//		boolean matches(StringBuilder string, int position) {
//			return string.indexOf( placeholder, position ) >= 0;
//		}
//
		int apply(StringBuilder string, int position) {
			if ( position + placeholder.length() > string.length() ) {
				return -1;
			}
			for (int index = 0; index < placeholder.length(); index++ ) {
				if ( string.charAt( position + index ) != placeholder.charAt( index ) ) {
					return -1;
				}
			}
			string.replace( position, position + placeholder.length(), replacement );
			return replacement.length();
		}
	}

	public Replacer(String format, String quote, String delimiter) {
		this.delimiter = delimiter;
		this.chunks = StringHelper.splitFull( quote, format );
		this.quote = quote;
	}

	public Replacer replace(String placeholder, String replacement) {
		for ( Replacement old : replacements ) {
			if ( old.placeholder.equals( placeholder ) ) {
				old.replacement = replacement;
				return this;
			}
		}
		replacements.add( new Replacement( placeholder, replacement ) );
		return this;
	}

	public String result() {
		for ( int i=0; i<chunks.length; i+=2 ) {
			StringBuilder chunk = new StringBuilder( chunks[i] );
			for ( int position = 0; position < chunk.length(); position++ ) {
				for ( Replacement replacement : replacements ) {
					int result = replacement.apply( chunk, position );
					if ( result >= 0 ) {
						position += result - 1;
						break;
					}
				}
			}
			chunks[i] = chunk.toString();
		}
		for ( int i=1; i<chunks.length; i+=2 ) {
			if ( chunks[i].isEmpty() ) {
				//a doubled quote character is
				//interpreted as a literal quote
				if ( delimiter.equals( quote ) ) {
					chunks[i] = quote + quote;
				}
				if ( delimiter.equals( quote + quote ) ) {
					chunks[i] = quote + quote + quote + quote;
				}
				else {
					chunks[i] = quote;
				}
			}
			else if ( !delimiter.isEmpty() && !delimiter.equals(quote) ) {
				chunks[i] = chunks[i].replace( delimiter, "\\" + delimiter );
			}
		}
		return String.join( delimiter, chunks );
	}
}
