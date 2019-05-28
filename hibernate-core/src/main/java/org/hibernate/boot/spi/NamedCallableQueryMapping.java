/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.spi;

import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.procedure.spi.NamedCallableQueryMemento;

/**
 * Named query mapping for callable queries
 *
 * @author Steve Ebersole
 * @author Gavin King
 */
public interface NamedCallableQueryMapping extends NamedQueryMapping {
	List<String> getResultSetMappingNames();
	List<Class> getResultSetMappingClasses();

	@Override
	NamedCallableQueryMemento resolve(SessionFactoryImplementor factory);
}
