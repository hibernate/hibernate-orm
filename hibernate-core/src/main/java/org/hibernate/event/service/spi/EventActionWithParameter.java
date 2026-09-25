package org.hibernate.event.service.spi;

import org.hibernate.Incubating;
import jakarta.annotation.Nonnull;

@Incubating(since = "5.4")
@FunctionalInterface
public interface EventActionWithParameter<T, U, X> {

	void applyEventToListener(@Nonnull T eventListener, @Nonnull U action, @Nonnull X param);

}
