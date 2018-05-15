/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.consume.spi;

import org.hibernate.engine.spi.SessionFactoryImplementor;

/**
 * @author Steve Ebersole
 */
public interface ConversionContext {
	/**
	 * Retrieves the SessionFactory for this context.
	 */
	SessionFactoryImplementor getSessionFactory();
}
