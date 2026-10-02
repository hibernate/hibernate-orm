package org.hibernate.procedure.spi;

import org.hibernate.query.spi.QueryParameterBinding;

/**
 * Describes an input value binding for any IN/INOUT parameters.
 *
 * @author Steve Ebersole
 */
public interface ProcedureParameterBinding<T> extends QueryParameterBinding<T> {
}
