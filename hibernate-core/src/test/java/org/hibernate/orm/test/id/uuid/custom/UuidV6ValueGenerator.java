package org.hibernate.orm.test.id.uuid.custom;

import java.util.UUID;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.UUIDGenerationStrategy;
import org.hibernate.id.uuid.StandardRandomStrategy;
import org.hibernate.id.uuid.UuidValueGenerator;

public class UuidV6ValueGenerator implements UUIDGenerationStrategy, UuidValueGenerator {
	public static final UuidV6ValueGenerator INSTANCE = new UuidV6ValueGenerator();

	/**
	 * A variant 6
	 */
	@Override
	public int getGeneratedVersion() {
		// UUID v6
		return 6;
	}

	/**
	 * Delegates to {@link UUID#randomUUID()}
	 */
	@Override
	public UUID generateUUID(SharedSessionContractImplementor session) {
		return generateUuid( session );
	}

	@Override
	public UUID generateUuid(SharedSessionContractImplementor session) {
		return UUIDv6.nextIdentifier();
	}
}
