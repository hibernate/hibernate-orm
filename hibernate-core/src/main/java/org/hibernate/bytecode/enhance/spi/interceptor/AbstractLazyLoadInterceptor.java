/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
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

	@SuppressWarnings("WeakerAccess")
	public AbstractLazyLoadInterceptor(String entityName, SharedSessionContractImplementor session) {
		super( entityName );
		setSession( session );
	}
}
