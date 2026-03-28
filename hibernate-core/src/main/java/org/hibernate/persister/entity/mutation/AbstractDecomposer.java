/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

import org.hibernate.action.internal.EntityAction;
import org.hibernate.action.queue.graph.MutationDecomposer;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.generator.OnExecutionGenerator;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.model.ast.builder.AssigningTableMutationBuilder;

/// Base support for [EntityAction]-based [MutationDecomposer] implementations.
///
/// @author Steve Ebersole
public abstract class AbstractDecomposer<T extends EntityAction> implements MutationDecomposer<T> {
	protected final EntityPersister entityPersister;
	protected final SessionFactoryImplementor sessionFactory;

	public AbstractDecomposer(EntityPersister entityPersister, SessionFactoryImplementor sessionFactory) {
		this.entityPersister = entityPersister;
		this.sessionFactory = sessionFactory;
	}

	protected void handleValueGeneration(
			AttributeMapping attributeMapping,
			AssigningTableMutationBuilder<?> builder,
			OnExecutionGenerator generator) {
		final Dialect dialect = sessionFactory.getJdbcServices().getDialect();
		final boolean writePropertyValue = generator.writePropertyValue();
		final String[] columnValues = writePropertyValue ? null : generator.getReferencedColumnValues( dialect );
		attributeMapping.forEachSelectable( (j, mapping) -> {
			if ( writePropertyValue ) {
				builder.addValueColumn( mapping );
			}
			else {
				builder.addValueColumn( columnValues[j], mapping );
			}
		} );
	}
}
