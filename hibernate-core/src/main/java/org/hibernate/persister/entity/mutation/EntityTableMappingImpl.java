package org.hibernate.persister.entity.mutation;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.jdbc.Expectation;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.SelectableMappings;
import org.hibernate.metamodel.mapping.TableDetails;
import org.hibernate.metamodel.mapping.internal.SelectableMappingImpl;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.creation.SqlAstCreationState;
import org.hibernate.sql.ast.spi.query.select.SqlSelection;
import org.hibernate.sql.ast.spi.query.from.TableReference;
import org.hibernate.sql.spi.mutation.MutationType;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.basic.BasicResult;

import java.util.BitSet;
import java.util.List;
import java.util.Objects;

import static org.hibernate.internal.util.collections.ArrayHelper.contains;

/**
 * Descriptor for the mapping of a table relative to an entity
 *
 * @author Steve Ebersole
 */
@org.hibernate.Internal
public class EntityTableMappingImpl implements EntityTableMapping {
	private enum Flag {
		OPTIONAL,
		INVERSE,
		ID_TABLE,
		CASCADE_DELETE,
		SECONDARY_TABLE
	}

	private final String tableName;
	private final int relativePosition;
	private final KeyMapping keyMapping;

	private final BitSet flags = new BitSet();

	private final int[] attributeIndexes;

	private final MutationDetails insertDetails;
	private final MutationDetails updateDetails;
	private final MutationDetails deleteDetails;

	public EntityTableMappingImpl(
			@Nonnull String tableName,
			int relativePosition,
			@Nonnull KeyMapping keyMapping,
			boolean isOptional,
			boolean isInverse,
			boolean isIdentifierTable,
			boolean isSecondaryTable,
			@Nonnull int[] attributeIndexes,
			@Nonnull Expectation insertExpectation,
			@Nullable String insertCustomSql,
			boolean insertCallable,
			@Nonnull Expectation updateExpectation,
			@Nullable String updateCustomSql,
			boolean updateCallable,
			boolean cascadeDeleteEnabled,
			@Nonnull Expectation deleteExpectation,
			@Nullable String deleteCustomSql,
			boolean deleteCallable,
			boolean dynamicUpdate,
			boolean dynamicInsert) {
		this.tableName = tableName;
		this.relativePosition = relativePosition;
		this.keyMapping = keyMapping;
		this.attributeIndexes = attributeIndexes;
		this.insertDetails = new MutationDetails(
				MutationType.INSERT,
				insertExpectation,
				insertCustomSql,
				insertCallable,
				dynamicInsert
		);
		this.updateDetails = new MutationDetails(
				MutationType.UPDATE,
				updateExpectation,
				updateCustomSql,
				updateCallable,
				dynamicUpdate
		);
		this.deleteDetails = new MutationDetails(
				MutationType.DELETE,
				deleteExpectation,
				deleteCustomSql,
				deleteCallable
		);

		if ( isOptional ) {
			flags.set( Flag.OPTIONAL.ordinal() );
		}

		if ( isInverse ) {
			flags.set( Flag.INVERSE.ordinal() );
		}

		if ( isIdentifierTable ) {
			flags.set( Flag.ID_TABLE.ordinal() );
		}

		if ( cascadeDeleteEnabled ) {
			flags.set( Flag.CASCADE_DELETE.ordinal() );
		}

		if ( isSecondaryTable ) {
			flags.set( Flag.SECONDARY_TABLE.ordinal() );
		}
	}

	@Nonnull
	@Override public String getTableName() {
		return tableName;
	}

	@Nullable
	@Override
	public KeyDetails getKeyDetails() {
		return keyMapping;
	}

	@Override public int relativePosition() {
		return relativePosition;
	}

	@Override public boolean isOptional() {
		return flags.get( Flag.OPTIONAL.ordinal() );
	}

	@Override public boolean isInverse() {
		return flags.get( Flag.INVERSE.ordinal() );
	}

	@Override public boolean isIdentifierTable() {
		return flags.get( Flag.ID_TABLE.ordinal() );
	}

