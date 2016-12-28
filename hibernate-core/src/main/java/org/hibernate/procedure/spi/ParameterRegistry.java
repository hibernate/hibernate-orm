/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.procedure.spi;

import java.util.List;

import org.hibernate.query.spi.ParameterMetadataImplementor;
import org.hibernate.query.spi.QueryParameterBindings;

/**
 * @author Steve Ebersole
 */
public interface ParameterRegistry extends ParameterMetadataImplementor, QueryParameterBindings {
	ProcedureCallImplementor getProcedureCall();

	ParameterStrategy getParameterStrategy();
	List<ParameterRegistrationImplementor> getParameterRegistrations();

	boolean hasAnyParameterRegistrations();
}
