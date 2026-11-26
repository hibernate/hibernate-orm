package org.hibernate.persister.entity.mutation;

import jakarta.annotation.Nonnull;

import org.hibernate.Internal;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.TemporalMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.ast.spi.query.expression.ColumnReference;
import org.hibernate.sql.ast.spi.model.builder.RestrictedTableMutationBuilder;
import org.hibernate.sql.ast.spi.model.builder.TableDeleteBuilderStandard;
import org.hibernate.sql.ast.spi.model.builder.TableUpdateBuilder;
import org.hibernate.sql.ast.spi.model.builder.TableUpdateBuilderStandard;
import org.hibernate.sql.spi.mutation.MutationType;
import org.hibernate.sql.spi.mutation.TableMapping;

import static org.hibernate.temporal.TemporalTableStrategy.NATIVE;

public class TemporalMutationHelper {
	@Internal
	public static boolean isRevisionFromCurrentTransaction(
			@Nonnull EntityPersister persister, @Nonnull Object id, @Nonnull SharedSessionContractImplementor session) {
		final var persistenceContext = session.getPersistenceContextInternal();
		return persistenceContext.wasInsertedDuringTransaction( persister, id )
				|| persistenceContext.wasTemporalRevisionUpdated( session.generateEntityKey( id, persister ) );
	}

	/// A revision created and removed in one changeset has no period of validity.
	/// Delete its open row. For an older revision, set the end of the period.
	@Internal
	public static RestrictedTableMutationBuilder<?, ?> createRevisionEndBuilder(
			@Nonnull EntityPersister persister, @Nonnull TableMapping table, @Nonnull MutationType type) {
		if ( type == MutationType.DELETE ) {
			return createCurrentRevisionDeleteBuilder( persister, table );
		}
		assert type == MutationType.UPDATE;
		final var builder = new TableUpdateBuilderStandard<>( persister, table, persister.getFactory() );
		applyTemporalEnding( builder, persister.getTemporalMapping() );
		return builder;
	}

	static TableDeleteBuilderStandard createCurrentRevisionDeleteBuilder(
			@Nonnull EntityPersister persister, @Nonnull TableMapping table) {
		final var builder = new TableDeleteBuilderStandard( persister, table, persister.getFactory() );
		builder.addNullRestriction( persister.getTemporalMapping().getEndingColumnMapping() );
		return builder;
	}

	static void applyTemporalEnding(@Nonnull TableUpdateBuilder<?> builder, @Nonnull TemporalMapping temporalMapping) {
		final var endingColumn = temporalMapping.getEndingColumnMapping();
		final var columnReference = new ColumnReference( builder.getMutatingTable(), endingColumn );
		builder.addValueColumn( temporalMapping.createEndingValueBinding( columnReference ) );
		builder.addNullRestriction( endingColumn );
	}

	public static boolean isUsingParameters(@Nonnull SharedSessionContractImplementor session) {
		final var factory = session.getFactory();
		return factory.getSessionFactoryOptions().getTemporalTableStrategy() != NATIVE
			&& !factory.getChangesetCoordinator().useServerTimestamp( session.getDialect() );
	}
}
