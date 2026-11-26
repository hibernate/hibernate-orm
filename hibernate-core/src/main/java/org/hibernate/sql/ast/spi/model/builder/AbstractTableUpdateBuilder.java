package org.hibernate.sql.ast.spi.model.builder;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.hibernate.SPI;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.sql.spi.mutation.MutationOperation;
import org.hibernate.sql.spi.mutation.MutationTarget;
import org.hibernate.sql.spi.mutation.MutationType;
import org.hibernate.sql.spi.mutation.TableMapping;
import org.hibernate.sql.ast.spi.model.ColumnValueBinding;
import org.hibernate.sql.ast.spi.model.MutatingTableReference;
import org.hibernate.sql.ast.spi.model.RestrictedTableMutation;
import org.hibernate.sql.ast.spi.model.TableUpdate;

import static org.hibernate.SPI.Role.IMPLEMENT;

/// Base for provider-owned [TableUpdateBuilder] implementations.
///
/// Extend this class to customize construction of an update operation while
/// retaining standard assignment, restriction, and LOB-ordering behavior.
///
/// @since 8.0
/// @author Steve Ebersole
@SPI(IMPLEMENT)
public abstract class AbstractTableUpdateBuilder<O extends MutationOperation>
		extends AbstractRestrictedTableMutationBuilder<O, RestrictedTableMutation<O>>
		implements TableUpdateBuilder<O> {
	private final List<ColumnValueBinding> keyBindings = new ArrayList<>();
	private final List<ColumnValueBinding> valueBindings = new ArrayList<>();
	private List<ColumnValueBinding> lobValueBindings;

	private boolean rowKnownToExist;
	private String sqlComment;
	private Predicate<SelectableMapping> columnInclusion;

	/// Create an update builder which owns a new reference to `tableMapping`.
	@SPI(IMPLEMENT)
	public AbstractTableUpdateBuilder(
			MutationTarget mutationTarget,
			TableMapping tableMapping,
			SessionFactoryImplementor sessionFactory) {
		super( MutationType.UPDATE, mutationTarget, tableMapping, sessionFactory );
		this.sqlComment = "update for " + mutationTarget.getRolePath();
	}

	/// Create an update builder using the supplied mutating-table reference.
	@SPI(IMPLEMENT)
	public AbstractTableUpdateBuilder(
			MutationTarget mutationTarget,
			MutatingTableReference tableReference,
			SessionFactoryImplementor sessionFactory) {
		super( MutationType.UPDATE, mutationTarget, tableReference, sessionFactory );
		this.sqlComment = "update for " + mutationTarget.getRolePath();
	}

	public String getSqlComment() {
		return sqlComment;
	}

	public void setSqlComment(String sqlComment) {
		this.sqlComment = sqlComment;
	}

	/**
	 * The bindings for each key restriction (WHERE clause).
	 *
	 * @see TableUpdate#getKeyBindings
	 */
	protected List<ColumnValueBinding> getKeyBindings() {
		return keyBindings;
	}

	/**
	 * The (non-LOB) bindings for each column being updated (SET clause)
	 *
	 * @see TableUpdate#getValueBindings
	 */
	protected List<ColumnValueBinding> getValueBindings() {
		return valueBindings;
	}

	/**
	 * @apiNote The distinction with {@link #getValueBindings} is to help
	 * in cases e.g. where a dialect needs to order all LOB bindings after
	 * all non-LOB bindings
	 *
	 * @see TableUpdate#getValueBindings
	 */
	protected List<ColumnValueBinding> getLobValueBindings() {
		return lobValueBindings;
	}

	@Override
	public boolean hasAssignmentBindings() {
		return !valueBindings.isEmpty() || CollectionHelper.isNotEmpty( lobValueBindings );
	}

	@Override
	public void addColumnAssignment(ColumnValueBinding valueBinding) {
		if ( valueBinding.getColumnReference().getJdbcMapping().getJdbcType().isLob()
				&& getJdbcServices().getDialect().getLobSupport().forceLobAsLastValue() ) {
			if ( lobValueBindings == null ) {
				lobValueBindings = new ArrayList<>();
			}
			lobValueBindings.add( valueBinding );
		}
		else {
			valueBindings.add( valueBinding );
		}
	}

	/** Limit assignments made through selectable mappings, including generated assignments. */
	@Override
	public void setColumnInclusion(Predicate<SelectableMapping> columnInclusion) {
		this.columnInclusion = columnInclusion;
	}

	@Override
	public void setRowKnownToExist(boolean rowKnownToExist) {
		this.rowKnownToExist = rowKnownToExist;
	}

	protected boolean isRowKnownToExist() {
		return rowKnownToExist;
	}

	@Override
	public void addColumnAssignment(SelectableMapping columnMapping, String assignment) {
		if ( columnInclusion == null || columnInclusion.test( columnMapping ) ) {
			addColumnAssignment( createValueBinding( assignment, columnMapping ) );
		}
	}

	/// Whether the given assignments include every column of the table which holds
	/// state of the given entity. When they do not, a row of an optional table might
	/// have data even though every assigned value is null. A column which is left out
	/// by the [column inclusion][#setColumnInclusion] is accounted for by whoever set
	/// it, which says whether the [row is known to exist][#setRowKnownToExist].
	protected boolean assignsEveryColumn(EntityMappingType entity, List<ColumnValueBinding> assignments) {
		final String tableName = getMutatingTable().getTableName();
		final var persister = entity.getEntityPersister();
		final var attributes = entity.getAttributeMappings();
		for ( int i = 0; i < attributes.size(); i++ ) {
			final var attribute = attributes.get( i );
			for ( int j = 0; j < attribute.getJdbcTypeCount(); j++ ) {
				final var selectable = attribute.getSelectable( j );
				if ( !selectable.isFormula()
						&& tableName.equals( persister.physicalTableNameForMutation( selectable ) )
						&& ( columnInclusion == null || columnInclusion.test( selectable ) )
						&& !isAssigned( selectable, assignments ) ) {
					return false;
				}
			}
		}
		return true;
	}

	private static boolean isAssigned(SelectableMapping selectable, List<ColumnValueBinding> assignments) {
		final String column = selectable.getSelectionExpression();
		for ( int i = 0; i < assignments.size(); i++ ) {
			if ( assignments.get( i ).getColumnReference().getColumnExpression().equals( column ) ) {
				return true;
			}
		}
		return false;
	}

	/// Whether the given assignment is to a column by which the key of the row is
	/// restricted, as a partition key column is. Such a column is not null, so its
	/// row has data, and a statement which matches the row by its key, and inserts
	/// it if there is no such row, cannot name the column a second time.
	protected boolean isKeyRestrictionColumn(ColumnValueBinding valueBinding) {
		final String column = valueBinding.getColumnReference().getColumnExpression();
		final var keyRestrictions = getKeyRestrictionBindings();
		for ( int i = 0; i < keyRestrictions.size(); i++ ) {
			if ( keyRestrictions.get( i ).getColumnReference().getColumnExpression().equals( column ) ) {
				return true;
			}
		}
		return false;
	}
}
