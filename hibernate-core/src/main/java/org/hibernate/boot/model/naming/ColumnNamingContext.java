package org.hibernate.boot.model.naming;

import org.hibernate.Incubating;

import jakarta.annotation.Nullable;

/**
 * Describes the entity which owns a column. Passed to
 * {@link PhysicalNamingStrategy#toPhysicalColumnName(Identifier, org.hibernate.engine.jdbc.env.spi.JdbcEnvironment, ColumnNamingContext)}
 * so that the physical column name can depend on the entity.
 * <p>
 * For a column of a collection table, the owning entity is the entity which owns the collection.
 *
 * @param entityName The name of the owning entity, or {@code null} if it is unknown.
 * @param className The name of the Java class of the owning entity, or {@code null}
 *                  if it is unknown or the entity is a dynamic (map) entity.
 *
 * @since 8.1
 *
 * @author Vasily Petrov
 */
@Incubating(since = "8.1")
public record ColumnNamingContext(@Nullable String entityName, @Nullable String className) {
}
