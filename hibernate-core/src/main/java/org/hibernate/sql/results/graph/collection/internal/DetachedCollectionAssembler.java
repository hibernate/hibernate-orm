/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph.collection.internal;

import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Gavin King
 */
public class DetachedCollectionAssembler<T> implements DomainResultAssembler<T> {
	private final PluralAttributeMapping collectionAttribute;
	private final CollectionPart.Nature selectedPartNature;
	private final DomainResultAssembler<?> collectionKeyAssembler;
	private final JavaType<?> resultJavaType;

	public DetachedCollectionAssembler(
			PluralAttributeMapping collectionAttribute,
			CollectionPart.Nature selectedPartNature,
			DomainResultAssembler<?> collectionKeyAssembler,
			JavaType<?> resultJavaType) {
		this.collectionAttribute = collectionAttribute;
		this.selectedPartNature = selectedPartNature;
		this.collectionKeyAssembler = collectionKeyAssembler;
		this.resultJavaType = resultJavaType;
	}

	@Override
	@SuppressWarnings("unchecked")
	public T assemble(RowProcessingState rowProcessingState) {
		final Object collectionKey = collectionKeyAssembler.assemble( rowProcessingState );
		return (T) DetachedCollectionHelper.loadAndCopy(
				collectionAttribute,
				collectionKey,
				rowProcessingState.getSession(),
				selectedPartNature
		);
	}

	@Override
	@SuppressWarnings("unchecked")
	public JavaType<T> getAssembledJavaType() {
		return (JavaType<T>) resultJavaType;
	}

	@Override
	public void resolveState(RowProcessingState rowProcessingState) {
		collectionKeyAssembler.resolveState( rowProcessingState );
	}
}
