/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.bytecode.enhance.spi.interceptor;

import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractLazyLoadInterceptor extends AbstractInterceptor implements BytecodeLazyAttributeInterceptor {

	@SuppressWarnings("unused")
	public AbstractLazyLoadInterceptor(String entityName) {
		super( entityName );
	}

	public AbstractLazyLoadInterceptor(String entityName, SharedSessionContractImplementor session) {
		super( entityName );
		setSession( session );
	}
}