	@Override
	public boolean isSecondaryTable() {
		return flags.get( Flag.SECONDARY_TABLE.ordinal() );
	}

	@Nonnull
	@Override
	public KeyMapping getKeyMapping() {
		return keyMapping;
	}

	@Override
	public boolean hasColumns() {
		return attributeIndexes.length > 0;
	}

	@Override
	public boolean containsAttributeColumns(int attributeIndex) {
		return contains( attributeIndexes, attributeIndex );
	}

	@Nonnull
	@Override
	public int[] getAttributeIndexes() {
		return attributeIndexes;
	}

	@Nonnull
	@Override public MutationDetails getInsertDetails() {
		return insertDetails;
	}

	@Nonnull
	@Override
	public Expectation getInsertExpectation() {
		return getInsertDetails().getExpectation();
	}

	@Nullable
	@Override
	public String getInsertCustomSql() {
		return getInsertDetails().getCustomSql();
	}

	@Override
	public boolean isInsertCallable() {
		return getInsertDetails().isCallable();
	}

	@Nonnull
	@Override public MutationDetails getUpdateDetails() {
		return updateDetails;
	}

	@Nonnull
	@Override
	public Expectation getUpdateExpectation() {
		return getUpdateDetails().getExpectation();
	}

	@Nullable
	@Override
	public String getUpdateCustomSql() {
		return getUpdateDetails().getCustomSql();
	}

	@Override
	public boolean isUpdateCallable() {
		return getUpdateDetails().isCallable();
	}

	@Override public boolean isCascadeDeleteEnabled() {
		return flags.get( Flag.CASCADE_DELETE.ordinal() );
	}

	@Nonnull
	@Override public MutationDetails getDeleteDetails() {
		return deleteDetails;
	}

	@Nonnull
	@Override
	public Expectation getDeleteExpectation() {
		return getDeleteDetails().getExpectation();
	}

	@Nullable
	@Override
	public String getDeleteCustomSql() {
		return getDeleteDetails().getCustomSql();
	}

	@Override
	public boolean isDeleteCallable() {
		return getDeleteDetails().isCallable();
	}

