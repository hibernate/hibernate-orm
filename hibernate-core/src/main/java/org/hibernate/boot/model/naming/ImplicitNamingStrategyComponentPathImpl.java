/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming;

import org.hibernate.boot.model.source.spi.AttributePath;
import org.hibernate.spi.NavigablePath;

import static org.hibernate.internal.util.StringHelper.isNotEmpty;

/**
 * An ImplicitNamingStrategy implementation which uses full composite paths
 * extracted from AttributePath, as opposed to just the terminal property part.
 *
 * Mainly a port of the older DefaultComponentSafeNamingStrategy class implementing
 * the no longer supported NamingStrategy contract
 *
 * @author Steve Ebersole
 * @author Emmanuel Bernard
 */
public class ImplicitNamingStrategyComponentPathImpl extends ImplicitNamingStrategyJpaCompliantImpl {
	public static final ImplicitNamingStrategyComponentPathImpl INSTANCE = new ImplicitNamingStrategyComponentPathImpl();

	@Override
	protected String transformAttributePath(AttributePath attributePath) {
		final var name = new StringBuilder();
		process( attributePath, name );
		return name.toString();
	}

	public static void process(AttributePath attributePath, StringBuilder name) {
		final var parent = attributePath.getParent();
		if ( parent != null && isNotEmpty( parent.getProperty() ) ) {
			process( parent, name );
		}
		if ( !attributePath.isCollectionElement() ) {
			if ( !name.isEmpty() ) {
				name.append( '_' );
			}
			final String property = attributePath.getProperty();
			name.append( isIdentifierMapper( property ) ? "id" : stripAngles( property ) );
		}
		// else skip synthetic marker for collection elements
	}

	private static boolean isIdentifierMapper(String property) {
		return NavigablePath.IDENTIFIER_MAPPER_PROPERTY.equals( property );
	}

	private static String stripAngles(String property) {
		return property
				.replace( "<", "" )
				.replace( ">", "" );
	}
}
