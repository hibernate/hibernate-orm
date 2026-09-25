package org.hibernate.event.spi;

import org.hibernate.service.Service;
import jakarta.annotation.Nonnull;

/**
 * A {@linkplain Service service} which creates new instances of {@link EntityCopyObserver}.
 */
@FunctionalInterface
public interface EntityCopyObserverFactory extends Service {
	@Nonnull EntityCopyObserver createEntityCopyObserver();
}
