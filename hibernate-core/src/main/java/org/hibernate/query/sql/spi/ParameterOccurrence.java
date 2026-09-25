package org.hibernate.query.sql.spi;

import org.hibernate.query.spi.QueryParameterImplementor;

/**
 * @author Christian Beikov
 */
public record ParameterOccurrence(QueryParameterImplementor<?> parameter, int sourcePosition) {
}
