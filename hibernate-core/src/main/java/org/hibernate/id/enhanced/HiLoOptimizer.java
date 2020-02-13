/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id.enhanced;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.HibernateException;
import org.hibernate.id.IntegralDataTypeHolder;

import org.jboss.logging.Logger;

/**
 * Optimizer which applies a 'hilo' algorithm in memory to achieve
 * optimization.
 * <p/>
 * A 'hilo' algorithm is simply a means for a single value stored in the
 * database to represent a "bucket" of possible, contiguous values.  The
 * database value identifies which particular bucket we are on.
 * <p/>
 * This database value must be paired with another value that defines the
 * size of the bucket; the number of possible values available.
 * The {@link #getIncrementSize() incrementSize} serves this purpose.  The
 * naming here is meant more for consistency in that this value serves the
 * same purpose as the increment supplied to the {@link PooledOptimizer}.
 * <p/>
 * The general algorithms used to determine the bucket are:<ol>
 * <li>{@code upperLimit = (databaseValue * incrementSize) + 1}</li>
 * <li>{@code lowerLimit = upperLimit - incrementSize}</li>
 * </ol>
 * As an example, consider a case with incrementSize of 20.  Initially the
 * database holds 1:<ol>
 * <li>{@code upperLimit = (1 * 20) + 1 = 21}</li>
 * <li>{@code lowerLimit = 21 - 20 = 1}</li>
 * </ol>
 * From there we increment the value from lowerLimit until we reach the
 * upperLimit, at which point we would define a new bucket.  The database
 * now contains 2, though incrementSize remains unchanged:<ol>
 * <li>{@code upperLimit = (2 * 20) + 1 = 41}</li>
 * <li>{@code lowerLimit = 41 - 20 = 21}</li>
 * </ol>
 * And so on...
 * <p/>
 * Note, 'value' always (after init) holds the next value to return
 *
 * @author Steve Ebersole
 */
public class HiLoOptimizer extends AbstractOptimizer {
	private static final Logger log = Logger.getLogger( HiLoOptimizer.class );

	protected static class GenerationState {
		private final int incrementSize;
		private IntegralDataTypeHolder lastSourceValue;
		private IntegralDataTypeHolder upperLimit;
		private IntegralDataTypeHolder value;

		/**
		 * @param incrementSize - the increment size.
		 * @throws HibernateException if {@code incrementSize < 1}.
		 */
		private GenerationState(int incrementSize) {
			if ( incrementSize < 1 ) {
				throw new HibernateException( "increment size cannot be less than 1" );
			}
			this.incrementSize = incrementSize;
		}

		/**
		 * Has this {@link GenerationState} been initialized with a
		 * "source" value (e.g., from the database).
		 *
		 * @return true, if it is initialized; false, otherwise.
		 */
		public final boolean isInitialized() {
			return lastSourceValue != null;
		}

		/**
		 * Is a new "source" value (e.g., from the database) required
		 * in order to obtain the next value? A new source value is
		 * required when {@link #isInitialized()} is {@code false},
		 * or the current value has already reached the upper limit.
		 * When {@code true} is returned, {@link #updateFromNewSourceValue(IntegralDataTypeHolder)}
		 * must be called in order to obtain the next value.
		 *
		 * @return true, if a new source value is required in order
		 *               to obtain the next value; false, otherwise.
		 */
		public final boolean requiresNewSourceValue() {
			return ! isInitialized() || ! upperLimit.gt( value );
		}

		/**
		 * Returns the next value, and increments the internal value
		 * in preparation for the following value to be requested.
		 *
		 * @return the next value.
		 * @throws HibernateException if {@link #requiresNewSourceValue()} is {@code true}.
		 */
		public final Number makeValueThenIncrement() {
			if ( requiresNewSourceValue() ) {
				throw new HibernateException( "Value is either uninitialized or has already reached the upper limit. GenerationState#updateFromNewSourceValue must be called before obtaining the next value." );
			}
			return value.makeValueThenIncrement();
		}

