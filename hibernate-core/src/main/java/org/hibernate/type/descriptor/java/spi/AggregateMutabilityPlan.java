/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.type.descriptor.java.spi;

import org.hibernate.SharedSessionContract;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.format.FormatMapper;

import java.io.Serializable;

public abstract class AggregateMutabilityPlan<A> implements MutabilityPlan<A> {

	protected AggregateMutabilityPlan() {
	}

	@Override
	public boolean isMutable() {
		return true;
	}

	@Override
	public A deepCopy(A value) {
		throw new UnsupportedOperationException("WrapperOptions for retrieving FormatMapper is required, please use deepCopy(T, WrapperOptions)");
	}

	@Override
	public A deepCopy(A value, WrapperOptions options) {
		if (value == null || options == null) {
			return null;
		}
		FormatMapper formatMapper = getFormatMapper(options);
		if (formatMapper == null) {
			return null;
		}

		String serializedValue = formatMapper.toString(value, new UnknownBasicJavaType<>((Class<A>) value.getClass()), options);
		return formatMapper.fromString(serializedValue, new UnknownBasicJavaType<>((Class<A>) value.getClass()), options);
	}

	@Override
	public Serializable disassemble(A value, SharedSessionContract session) {
		return (Serializable) deepCopy(value);
	}

	@Override
	public A assemble(Serializable cached, SharedSessionContract session) {
		//noinspection unchecked
		return deepCopy((A) cached);
	}

	protected abstract FormatMapper getFormatMapper(WrapperOptions options);
}
