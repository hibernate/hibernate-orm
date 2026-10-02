package org.hibernate.query.named.spi;

/**
 * @author Steve Ebersole
 */
public interface TypedQueryReferenceProducer {
	NamedSelectionMemento<?> toSelectionMemento(String name);
}
