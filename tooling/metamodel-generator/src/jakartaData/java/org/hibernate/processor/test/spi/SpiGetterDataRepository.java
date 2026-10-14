package org.hibernate.processor.test.spi;

import jakarta.data.repository.Find;
import jakarta.data.repository.Repository;

/**
 * A Jakarta Data repository for which the extension provides the session as an expression:
 * nothing must be generated to acquire or release a session.
 */
@Repository
@ExtensionMarker
public interface SpiGetterDataRepository {
	@Find
	SpiBook findById(Long id);
}
