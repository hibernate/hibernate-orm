/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.spi;

import org.hibernate.Incubating;
import org.hibernate.engine.spi.SessionFactoryImplementor;

/**
 * Contract for Query impls that can be converted to named queries and
 * stored in the {@link org.hibernate.query.spi.NamedQueryRepository}
 *
 * @author Steve Ebersole
 */
@Incubating
public interface NameableQuery {
	NamedQueryMemento toMemento(String name, SessionFactoryImplementor factory);
}
