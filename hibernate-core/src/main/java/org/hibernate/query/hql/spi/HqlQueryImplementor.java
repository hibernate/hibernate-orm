/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.hql.spi;

import org.hibernate.query.spi.NameableQuery;
import org.hibernate.query.spi.ParameterMetadataImplementor;
import org.hibernate.query.spi.QueryImplementor;

/**
 * @author Steve Ebersole
 */
public interface HqlQueryImplementor<R> extends QueryImplementor<R>, NameableQuery {
	@Override
	NamedHqlQueryMemento toMemento(String name);

	@Override
	ParameterMetadataImplementor getParameterMetadata();
}
