package org.hibernate.query.sqm;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.query.SemanticException;
import org.hibernate.query.hql.spi.HqlTranslator;

/**
 * Indicates a failure to resolve an entity name in HQL to a known mapped
 * entity type.
 *
 * @apiNote The JPA criteria API requires that this problem be reported
 *          as an {@link IllegalArgumentException}, and so we usually
 *          throw {@link EntityTypeException} from the SQM objects, and
 *          then wrap as an instance of this exception type in the
 *          {@link HqlTranslator}.
 *
 * @author Steve Ebersole
 *
 * @see EntityTypeException
 */
public class UnknownEntityException extends SemanticException {
	@Nonnull
	private final String entityName;

	public UnknownEntityException(@Nonnull String entityName) {
		this( "Could not resolve entity '" + entityName + "'", entityName );
	}

	public UnknownEntityException(@Nonnull String message, @Nonnull String entityName) {
		super( message );
		this.entityName = entityName;
	}

	public UnknownEntityException(@Nonnull String message, @Nonnull String entityName, @Nullable Exception cause) {
		super( message, cause );
		this.entityName = entityName;
	}

	@Nonnull
	public String getEntityName() {
		return entityName;
	}
}
