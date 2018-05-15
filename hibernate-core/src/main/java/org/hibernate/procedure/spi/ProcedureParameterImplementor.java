/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.procedure.spi;

import org.hibernate.procedure.ProcedureParameter;
import org.hibernate.query.spi.QueryParameterImplementor;

/**
 * @author Steve Ebersole
 */
public interface ProcedureParameterImplementor<T> extends ProcedureParameter<T>, QueryParameterImplementor<T> {

}
