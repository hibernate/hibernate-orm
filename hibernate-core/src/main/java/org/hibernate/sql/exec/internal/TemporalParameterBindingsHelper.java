/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.exec.internal;

import java.util.List;

import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;

/**
 * Applies bindings for temporal parameters before execution.
 *
 * @author Gavin King
 */
public final class TemporalParameterBindingsHelper {

	private TemporalParameterBindingsHelper() {
	}

	public static JdbcParameterBindings applyTemporalParameterBindings(
			JdbcParameterBindings jdbcParameterBindings,
			List<JdbcParameterBinder> parameterBinders,
			LoadQueryInfluencers loadQueryInfluencers) {
		if ( parameterBinders == null || parameterBinders.isEmpty() ) {
			return jdbcParameterBindings;
		}

		var effectiveBindings =
				jdbcParameterBindings == null
						? JdbcParameterBindings.NO_BINDINGS
						: jdbcParameterBindings;
		Object temporalIdentifier = null;

		for ( var binder : parameterBinders ) {
			if ( binder instanceof TemporalJdbcParameter parameter ) {
				if ( loadQueryInfluencers != null && temporalIdentifier == null ) {
					temporalIdentifier = loadQueryInfluencers.getTemporalIdentifier();
				}
				if ( effectiveBindings == JdbcParameterBindings.NO_BINDINGS ) {
					effectiveBindings = new JdbcParameterBindingsImpl( 1 );
				}
				if ( effectiveBindings.getBinding( parameter ) == null ) {
					final var jdbcMapping = parameter.getJdbcMapping();
					final var jdbcValue = jdbcMapping.convertToRelationalValue( temporalIdentifier );
					effectiveBindings.addBinding( parameter,
							new JdbcParameterBindingImpl( jdbcMapping, jdbcValue ) );
				}
			}
		}

		return effectiveBindings;
	}
}
