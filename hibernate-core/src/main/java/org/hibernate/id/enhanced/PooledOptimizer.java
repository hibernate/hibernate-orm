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
import org.hibernate.internal.CoreMessageLogger;

import org.jboss.logging.Logger;

/**
 * Optimizer which uses a pool of values, storing the next low value of the
 * range in the database.
 * <p/>
 * Note that this optimizer works essentially the same as the
 * {@link org.hibernate.id.enhanced.HiLoOptimizer} except that here the bucket ranges are actually
 * encoded into the database structures.
 * <p/>
 * Note if you prefer that the database value be interpreted as the bottom end of our current range,
 * then use the {@link PooledLoOptimizer} strategy
 *
 * @author Steve Ebersole
 *
 * @see PooledLoOptimizer
 */
public class PooledOptimizer extends AbstractOptimizer implements InitialValueAwareOptimizer {
	private static final CoreMessageLogger log = Logger.getMessageLogger(
			CoreMessageLogger.class,
			PooledOptimizer.class.getName()
	);

	private static class GenerationState {
		private IntegralDataTypeHolder hiValue;
		private IntegralDataTypeHolder value;
	}

	private long initialValue = -1;

	/**
	 * Constructs a PooledOptimizer
	 *
	 * @param returnClass The Java type of the values to be generated
	 * @param incrementSize The increment size.
	 */
	public PooledOptimizer(Class returnClass, int incrementSize) {
		super( returnClass, incrementSize );
		if ( incrementSize < 1 ) {
			throw new HibernateException( "increment size cannot be less than 1" );
		}
		if ( log.isTraceEnabled() ) {
			log.tracev(
					"Creating pooled optimizer with [incrementSize={0}; returnClass={1}]",
					incrementSize,
					returnClass.getName()
			);
		}
	}


	@Override
	public synchronized Serializable generate(AccessCallback callback) {
		final GenerationState generationState = locateGenerationState( callback.getTenantIdentifier() );

		if ( generationState.hiValue == null ) {
			generationState.value = callback.getNextValue();
			// unfortunately not really safe to normalize this
			// to 1 as an initial value like we do the others
			// because we would not be able to control this if
			// we are using a sequence...
			if ( generationState.value.lt( 1 ) ) {
				log.pooledOptimizerReportedInitialValue( generationState.value );
			}
			// the call to obtain next-value just gave us the initialValue
			if ( ( initialValue == -1
					&& generationState.value.lt( incrementSize ) )
					|| generationState.value.eq( initialValue ) ) {
				generationState.hiValue = callback.getNextValue();
			}
			else {
				generationState.hiValue = generationState.value;
				generationState.value = generationState.hiValue.copy().subtract( incrementSize - 1 );
			}
		}
		else if ( generationState.value.gt( generationState.hiValue ) ) {
			generationState.hiValue = callback.getNextValue();
			generationState.value = generationState.hiValue.copy().subtract( incrementSize - 1 );
		}

		return generationState.value.makeValueThenIncrement();
	}

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
				tenantSpecificState = new ConcurrentHashMap<String, GenerationState>();
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
		return noTenantGenerationState().hiValue;
	}

	@Override
	public boolean applyIncrementSizeToSourceValues() {
		return true;
	}

	/**
	 * Getter for property 'lastValue'.
	 * <p/>
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
}
