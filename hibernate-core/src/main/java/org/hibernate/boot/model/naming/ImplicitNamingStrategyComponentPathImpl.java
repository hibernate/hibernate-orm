/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming;

import org.hibernate.boot.model.source.spi.AttributePath;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.spi.NavigablePath;

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
		final StringBuilder sb = new StringBuilder();
		process( attributePath, sb );
		return sb.toString();
	}

	public static void process(AttributePath attributePath, StringBuilder sb) {
		String property = attributePath.getProperty();
		final AttributePath parent = attributePath.getParent();
		if ( parent != null && StringHelper.isNotEmpty( parent.getProperty() ) ) {
			process( parent, sb );
			sb.append( '_' );
		}
		else if ( NavigablePath.IDENTIFIER_MAPPER_PROPERTY.equals( property ) ) {
			// skip it, do not pass go
			sb.append( "id" );
			return;
		}
		property = property.replace( "<", "" );
		property = property.replace( ">", "" );

		sb.append( property );
	}
}
