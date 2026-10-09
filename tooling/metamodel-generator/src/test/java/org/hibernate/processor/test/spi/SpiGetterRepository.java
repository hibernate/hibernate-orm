package org.hibernate.processor.test.spi;

import org.hibernate.annotations.processing.Find;

/**
 * A repository for which the extension declares a session getter expression instead of an injected session.
 */
@ExtensionMarker
public interface SpiGetterRepository {
	@Find
	SpiBook findById(Long id);
}
