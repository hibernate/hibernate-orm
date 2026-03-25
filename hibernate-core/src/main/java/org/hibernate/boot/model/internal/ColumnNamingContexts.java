package org.hibernate.boot.model.internal;

import org.hibernate.boot.model.naming.ColumnNamingContext;
import org.hibernate.boot.model.naming.EntityNaming;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.PersistentClass;

import jakarta.annotation.Nullable;

/**
 * Factory methods for the {@link ColumnNamingContext} passed to
 * {@link org.hibernate.boot.model.naming.PhysicalNamingStrategy#toPhysicalColumnName(
 * org.hibernate.boot.model.naming.Identifier,
 * org.hibernate.engine.jdbc.env.spi.JdbcEnvironment, ColumnNamingContext)}.
 *
 * @since 8.1
 *
 * @author Vasily Petrov
 */
public final class ColumnNamingContexts {

	/**
	 * The context of a column whose owning entity is unknown.
	 */
	public static final ColumnNamingContext UNKNOWN = new ColumnNamingContext( null, null );

	private ColumnNamingContexts() {
	}

	public static ColumnNamingContext forEntity(PersistentClass entity) {
		return new ColumnNamingContext( entity.getEntityName(), entity.getClassName() );
	}

	public static ColumnNamingContext forEntity(EntityNaming entity) {
		return new ColumnNamingContext( entity.getEntityName(), entity.getClassName() );
	}

	/**
	 * The context of a column of a collection, that is, of the entity which owns the collection.
	 */
	public static ColumnNamingContext forCollection(Collection collection) {
		return new ColumnNamingContext( collection.getOwnerEntityName(), collection.getOwner().getClassName() );
	}

	/**
	 * The context of a column of an entity or a collection mapping.
	 */
	public static ColumnNamingContext forModel(Object model) {
		if ( model instanceof PersistentClass entity ) {
			return forEntity( entity );
		}
		else if ( model instanceof Collection collection ) {
			return forCollection( collection );
		}
		else {
			return UNKNOWN;
		}
	}

	/**
	 * The context of a column of an entity, an embeddable, or a collection,
	 * that is, of the entity which ultimately owns the property holder.
	 */
	public static ColumnNamingContext forPropertyHolder(@Nullable PropertyHolder propertyHolder) {
		final var entity = propertyHolder == null ? null : propertyHolder.getPersistentClass();
		return entity == null ? UNKNOWN : forEntity( entity );
	}

	public static ColumnNamingContext forColumns(@Nullable AnnotatedColumns columns) {
		return forPropertyHolder( columns == null ? null : columns.getPropertyHolder() );
	}
}
