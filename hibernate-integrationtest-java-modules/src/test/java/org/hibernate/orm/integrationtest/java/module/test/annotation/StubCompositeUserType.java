/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.integrationtest.java.module.test.annotation;

import java.io.Serializable;

import org.hibernate.metamodel.spi.ValueAccess;
import org.hibernate.usertype.CompositeUserType;

public class StubCompositeUserType implements CompositeUserType<StubCompositeType> {
	@Override
	public Object getPropertyValue(StubCompositeType component, int property) {
		return component.value;
	}

	@Override
	public StubCompositeType instantiate(ValueAccess values) {
		return new StubCompositeType( values.getValue( 0, String.class ) );
	}

	@Override
	public Class<?> embeddable() {
		return StubEmbeddable.class;
	}

	@Override
	public Class<StubCompositeType> returnedClass() {
		return StubCompositeType.class;
	}

	@Override
	public boolean equals(StubCompositeType x, StubCompositeType y) {
		return x == y || ( x != null && y != null && x.value.equals( y.value ) );
	}

	@Override
	public int hashCode(StubCompositeType x) {
		return x == null ? 0 : x.value.hashCode();
	}

	@Override
	public StubCompositeType deepCopy(StubCompositeType value) {
		return value;
	}

	@Override
	public boolean isMutable() {
		return false;
	}

	@Override
	public Serializable disassemble(StubCompositeType value) {
		return value == null ? null : value.value;
	}

	@Override
	public StubCompositeType assemble(Serializable cached, Object owner) {
		return cached == null ? null : new StubCompositeType( (String) cached );
	}

	@Override
	public StubCompositeType replace(StubCompositeType detached, StubCompositeType managed, Object owner) {
		return detached;
	}
}
