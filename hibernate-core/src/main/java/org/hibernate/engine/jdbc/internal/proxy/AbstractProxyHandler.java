/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.engine.jdbc.internal.proxy;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import org.hibernate.HibernateException;

/**
 * Basic support for building proxy handlers.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractProxyHandler implements InvocationHandler {
	private boolean valid = true;
	private final int hashCode;

	public AbstractProxyHandler(int hashCode) {
		this.hashCode = hashCode;
	}

	protected abstract Object continueInvocation(Object proxy, Method method, Object[] args) throws Throwable;

	public String toString() {
		return super.toString() + "[valid=" + valid + "]";
	}

	public final int hashCode() {
		return hashCode;
	}

	protected final boolean isValid() {
		return valid;
	}

	protected final void invalidate() {
		valid = false;
	}

	protected final void errorIfInvalid() {
		if ( !isValid() ) {
			throw new HibernateException( "proxy handle is no longer valid" );
		}
	}

	public final Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		String methodName = method.getName();

		// basic Object methods ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		if ( "toString".equals( methodName ) ) {
			return this.toString();
		}
		if ( "hashCode".equals( methodName ) ) {
			return this.hashCode();
		}
		if ( "equals".equals( methodName ) ) {
			return this.equals( args[0] );
		}

		return continueInvocation( proxy, method, args );
	}

}