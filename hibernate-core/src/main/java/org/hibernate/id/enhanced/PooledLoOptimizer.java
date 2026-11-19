package org.hibernate.id.enhanced;

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
		final long[] buckets;
		final int availableValues;
		final long baseValue;
		lock.lock();
		try {
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
				buckets = fetchBuckets( neededValues, incrementSize, callback );
				int previousValues = 0;
				for ( int i = 0; i < buckets.length - 1; i++ ) {
					previousValues += bucketSize( buckets[i], incrementSize );
				}
				generationState.setLastSourceValue( buckets[buckets.length - 1], incrementSize );
				generationState.value += neededValues - previousValues;
			}
			else {
				buckets = ArrayHelper.EMPTY_LONG_ARRAY;
			}
			availableValues = available;
			baseValue = base;
		}
		finally {
			lock.unlock();
		}
		final Serializable[] result = new Serializable[count];
		for ( int i = 0; i < availableValues; i++ ) {
			result[i] = makeIntegralValue( baseValue + i, returnClass );
		}
		int position = availableValues;
		for ( long bucket : buckets ) {
			final long start = GenerationState.determineValue( bucket, incrementSize );
			final int size = bucketSize( bucket, incrementSize );
			for ( int j = 0; j < size && position < count; j++ ) {
				result[position++] = makeIntegralValue( start + j, returnClass );
			}
		}
		return result;
	}

	static int bucketSize(long sourceValue, int incrementSize) {
		// mirrors the singular generate(): values run from max(source,1) up to (excluding) source + incrementSize,
		// but the first value after fetching is always used, even if the bucket is already exhausted
		return (int) Math.max( 1L, sourceValue + incrementSize - Math.max( sourceValue, 1L ) );
	}

	static long[] fetchBuckets(int neededValues, int incrementSize, AccessCallback callback) {
		// a bucket never holds more than incrementSize values, so fetching ceil(remaining / incrementSize)
		// buckets at a time never retrieves more source values than repeated singular generation would
		long[] buckets = ArrayHelper.EMPTY_LONG_ARRAY;
		int remaining = neededValues;
		while ( remaining > 0 ) {
			final long[] newBuckets = callback.getNextValues( MathHelper.ceilDiv( remaining, incrementSize ) );
			final int previous = buckets.length;
			buckets = Arrays.copyOf( buckets, previous + newBuckets.length );
			System.arraycopy( newBuckets, 0, buckets, previous, newBuckets.length );
			for ( long newBucket : newBuckets ) {
				remaining -= bucketSize( newBucket, incrementSize );
			}
		}
		return buckets;
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
