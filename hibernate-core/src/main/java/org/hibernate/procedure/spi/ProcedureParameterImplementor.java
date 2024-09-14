/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.procedure.spi;

import org.hibernate.Incubating;
import org.hibernate.query.procedure.ProcedureParameter;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.sql.exec.spi.JdbcCallParameterRegistration;

/**
 * SPI extension for ProcedureParameter
 *
 * @author Steve Ebersole
 */
@Incubating
public interface ProcedureParameterImplementor<T> extends ProcedureParameter<T>, QueryParameterImplementor<T> {

	JdbcCallParameterRegistration toJdbcParameterRegistration(int startIndex, ProcedureCallImplementor<?> procedureCall);

}
