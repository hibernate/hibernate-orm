/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.id.enhanced;

import org.hibernate.AssertionFailure;

import static org.hibernate.internal.util.StringHelper.isEmpty;

/**
 * Enumeration of the standard Hibernate id generation optimizers.
 *
 * @author Steve Ebersole
 */
public enum StandardOptimizerDescriptor implements OptimizerDescriptor {
	/**
	 * Describes the optimizer for no optimization.
	 */
	NONE,
	/**
	 * Describes the optimizer for using a custom "hilo" algorithm optimization.
	 */
	HILO,
	/**
	 * Describes the optimizer for using a custom "hilo" algorithm optimization, following the
	 * legacy Hibernate hilo algorithm.
	 */
	LEGACY_HILO,
	/**
	 * Describes the optimizer for use with tables/sequences that store the chunk information.
	 * Here, specifically the hi value is stored in the database.
	 */
	POOLED,
	/**
	 * Describes the optimizer for use with tables/sequences that store the chunk information.
	 * Here, specifically the lo value is stored in the database.
	 */
	POOLED_LO,
	/**
	 * Describes the optimizer for use with tables/sequences that store the chunk information.
	 * Here, specifically the lo value is stored in the database and ThreadLocal used to cache
	 * the generation state.
	 */
	POOLED_LOTL;

	@Override
	public String getExternalName() {
		switch ( this ) {
			case NONE:
				return "none";
			case HILO:
				return "hilo";
			case LEGACY_HILO:
				return "legacy-hilo";
			case POOLED:
				return "pooled";
			case POOLED_LO:
				return "pooled-lo";
			case POOLED_LOTL:
				return "pooled-lotl";
		}
		throw new AssertionFailure( "unknown StandardOptimizerDescriptor" );
	}

	@Override
	public Class<? extends Optimizer> getOptimizerClass() {
		switch ( this ) {
			case NONE:
				return NoopOptimizer.class;
			case HILO:
				return HiLoOptimizer.class;
			case LEGACY_HILO:
				return LegacyHiLoAlgorithmOptimizer.class;
			case POOLED:
				return PooledOptimizer.class;
			case POOLED_LO:
				return PooledLoOptimizer.class;
			case POOLED_LOTL:
				return PooledLoThreadLocalOptimizer.class;
		}
		throw new AssertionFailure( "unknown StandardOptimizerDescriptor" );
	}

	@Override
	public boolean isPooled() {
		switch ( this ) {
			case NONE:
			case HILO:
			case LEGACY_HILO:
				return false;
			case POOLED:
			case POOLED_LO:
			case POOLED_LOTL:
				return true;
		}
		throw new AssertionFailure( "unknown StandardOptimizerDescriptor" );
	}

	/**
	 * Interpret the incoming external name into the appropriate enum value
	 *
	 * @param externalName The external name
	 *
	 * @return The corresponding enum value; if no external name is supplied,
	 * {@link #NONE} is returned; if an unrecognized external name is supplied,
	 * {@code null} is returned
	 */
	public static OptimizerDescriptor fromExternalName(String externalName) {
		if ( isEmpty( externalName ) ) {
			return NONE;
		}
		else {
			for ( StandardOptimizerDescriptor value: values() ) {
				if ( value.getExternalName().equals( externalName ) ) {
					return value;
				}
			}
			return new CustomOptimizerDescriptor( externalName );
		}
	}
}
