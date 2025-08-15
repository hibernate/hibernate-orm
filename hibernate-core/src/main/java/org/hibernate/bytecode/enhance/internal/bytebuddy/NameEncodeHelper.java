/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.bytecode.enhance.internal.bytebuddy;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

import static org.hibernate.bytecode.internal.bytebuddy.BytecodeProviderImpl.EMBEDDED_MEMBER;

public class NameEncodeHelper {

	public static String encodeName(String[] propertyNames, Member[] getters, Member[] setters) {
		final StringBuilder sb = new StringBuilder();
		for ( int i = 0; i < propertyNames.length; i++ ) {
			final String propertyName = propertyNames[i];
			final Member getter = getters[i];
			final Member setter = setters[i];
			// Encode the two member types as 4 bit integer encoded as hex character
			sb.append( Integer.toHexString( getKind( getter ) << 2 | getKind( setter ) ) );
			sb.append( propertyName );
		}
		return sb.toString();
	}

	private static int getKind(Member member) {
		// Encode the member type as 2 bit integer
		if ( member == EMBEDDED_MEMBER ) {
			return 0;
		}
		else if ( member instanceof Field ) {
			return 1;
		}
		else if ( member instanceof Method ) {
			return 2;
		}
		else if ( member instanceof ForeignPackageMember ) {
			return 3;
		}
		else {
			throw new IllegalArgumentException( "Unknown member type: " + member );
		}
	}

}
