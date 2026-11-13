package org.hibernate.id.enhanced;

import jakarta.annotation.Nonnull;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.MathHelper;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.query.sqm.BinaryArithmeticOperator;
import org.hibernate.sql.ast.spi.query.expression.BinaryArithmeticExpression;
import org.hibernate.sql.ast.spi.query.expression.Expression;
import org.hibernate.sql.ast.spi.query.expression.QueryLiteral;

import static org.hibernate.id.IdentifierGeneratorHelper.makeIntegralValue;
import static org.hibernate.id.enhanced.OptimizerLogger.OPTIMIZER_MESSAGE_LOGGER;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Optimizer which applies a 'hilo' algorithm in memory to achieve
 * optimization.
 * <p>
 * A 'hilo' algorithm is simply a means for a single value stored in the
 * database to represent a "bucket" of possible, contiguous values. The
 * database value identifies which particular bucket we are on.
 * <p>
 * This database value must be paired with another value that defines the
 * size of the bucket; the number of possible values available.
 * The {@link #getIncrementSize() incrementSize} serves this purpose. The
 * naming here is meant more for consistency in that this value serves the
 * same purpose as the increment supplied to the {@link PooledOptimizer}.
 * <p>
 * The general algorithms used to determine the bucket is:
 * <ol>
 * <li>{@code upperLimit = (databaseValue * incrementSize) + 1}</li>
 * <li>{@code lowerLimit = upperLimit - incrementSize}</li>
 * </ol>
 * <p>
 * As an example, consider a case with incrementSize of 20. Initially, the
 * database holds 1:<ol>
 * <li>{@code upperLimit = (1 * 20) + 1 = 21}</li>
 * <li>{@code lowerLimit = 21 - 20 = 1}</li>
 * </ol>
 * <p>
 * From there we increment the value from lowerLimit until we reach the
 * upperLimit, at which point we would define a new bucket. The database
 * now contains 2, though incrementSize remains unchanged:<ol>
 * <li>{@code upperLimit = (2 * 20) + 1 = 41}</li>
 * <li>{@code lowerLimit = 41 - 20 = 21}</li>
 * </ol>
 * And so on...
 * <p>
 * Note, 'value' always (after init) holds the next value to return
 *
 * @author Steve Ebersole
 */
public class HiLoOptimizer extends AbstractOptimizer {

	private static class GenerationState {
		private Long lastSourceValue;
		private long upperLimit;
		private long value;

		void setLastSourceValue(long lastSourceValue, int incrementSize) {
			this.lastSourceValue = lastSourceValue;
			// upperLimit defines the upper end of the bucket values
			upperLimit = lastSourceValue * incrementSize + 1;
			// initialize value to the lower end of the bucket
			value = upperLimit - incrementSize;
		}

		static long determineValue(long lastSourceValue, int incrementSize) {
			return (lastSourceValue * incrementSize + 1) - incrementSize;
		}
	}


	/**
	 * Constructs a {@code HiLoOptimizer}
	 *
	 * @param returnClass The Java type of the values to be generated
	 * @param incrementSize The increment size.
	 */
	public HiLoOptimizer(Class<?> returnClass, int incrementSize) {
		super( returnClass, incrementSize );
		if ( incrementSize < 1 ) {
			throw new HibernateException( "increment size cannot be less than 1" );
		}
		OPTIMIZER_MESSAGE_LOGGER.creatingHiLoOptimizer( incrementSize, returnClass.getName() );
	}

	@Override
	public Serializable generate(AccessCallback callback) {
		lock.lock();
		try {
			final GenerationState generationState = locateGenerationState( callback.getTenantIdentifier() );
			if ( generationState.lastSourceValue == null ) {
				// first call, so initialize ourselves.  we need to read the database
				// value and set up the 'bucket' boundaries
				long lastSourceValue = callback.getNextValue();
				while ( lastSourceValue < 1 ) {
					lastSourceValue = callback.getNextValue();
				}
				generationState.setLastSourceValue( lastSourceValue, incrementSize );
			}
			else if ( generationState.value >= generationState.upperLimit ) {
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
		long baseValue;
		lock.lock();
		try {
			final GenerationState generationState = locateGenerationState( callback.getTenantIdentifier() );

			if ( generationState.lastSourceValue == null ) {
				// first call, so initialize ourselves.  we need to read the database
				// value and set up the 'bucket' boundaries
				buckets = determineInitialBuckets( count, callback );
				generationState.setLastSourceValue( buckets[buckets.length - 1], incrementSize );
				availableValues = 0;
				baseValue = generationState.value;
				generationState.value += ((count - 1) % incrementSize) + 1;
			}
			else if ( generationState.value + count > generationState.upperLimit ) {
				availableValues = (int) (generationState.upperLimit - generationState.value);
				baseValue = generationState.value;
				buckets = callback.getNextValues( MathHelper.ceilDiv( count - availableValues, incrementSize ) );
				generationState.setLastSourceValue( buckets[buckets.length - 1], incrementSize );
				generationState.value += ((count - availableValues - 1) % incrementSize) + 1;
			}
			else {
				availableValues = count;
				baseValue = generationState.value;
				generationState.value += count;
				buckets = ArrayHelper.EMPTY_LONG_ARRAY;
			}
		}
		finally {
			lock.unlock();
		}
		final Serializable[] result = new Serializable[count];
		for ( int i = 0; i < availableValues; i++ ) {
			result[i] = makeIntegralValue( baseValue + i, returnClass );
		}
		for ( int i = 0; i < buckets.length; i++ ) {
			baseValue = GenerationState.determineValue( buckets[i], incrementSize );
			final var offset = availableValues + i * incrementSize;
			for ( int j = 0; j < incrementSize && offset + j < result.length ; j++ ) {
				result[offset + j] = makeIntegralValue( baseValue++, returnClass );
			}
		}
		return result;
	}

	private @Nonnull long[] determineInitialBuckets(int count, AccessCallback callback) {
		long[] goodBuckets = null;
		int invalidBuckets = MathHelper.ceilDiv( count, incrementSize );
		do {
			final long[] newBuckets = callback.getNextValues( invalidBuckets );
			if ( goodBuckets == null ) {
				goodBuckets = newBuckets;
			}
			else {
				// Shift the values to the left until we have only values greater or equal to 1
				System.arraycopy( goodBuckets, invalidBuckets, goodBuckets, 0, goodBuckets.length - invalidBuckets );
				System.arraycopy( newBuckets, 0, goodBuckets, goodBuckets.length - invalidBuckets, newBuckets.length );
			}

			invalidBuckets = 0;
			for ( long bucketValue : goodBuckets ) {
				if ( bucketValue > 0L ) {
					break;
				}
				invalidBuckets++;
			}
		} while ( invalidBuckets != 0 );
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

	private GenerationState locateGenerationState(String tenantId) {
		if ( tenantId == null ) {
			if ( noTenantState == null ) {
				noTenantState = new GenerationState();
			}
			return noTenantState;
		}
		else {
			if ( tenantSpecificState == null ) {
				tenantSpecificState = new ConcurrentHashMap<>();
				final var state = new GenerationState();
				tenantSpecificState.put( tenantId, state );
				return state;
			}
			else {
				var state = tenantSpecificState.get( tenantId );
				if ( state == null ) {
					state = new GenerationState();
					tenantSpecificState.put( tenantId, state );
				}
				return state;
			}
		}
	}

	private GenerationState noTenantGenerationState() {
		if ( noTenantState == null ) {
			throw new IllegalStateException( "Could not locate previous generation state for no-tenant" );
		}
		return noTenantState;
	}

	@Override
	public Long getLastSourceValue() {
		lock.lock();
		try {
			return noTenantGenerationState().lastSourceValue;
		}
		finally {
			lock.unlock();
		}
	}

	@Override
	public boolean applyIncrementSizeToSourceValues() {
		return false;
	}

	/**
	 * Getter for property 'lastValue'.
	 * <p>
	 * Exposure intended for testing purposes.
	 *
	 * @return Value for property 'lastValue'.
	 */
	public long getLastValue() {
		lock.lock();
		try {
			return noTenantGenerationState().value - 1;
		}
		finally {
			lock.unlock();
		}
	}

	/**
	 * Getter for property 'upperLimit'.
	 * <p>
	 * Exposure intended for testing purposes.
	 *
	 * @return Value for property 'upperLimit'.
	 */
	public long getHiValue() {
		lock.lock();
		try {
			return noTenantGenerationState().upperLimit;
		}
		finally {
			lock.unlock();
		}
	}

	@Override
	public Expression createLowValueExpression(Expression databaseValue, SessionFactoryImplementor sessionFactory) {
		BasicValuedMapping integerType = sessionFactory.getTypeConfiguration().getBasicTypeForJavaType( Integer.class );
		return new BinaryArithmeticExpression(
				new BinaryArithmeticExpression(
						databaseValue,
						BinaryArithmeticOperator.MULTIPLY,
						new QueryLiteral<>( incrementSize, integerType ),
						integerType
				),
				BinaryArithmeticOperator.SUBTRACT,
				new QueryLiteral<>( incrementSize - 1, integerType ),
				integerType
		);
	}
}
