package org.hibernate.id.uuid;

import java.util.UUID;

import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * Represents a specific algorithm for producing UUID values.
 * <p>
 * Used in conjunction with {@link UuidGenerator} and
 * {@link org.hibernate.annotations.UuidGenerator @UuidGenerator}.
 *
 * @author Steve Ebersole
 */
public interface UuidValueGenerator {
	/**
	 * Generate the UUID value
	 */
	UUID generateUuid(SharedSessionContractImplementor session);
	/**
	 * Generate the UUID values
	 */
	default UUID[] generateUuids(int count, SharedSessionContractImplementor session) {
		UUID[] uuids = new UUID[count];
		for (int i = 0; i < count; i++) {
			uuids[i] = generateUuid( session );
		}
		return uuids;
	}
}
