/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.spi.model.builder;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.spi.mutation.MutationTarget;
import org.hibernate.sql.spi.mutation.TableMapping;
import org.hibernate.sql.ast.spi.model.MutatingTableReference;
import org.hibernate.sql.ast.spi.model.TableInsert;
import org.hibernate.sql.ast.spi.model.TableInsertCustomSql;
import org.hibernate.sql.ast.spi.model.TableInsertStandard;

import java.util.Collections;

/**
 * Standard TableInsertBuilder
 *
 * @author Steve Ebersole
 */
public class TableInsertBuilderStandard extends AbstractTableInsertBuilder {
	private final boolean isCustomSql;

	public TableInsertBuilderStandard(
			MutationTarget mutationTarget,
			TableMapping table,
			SessionFactoryImplementor sessionFactory) {
		super( mutationTarget, table, sessionFactory );
		this.isCustomSql = table.getInsertDetails().getCustomSql() != null;
	}

	public TableInsertBuilderStandard(
			MutationTarget mutationTarget,
			MutatingTableReference tableReference,
			SessionFactoryImplementor sessionFactory) {
		super( mutationTarget, tableReference, sessionFactory );
		this.isCustomSql = tableReference.getTableMapping().getInsertDetails().getCustomSql() != null;
	}

	@Override
	public TableInsert buildMutation() {
		if ( isCustomSql ) {
			return new TableInsertCustomSql(
					getMutatingTable(),
					getMutationTarget(),
					combine( getValueBindingList(), getLobValueBindingList() ),
					getParameters()
			);
		}

		return new TableInsertStandard(
				getMutatingTable(),
				getMutationTarget(),
				combine( getValueBindingList(), getLobValueBindingList() ),
				Collections.emptyList(),
				getParameters()
		);
	}
}
