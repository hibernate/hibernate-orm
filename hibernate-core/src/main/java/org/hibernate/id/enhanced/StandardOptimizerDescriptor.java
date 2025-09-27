/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.id.enhanced;

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
		return switch ( this ) {
			case NONE -> "none";
			case HILO -> "hilo";
			case LEGACY_HILO -> "legacy-hilo";
			case POOLED -> "pooled";
			case POOLED_LO -> "pooled-lo";
			case POOLED_LOTL -> "pooled-lotl";
		};
	}

	@Override
	public Class<? extends Optimizer> getOptimizerClass() {
		return switch ( this ) {
			case NONE -> NoopOptimizer.class;
			case HILO -> HiLoOptimizer.class;
			case LEGACY_HILO -> LegacyHiLoAlgorithmOptimizer.class;
			case POOLED -> PooledOptimizer.class;
			case POOLED_LO -> PooledLoOptimizer.class;
			case POOLED_LOTL -> PooledLoThreadLocalOptimizer.class;
		};
	}

	@Override
	public boolean isPooled() {
		return switch ( this ) {
			case NONE, HILO, LEGACY_HILO -> false;
			case POOLED, POOLED_LO, POOLED_LOTL -> true;
		};
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
			for ( var value: values() ) {
				if ( value.getExternalName().equalsIgnoreCase( externalName ) ) {
					return value;
				}
			}
			return new CustomOptimizerDescriptor( externalName );
		}
	}
}
