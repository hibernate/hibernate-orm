/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.exec.spi;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.sql.ast.consume.spi.ParameterBindingResolutionContext;

/**
 * Contextual information for performing JDBC parameter binding.  Generally
 * speaking this is the source of all bind values
 *
 * @author Steve Ebersole
 */
public interface ParameterBindingContext extends ParameterBindingResolutionContext {
	SharedSessionContractImplementor getSession();

	@Override
	default SessionFactoryImplementor getSessionFactory() {
		return getSession().getFactory();
	}
}
