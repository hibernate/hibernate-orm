/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.spi;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.named.spi.NameableQuery;
import org.hibernate.query.named.spi.NamedHqlQueryMemento;

/**
 * @author Steve Ebersole
 */
public interface HqlQueryImplementor<R> extends QueryImplementor<R>, NameableQuery {
	@Override
	NamedHqlQueryMemento toMemento(String name, SessionFactoryImplementor factory);

	@Override
	ParameterMetadataImplementor<QueryParameterImplementor<?>> getParameterMetadata();
}
