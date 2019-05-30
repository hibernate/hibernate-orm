/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.spi;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.hql.spi.NamedHqlQueryMemento;

/**
 * Boot-time descriptor of a named HQL query, as defined in
 * annotations or xml
 *
 * @see javax.persistence.NamedQuery
 * @see org.hibernate.annotations.NamedQuery
 *
 * @author Steve Ebersole
 * @author Gavin King
 */
public interface NamedHqlQueryDefinition extends NamedQueryDefinition {
	String getHqlString();

	@Override
	NamedHqlQueryMemento resolve(SessionFactoryImplementor factory);
}
