/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming;

import org.hibernate.SPI;
import org.hibernate.boot.model.source.spi.AttributePath;

/**
 * An ImplicitNamingStrategy implementation which uses full composite paths
 * extracted from AttributePath, as opposed to just the terminal property part.
 *
 * Mainly a port of the older DefaultComponentSafeNamingStrategy class implementing
 * the no longer supported NamingStrategy contract
 *
 * @deprecated Use {@link org.hibernate.boot.model.naming.spi.StandardImplicitNamingStrategy}
 * or {@link ImplicitNamingStrategyJpaCompliantImpl}. Verify mapping names when migrating.
 *
 * @author Steve Ebersole
 * @author Emmanuel Bernard
 */
@Deprecated(since = "9.0", forRemoval = true)
@SPI({ SPI.Role.USE, SPI.Role.IMPLEMENT })
public class ImplicitNamingStrategyComponentPathImpl extends ImplicitNamingStrategyJpaCompliantImpl {
	@SPI(SPI.Role.USE)
	public ImplicitNamingStrategyComponentPathImpl() {
	}

	public static final ImplicitNamingStrategyComponentPathImpl INSTANCE = new ImplicitNamingStrategyComponentPathImpl();

	@Override
	protected String transformAttributePath(AttributePath attributePath) {
		final StringBuilder sb = new StringBuilder();
		process( attributePath, sb );
		return sb.toString();
	}

	public static void process(AttributePath attributePath, StringBuilder sb) {
		appendAttributePath( attributePath, sb );
	}
}
