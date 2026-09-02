/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.id.insert;

import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.ast.spi.query.expression.ColumnReference;
import org.hibernate.sql.ast.spi.model.MutatingTableReference;
import org.hibernate.sql.ast.spi.model.TableInsert;
import org.hibernate.sql.ast.spi.model.builder.AbstractTableInsertBuilder;
import org.hibernate.sql.ast.spi.model.TableInsertStandard;

/**
 * @author Steve Ebersole
 */
public class TableInsertReturningBuilder extends AbstractTableInsertBuilder {
	private final List<ColumnReference> generatedColumns;

	@org.hibernate.SPI(org.hibernate.SPI.Role.USE)
	public TableInsertReturningBuilder(
			EntityPersister mutationTarget,
			MutatingTableReference tableReference,
			List<ColumnReference> generatedColumns,
			SessionFactoryImplementor sessionFactory) {
		super( mutationTarget, tableReference, sessionFactory );
		this.generatedColumns = generatedColumns;
	}

	@Override
	@org.hibernate.SPI(org.hibernate.SPI.Role.USE)
	protected EntityPersister getMutationTarget() {
		return (EntityPersister) super.getMutationTarget();
	}

	@Override
	public TableInsert buildMutation() {
		return new TableInsertStandard(
				getMutatingTable(),
				getMutationTarget(),
				combine( getValueBindingList(), getLobValueBindingList() ),
				generatedColumns,
				getParameters()
		);
	}
}
