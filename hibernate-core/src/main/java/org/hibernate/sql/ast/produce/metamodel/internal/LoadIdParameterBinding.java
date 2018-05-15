/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.metamodel.internal;

import java.util.Collection;
import java.util.List;
import javax.persistence.TemporalType;

import org.hibernate.metamodel.model.domain.spi.AllowableParameterType;
import org.hibernate.query.spi.QueryParameterBinding;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public class LoadIdParameterBinding<T> implements QueryParameterBinding<T> {
	private final List<T> values;
	private final AllowableParameterType<T> type;
	private final int idValueIndex;

	public LoadIdParameterBinding(List<T> values, AllowableParameterType<T> type, int idValueIndex) {
		this.values = values;
		this.type = type;
		this.idValueIndex = idValueIndex;
	}

	@Override
	public boolean isBound() {
		return true;
	}

	@Override
	public boolean allowsMultiValued() {
		return false;
	}

	@Override
	public boolean isMultiValued() {
		return false;
	}

	@Override
	public AllowableParameterType<T> getBindType() {
		return type;
	}

	@Override
	public void setBindValue(T value, AllowableParameterType<T> clarifiedType) {
		throw new UnsupportedOperationException( "Cannot change parameter binding value" );
	}

	@Override
	public void setBindValue(Object value) {
		throw new UnsupportedOperationException( "Cannot change parameter binding value" );
	}

	@Override
	public void setBindValue(Object value, TemporalType temporalTypePrecision) {
		throw new UnsupportedOperationException( "Cannot change parameter binding value" );
	}

	@Override
	public T getBindValue() {
		return values.get( idValueIndex );
	}

	@Override
	public void setBindValues(
			Collection<T> values, AllowableParameterType<T> clarifiedType) {
		throw new UnsupportedOperationException( "Cannot change parameter binding value" );

	}

	@Override
	public void setBindValues(Collection values) {
		throw new UnsupportedOperationException( "Cannot change parameter binding value" );
	}

	@Override
	public void setBindValues(
			Collection values,
			TemporalType temporalTypePrecision,
			TypeConfiguration typeConfiguration) {
		throw new UnsupportedOperationException( "Cannot change parameter binding value" );
	}

	@Override
	public Collection<T> getBindValues() {
		return null;
	}
}
