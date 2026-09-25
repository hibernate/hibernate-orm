package org.hibernate.metamodel.model.domain;

import org.hibernate.Incubating;

/**
 * Specialization of {@link SimpleDomainType} for types that can
 * be used as function returns.
 *
 * @author Steve Ebersole
 */
@Incubating(since = "5.4")
public interface ReturnableType<T> extends SimpleDomainType<T> {
}
