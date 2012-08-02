/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.internal.source.annotations;

import java.util.Map;

import org.hibernate.metamodel.spi.source.JpaCallbackSource;

/**
 * @author Hardy Ferentschik
 */
public class JpaCallbackSourceImpl implements JpaCallbackSource {

	private final Map<Class<?>, String> callbacksByType;
	private final String name;
	private final boolean isListener;

	public JpaCallbackSourceImpl(String name,
						  Map<Class<?>, String> callbacksByType,
						  boolean isListener) {
		this.name = name;
		this.callbacksByType = callbacksByType;
		this.isListener = isListener;
	}

	@Override
	public String getCallbackMethod(Class<?> callbackType) {
		return callbacksByType.get( callbackType );
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean isListener() {
		return isListener;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "JpaCallbackSourceImpl" );
		sb.append( "{name='" ).append( name ).append( '\'' );
		sb.append( ", isListener=" ).append( isListener );
		sb.append( '}' );
		return sb.toString();
	}
}


