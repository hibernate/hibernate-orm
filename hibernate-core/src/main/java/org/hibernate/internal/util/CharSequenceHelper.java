/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.util;

/**
 * @author Christian Beikov
 */
public final class CharSequenceHelper {

	private CharSequenceHelper() {
	}

	public static CharSequence subSequence(CharSequence sequence, int start, int end) {
		if ( start == 0 && end == sequence.length() ) {
			return sequence;
		}
		else if ( sequence instanceof SubSequence ) {
			return sequence.subSequence( start, end );
		}
		else {
			return new SubSequence( sequence, start, end );
		}
	}

	public static CharSequence subSequence(CharSequence sequence) {
		return subSequence(sequence, 0, sequence.length());
	}

	public static boolean isEmpty(CharSequence string) {
		return string == null || string.length() == 0;
	}

	public static int lastIndexOf(CharSequence charSequence, char c) {
		return lastIndexOf( charSequence, c, 0, charSequence.length() - 1 );
	}

	public static int lastIndexOf(CharSequence charSequence, char c, int fromIndex, int endIndex) {
		if ( charSequence instanceof String string ) {
			int idx = string.lastIndexOf( c, endIndex );
			if ( idx < fromIndex ) {
				return -1;
			}
			return idx;
		}
		else if ( charSequence instanceof SubSequence subSequence ) {
			return subSequence.lastIndexOf( c, fromIndex, endIndex );
		}
		else {
			return lastIndexOf( charSequence.toString(), c, fromIndex, endIndex );
		}
	}

	public static int indexOf(CharSequence charSequence, char c) {
		return indexOf( charSequence, c, 0 );
	}

	public static int indexOf(CharSequence charSequence, char c, int fromIndex) {
		return indexOf( charSequence, c, fromIndex, charSequence.length() - 1 );
	}

	public static int indexOf(CharSequence charSequence, char c, int fromIndex, int endIndex) {
		if ( charSequence instanceof String string ) {
			int idx = string.indexOf( c, fromIndex );
			if ( idx > endIndex ) {
				return -1;
			}
			return idx;
		}
		else if ( charSequence instanceof SubSequence subSequence ) {
			return subSequence.indexOf( c, fromIndex, endIndex );
		}
		else {
			return indexOf( charSequence.toString(), c, fromIndex, endIndex );
		}
	}

	public static int indexOf(CharSequence charSequence, String target, int fromIndex) {
		return indexOf( charSequence, target, fromIndex, charSequence.length() - 1 );
	}

	public static int indexOf(CharSequence charSequence, String target, int fromIndex, int endIndex) {
		if ( charSequence instanceof String string ) {
			int idx = string.indexOf( target, fromIndex );
			if ( idx > endIndex ) {
				return -1;
			}
			return idx;
		}
		else if ( charSequence instanceof SubSequence subSequence ) {
			return subSequence.indexOf( target, fromIndex, endIndex );
		}
		else {
			return indexOf( charSequence.toString(), target, fromIndex, endIndex );
		}
	}

	public static boolean regionMatchesIgnoreCase(
			CharSequence lhs,
			int lhsStart,
			CharSequence rhs,
			int rhsStart,
			int length) {
		if ( lhsStart + length <= lhs.length() && rhsStart + length <= rhs.length() ) {
			for ( int i = 0; i < length; i++ ) {
				final char c1 = lhs.charAt( lhsStart + i );
				final char c2 = rhs.charAt( rhsStart + i );
				if ( c1 != c2 && Character.toLowerCase( c1 ) != Character.toLowerCase( c2 ) ) {
					return false;
				}
			}
			return true;
		}
		return false;
	}
}
