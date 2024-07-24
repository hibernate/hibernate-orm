/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.id.uuid.annotation;

import java.util.UUID;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.uuid.UuidValueGenerator;

/**
 * @author Steve Ebersole
 */
public class CustomUuidValueGenerator implements UuidValueGenerator {
	private long counter = 0;

	@Override
	public UUID generateUuid(SharedSessionContractImplementor session) {
		final UUID sessionIdentifier = session.getSessionIdentifier();
		return new UUID( sessionIdentifier.getMostSignificantBits(), ++counter );
	}
}
