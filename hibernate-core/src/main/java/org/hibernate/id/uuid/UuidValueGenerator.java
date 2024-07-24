/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.id.uuid;

import java.util.UUID;

import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * Represents a specific algorithm for producing UUID values.  Used in
 * conjunction with {@linkplain UuidGenerator} and
 *
 * @author Steve Ebersole
 */
public interface UuidValueGenerator {
	/**
	 * Generate the UUID value
	 */
	UUID generateUuid(SharedSessionContractImplementor session);
}
