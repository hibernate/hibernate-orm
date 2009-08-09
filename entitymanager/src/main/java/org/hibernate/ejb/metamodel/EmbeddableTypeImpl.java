package org.hibernate.ejb.metamodel;

import java.util.Iterator;
import java.io.Serializable;
import javax.persistence.metamodel.EmbeddableType;
import javax.persistence.metamodel.Type;

import org.hibernate.mapping.Property;

/**
 * @author Emmanuel Bernard
 */
public class EmbeddableTypeImpl<X> extends ManagedTypeImpl<X> implements EmbeddableType<X>, Serializable {
	EmbeddableTypeImpl(Class<X> clazz, Iterator<Property> properties, MetadataContext context) {
		super(clazz, properties, context);
	}

	public PersistenceType getPersistenceType() {
		return PersistenceType.EMBEDDABLE;
	}
}
