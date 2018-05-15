/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.procedure.spi;

import org.hibernate.metamodel.model.domain.spi.AllowableParameterType;
import org.hibernate.procedure.ProcedureParameterBinding;

/**
 * @author Steve Ebersole
 */
public interface ProcedureParameterBindingImplementor<T> extends ProcedureParameterBinding<T> {
	@Override
	default AllowableParameterType<T> getBindType() {
		return null;
	}
}
