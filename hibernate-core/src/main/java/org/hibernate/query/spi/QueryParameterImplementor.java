/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.spi;

import org.hibernate.query.BindableType;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.named.NamedQueryMemento;

/**
 * @author Steve Ebersole
 */
public interface QueryParameterImplementor<T> extends QueryParameter<T> {
	void disallowMultiValuedBinding();

	void applyAnticipatedType(BindableType<?> type);

	NamedQueryMemento.ParameterMemento toMemento();
}
