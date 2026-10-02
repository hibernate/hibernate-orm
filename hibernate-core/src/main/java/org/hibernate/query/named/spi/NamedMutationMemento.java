package org.hibernate.query.named.spi;

import org.hibernate.query.spi.JpaStatementReference;

/**
 * @author Steve Ebersole
 */
public interface NamedMutationMemento<T> extends NamedQueryMemento<T>, JpaStatementReference<T> {
}