	@Override
	public boolean equals(@Nullable Object object) {
		if ( this == object ) {
			return true;
		}
		else if ( !(object instanceof EntityTableMappingImpl that) ) {
			return false;
		}
		else {
			return tableName.equals( that.tableName );
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash( tableName );
	}

	@Nonnull
	@Override
	public String toString() {
		return "TableMapping(" + tableName + ")";
	}

	public interface KeyMapping extends KeyDetails, SelectableMappings {
	}

	public static abstract class AbstractKeyMapping implements KeyMapping {
		protected final List<KeyColumn> keyColumns;
		protected final ModelPart identifierPart;

		public AbstractKeyMapping(@Nonnull List<KeyColumn> keyColumns, @Nonnull ModelPart identifierPart) {
			this.keyColumns = keyColumns;
			this.identifierPart = identifierPart;
		}

		@Nonnull
		@Override
		public List<? extends KeyColumn> getKeyColumns() {
			return keyColumns;
		}

		@Override
		public int getColumnCount() {
			return getKeyColumns().size();
		}

		@Nonnull
		@Override
		public KeyColumn getKeyColumn(int position) {
			return getKeyColumns().get( position );
		}

		@Override
		public void forEachKeyColumn(@Nonnull KeyColumnConsumer consumer) {
			final var keyColumns = getKeyColumns();
			for ( int i = 0; i < keyColumns.size(); i++ ) {
				consumer.consume( i, keyColumns.get( i ) );
			}
		}

		@Override
		public int getJdbcTypeCount() {
			return getKeyColumns().size();
		}

		@Nonnull
		@Override
		public SelectableMapping getSelectable(int columnIndex) {
			return getKeyColumns().get( columnIndex );
		}

		@Override
		public int forEachSelectable(int offset, @Nonnull SelectableConsumer consumer) {
			final var keyColumns = getKeyColumns();
			for ( int i = 0; i < keyColumns.size(); i++ ) {
				consumer.accept( i, keyColumns.get( i ) );
			}
			return getJdbcTypeCount();
		}

		public void breakDownKeyJdbcValues(
				@Nonnull Object domainValue,
				@Nonnull KeyValueConsumer valueConsumer,
				@Nonnull SharedSessionContractImplementor session) {
			identifierPart.forEachJdbcValue(
					domainValue,
					getKeyColumns(),
					valueConsumer,
					(selectionIndex, keys, consumer, jdbcValue, jdbcMapping) -> consumer.consume(
							jdbcValue,
							keys.get( selectionIndex )
					),
					session
			);
		}

		@Nonnull
		protected SqlSelection resolveSqlSelection(
				@Nonnull TableReference tableReference,
				@Nonnull KeyColumn keyColumn,
				@Nonnull SqlAstCreationState creationState) {
			final var expressionResolver = creationState.getSqlExpressionResolver();
			return expressionResolver.resolveSqlSelection(
					expressionResolver.resolveSqlExpression( tableReference, keyColumn ),
					keyColumn.getJdbcMapping().getJdbcJavaType(),
					null,
					creationState.getCreationContext().getTypeConfiguration()
			);
		}

		@Override
		public int forEachSelectable(@Nonnull SelectableConsumer consumer) {
			forEachKeyColumn( consumer::accept );
			return getJdbcTypeCount();
		}
	}

	public static class SimpleKeyMapping extends AbstractKeyMapping {
		private final KeyColumn keyColumn;

		public SimpleKeyMapping(@Nonnull List<KeyColumn> keyColumns, @Nonnull BasicValuedModelPart identifierPart) {
			super( keyColumns, identifierPart );
			this.keyColumn = keyColumns.get( 0 );
		}

		@Nonnull
		@Override
		public <K> DomainResult<K> createDomainResult(
				@Nonnull NavigablePath navigablePath,
				@Nonnull TableReference tableReference,
				@Nullable String resultVariable,
				@Nonnull DomainResultCreationState creationState) {
			// create SqlSelection based on the underlying JdbcMapping
			final var sqlSelection = resolveSqlSelection(
					tableReference,
					keyColumn,
					creationState.getSqlAstCreationState()
			);

			// return a BasicResult with conversion the entity class or entity-name
			//noinspection unchecked,rawtypes
			return new BasicResult(
					sqlSelection.getValuesArrayPosition(),
					resultVariable,
					identifierPart.getJavaType(),
					null,
					navigablePath,
					false,
					!sqlSelection.isVirtual()
			);
		}
	}

	public static class CompositeKeyMapping extends AbstractKeyMapping {
		public CompositeKeyMapping(@Nonnull List<KeyColumn> keyColumns, @Nonnull EmbeddableValuedModelPart identifierPart) {
			super( keyColumns, identifierPart );
		}

		@Nonnull
		@Override
		public <K> DomainResult<K> createDomainResult(
				@Nonnull NavigablePath navigablePath,
				@Nonnull TableReference tableReference,
				@Nullable String resultVariable,
				@Nonnull DomainResultCreationState creationState) {
			// this will be challenging if the embeddable defines to-ones.
			// just error for now.
			throw new UnsupportedOperationException( "Not implemented yet" );
		}
	}

	public static class KeyColumn extends SelectableMappingImpl implements TableDetails.KeyColumn {

		public KeyColumn(@Nonnull String tableName, @Nonnull SelectableMapping originalMapping) {
			super(
					tableName,
					originalMapping.getSelectionExpression(),
					null, // Leads to construction of a fresh path based on selection expression
					originalMapping.getCustomReadExpression(),
					originalMapping.getCustomWriteExpression(),
					originalMapping.getLength(),
					originalMapping.getPrecision(),
					originalMapping.getScale(),
					originalMapping.getTemporalPrecision(),
					originalMapping.isLob(),
					originalMapping.isNullable(),
					originalMapping.isInsertable(),
					originalMapping.isUpdateable(),
					originalMapping.isPartitioned(),
					originalMapping.isFormula(),
					originalMapping.getJdbcMapping()
			);
		}

		@Nonnull
		@Override
		public String getColumnName() {
			return getSelectionExpression();
		}
	}
}
