/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.spi;

import com.fasterxml.classmate.MemberResolver;
import com.fasterxml.classmate.TypeResolver;
import org.hibernate.Incubating;

/**
 * Exposes the Classmate {@link TypeResolver} and {@link MemberResolver}.
 *
 * @author Steve Ebersole
 */
@Incubating
public class ClassmateContext {
	private TypeResolver typeResolver = new TypeResolver();
	private MemberResolver memberResolver = new MemberResolver( typeResolver );

	public TypeResolver getTypeResolver() {
		if ( typeResolver == null ) {
			throw new IllegalStateException( "Classmate context has been released" );
		}
		return typeResolver;
	}

	public MemberResolver getMemberResolver() {
		if ( memberResolver == null ) {
			throw new IllegalStateException( "Classmate context has been released" );
		}
		return memberResolver;
	}

	public void release() {
		typeResolver = null;
		memberResolver = null;
	}
}
