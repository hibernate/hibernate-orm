/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.id;

import java.io.Serializable;
import java.util.UUID;

import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * A strategy for generating a variant 2 {@link UUID} value.
 *
 * @deprecated see {@link UUIDGenerator}
 */
@Deprecated(since = "6.0")
public interface UUIDGenerationStrategy extends Serializable {
	/**
	 * Which variant, according to IETF RFC 4122, of UUID does this strategy generate?  RFC 4122 defines
	 * 5 variants (though it only describes algorithms to generate 4):<ul>
	 * <li>1 = time based</li>
	 * <li>2 = DCE based using POSIX UIDs</li>
	 * <li>3 = name based (md5 hash)</li>
	 * <li>4 = random numbers based</li>
	 * <li>5 = name based (sha-1 hash)</li>
	 * </ul>
	 * <p>
	 * Returning the values above should be reserved to those generators creating variants compliant with the
	 * corresponding RFC definition; others can feel free to return other values as they see fit.
	 * <p>
	 * Informational only, and not used at this time.
	 *
	 * @return The supported generation version
	 */
	int getGeneratedVersion();

	/**
	 * Generate the UUID.
	 *
	 * @param session The session asking for the generation
	 *
	 * @return The generated UUID.
	 */
	UUID generateUUID(SharedSessionContractImplementor session);

}
