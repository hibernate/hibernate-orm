/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.model.ast.builder;

import java.util.Collections;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.model.MutationTarget;
import org.hibernate.sql.model.TableMapping;
import org.hibernate.sql.model.ast.MutatingTableReference;
import org.hibernate.sql.model.ast.TableInsert;
import org.hibernate.sql.model.internal.TableInsertCustomSql;
import org.hibernate.sql.model.internal.TableInsertStandard;

/**
 * Standard TableInsertBuilder
 *
 * @author Steve Ebersole
 */
public class TableInsertBuilderStandard extends AbstractTableInsertBuilder {
	private final boolean isCustomSql;

	public TableInsertBuilderStandard(
			MutationTarget<?> mutationTarget,
			TableMapping table,
			SessionFactoryImplementor sessionFactory) {
		super( mutationTarget, table, sessionFactory );
		this.isCustomSql = table.getInsertDetails().getCustomSql() != null;
	}

	public TableInsertBuilderStandard(
			MutationTarget<?> mutationTarget,
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
					combine( getValueBindingList(), getKeyBindingList(), getLobValueBindingList() ),
					getParameters()
			);
		}

		return new TableInsertStandard(
				getMutatingTable(),
				getMutationTarget(),
				combine( getValueBindingList(), getKeyBindingList(), getLobValueBindingList() ),
				Collections.emptyList(),
				getParameters()
		);
	}
}
