/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

import org.hibernate.action.internal.EntityAction;
import org.hibernate.action.queue.graph.MutationDecomposer;
import org.hibernate.action.queue.meta.ColumnDescriptor;
import org.hibernate.action.queue.mutation.ast.builder.AssigningGraphTableMutationBuilder;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.generator.OnExecutionGenerator;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.persister.entity.EntityPersister;

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
			ColumnDescriptor columnDescriptor,
			AssigningGraphTableMutationBuilder<?, ?> builder,
			OnExecutionGenerator generator) {
		final Dialect dialect = sessionFactory.getJdbcServices().getDialect();
		final boolean writePropertyValue = generator.writePropertyValue();
		if ( writePropertyValue ) {
			builder.addValueColumn( columnDescriptor );
			// EARLY EXIT!!
			return;
		}

		final String[] columnValues = generator.getReferencedColumnValues( dialect );
		final String valueExpression;
		if ( columnValues == null ) {
			// ???
			valueExpression = null;
		}
		else {
			if ( columnValues.length > 1 ) {
				throw new UnsupportedOperationException( "Only one column value is allowed" );
			}
			valueExpression = columnValues[0];
		}
		builder.addValueColumn( valueExpression, columnDescriptor );
	}

	protected void handleValueGeneration(
			AttributeMapping attributeMapping,
			AssigningGraphTableMutationBuilder<?, ?> builder,
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
