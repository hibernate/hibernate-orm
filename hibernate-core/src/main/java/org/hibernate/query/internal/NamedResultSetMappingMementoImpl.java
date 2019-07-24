/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.internal;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.spi.NamedResultSetMappingMemento;
import org.hibernate.query.spi.ResultSetMapping;

/**
 * @author Steve Ebersole
 */
public class NamedResultSetMappingMementoImpl implements NamedResultSetMappingMemento {
	private final String name;

	public NamedResultSetMappingMementoImpl(String name, SessionFactoryImplementor factory) {
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public ResultSetMapping toResultSetMapping() {
		throw new NotYetImplementedFor6Exception( getClass() );
	}
}
