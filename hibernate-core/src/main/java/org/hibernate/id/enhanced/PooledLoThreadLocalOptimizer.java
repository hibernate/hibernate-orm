/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.id.enhanced;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.id.IntegralDataTypeHolder;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.sql.ast.tree.expression.Expression;

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

	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( PooledLoOptimizer.class );

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
		LOG.creatingPooledLoOptimizer( incrementSize, returnClass.getName() );
	}

	@Override
	public Serializable generate(AccessCallback callback) {
		return locateGenerationState( callback.getTenantIdentifier() )
				.generate( callback, incrementSize );
	}

	private GenerationState locateGenerationState(String tenantIdentifier) {
		if ( tenantIdentifier == null ) {
			return singleTenantState.get();
		}
		else {
			Map<String, GenerationState> states = multiTenantStates.get();
			GenerationState state = states.get( tenantIdentifier );
			if ( state == null ) {
				state = new GenerationState();
				states.put( tenantIdentifier, state );
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

	private static class GenerationState {
		// last value read from db source
		private IntegralDataTypeHolder lastSourceValue;
		// the current generator value
		private IntegralDataTypeHolder value;
		// the value at which we'll hit the db again
		private IntegralDataTypeHolder upperLimitValue;

		private Serializable generate(AccessCallback callback, int incrementSize) {
			if ( value == null || !value.lt( upperLimitValue ) ) {
				lastSourceValue = callback.getNextValue();
				upperLimitValue = lastSourceValue.copy().add( incrementSize );
				value = lastSourceValue.copy();
				// handle cases where initial-value is less that one (hsqldb for instance).
				while ( value.lt( 1 ) ) {
					value.increment();
				}
			}
			return value.makeValueThenIncrement();
		}
	}

	@Override
	public Expression createLowValueExpression(Expression databaseValue, SessionFactoryImplementor sessionFactory) {
		return databaseValue;
	}
}
