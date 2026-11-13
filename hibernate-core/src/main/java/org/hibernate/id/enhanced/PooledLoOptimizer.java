package org.hibernate.id.enhanced;

import jakarta.annotation.Nonnull;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import static org.hibernate.id.IdentifierGeneratorHelper.makeIntegralValue;
import static org.hibernate.id.enhanced.OptimizerLogger.OPTIMIZER_MESSAGE_LOGGER;

import org.hibernate.internal.util.MathHelper;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.sql.ast.spi.query.expression.Expression;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Optimizer which uses a pool of values, backed by a <em>logical sequence</em>.
 * A logical sequence is usually just an unpooled sequence or table generator.
 * <p>
 * The pool size is controlled by the {@code allocationSize} of a
 * {@linkplain jakarta.persistence.SequenceGenerator sequence generator} or
 * {@linkplain jakarta.persistence.TableGenerator sequence generator}.
 * <p>
 * From time to time, the optimizer allocates a range of values to itself,
 * interpreting the next value retrieved from the logical sequence as the
 * lower bound on the range of newly allocated ids. Thus, the generated ids
 * begin with the value retrieved from the logical sequence.
 * <p>
 * The {@link PooledOptimizer} is similar, but interprets the current value
 * of the logical sequence as an upper bound on the range of already-allocated
 * ids.
 *
 * @author Steve Ebersole
 *
 * @see PooledOptimizer
 * @see jakarta.persistence.SequenceGenerator#allocationSize
 * @see jakarta.persistence.TableGenerator#allocationSize
 */
