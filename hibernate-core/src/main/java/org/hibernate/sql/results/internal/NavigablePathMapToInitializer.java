/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.internal;

import java.util.HashMap;
import java.util.Objects;

import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.Initializer;


/**
 * This is in all practical terms a {@code Map<NavigablePath, Initializer>}
 * but wrapping a {@code HashMap} to keep the client code readable as we need
 * to:
 * <ul>
 *     <li>have a way to log all initializers</li>
 *     <li>prevent type pollution from happening on Initializer retrieval</li>
 * </ul>
 * I also consider it good practice to only expose the minimal set of
 * operations the client actually needs.
 */
public final class NavigablePathMapToInitializer {

	private HashMap<NavigablePath, InitializerHolder> map = null;

	public Initializer<?> get(final NavigablePath navigablePath) {
		if ( map != null && navigablePath != null ) {
			final InitializerHolder h = map.get( navigablePath );
			if ( h != null ) {
				return h.initializer;
			}
		}
		return null;
	}

	public void put(final NavigablePath navigablePath, final Initializer<?> initializer) {
		Objects.requireNonNull( navigablePath );
		Objects.requireNonNull( initializer );
		if ( map == null ) {
			map = new HashMap<>();
		}
		map.put( navigablePath, new InitializerHolder( initializer ) );
	}

	public void logInitializers() {
		// Disabling for now because way too verbose and messy, and
		// because it duplicates the 'Registering initializer' logs
//		final ResultsLogger logger = ResultsLogger.RESULTS_MESSAGE_LOGGER;
//		if ( logger.isTraceEnabled() ) {
//			if ( map == null ) {
//				logger.trace( "Initializer list is empty" );
//			}
//			else {
//				//Apparently we want to log this on multiple lines (existing code did this - not sure if that was by design):
//				//using a StringBuilder to avoid potentially interleaving the logs from different operations.
//				final StringBuilder sb = new StringBuilder( "Initializer list:\n" );
//				for ( var holderEntry : map.entrySet() ) {
//					final NavigablePath navigablePath = holderEntry.getKey();
//					final Initializer<?> initializer = holderEntry.getValue().initializer;
//					final String formatted = String.format(
//							"  %s -> %s [@%s] %s",
//							navigablePath,
//							initializer,
//							initializer.hashCode(),
//							initializer.getInitializedPart()
//					);
//					sb.append( '\t' );
//					sb.append( formatted );
//					sb.append( '\n' );
//				}
//				logger.trace( sb.toString() );
//			}
//		}
	}

	//Custom holder to avoid type pollution:
	//we make the type explicit, and this is a concrete class.
	private static final class InitializerHolder {
		final Initializer<?> initializer;

		private InitializerHolder(final Initializer<?> init) {
			this.initializer = init;
		}
	}

}
