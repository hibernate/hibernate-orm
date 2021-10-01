/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal.util;

/**
 * @author Christian Beikov
 */
public final class CharSequenceHelper {

	private CharSequenceHelper() {
	}

	public static CharSequence subSequence(CharSequence sequence, int start, int end) {
		if ( sequence instanceof SubSequence ) {
			return sequence.subSequence( start, end );
		}
		else {
			return new SubSequence( sequence, start, end );
		}
	}

	public static boolean isEmpty(CharSequence string) {
		return string == null || string.length() == 0;
	}

	public static int lastIndexOf(CharSequence charSequence, char c) {
		return lastIndexOf( charSequence, c, 0, charSequence.length() - 1 );
	}

	public static int lastIndexOf(CharSequence charSequence, char c, int fromIndex, int endIndex) {
		if ( charSequence instanceof String ) {
			int idx = ( (String) charSequence ).lastIndexOf( c, endIndex );
			if ( idx < fromIndex ) {
				return -1;
			}
			return idx;
		}
		else if ( charSequence instanceof SubSequence ) {
			int idx = ( (SubSequence) charSequence ).lastIndexOf( c, fromIndex, endIndex );
			if ( idx == -1 ) {
				return -1;
			}
			return idx;
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
		if ( charSequence instanceof String ) {
			int idx = ( (String) charSequence ).indexOf( c, fromIndex );
			if ( idx > endIndex ) {
				return -1;
			}
			return idx;
		}
		else if ( charSequence instanceof SubSequence ) {
			int idx = ( (SubSequence) charSequence ).indexOf( c, fromIndex, endIndex );
			if ( idx == -1 ) {
				return -1;
			}
			return idx;
		}
		else {
			return indexOf( charSequence.toString(), c, fromIndex, endIndex );
		}
	}

	public static int indexOf(CharSequence charSequence, String target, int fromIndex) {
		return indexOf( charSequence, target, fromIndex, charSequence.length() - 1 );
	}

	public static int indexOf(CharSequence charSequence, String target, int fromIndex, int endIndex) {
		if ( charSequence instanceof String ) {
			int idx = ( (String) charSequence ).indexOf( target, fromIndex );
			if ( idx > endIndex ) {
				return -1;
			}
			return idx;
		}
		else if ( charSequence instanceof SubSequence ) {
			int idx = ( (SubSequence) charSequence ).indexOf( target, fromIndex, endIndex );
			if ( idx == -1 ) {
				return -1;
			}
			return idx;
		}
		else {
			return indexOf( charSequence.toString(), target, fromIndex, endIndex );
		}
	}
}
