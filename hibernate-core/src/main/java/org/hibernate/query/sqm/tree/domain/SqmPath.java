/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import java.util.function.Supplier;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.annotations.Remove;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.metamodel.model.domain.spi.EmbeddedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.EmbeddedValuedNavigable;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityValuedNavigable;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.metamodel.model.domain.spi.PluralValuedNavigable;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.SemanticException;
import org.hibernate.query.sqm.produce.path.spi.SemanticPathPart;
import org.hibernate.query.sqm.produce.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.domain.SqmRestrictedCollectionElementReference;

/**
 * Models a reference to a part of the application's domain model (a Navigable)
 * as part of an SQM tree.
 *
 * This correlates roughly to the JPA Criteria notion of Path, hence the name.
 *
 * todo (6.0) : Better name for this.
 * 		* SqmNavigablePath?
 * 		* Maybe just re-purpose SqmNavigableReference for this purpose?
 * 		* SqmPathExpression?
 * 		* SqmDomainPath
 *
 * todo (6.0) : part of this might be renaming NavigablePath which is also not the best name
 *
 * @author Steve Ebersole
 */
public interface SqmPath extends SqmExpression, SemanticPathPart {
	/**
	 * @deprecated Prefer {@link #getNavigablePath()} as the unique identifier
	 */
	@Deprecated
	@Remove
	String getUniqueIdentifier();

	/**
	 * Returns the NavigablePath.
	 */
	NavigablePath getNavigablePath();

	/**
	 * The Navigable represented by this reference.
	 */
	Navigable<?> getReferencedNavigable();

	/**
	 * Get the left-hand side of this path - may be null, indicating a
	 * root, cross-join or entity-join
	 */
	SqmPath getLhs();

	/**
	 * Retrieve the explicit alias, if one.  May return null
	 */
	String getExplicitAlias();

	void setExplicitAlias(String explicitAlias);

	/**
	 * Treat this path as the given type.  "Cast it" to the target type.
	 *
	 * May throw an exception if the Path is not treatable as the requested type.
	 *
	 * Also recognizes any {@link Navigable} target type and applies it to the
	 * {@link #getReferencedNavigable()}.
	 *
	 * @return The "casted" reference
	 */
	@SuppressWarnings("unchecked")
	default <T> T as(Class<T> targetType) {
		if ( targetType.isInstance( this ) ) {
			return (T) this;
		}

		if ( Navigable.class.isAssignableFrom( targetType ) ) {
			final Navigable<?> referencedNavigable = getReferencedNavigable();

			if ( EntityTypeDescriptor.class.isAssignableFrom( targetType ) ) {
				return (T) ( (EntityValuedNavigable) referencedNavigable ).getEntityDescriptor();
			}

			if ( EmbeddedTypeDescriptor.class.isAssignableFrom( targetType ) ) {
				return (T) ( (EmbeddedValuedNavigable<?>) referencedNavigable ).getEmbeddedDescriptor();
			}

			if ( PersistentCollectionDescriptor.class.isAssignableFrom( targetType ) ) {
				return (T) ( (PluralValuedNavigable) referencedNavigable ).getCollectionDescriptor();
			}

			if ( ManagedDomainType.class.isAssignableFrom( targetType ) ) {
				if ( referencedNavigable instanceof EntityValuedNavigable<?> ) {
					return (T) ( (EntityValuedNavigable) referencedNavigable ).getEntityDescriptor();
				}
				if ( referencedNavigable instanceof EmbeddedValuedNavigable<?> ) {
					return (T) ( (EmbeddedValuedNavigable) referencedNavigable ).getEmbeddedDescriptor();
				}
			}

			if ( targetType.isInstance( referencedNavigable ) ) {
				return (T) referencedNavigable;
			}
		}

		throw new ClassCastException( "Don't know how to treat `" + getClass().getName() + "` as `" + targetType.getName() + '`' );
	}

	default <T> T as(Class<T> targetType, Supplier<RuntimeException> exceptionSupplier) {
		try {
			return as( targetType );
		}
		catch (ClassCastException e) {
			throw exceptionSupplier.get();
		}
	}


	@Override
	default SqmRestrictedCollectionElementReference resolveIndexedAccess(
			SqmExpression selector,
			String currentContextKey,
			boolean isTerminal,
			SqmCreationState creationState) {
		if ( getReferencedNavigable() instanceof PluralValuedNavigable<?> ) {
			throw new NotYetImplementedFor6Exception();
		}

		throw new SemanticException( "Non-plural path [" + currentContextKey + "] cannot be index-accessed" );
	}
}
