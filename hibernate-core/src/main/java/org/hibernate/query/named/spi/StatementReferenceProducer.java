package org.hibernate.query.named.spi;

/**
 * @author Steve Ebersole
 */
public interface StatementReferenceProducer {
	NamedMutationMemento<?> toMutationMemento(String name);
}
