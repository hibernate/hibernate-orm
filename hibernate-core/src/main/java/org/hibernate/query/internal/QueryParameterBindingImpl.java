/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.internal;

import java.util.Collection;
import javax.persistence.TemporalType;

import org.hibernate.query.QueryParameter;
import org.hibernate.query.spi.QueryParameterBinding;
import org.hibernate.query.spi.QueryParameterBindingTypeResolver;
import org.hibernate.type.spi.Type;

/**
 * The standard Hibernate QueryParameterBinding implementation
 *
 * @author Steve Ebersole
 */
public class QueryParameterBindingImpl<T> implements QueryParameterBinding<T> {
	private final QueryParameter<T> queryParameter;
	private final QueryParameterBindingTypeResolver typeResolver;

	private boolean isBound;
	private boolean isMultiValued;

	private Type bindType;

	private T bindValue;
	private Collection<T> bindValues;

	public QueryParameterBindingImpl(
			QueryParameter<T> queryParameter,
			QueryParameterBindingTypeResolver typeResolver) {
		this( queryParameter.getType(), queryParameter, typeResolver );
	}

	public QueryParameterBindingImpl(
			Type bindType,
			QueryParameter<T> queryParameter,
			QueryParameterBindingTypeResolver typeResolver) {
		this.bindType = bindType;
		this.queryParameter = queryParameter;
		this.typeResolver = typeResolver;
	}

	@Override
	public Type getBindType() {
		return bindType;
	}

	@Override
	public boolean allowsMultiValued() {
		return queryParameter.allowsMultiValuedBinding();
	}

	@Override
	public boolean isBound() {
		return isBound;
	}

	@Override
	public boolean isMultiValued() {
		return isMultiValued;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// single-valued binding support

	@Override
	public T getBindValue() {
		if ( isMultiValued ) {
			throw new IllegalStateException( "Binding is multi-valued; illegal call to #getBindValue" );
		}

		return bindValue;
	}

	@Override
	public void setBindValue(T value) {
		this.isBound = true;
		this.isMultiValued = false;

		this.bindValue = value;
		this.bindValues = null;

		if ( bindType == null ) {
			this.bindType = typeResolver.resolveParameterBindType( value );
		}
	}

	@Override
	public void setBindValue(T value, Type clarifiedType) {
		setBindValue( value );
		if ( clarifiedType != null ) {
			this.bindType = clarifiedType;
		}
	}

	@Override
	public void setBindValue(T value, TemporalType clarifiedTemporalType) {
		setBindValue( value );
		this.bindType = BindingTypeHelper.INSTANCE.determineTypeForTemporalType( clarifiedTemporalType, bindType, value );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// multi-valued binding support

	@Override
	public Collection<T> getBindValues() {
		if ( !isMultiValued ) {
			throw new IllegalStateException( "Binding is not multi-valued; illegal call to #getBindValues" );
		}

		return bindValues;
	}

	@Override
	public void setBindValues(Collection<T> values) {
		this.isBound = true;
		this.isMultiValued = true;

		this.bindValue = null;
		this.bindValues = values;

		if ( bindType == null && !values.isEmpty() ) {
			this.bindType = typeResolver.resolveParameterBindType( values.iterator().next() );
		}

	}

	@Override
	public void setBindValues(Collection<T> values, Type clarifiedType) {
		setBindValues( values );
		if ( clarifiedType != null ) {
			this.bindType = clarifiedType;
		}
	}

	@Override
	public void setBindValues(Collection<T> values, TemporalType clarifiedTemporalType) {
		setBindValues( values );
		final Object exampleValue = values.isEmpty() ? null : values.iterator().next();
		this.bindType = BindingTypeHelper.INSTANCE.determineTypeForTemporalType( clarifiedTemporalType, bindType, exampleValue );
	}
}
