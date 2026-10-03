package org.hibernate.processor.test.spi;

/**
 * A repository for which the extension declares a session getter expression instead of an injected session.
 */
@ExtensionMarker
public interface SpiGetterRepository {
}
