/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.id.insert;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.id.PostInsertIdentityPersister;
import org.hibernate.metamodel.mapping.EntityRowIdMapping;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.model.ast.MutatingTableReference;
import org.hibernate.sql.model.ast.TableInsert;
import org.hibernate.sql.model.ast.builder.AbstractTableInsertBuilder;
import org.hibernate.sql.model.internal.TableInsertStandard;

import static org.hibernate.generator.values.internal.GeneratedValuesHelper.getActualGeneratedModelPart;
import static org.hibernate.internal.util.NullnessUtil.castNonNull;

/**
 * @author Steve Ebersole
 */
public class TableInsertReturningBuilder extends AbstractTableInsertBuilder {
	private final List<ColumnReference> generatedColumns;

	/**
	 * @deprecated Use {@link #TableInsertReturningBuilder(EntityPersister, MutatingTableReference, List, SessionFactoryImplementor)} instead.
	 */
	@Deprecated( forRemoval = true, since = "6.5" )
	public TableInsertReturningBuilder(
			PostInsertIdentityPersister mutationTarget,
			SessionFactoryImplementor sessionFactory) {
		this(
				mutationTarget,
				new MutatingTableReference( mutationTarget.getIdentifierTableMapping() ),
				new ArrayList<>(),
				sessionFactory
		);
		final List<? extends ModelPart> insertGeneratedProperties = getMutationTarget().getInsertGeneratedProperties();
		for ( final ModelPart prop : insertGeneratedProperties ) {
			generatedColumns.add( new ColumnReference(
					getMutatingTable(),
					getActualGeneratedModelPart( castNonNull( prop.asBasicValuedModelPart() ) )
			) );
		}
		// special case for rowid when the dialect supports it
		final EntityRowIdMapping rowIdMapping = getMutationTarget().getRowIdMapping();
		if ( rowIdMapping != null && getJdbcServices().getDialect().supportsInsertReturningRowId() ) {
			generatedColumns.add( new ColumnReference( getMutatingTable(), rowIdMapping ) );
		}
	}

	public TableInsertReturningBuilder(
			EntityPersister mutationTarget,
			MutatingTableReference tableReference,
			List<ColumnReference> generatedColumns,
			SessionFactoryImplementor sessionFactory) {
		super( mutationTarget, tableReference, sessionFactory );
		this.generatedColumns = generatedColumns;
	}

	@Override
	protected EntityPersister getMutationTarget() {
		return (EntityPersister) super.getMutationTarget();
	}

	@Override
	public TableInsert buildMutation() {
		return new TableInsertStandard(
				getMutatingTable(),
				getMutationTarget(),
				combine( getValueBindingList(), getKeyBindingList(), getLobValueBindingList() ),
				generatedColumns,
				getParameters()
		);
	}
}
