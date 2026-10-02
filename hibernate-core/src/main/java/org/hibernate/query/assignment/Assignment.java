package org.hibernate.query.assignment;


import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.metamodel.SingularAttribute;
import org.hibernate.Incubating;
import org.hibernate.query.restriction.Path;
import org.hibernate.query.sqm.tree.spi.update.SqmUpdateStatement;

/**
 * An assignment to a field or property of an entity or embeddable.
 *
 * @param <T> The target entity type of the assignment
 *
 * @since 7.2
 *
 * @author Gavin King
 */
@Incubating(since = "7.2", group = "query-specifications")
public interface Assignment<T> {

	/**
	 * An assigment of the given literal value to the given attribute
	 * of the root entity.
	 */
	@Nonnull
	static <T,X> Assignment<T> set(@Nonnull SingularAttribute<T,X> attribute, @Nullable X value) {
		return new AttributeAssignment<>( attribute, value );
	}

	/**
	 * An assigment of the given literal value to the entity or embeddable
	 * field or property identified by the given path from the root entity.
	 */
	@Nonnull
	static <T,X> Assignment<T> set(@Nonnull Path<T,X> path, @Nullable X value) {
		return new PathAssignment<>( path, value );
	}

	/**
	 * An assigment of the entity or embeddable field or property identified
	 * by the given path from the root entity to the given attribute of the
	 * root entity.
	 */
	@Nonnull
	static <T,X> Assignment<T> set(@Nonnull SingularAttribute<T,X> attribute, @Nonnull Path<T,X> value) {
		return new PathToAttributeAssignment<>( attribute, value );
	}

	/**
	 * An assigment of one entity or embeddable field or property to another
	 * entity or embeddable field or property, each identified by a given path
	 * from the root entity.
	 */
	@Nonnull
	static <T,X> Assignment<T> set(@Nonnull Path<T,X> path, @Nonnull Path<T,X> value) {
		return new PathToPathAssignment<>( path, value );
	}

	void apply(@Nonnull SqmUpdateStatement<? extends T> update);
}
