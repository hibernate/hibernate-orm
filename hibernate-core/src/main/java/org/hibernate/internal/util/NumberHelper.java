/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal.util;


public final class NumberHelper {

	private NumberHelper() {
	}

	public static int digitCount(int number) {
		if ( number < 100000 ) {
			if ( number < 100 ) {
				if ( number < 10 ) {
					return 1;
				}
				else {
					return 2;
				}
			}
			else {
				if ( number < 1000 ) {
					return 3;
				}
				else {
					if ( number < 10000 ) {
						return 4;
					}
					else {
						return 5;
					}
				}
			}
		}
		else {
			if ( number < 10000000 ) {
				if ( number < 1000000 ) {
					return 6;
				}
				else {
					return 7;
				}
			}
			else {
				if ( number < 100000000 ) {
					return 8;
				}
				else {
					if ( number < 1000000000 ) {
						return 9;
					}
					else {
						return 10;
					}
				}
			}
		}
	}
}
