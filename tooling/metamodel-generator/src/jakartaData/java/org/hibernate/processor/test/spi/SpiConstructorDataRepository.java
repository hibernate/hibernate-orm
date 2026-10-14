package org.hibernate.processor.test.spi;

import jakarta.data.repository.Find;
import jakarta.data.repository.Repository;

/**
 * A Jakarta Data repository for which the extension asks for a constructor taking the session,
 * without constructor injection.
 */
@Repository
@ExtensionMarker
public interface SpiConstructorDataRepository {
	@Find
	SpiBook findById(Long id);
}
