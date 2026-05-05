/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.internal.decompose.entity;


import org.hibernate.action.internal.EntityDeleteAction;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.model.ast.ColumnValueBinding;

import java.util.List;

/// @author Steve Ebersole
public abstract class AbstractDeleteDecomposer extends AbstractDecomposer<EntityDeleteAction> implements DeleteDecomposer {
	public AbstractDeleteDecomposer(EntityPersister entityPersister, SessionFactoryImplementor sessionFactory) {
		super( entityPersister, sessionFactory );
	}

	protected boolean containsColumn(List<ColumnValueBinding> bindings, SelectableMapping selectableMapping) {
		for ( ColumnValueBinding binding : bindings ) {
			if ( binding.matches( selectableMapping ) ) {
				return true;
			}
		}
		return false;
	}
}
