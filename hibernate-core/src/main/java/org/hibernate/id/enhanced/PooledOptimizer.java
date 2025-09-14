/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.id.enhanced;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.id.IntegralDataTypeHolder;
import static org.hibernate.id.enhanced.OptimizerLogger.OPTIMIZER_MESSAGE_LOGGER;
import org.hibernate.query.sqm.BinaryArithmeticOperator;
import org.hibernate.sql.ast.tree.expression.BinaryArithmeticExpression;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.QueryLiteral;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Optimizer which uses a pool of values, storing the next low value of the range
 * in the database.
 * <p>
 * This optimizer works essentially the same as the {@link HiLoOptimizer}, except
 * that here the bucket ranges are actually encoded into the database structures.
 * <p>
 * If you prefer that the database value be interpreted as the bottom end of our
 * current range, then use the {@link PooledLoOptimizer} strategy.
 *
 * @author Steve Ebersole
 *
 * @see PooledLoOptimizer
 */
public class PooledOptimizer extends AbstractOptimizer implements InitialValueAwareOptimizer {

	private static class GenerationState {
		private IntegralDataTypeHolder hiValue;
		private IntegralDataTypeHolder value;
	}

	private long initialValue = -1;

	/**
	 * Constructs a {@code PooledOptimizer}
	 *
	 * @param returnClass The Java type of the values to be generated
	 * @param incrementSize The increment size.
	 */
	public PooledOptimizer(Class<?> returnClass, int incrementSize) {
		super( returnClass, incrementSize );
		if ( incrementSize < 1 ) {
			throw new HibernateException( "increment size cannot be less than 1" );
		}
		OPTIMIZER_MESSAGE_LOGGER.creatingPooledOptimizer( incrementSize, returnClass.getName() );
	}


	@Override
	public Serializable generate(AccessCallback callback) {
		lock.lock();
		try {
			final GenerationState generationState = locateGenerationState( callback.getTenantIdentifier() );

			if ( generationState.hiValue == null ) {
				generationState.hiValue = callback.getNextValue();
				// unfortunately not really safe to normalize this
				// to 1 as an initial value like we do for the others
				// because we would not be able to control this if
				// we are using a sequence...
				if ( generationState.hiValue.lt( 1 ) ) {
					OPTIMIZER_MESSAGE_LOGGER.pooledOptimizerReportedInitialValue( generationState.hiValue );
				}
				// the call to obtain next-value just gave us the initialValue
				if ( ( initialValue == -1
						&& generationState.hiValue.lt( incrementSize ) )
						|| generationState.hiValue.eq( initialValue ) ) {
					generationState.value = generationState.hiValue.copy();
				}
				else {
					generationState.value = generationState.hiValue.copy().subtract( incrementSize - 1 );
				}
			}
			else if ( generationState.value.gt( generationState.hiValue ) ) {
				generationState.hiValue = callback.getNextValue();
				generationState.value = generationState.hiValue.copy().subtract( incrementSize - 1 );
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
		return noTenantGenerationState().hiValue;
	}

	@Override
	public boolean applyIncrementSizeToSourceValues() {
		return true;
	}

	/**
	 * Getter for property 'lastValue'.
	 * <p>
	 * Exposure intended for testing purposes.
	 *
	 * @return Value for property 'lastValue'.
	 */
	public IntegralDataTypeHolder getLastValue() {
		return noTenantGenerationState().value.copy().decrement();
	}

	@Override
	public void injectInitialValue(long initialValue) {
		this.initialValue = initialValue;
	}

	@Override
	public Expression createLowValueExpression(Expression databaseValue, SessionFactoryImplementor sessionFactory) {
		final var integerType = sessionFactory.getTypeConfiguration().getBasicTypeForJavaType( Integer.class );
		return new BinaryArithmeticExpression(
				databaseValue,
				BinaryArithmeticOperator.SUBTRACT,
				new QueryLiteral<>( incrementSize - 1, integerType ),
				integerType
		);
	}
}
