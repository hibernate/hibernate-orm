package org.hibernate.service.internal;

import jakarta.annotation.Nonnull;

/**
 * A service provided as-is.
 *
 * @author Steve Ebersole
 */
public record ProvidedService<R>(@Nonnull Class<R> serviceRole, @Nonnull R service) {
}
