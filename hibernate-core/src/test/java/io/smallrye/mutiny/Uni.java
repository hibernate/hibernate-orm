package io.smallrye.mutiny;

/**
 * Minimal stand-in for Mutiny's {@code Uni}, which is not on the test classpath.
 * Hibernate recognizes it by name when binding the static queries of reactive repositories.
 */
public interface Uni<T> {
}
