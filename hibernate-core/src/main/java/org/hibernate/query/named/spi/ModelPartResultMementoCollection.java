package org.hibernate.query.named.spi;

import org.hibernate.metamodel.mapping.PluralAttributeMapping;

/**
 * @author Steve Ebersole
 */
public interface ModelPartResultMementoCollection extends ModelPartResultMemento {
	PluralAttributeMapping getPluralAttributeDescriptor();
}
