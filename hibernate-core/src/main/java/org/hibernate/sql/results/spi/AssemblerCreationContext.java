/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.spi;

import org.hibernate.engine.spi.SessionFactoryImplementor;

/**
 * Access to information available within the context for creating
 * {@link DomainResultAssembler} instances.
 *
 * @see DomainResult#createResultAssembler
 *
 * @author Steve Ebersole
 */
public interface AssemblerCreationContext {
	SessionFactoryImplementor getSessionFactory();
}
