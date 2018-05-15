/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.internal;

import java.util.Iterator;

import org.hibernate.HibernateException;
import org.hibernate.PropertyValueException;
import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.hibernate.engine.spi.CascadingActions;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.model.domain.spi.CollectionElement;
import org.hibernate.metamodel.model.domain.spi.CollectionElementEmbedded;
import org.hibernate.metamodel.model.domain.spi.EmbeddedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.EmbeddedValuedNavigable;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.ManagedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.metamodel.model.domain.spi.BagPersistentAttribute;

/**
 * Implements the algorithm for validating property values for illegal null values
 * 
 * @author Gavin King
 */
public final class Nullability {
	private final SharedSessionContractImplementor session;
	private final boolean checkNullability;

	/**
	 * Constructs a Nullability
	 *
	 * @param session The session
	 */
	public Nullability(SharedSessionContractImplementor session) {
		this.session = session;
		this.checkNullability = session.getFactory().getSessionFactoryOptions().isCheckNullability();
	}
	/**
	 * Check nullability of the class entityDescriptor properties
	 *
	 * @param values entity properties
	 * @param entityDescriptor class entity descriptor
	 * @param isUpdate whether it is intended to be updated or saved
	 *
	 * @throws PropertyValueException Break the nullability of one property
	 * @throws HibernateException error while getting Component values
	 */
	public void checkNullability(
			final Object[] values,
			final EntityTypeDescriptor entityDescriptor,
			final boolean isUpdate) {
		checkNullability( values, entityDescriptor, isUpdate ? NullabilityCheckType.UPDATE : NullabilityCheckType.CREATE );
	}

	public enum NullabilityCheckType {
		CREATE,
		UPDATE,
		DELETE
	}

	public void checkNullability(
			final Object[] values,
			final EntityTypeDescriptor entityDescriptor,
			final NullabilityCheckType checkType) {
		/*
		 * Typically when Bean Validation is on, we don't want to validate null values
		 * at the Hibernate Core level. Hence the checkNullability setting.
		 */
		if ( !checkNullability ) {
			return;
		}

		final PathCollector collector = new PathCollector();
		checkNullabilityInternal( values, entityDescriptor, checkType, collector );
		if ( collector.hasAny() ) {
			throw new PropertyValueException(
					"not-null property references a null or transient value",
					entityDescriptor.getEntityName(),
					collector.collectedString()
			);
		}
	}

	@SuppressWarnings("WeakerAccess")
	private static class PathCollector {
		final StringBuilder collector = new StringBuilder();
		boolean any;

		public void add(NavigableRole role) {
			if ( any ) {
				collector.append( ", " );
			}
			collector.append( role.getFullPath() );
			any = true;
		}

		public boolean hasAny() {
			return any;
		}

		public String collectedString() {
			return collector.toString();
		}
	}

	private void checkNullabilityInternal(
			final Object[] values,
			final ManagedTypeDescriptor<?> managedTypeDescriptor,
			final NullabilityCheckType checkType,
			PathCollector collector) {

		/*
		  * Algorithm
		  * Check for any level one nullability breaks
		  * Look at non null components to
		  *   recursively check next level of nullability breaks
		  * Look at Collections containing components to
		  *   recursively check next level of nullability breaks
		  *
		  *
		  * In the previous implementation, not-null stuffs where checked
		  * filtering by level one only updateable
		  * or insertable columns. So setting a sub component as update="false"
		  * has no effect on not-null check if the main component had good checkeability
		  * In this implementation, we keep this feature.
		  * However, I never see any documentation mentioning that, but it's for
		  * sure a limitation.
		  */

		managedTypeDescriptor.visitStateArrayContributors(
				contributor -> {
					final boolean check;
					switch ( checkType ) {
						case CREATE: {
							check = contributor.isInsertable();
							break;
						}
						case UPDATE:
						case DELETE: {
							check = contributor.isUpdatable();
							break;
						}
						default: {
							throw new IllegalArgumentException( "Unknown NullabilityCheckType value : " + checkType );
						}
					}

					if ( ! check ) {
						return;
					}

					final Object value = values[contributor.getStateArrayPosition()];

					if ( value == LazyPropertyInitializer.UNFETCHED_PROPERTY ) {
						return;
					}

					// todo (6.0) : Check InMemoryValueGenerationStrategy for value
					//		if the strategy != GenerationTiming.NEVER, we should return null
					//		See HHH-11096

					if ( value == null ) {
						if ( ! contributor.isNullable() ) {
							collector.add( contributor.getNavigableRole() );
						}
					}
					else {
						checkSubElementsNullability( contributor, value, checkType, collector );
					}
				}
		);
	}

	private void checkSubElementsNullability(
			Navigable navigable,
			Object value,
			NullabilityCheckType checkType,
			PathCollector collector) {
		if ( navigable instanceof EmbeddedValuedNavigable ) {
			final EmbeddedTypeDescriptor embeddedDescriptor = ( (EmbeddedValuedNavigable) navigable ).getEmbeddedDescriptor();
			checkNullabilityInternal(
					embeddedDescriptor.getPropertyValues( value ),
					embeddedDescriptor,
					checkType,
					collector
			);
		}
		else if ( navigable instanceof BagPersistentAttribute ) {
			final BagPersistentAttribute collection = (BagPersistentAttribute) navigable;
			final CollectionElement elementDescriptor = collection.getPersistentCollectionDescriptor()
					.getElementDescriptor();
			if ( elementDescriptor instanceof CollectionElementEmbedded ) {
				final Iterator itr = CascadingActions.getLoadedElementsIterator( session, collection.getPersistentCollectionDescriptor(), value );
				while ( itr.hasNext() ) {
					final Object elementValue = itr.next();
					if ( elementValue != null ) {
						final EmbeddedTypeDescriptor embeddedDescriptor = ( (CollectionElementEmbedded) elementDescriptor )
								.getEmbeddedDescriptor();
						checkNullabilityInternal(
								embeddedDescriptor.getPropertyValues( value ),
								embeddedDescriptor,
								checkType,
								collector
						);
					}
				}
			}
		}
	}
}
