package org.hibernate.id.enhanced;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import static org.hibernate.id.IdentifierGeneratorHelper.makeIntegralValue;
import static org.hibernate.id.enhanced.OptimizerLogger.OPTIMIZER_MESSAGE_LOGGER;

import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.sql.ast.spi.query.expression.Expression;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Variation of {@link PooledOptimizer} which interprets the incoming database
 * value as the lo value, rather than the hi value, as well as using thread local
 * to cache the generation state.
 *
 * @author Stuart Douglas
 * @author Scott Marlow
 * @author Steve Ebersole
 * @see PooledOptimizer
 */
public class PooledLoThreadLocalOptimizer extends AbstractOptimizer {

	private final ThreadLocal<GenerationState> singleTenantState = ThreadLocal.withInitial( GenerationState::new );
	private final ThreadLocal<Map<String, GenerationState>> multiTenantStates = ThreadLocal.withInitial( HashMap::new );

	/**
	 * Constructs a {@code PooledLoThreadLocalOptimizer}.
	 *
	 * @param returnClass The Java type of the values to be generated
	 * @param incrementSize The increment size.
	 */
	public PooledLoThreadLocalOptimizer(Class<?> returnClass, int incrementSize) {
		super( returnClass, incrementSize );
		if ( incrementSize < 1 ) {
			throw new HibernateException( "increment size cannot be less than 1" );
		}
		OPTIMIZER_MESSAGE_LOGGER.creatingPooledLoOptimizer( incrementSize, returnClass.getName() );
	}

	@Override
	public Serializable generate(AccessCallback callback) {
		final var generationState = locateGenerationState( callback.getTenantIdentifier() );
		if ( generationState.lastSourceValue == null || generationState.value >= generationState.upperLimitValue ) {
			generationState.setLastSourceValue( callback.getNextValue(), incrementSize );
		}
		return makeIntegralValue( generationState.value++, returnClass );
	}

	@Override
	public Serializable[] generateBatch(int count, AccessCallback callback) {
		final long[] buckets;
		final int availableValues;
		final long baseValue;
		final GenerationState generationState = locateGenerationState( callback.getTenantIdentifier() );
		int available = 0;
		long base = 0;
		if ( generationState.lastSourceValue != null ) {
			available = Math.min( count, (int) Math.max( generationState.upperLimitValue - generationState.value, 0L ) );
			base = generationState.value;
			generationState.value += available;
		}
		final int neededValues = count - available;
		if ( neededValues > 0 ) {
			buckets = PooledLoOptimizer.fetchBuckets( neededValues, incrementSize, callback );
			int previousValues = 0;
			for ( int i = 0; i < buckets.length - 1; i++ ) {
				previousValues += PooledLoOptimizer.bucketSize( buckets[i], incrementSize );
			}
			generationState.setLastSourceValue( buckets[buckets.length - 1], incrementSize );
			generationState.value += neededValues - previousValues;
		}
		else {
			buckets = ArrayHelper.EMPTY_LONG_ARRAY;
		}
		availableValues = available;
		baseValue = base;
		final Serializable[] result = new Serializable[count];
		for ( int i = 0; i < availableValues; i++ ) {
			result[i] = makeIntegralValue( baseValue + i, returnClass );
		}
		int position = availableValues;
		for ( long bucket : buckets ) {
			final long start = GenerationState.determineValue( bucket, incrementSize );
			final int size = PooledLoOptimizer.bucketSize( bucket, incrementSize );
			for ( int j = 0; j < size && position < count; j++ ) {
				result[position++] = makeIntegralValue( start + j, returnClass );
			}
		}
		return result;
	}

	private GenerationState locateGenerationState(String tenantIdentifier) {
		if ( tenantIdentifier == null ) {
			return singleTenantState.get();
		}
		else {
			var states = multiTenantStates.get();
			var state = states.get( tenantIdentifier );
			if ( state == null ) {
				state = new GenerationState();
				states.put( tenantIdentifier, state );
			}
			return state;
		}
	}

	@Override
	public void reset() {
		singleTenantState.remove();
		multiTenantStates.remove();
	}

	// for Hibernate testsuite use only
	private GenerationState noTenantGenerationState() {
		final var noTenantState = locateGenerationState( null );
		if ( noTenantState == null ) {
			throw new IllegalStateException( "Could not locate previous generation state for no-tenant" );
		}
		return noTenantState;
	}

	// for Hibernate testsuite use only
	@Override
	public Long getLastSourceValue() {
		return noTenantGenerationState().lastSourceValue;
	}

	@Override
	public boolean applyIncrementSizeToSourceValues() {
		return true;
	}

	private static class GenerationState {
		// last value read from db source
		private Long lastSourceValue;
		// the current generator value
		private long value;
		// the value at which we'll hit the db again
		private long upperLimitValue;

		void setLastSourceValue(long lastSourceValue, int incrementSize) {
			this.lastSourceValue = lastSourceValue;
			upperLimitValue = lastSourceValue + incrementSize;
			// handle cases where initial-value is less that one (hsqldb for instance).
			value = Math.max( lastSourceValue, 1L );
		}

		static long determineValue(long lastSourceValue, int incrementSize) {
			return Math.max( lastSourceValue, 1L );
		}
	}

	@Override
	public Expression createLowValueExpression(Expression databaseValue, SessionFactoryImplementor sessionFactory) {
		return databaseValue;
	}
}