public class PooledLoOptimizer extends AbstractOptimizer {

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
			// handle cases where the initial value is less than one (hsqldb, for instance)
			value = Math.max( lastSourceValue, 1L );
		}

		static long determineValue(long lastSourceValue, int incrementSize) {
			return Math.max( lastSourceValue, 1L );
		}
	}

	/**
	 * Constructs a {@code PooledLoOptimizer}.
	 *
	 * @param returnClass The Java type of the values to be generated
	 * @param incrementSize The increment size.
	 */
	public PooledLoOptimizer(Class<?> returnClass, int incrementSize) {
		super( returnClass, incrementSize );
		if ( incrementSize < 1 ) {
			throw new HibernateException( "increment size cannot be less than 1" );
		}
		OPTIMIZER_MESSAGE_LOGGER.creatingPooledLoOptimizer( incrementSize, returnClass.getName() );
	}

	@Override
	public Serializable generate(AccessCallback callback) {
		lock.lock();
		try {
			final var generationState = locateGenerationState( callback.getTenantIdentifier() );
			if ( generationState.lastSourceValue == null
					|| generationState.value >= generationState.upperLimitValue ) {
				generationState.setLastSourceValue( callback.getNextValue(), incrementSize );
			}
			return makeIntegralValue( generationState.value++, returnClass );
		}
		finally {
			lock.unlock();
		}
	}

	@Override
	public Serializable[] generateBatch(int count, AccessCallback callback) {
		final var valuesPerInvalidBucket = Math.max(incrementSize - 1, 1);
		final long[] buckets;
		final int invalidBuckets;
		final int availableValues;
		long baseValue;
		lock.lock();
		try {
			final GenerationState generationState = locateGenerationState( callback.getTenantIdentifier() );

			if ( generationState.lastSourceValue == null ) {
				// first call, so initialize ourselves.  we need to read the database
				// value and set up the 'bucket' boundaries
				buckets = determineInitialBuckets( count, callback );
				invalidBuckets = determineInvalidBuckets( buckets, count );
				generationState.setLastSourceValue( buckets[buckets.length - 1], incrementSize );
				baseValue = generationState.value;
				generationState.value += determineConsumedValuesFromLastBucket( count, buckets.length, invalidBuckets, valuesPerInvalidBucket, incrementSize );
				availableValues = 0;
			}
			else if ( generationState.value + count - 1 >= generationState.upperLimitValue ) {
				availableValues = Math.max( (int) (generationState.upperLimitValue - generationState.value), 0 );
				baseValue = generationState.value;
				buckets = callback.getNextValues( MathHelper.ceilDiv( count - availableValues, incrementSize ) );
				invalidBuckets = 0;
				generationState.setLastSourceValue( buckets[buckets.length - 1], incrementSize );
				generationState.value += ((count - availableValues - 1) % incrementSize) + 1;
			}
			else {
				availableValues = count;
				baseValue = generationState.value;
				generationState.value += count;
				buckets = ArrayHelper.EMPTY_LONG_ARRAY;
				invalidBuckets = 0;
			}
		}
		finally {
			lock.unlock();
		}
		final Serializable[] result = new Serializable[count];
		for ( int i = 0; i < availableValues; i++ ) {
			result[i] = makeIntegralValue( baseValue + i, returnClass );
		}
		for ( int i = 0; i < invalidBuckets; i++ ) {
			for (int j = 0; j < valuesPerInvalidBucket; j++ ) {
				result[i * valuesPerInvalidBucket + j] = makeIntegralValue( j + 1, returnClass );
			}
		}
		final var baseOffset = availableValues + invalidBuckets * valuesPerInvalidBucket;
		for ( int i = invalidBuckets; i < buckets.length; i++ ) {
			baseValue = GenerationState.determineValue( buckets[i], incrementSize );
			final var offset = baseOffset + (i - invalidBuckets) * incrementSize;
			for ( int j = 0; j < incrementSize && offset + j < result.length ; j++ ) {
				result[offset + j] = makeIntegralValue( baseValue++, returnClass );
			}
		}
		return result;
	}

	private int determineConsumedValuesFromLastBucket(int count, int bucketCount, int invalidBuckets, int valuesPerInvalidBucket, int incrementSize) {
		return count - Math.min( invalidBuckets, bucketCount - 1 ) * valuesPerInvalidBucket
			- Math.max( bucketCount - 1 - invalidBuckets, 0 ) * incrementSize;
	}

	private int determineInvalidBuckets(long[] buckets, int count) {
		int invalidBuckets = 0;
		for ( long bucketValue : buckets ) {
			if ( bucketValue > 0L ) {
				break;
			}
			invalidBuckets++;
		}
		return invalidBuckets;
	}

	private @Nonnull long[] determineInitialBuckets(int count, AccessCallback callback) {
		long[] goodBuckets = null;
		int neededBuckets = MathHelper.ceilDiv( count, incrementSize );
		do {
			final long[] newBuckets = callback.getNextValues( neededBuckets );
			if ( goodBuckets == null ) {
				goodBuckets = newBuckets;
			}
			else {
				goodBuckets = Arrays.copyOf(goodBuckets, goodBuckets.length + newBuckets.length);
				System.arraycopy( newBuckets, 0, goodBuckets, goodBuckets.length - newBuckets.length, newBuckets.length );
			}

			int invalidBuckets = 0;
			for ( long bucketValue : goodBuckets ) {
				if ( bucketValue > 0L ) {
					break;
				}
				invalidBuckets++;
			}
			final var valuesPerInvalidBucket = Math.max(incrementSize - 1, 1);
			final var availableValues = invalidBuckets * valuesPerInvalidBucket + (goodBuckets.length - invalidBuckets) * incrementSize;
			neededBuckets = availableValues >= count
					? 0
					: MathHelper.ceilDiv( count - availableValues, incrementSize );
		} while ( neededBuckets != 0 );
		return goodBuckets;
	}

	/**
	 * Use a lock instead of the monitor lock to avoid pinning when using virtual threads.
	 */
	private final Lock lock = new ReentrantLock();
	private GenerationState noTenantState;
	private Map<String,GenerationState> tenantSpecificState;

	@Override
	public void reset() {
		noTenantState = null;
		tenantSpecificState = null;
	}

	private GenerationState locateGenerationState(String tenantIdentifier) {
		if ( tenantIdentifier == null ) {
			if ( noTenantState == null ) {
				noTenantState = new GenerationState();
			}
			return noTenantState;
		}
		else {
			return generationState( tenantIdentifier );
		}
	}

	private GenerationState generationState(String tenantIdentifier) {
		if ( tenantSpecificState == null ) {
			tenantSpecificState = new ConcurrentHashMap<>();
			return assignNewStateToTenant( tenantIdentifier );
		}
		else {
			final var state = tenantSpecificState.get( tenantIdentifier );
			return state == null
					? assignNewStateToTenant( tenantIdentifier )
					: state;
		}
	}

	private GenerationState assignNewStateToTenant(String tenantIdentifier) {
		final var newState = new GenerationState();
		tenantSpecificState.put( tenantIdentifier, newState );
		return newState;
	}

	private GenerationState noTenantGenerationState() {
		if ( noTenantState == null ) {
			throw new IllegalStateException( "Could not locate previous generation state for no-tenant" );
		}
		return noTenantState;
	}

	@Override
	public Long getLastSourceValue() {
		return noTenantGenerationState().lastSourceValue;
	}

	@Override
	public boolean applyIncrementSizeToSourceValues() {
		return true;
	}

	@Override
	public Expression createLowValueExpression(Expression databaseValue, SessionFactoryImplementor sessionFactory) {
		return databaseValue;
	}
}
