/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.spi;

import java.io.Serializable;

import org.hibernate.Incubating;
import org.hibernate.query.Query;

/**
 * @author Steve Ebersole
 */
@Incubating
public interface QueryImplementor<R> extends Query<R> {
	@Override
	QueryProducerImplementor getProducer();

	void setOptionalId(Serializable id);

	void setOptionalEntityName(String entityName);

	void setOptionalObject(Object optionalObject);
}
