package org.hibernate.orm.test.mapping.identifier.uuid.custom;

import java.util.UUID;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.uuid.UuidValueGenerator;

/**
 * @author Steve Ebersole
 */
public class CustomUuidValueCreator implements UuidValueGenerator {
	@Override
	public UUID generateUuid(SharedSessionContractImplementor session) {
		return UUID.randomUUID();
	}
}
