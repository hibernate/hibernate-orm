package org.hibernate.action.internal;

import java.sql.SQLException;

import org.hibernate.LockMode;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.internal.SimpleForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.JoinedSubclassEntityPersister;
import org.hibernate.sql.SimpleSelect;
import org.hibernate.sql.Update;

import static org.hibernate.annotations.OnDeleteAction.CASCADE;
import static org.hibernate.sql.ComparisonRestriction.Operator.EQ;

/// A delete prerequisite for a nullable self-reference from the root to a joined table.
public final class JoinedSelfReferenceDeleteCleanup {
	private JoinedSelfReferenceDeleteCleanup() {
	}

	public static boolean isEligible(JoinedSubclassEntityPersister joined) {
		return joined.getFactory().getJdbcServices().getDialect() instanceof H2Dialect
			&& !joined.getRootTableName().equals( joined.getTableName() )
			&& !joined.isVersioned()
			&& !joined.optimisticLockStyle().isAllOrDirty()
			&& !joined.hasPartitionedSelectionMapping()
			&& joined.getTenantIdMapping() == null
			&& joined.getSoftDeleteMapping() == null
			&& joined.getRootTableKeyColumnNames().length == 1
			&& !hasCustomDelete( joined );
	}

	private static boolean hasCustomDelete(JoinedSubclassEntityPersister joined) {
		for ( final var table : joined.getTableMappings() ) {
			if ( !table.isInverse() && table.getDeleteCustomSql() != null ) {
				return true;
			}
		}
		return false;
	}

	private static boolean eligibleColumn(SelectableMapping selectable) {
		return !selectable.isFormula() && selectable.isNullable() && selectable.isUpdateable();
	}

	public static SelectableMapping cleanupColumn(
			JoinedSubclassEntityPersister joined, String propertyName) {
		final var onDeleteActions = joined.getPropertyOnDeleteActions();
		if ( joined.findAttributeMapping( propertyName ) instanceof ToOneAttributeMapping toOne
				&& toOne.isNullable()
				&& toOne.isReferenceToPrimaryKey()
				&& onDeleteActions[toOne.getStateArrayPosition()] == CASCADE
				&& toOne.getSideNature() == ForeignKeyDescriptor.Nature.KEY
				&& toOne.getForeignKeyDescriptor() instanceof SimpleForeignKeyDescriptor foreignKey
				&& foreignKey.hasConstraint()
				&& foreignKey.getKeyTable().equals( joined.getRootTableName() )
				&& foreignKey.getTargetTable().equals( joined.getTableName() ) ) {
			final var selectable = foreignKey.getSelectable( 0 );
			return eligibleColumn( selectable ) ? selectable : null;
		}
		return null;
	}

	/// Read the physical FK after pre-delete callbacks. Even a zero-row H2 UPDATE
	/// would require UPDATE permission and fire statement-level triggers.
	/// Lock only an existing self-edge to keep its state stable until the UPDATE.
	public static boolean hasPhysicalSelfReference(
			JoinedSubclassEntityPersister joined,
			Object id,
			String keyColumn,
			String fkColumn,
			SharedSessionContractImplementor session) {
		final String sql = new SimpleSelect( session.getFactory() )
				.setTableName( joined.getRootTableName() )
				.addColumn( keyColumn )
				.addRestriction( keyColumn )
				.addRestriction( fkColumn, EQ, keyColumn )
				.setLockMode( LockMode.PESSIMISTIC_WRITE )
				.toStatementString();
		final var jdbc = session.getJdbcCoordinator();
		final var statement = jdbc.getStatementPreparer().prepareStatement( sql );
		try {
			joined.getIdentifierType().nullSafeSet( statement, id, 1, session );
			final var result = jdbc.getResultSetReturn().extract( statement, sql );
			try {
				return result.next();
			}
			finally {
				jdbc.getLogicalConnection().getResourceRegistry().release( result, statement );
			}
		}
		catch (SQLException e) {
			throw session.getJdbcServices().getSqlExceptionHelper()
					.convert( e, "Could not read the joined self-reference before deletion", sql );
		}
		finally {
			jdbc.getLogicalConnection().getResourceRegistry().release( statement );
			jdbc.afterStatementExecution();
		}
	}

	public static void execute(
			EntityPersister persister,
			Object id,
			SharedSessionContractImplementor session) {
		if ( !( persister instanceof JoinedSubclassEntityPersister joined ) || !isEligible( joined ) ) {
			return;
		}
		final String[] keyColumns = joined.getRootTableKeyColumnNames();
		for ( final var name : joined.getPropertyNames() ) {
			final var selectable = cleanupColumn( joined, name );
			if ( selectable != null ) {
				// Only clear the edge if the stored FK points back to this same entity.
				final String fkColumn = selectable.getSelectionExpression();
				if ( !hasPhysicalSelfReference( joined, id, keyColumns[0], fkColumn, session ) ) {
					continue;
				}
				final String sql = new Update( session.getFactory() )
						.setTableName( joined.getRootTableName() )
						.addAssignment( fkColumn, "null" )
						.addRestriction( keyColumns[0] )
						.addRestriction( fkColumn, keyColumns[0] )
						.toStatementString();
				final var jdbcCoordinator = session.getJdbcCoordinator();
				final var statement = jdbcCoordinator.getStatementPreparer().prepareStatement( sql );
				try {
					joined.getIdentifierType().nullSafeSet( statement, id, 1, session );
					jdbcCoordinator.getResultSetReturn().executeUpdate( statement, sql );
				}
				catch (SQLException e) {
					throw session.getJdbcServices().getSqlExceptionHelper()
							.convert( e, "Could not clear the joined self-reference before deletion", sql );
				}
				finally {
					jdbcCoordinator.getLogicalConnection().getResourceRegistry().release( statement );
					jdbcCoordinator.afterStatementExecution();
				}
			}
		}
	}
}
