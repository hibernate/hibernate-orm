package org.hibernate.metamodel.mapping;

/**
 * An "aggregated" composite identifier, which is another way to say that the
 * identifier is represented as an {@linkplain jakarta.persistence.EmbeddedId embeddable}.
 *
 * @see jakarta.persistence.EmbeddedId
 *
 * @author Steve Ebersole
 */
public interface AggregatedIdentifierMapping extends SingleAttributeIdentifierMapping {
}
