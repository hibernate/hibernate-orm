/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.named;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.spi.QueryImplementor;

/**
 * Specialization of NamedQueryMemento for mementos which can produce
 * {@link org.hibernate.query.spi.QueryImplementor} implementations
 *
 * @author Steve Ebersole
 */
public interface NamedQueryProducer extends NamedQueryMemento {
	QueryImplementor<?> toQuery(SharedSessionContractImplementor session);
	<T> QueryImplementor<T> toQuery(SharedSessionContractImplementor session, Class<T> javaType);
}
