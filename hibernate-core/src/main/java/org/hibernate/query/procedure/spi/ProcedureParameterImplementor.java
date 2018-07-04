/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.procedure.spi;

import org.hibernate.Incubating;
import org.hibernate.procedure.spi.ParameterRegistrationImplementor;
import org.hibernate.query.procedure.ProcedureParameter;

/**
 * NOTE: Consider this contract (and its sub-contracts) as incubating as we transition to 6.0 and SQM
 *
 * @author Steve Ebersole
 */
@Incubating
public interface ProcedureParameterImplementor<T> extends ProcedureParameter<T>, ParameterRegistrationImplementor<T> {
}