		/**
		 * Update this GenerationState with a new source value (e.g., from the database).
		 *
		 * @param sourceValue - the value from the "source" of values (e.g., from the database);
		 * @throws HibernateException if sourceValue < 1.
		 */
		public final void updateFromNewSourceValue(IntegralDataTypeHolder sourceValue) {
			// Copy sourceValue, so caller cannot modify the internal state.
			lastSourceValue = sourceValue.copy();

			// upperLimit defines the upper end of the bucket values
			upperLimit = lastSourceValue.copy().multiplyBy( incrementSize ).increment();
			// initialize value to the low end of the bucket
			value = upperLimit.copy().subtract( incrementSize );
		}
	}

	/**
	 * Constructs a HiLoOptimizer
	 *
	 * @param returnClass The Java type of the values to be generated
	 * @param incrementSize The increment size.
	 */
	public HiLoOptimizer(Class returnClass, int incrementSize) {
		super( returnClass, incrementSize );
		if ( incrementSize < 1 ) {
			throw new HibernateException( "increment size cannot be less than 1" );
		}
		if ( log.isTraceEnabled() ) {
			log.tracev( "Creating hilo optimizer with [incrementSize={0}; returnClass={1}]", incrementSize, returnClass.getName() );
		}
	}

	@Override
	public synchronized Serializable generate(AccessCallback callback) {
		final GenerationState generationState = locateGenerationState( callback.getTenantIdentifier() );

		if ( !generationState.isInitialized() ) {
			// first call, so initialize ourselves.  we need to read the database
			// value and set up the 'bucket' boundaries

			IntegralDataTypeHolder firstSourceValue = callback.getNextValue();
			while ( firstSourceValue.lt( 1 ) ) {
				firstSourceValue = callback.getNextValue();
			}
			generationState.updateFromNewSourceValue( firstSourceValue );
		}
		else if ( generationState.requiresNewSourceValue() ) {
			generationState.updateFromNewSourceValue( callback.getNextValue() );
		}
		return generationState.makeValueThenIncrement();
	}

	private GenerationState noTenantState;
	private Map<String,GenerationState> tenantSpecificState;

	protected GenerationState locateGenerationState(String tenantIdentifier) {
		if ( tenantIdentifier == null ) {
			if ( noTenantState == null ) {
				noTenantState = new GenerationState( incrementSize );
			}
			return noTenantState;
		}
		else {
			GenerationState state;
			if ( tenantSpecificState == null ) {
				tenantSpecificState = new ConcurrentHashMap<String, GenerationState>();
				state = new GenerationState( incrementSize );
				tenantSpecificState.put( tenantIdentifier, state );
			}
			else {
				state = tenantSpecificState.get( tenantIdentifier );
				if ( state == null ) {
					state = new GenerationState( incrementSize );
					tenantSpecificState.put( tenantIdentifier, state );
				}
			}
			return state;
		}
	}

	private GenerationState noTenantGenerationState() {
		if ( noTenantState == null ) {
			throw new IllegalStateException( "Could not locate previous generation state for no-tenant" );
		}
		return noTenantState;
	}

	@Override
	public synchronized IntegralDataTypeHolder getLastSourceValue() {
		return noTenantGenerationState().lastSourceValue;
	}

	@Override
	public boolean applyIncrementSizeToSourceValues() {
		return false;
	}

	/**
	 * Getter for property 'lastValue'.
	 * <p/>
	 * Exposure intended for testing purposes.
	 *
	 * @return Value for property 'lastValue'.
	 */
	public synchronized IntegralDataTypeHolder getLastValue() {
		return noTenantGenerationState().value.copy().decrement();
	}

	/**
	 * Getter for property 'upperLimit'.
	 * <p/>
	 * Exposure intended for testing purposes.
	 *
	 * @return Value for property 'upperLimit'.
	 */
	public synchronized IntegralDataTypeHolder getHiValue() {
		return noTenantGenerationState().upperLimit;
	}
}
