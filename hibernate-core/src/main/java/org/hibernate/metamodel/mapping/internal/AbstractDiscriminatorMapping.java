package org.hibernate.metamodel.mapping.internal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.function.BiConsumer;

import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.spi.IndexedConsumer;
import org.hibernate.metamodel.mapping.DiscriminatorConverter;
import org.hibernate.metamodel.mapping.DiscriminatorType;
import org.hibernate.metamodel.mapping.EntityDiscriminatorMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.creation.SqlAstCreationState;
import org.hibernate.sql.ast.spi.query.select.SqlSelection;
import org.hibernate.sql.ast.spi.query.from.TableGroup;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.basic.BasicFetch;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.JavaType;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;

/**
 * @implNote `discriminatorType` represents the mapping to Class, whereas `discriminatorType.getUnderlyingType()`
 * represents the "raw" JDBC mapping (String, Integer, etc.)
 *
 * @author Steve Ebersole
 */
public abstract class AbstractDiscriminatorMapping implements EntityDiscriminatorMapping {
	private final NavigableRole role;

	private final BasicType<Object> underlyingJdbcMapping;
	private final DiscriminatorType<Object> discriminatorType;
	private final ManagedMappingType mappingType;

	public AbstractDiscriminatorMapping(
			ManagedMappingType mappingType,
			DiscriminatorType<Object> discriminatorType,
			BasicType<Object> underlyingJdbcMapping) {
		this.underlyingJdbcMapping = underlyingJdbcMapping;
		this.mappingType = mappingType;
		this.discriminatorType = discriminatorType;
		this.role = castNonNull( mappingType.getNavigableRole() ).append( DISCRIMINATOR_ROLE_NAME );
	}

	@Nullable
	public EntityMappingType getEntityDescriptor() {
		return mappingType.asEntityMappingType();
	}

	@Nonnull
	@Override
	public BasicType<?> getUnderlyingJdbcMapping() {
		return discriminatorType.getUnderlyingJdbcMapping();
	}

	@Nonnull
	@Override
	public DiscriminatorConverter<?, ?> getValueConverter() {
		return discriminatorType.getValueConverter();
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// EntityDiscriminatorMapping

	@Nonnull
	@Override
	public NavigableRole getNavigableRole() {
		return role;
	}

	@Nonnull
	@Override
	public JdbcMapping getJdbcMapping() {
		return discriminatorType;
	}

	@Nullable
	@Override
	public EntityMappingType findContainingEntityMapping() {
		return mappingType.findContainingEntityMapping();
	}

	@Nonnull
	@Override
	public MappingType getMappedType() {
		return getJdbcMapping();
	}

	@Nonnull
	@Override
	public JavaType<?> getJavaType() {
		return getJdbcMapping().getJavaTypeDescriptor();
	}

	@Nonnull
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public DomainResult createDomainResult(
			@Nonnull NavigablePath navigablePath,
			@Nonnull TableGroup tableGroup,
			@Nullable String resultVariable,
			@Nonnull DomainResultCreationState creationState) {
		// create a SqlSelection based on the underlying JdbcMapping
		final var sqlSelection = resolveSqlSelection(
				navigablePath,
				underlyingJdbcMapping,
				tableGroup,
				null,
				creationState.getSqlAstCreationState()
		);

		// return a BasicResult with conversion the entity class or entity-name
		return new BasicResult(
				sqlSelection.getValuesArrayPosition(),
				resultVariable,
				discriminatorType.getJavaTypeDescriptor(),
				discriminatorType.getValueConverter(),
				navigablePath,
				false,
				!sqlSelection.isVirtual()
		);
	}

	private SqlSelection resolveSqlSelection(
			NavigablePath navigablePath,
			JdbcMapping jdbcMappingToUse,
			TableGroup tableGroup,
			@Nullable FetchParent fetchParent,
			SqlAstCreationState creationState) {
		return creationState.getSqlExpressionResolver().resolveSqlSelection(
				resolveSqlExpression( navigablePath, jdbcMappingToUse, tableGroup, creationState ),
				jdbcMappingToUse.getJdbcJavaType(),
				fetchParent,
				creationState.getCreationContext().getTypeConfiguration()
		);
	}

	@Nonnull
	@Override
	public BasicFetch<?> generateFetch(
			@Nonnull FetchParent fetchParent,
			@Nonnull NavigablePath fetchablePath,
			@Nonnull FetchTiming fetchTiming,
			boolean selected,
			@Nullable String resultVariable,
			@Nonnull DomainResultCreationState creationState) {
		final var tableGroup =
				creationState.getSqlAstCreationState().getFromClauseAccess()
						.getTableGroup( fetchParent.getNavigablePath() );
		assert tableGroup != null;

		// create a SqlSelection based on the underlying JdbcMapping
		final var sqlSelection = resolveSqlSelection(
				fetchablePath,
				getJdbcMapping(),
				tableGroup,
				fetchParent,
				creationState.getSqlAstCreationState()
		);

		// return a BasicFetch with conversion the entity class or entity-name
		return new BasicFetch<>(
				sqlSelection.getValuesArrayPosition(),
				fetchParent,
				fetchablePath,
				this,
				discriminatorType.getValueConverter(),
				fetchTiming,
				true,
				false,
				!sqlSelection.isVirtual()
		);
	}

	@Override
	public void applySqlSelections(
			@Nonnull NavigablePath navigablePath,
			@Nonnull TableGroup tableGroup,
			@Nonnull DomainResultCreationState creationState) {
		resolveSqlSelection(
				navigablePath,
				underlyingJdbcMapping,
				tableGroup,
				null,
				creationState.getSqlAstCreationState()
		);
	}

	@Override
	public void applySqlSelections(
			@Nonnull NavigablePath navigablePath,
			@Nonnull TableGroup tableGroup,
			@Nonnull DomainResultCreationState creationState,
			@Nonnull BiConsumer<SqlSelection, JdbcMapping> selectionConsumer) {
		selectionConsumer.accept(
				resolveSqlSelection( navigablePath, underlyingJdbcMapping, tableGroup, null, creationState.getSqlAstCreationState() ),
				getJdbcMapping()
		);
	}

	@Override
	public <X, Y> int forEachDisassembledJdbcValue(
			@Nullable Object value,
			int offset,
			@Nullable X x,
			@Nullable Y y,
			@Nonnull JdbcValuesBiConsumer<X, Y> valuesConsumer,
			@Nullable SharedSessionContractImplementor session) {
		valuesConsumer.consume( offset, x, y, value, underlyingJdbcMapping );
		return getJdbcTypeCount();
	}

	@Override
	public int forEachJdbcType(int offset, @Nonnull IndexedConsumer<JdbcMapping> action) {
		action.accept( offset, underlyingJdbcMapping );
		return getJdbcTypeCount();
	}

	@Override
	public <X, Y> int breakDownJdbcValues(
			@Nullable Object domainValue,
			int offset,
			@Nullable X x,
			@Nullable Y y,
			@Nonnull JdbcValueBiConsumer<X, Y> valueConsumer,
			@Nullable SharedSessionContractImplementor session) {
		valueConsumer.consume( offset, x, y, disassemble( domainValue, session ), this );
		return getJdbcTypeCount();
	}

	@Nullable
	@Override
	public Object disassemble(@Nullable Object value, @Nullable SharedSessionContractImplementor session) {
		return value;
	}

}
