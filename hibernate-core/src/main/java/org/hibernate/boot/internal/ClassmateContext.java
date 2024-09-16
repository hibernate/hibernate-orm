/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.internal;

import com.fasterxml.classmate.MemberResolver;
import com.fasterxml.classmate.TypeResolver;

/**
 * @author Steve Ebersole
 */
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
