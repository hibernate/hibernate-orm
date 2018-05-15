/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.model.query.spi;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.named.spi.NamedNativeQueryMemento;

/**
 * @author Steve Ebersole
 */
public interface NamedNativeQueryDefinition extends NamedQueryDefinition {
	String getQueryString();
	String getResultSetMappingName();

	@Override
	NamedNativeQueryMemento resolve(SessionFactoryImplementor factory);
}
