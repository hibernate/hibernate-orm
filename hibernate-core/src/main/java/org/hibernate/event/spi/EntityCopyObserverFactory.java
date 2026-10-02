package org.hibernate.event.spi;

import org.hibernate.service.Service;

/**
 * A {@linkplain Service service} which creates new instances of {@link EntityCopyObserver}.
 */
@FunctionalInterface
public interface EntityCopyObserverFactory extends Service {
	EntityCopyObserver createEntityCopyObserver();
}
