/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.named;

/**
 * @author Steve Ebersole
 */
public interface StatementReferenceProducer {
	NamedMutationMemento<?> toMutationMemento(String name);
}
