package org.hibernate.service.instrumentation.spi;

import org.hibernate.service.Service;

/**
 * Service caching instrumentation information
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public interface InstrumentationService extends Service {
	/**
	 * Is entityType instrumented
	 */
	public boolean isInstrumented(Class<?> entityType);

	/**
	 * Is entity instrumented
	 */
	public boolean isInstrumented(Object entity);
}
