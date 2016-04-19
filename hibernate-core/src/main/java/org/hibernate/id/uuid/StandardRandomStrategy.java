/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id.uuid;

import java.util.UUID;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.UUIDGenerationStrategy;

/**
 * Implements a "random" UUID generation strategy as defined by the {@link UUID#randomUUID()} method.
 *
 * @author Steve Ebersole
 */
public class StandardRandomStrategy implements UUIDGenerationStrategy {
	public static final StandardRandomStrategy INSTANCE = new StandardRandomStrategy();

	/**
	 * A variant 4 (random) strategy
	 */
	@Override
	public int getGeneratedVersion() {
		// a "random" strategy
		return 4;
	}

	/**
	 * Delegates to {@link UUID#randomUUID()}
	 */
	@Override
	public UUID generateUUID(SharedSessionContractImplementor session) {
		return UUID.randomUUID();
	}
}
