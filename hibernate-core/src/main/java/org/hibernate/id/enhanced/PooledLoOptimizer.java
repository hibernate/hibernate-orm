/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.id.enhanced;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.id.IntegralDataTypeHolder;
import static org.hibernate.id.enhanced.OptimizerLogger.OPTIMIZER_MESSAGE_LOGGER;
import org.hibernate.sql.ast.tree.expression.Expression;

import java.io.Serializable;
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
		private IntegralDataTypeHolder lastSourceValue;
		// the current generator value
		private IntegralDataTypeHolder value;
		// the value at which we'll hit the db again
		private IntegralDataTypeHolder upperLimitValue;
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
					|| ! generationState.value.lt( generationState.upperLimitValue ) ) {
				generationState.lastSourceValue = callback.getNextValue();
				generationState.upperLimitValue = generationState.lastSourceValue.copy().add( incrementSize );
				generationState.value = generationState.lastSourceValue.copy();
				// handle cases where the initial value is less than one (hsqldb, for instance)
				while ( generationState.value.lt( 1 ) ) {
					generationState.value.increment();
				}
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
	public IntegralDataTypeHolder getLastSourceValue() {
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
