package org.hibernate.engine.jdbc.batch.spi;

/**
 * Unique key for batch identification.
 *
 * @author Steve Ebersole
 */
public interface BatchKey {
	default String toLoggableString() {
		return toString();
	}
}
