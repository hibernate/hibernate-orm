/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.query;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.boot.internal.NamedNativeQueryDefinitionImpl;
import org.hibernate.boot.spi.AbstractNamedQueryDefinition;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.sql.spi.NamedNativeQueryMemento;

/**
 * Boot-time descriptor of a named native query, as defined in
 * annotations or xml
 *
 * @see jakarta.persistence.NamedNativeQuery
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
