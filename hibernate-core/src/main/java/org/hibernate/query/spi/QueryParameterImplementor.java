/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.spi;

import org.hibernate.query.QueryParameter;
import org.hibernate.query.named.NamedQueryMemento;
import org.hibernate.type.BindableType;

/**
 * @author Steve Ebersole
 */
public interface QueryParameterImplementor<T> extends QueryParameter<T> {
	void disallowMultiValuedBinding();

	void applyAnticipatedType(BindableType<?> type);

	NamedQueryMemento.ParameterMemento toMemento();
}
