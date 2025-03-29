/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.id.enhanced;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.hibernate.HibernateException;
import org.hibernate.id.IntegralDataTypeHolder;

import org.jboss.logging.Logger;

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
	private static final Logger log = Logger.getLogger( HiLoOptimizer.class );

	private static class GenerationState {
		private IntegralDataTypeHolder lastSourceValue;
		private IntegralDataTypeHolder upperLimit;
		private IntegralDataTypeHolder value;
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
		if ( log.isTraceEnabled() ) {
			log.tracev( "Creating hilo optimizer with [incrementSize={0}; returnClass={1}]", incrementSize, returnClass.getName() );
		}
	}

	@Override
	public Serializable generate(AccessCallback callback) {
		lock.lock();
		try {
			final GenerationState generationState = locateGenerationState( callback.getTenantIdentifier() );

			if ( generationState.lastSourceValue == null ) {
				// first call, so initialize ourselves.  we need to read the database
				// value and set up the 'bucket' boundaries
				generationState.lastSourceValue = callback.getNextValue();
				while ( generationState.lastSourceValue.lt( 1 ) ) {
					generationState.lastSourceValue = callback.getNextValue();
				}
				// upperLimit defines the upper end of the bucket values
				generationState.upperLimit = generationState.lastSourceValue.copy().multiplyBy( incrementSize ).increment();
				// initialize value to the lower end of the bucket
				generationState.value = generationState.upperLimit.copy().subtract( incrementSize );
			}
			else if ( ! generationState.upperLimit.gt( generationState.value ) ) {
				generationState.lastSourceValue = callback.getNextValue();
				generationState.upperLimit = generationState.lastSourceValue.copy().multiplyBy( incrementSize ).increment();
				generationState.value = generationState.upperLimit.copy().subtract( incrementSize );
			}
			return generationState.value.makeValueThenIncrement();
		}
		finally {
			lock.unlock();
		}
	}

	/**
	 * Use a lock instead of the monitor lock to avoid pinning when using virtual threads.
	 */
	private final Lock lock = new ReentrantLock();
	private GenerationState noTenantState;
	private Map<String,GenerationState> tenantSpecificState;

	private GenerationState locateGenerationState(String tenantIdentifier) {
		if ( tenantIdentifier == null ) {
			if ( noTenantState == null ) {
				noTenantState = new GenerationState();
			}
			return noTenantState;
		}
		else {
			GenerationState state;
			if ( tenantSpecificState == null ) {
				tenantSpecificState = new ConcurrentHashMap<>();
				state = new GenerationState();
				tenantSpecificState.put( tenantIdentifier, state );
			}
			else {
				state = tenantSpecificState.get( tenantIdentifier );
				if ( state == null ) {
					state = new GenerationState();
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
	public IntegralDataTypeHolder getLastSourceValue() {
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
	public IntegralDataTypeHolder getLastValue() {
		lock.lock();
		try {
			return noTenantGenerationState().value.copy().decrement();
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
	public IntegralDataTypeHolder getHiValue() {
		lock.lock();
		try {
			return noTenantGenerationState().upperLimit;
		}
		finally {
			lock.unlock();
		}
	}
}
