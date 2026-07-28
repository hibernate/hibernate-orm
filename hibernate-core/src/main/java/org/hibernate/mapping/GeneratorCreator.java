package org.hibernate.mapping;

import org.hibernate.Internal;

/**
 * @deprecated Use {@link GeneratorDescriptor}.
 *
 * @since 6.2
 *
 * @author Gavin King
 */
@Internal
@Deprecated(since = "9.0", forRemoval = true)
@FunctionalInterface
public interface GeneratorCreator extends GeneratorDescriptor {
}
