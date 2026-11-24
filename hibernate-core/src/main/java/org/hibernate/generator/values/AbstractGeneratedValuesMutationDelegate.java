package org.hibernate.generator.values;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import org.hibernate.action.queue.spi.plan.FlushOperation;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.EventType;
import org.hibernate.generator.values.internal.GeneratedValuesMappingProducer;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.spi.mutation.jdbc.PreparableMutationOperation;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMappingProducer;

import static org.hibernate.generator.values.internal.GeneratedValuesHelper.createMappingProducer;

/**
 * @author Marco Belladelli
 */
@org.hibernate.SPI({ org.hibernate.SPI.Role.USE, org.hibernate.SPI.Role.IMPLEMENT })
public abstract class AbstractGeneratedValuesMutationDelegate implements GeneratedValuesMutationDelegate {
	protected final EntityPersister persister;
	private final EventType timing;
	private final boolean supportsArbitraryValues;
	private final boolean supportsRowId;
	@org.hibernate.Internal
	protected final GeneratedValuesMappingProducer jdbcValuesMappingProducer;

	@org.hibernate.SPI(org.hibernate.SPI.Role.IMPLEMENT)
	public AbstractGeneratedValuesMutationDelegate(EntityPersister persister, EventType timing) {
		this( persister, timing, true, true );
	}

	@org.hibernate.SPI(org.hibernate.SPI.Role.IMPLEMENT)
	public AbstractGeneratedValuesMutationDelegate(
			EntityPersister persister,
			EventType timing,
			boolean supportsArbitraryValues,
			boolean supportsRowId) {
		this.persister = persister;
		this.timing = timing;
		this.supportsArbitraryValues = supportsArbitraryValues;
		this.supportsRowId = supportsRowId;
		this.jdbcValuesMappingProducer =
				createMappingProducer( persister, timing, supportsArbitraryValues, supportsRowId );
	}

	public AbstractGeneratedValuesMutationDelegate(
			EntityPersister persister,
			EventType timing,
			boolean supportsArbitraryValues,
			boolean supportsRowId,
			List<? extends ModelPart> generatedProperties) {
		this.persister = persister;
		this.timing = timing;
		this.supportsArbitraryValues = supportsArbitraryValues;
		this.supportsRowId = supportsRowId;
		this.jdbcValuesMappingProducer =
				createMappingProducer( persister, supportsArbitraryValues, generatedProperties );
	}

	@Override
	public EventType getTiming() {
		return timing;
	}

	@Override
	public final boolean supportsArbitraryValues() {
		return supportsArbitraryValues;
	}

	@Override
	public final boolean supportsRowId() {
		return supportsRowId;
	}

	@Override
	public JdbcValuesMappingProducer getGeneratedValuesMappingProducer() {
		return jdbcValuesMappingProducer;
	}

	protected Dialect dialect() {
		return persister.getFactory().getJdbcServices().getDialect();
	}

	/// Check the outcome of a graph mutation which matched no row. An update
	/// should always match one. An insert matches none only when it reads its
	/// row from rows which it references.
	protected final void checkUnmatchedRow(
			FlushOperation operation,
			PreparedStatement statement,
			String sql,
			SharedSessionContractImplementor session) {
		if ( getTiming() == EventType.UPDATE || operation.requiresMatchedRow() ) {
			try {
				final var checker = operation.getOperationResultChecker();
				if ( checker != null ) {
					checker.checkResult( 0, statement, -1, sql, session.getFactory() );
				}
				else {
					( (PreparableMutationOperation) operation.getJdbcOperation() ).getExpectation()
							.verifyOutcome( 0, statement, -1, sql );
				}
			}
			catch (SQLException e) {
				throw session.getJdbcServices().getSqlExceptionHelper()
						.convert( e, "Unable to check mutation outcome", sql );
			}
		}
	}
}
