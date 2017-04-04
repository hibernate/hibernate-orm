/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id.enhanced;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.id.IntegralDataTypeHolder;
import org.hibernate.internal.CoreMessageLogger;

import org.jboss.logging.Logger;

/**
 * Variation of {@link PooledOptimizer} which interprets the incoming database value as the lo value, rather than
 * the hi value, as well as using thread local to cache the generation state.
 *
 * @author Stuart Douglas
 * @author Scott Marlow
 * @author Steve Ebersole
 * @see PooledOptimizer
 */
public class PooledLoThreadLocalOptimizer extends AbstractOptimizer {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			PooledLoOptimizer.class.getName()
	);

	private static class GenerationState {
		// last value read from db source
		private IntegralDataTypeHolder lastSourceValue;
		// the current generator value
		private IntegralDataTypeHolder value;
		// the value at which we'll hit the db again
		private IntegralDataTypeHolder upperLimitValue;
	}

	/**
	 * Constructs a PooledLoThreadLocalOptimizer.
	 *
	 * @param returnClass The Java type of the values to be generated
	 * @param incrementSize The increment size.
	 */
	public PooledLoThreadLocalOptimizer(Class returnClass, int incrementSize) {
		super( returnClass, incrementSize );
		if ( incrementSize < 1 ) {
			throw new HibernateException( "increment size cannot be less than 1" );
		}
		LOG.creatingPooledLoOptimizer( incrementSize, returnClass.getName() );
	}

	@Override
	public Serializable generate(AccessCallback callback) {
		GenerationState local = null;
		if ( callback.getTenantIdentifier() == null ) {
			local = localAssignedIds.get();
			if ( local != null && local.value.lt( local.upperLimitValue ) ) {
				return local.value.makeValueThenIncrement();
			}
		}

		synchronized (this) {
			final GenerationState generationState = locateGenerationState( callback.getTenantIdentifier() );

			if ( generationState.lastSourceValue == null
					|| !generationState.value.lt( generationState.upperLimitValue )) {
				generationState.lastSourceValue = callback.getNextValue();
				generationState.upperLimitValue = generationState.lastSourceValue.copy().add( incrementSize );
				generationState.value = generationState.lastSourceValue.copy();
				// handle cases where initial-value is less that one (hsqldb for instance).
				while (generationState.value.lt( 1 )) {
					generationState.value.increment();
				}
			}
			return generationState.value.makeValueThenIncrement();
		}
	}

	private Map<String, GenerationState> tenantSpecificState;
	private final ThreadLocal<GenerationState> localAssignedIds = new ThreadLocal<GenerationState>();

	private GenerationState locateGenerationState(String tenantIdentifier) {
		if ( tenantIdentifier == null ) {
			GenerationState noTenantState = localAssignedIds.get();
			if ( noTenantState == null ) {
				noTenantState = new GenerationState();
				localAssignedIds.set(noTenantState);
			}
			return noTenantState;
		}
		else {
			GenerationState state;
			if ( tenantSpecificState == null ) {
				tenantSpecificState = new HashMap<String, GenerationState>();
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

	// for Hibernate testsuite use only
	private GenerationState noTenantGenerationState() {
		GenerationState noTenantState = locateGenerationState( null );

		if ( noTenantState == null ) {
			throw new IllegalStateException( "Could not locate previous generation state for no-tenant" );
		}
		return noTenantState;
	}

	// for Hibernate testsuite use only
	@Override
	public IntegralDataTypeHolder getLastSourceValue() {
		return noTenantGenerationState().lastSourceValue;
	}

	@Override
	public boolean applyIncrementSizeToSourceValues() {
		return true;
	}
}
