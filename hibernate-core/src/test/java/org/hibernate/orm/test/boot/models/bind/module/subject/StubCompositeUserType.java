/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.bind.module.subject;

import java.io.Serializable;

import org.hibernate.metamodel.spi.ValueAccess;
import org.hibernate.usertype.CompositeUserType;

public class StubCompositeUserType implements CompositeUserType<StubDomainType> {
	@Override
	public Object getPropertyValue(StubDomainType component, int property) {
		return component.value;
	}

	@Override
	public StubDomainType instantiate(ValueAccess values) {
		return new StubDomainType( values.getValue( 0, String.class ) );
	}

	@Override
	public Class<?> embeddable() {
		return StubEmbeddable.class;
	}

	@Override
	public Class<StubDomainType> returnedClass() {
		return StubDomainType.class;
	}

	@Override
	public boolean equals(StubDomainType x, StubDomainType y) {
		return x == y || ( x != null && y != null && x.value.equals( y.value ) );
	}

	@Override
	public int hashCode(StubDomainType x) {
		return x == null ? 0 : x.value.hashCode();
	}

	@Override
	public StubDomainType deepCopy(StubDomainType value) {
		return value;
	}

	@Override
	public boolean isMutable() {
		return false;
	}

	@Override
	public Serializable disassemble(StubDomainType value) {
		return value == null ? null : value.value;
	}

	@Override
	public StubDomainType assemble(Serializable cached, Object owner) {
		return cached == null ? null : new StubDomainType( (String) cached );
	}

	@Override
	public StubDomainType replace(StubDomainType detached, StubDomainType managed, Object owner) {
		return detached;
	}
}
