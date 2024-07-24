package org.hibernate.orm.test.id.uuid.custom;

import java.util.UUID;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.UUIDGenerationStrategy;
import org.hibernate.id.uuid.StandardRandomStrategy;
import org.hibernate.id.uuid.UuidValueGenerator;

public class UuidV7ValueGenerator implements UUIDGenerationStrategy, UuidValueGenerator {
	public static final StandardRandomStrategy INSTANCE = new StandardRandomStrategy();

	/**
	 * A variant 4 (random) strategy
	 */
	@Override
	public int getGeneratedVersion() {
		// a "random" strategy
		return 7;
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
		return UUIDv7.nextIdentifier();
	}
}
