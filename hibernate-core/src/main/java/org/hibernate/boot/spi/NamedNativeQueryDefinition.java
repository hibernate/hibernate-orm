/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.spi;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.sql.spi.NamedNativeQueryMemento;

/**
 * Boot-time descriptor of a named native query, as defined in
 * annotations or xml
 *
 * @see javax.persistence.NamedNativeQuery
 * @see org.hibernate.annotations.NamedNativeQuery
 *
 * @author Steve Ebersole
 * @author Gavin King
 */
public interface NamedNativeQueryDefinition extends NamedQueryDefinition {
	String getSqlQueryString();

	String getResultSetMappingName();
	String getResultSetMappingClassName();

	@Override
	NamedNativeQueryMemento resolve(SessionFactoryImplementor factory);
}
