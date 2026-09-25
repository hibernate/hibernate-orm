package org.hibernate.engine.jdbc.dialect.spi;

/**
 * Contract for the source of {@link DialectResolutionInfo}.
 */
@FunctionalInterface
public interface DialectResolutionInfoSource {
	/**
	 * Get the DialectResolutionInfo
	 */
	DialectResolutionInfo getDialectResolutionInfo();
}
