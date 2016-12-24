/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.query.internal;

import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.type.spi.Type;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractQueryParameter<T> implements QueryParameterImplementor<T> {
	private static final Logger log = Logger.getLogger( AbstractQueryParameter.class );

	private boolean allowMultiValuedBinding;
	private boolean isPassNullsEnabled;
	private Type anticipatedType;

	public AbstractQueryParameter(
			boolean allowMultiValuedBinding,
			boolean isPassNullsEnabled,
			Type anticipatedType) {
		this.allowMultiValuedBinding = allowMultiValuedBinding;
		this.isPassNullsEnabled = isPassNullsEnabled;
		this.anticipatedType = anticipatedType;
	}

	@Override
	public void allowMultiValuedBinding() {
		log.debugf( "QueryParameter#allowMultiValuedBinding() called" );
		this.allowMultiValuedBinding = true;
	}

	@Override
	public boolean allowsMultiValuedBinding() {
		return allowMultiValuedBinding;
	}

	@Override
	public boolean isPassNullsEnabled() {
		return isPassNullsEnabled;
	}

	@Override
	public Type getHibernateType() {
		return null;
	}

	@Override
	public String getName() {
		return null;
	}

	@Override
	public Integer getPosition() {
		return null;
	}

	@Override
	public Class<T> getParameterType() {
		return null;
	}
}
