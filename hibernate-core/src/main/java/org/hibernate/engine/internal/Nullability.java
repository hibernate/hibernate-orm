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
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.tuple.GenerationTiming;
import org.hibernate.tuple.InMemoryValueGenerationStrategy;
import org.hibernate.type.CollectionType;
import org.hibernate.type.CompositeType;
import org.hibernate.type.Type;

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
	 * Check nullability of the class persister properties
	 *
	 * @param values entity properties
	 * @param persister class persister
	 * @param isUpdate whether it is intended to be updated or saved
	 *
	 * @throws PropertyValueException Break the nullability of one property
	 * @throws HibernateException error while getting Component values
	 */
	public void checkNullability(
			final Object[] values,
			final EntityPersister persister,
			final boolean isUpdate) {
		checkNullability( values, persister, isUpdate ? NullabilityCheckType.UPDATE : NullabilityCheckType.CREATE );
	}

	public enum NullabilityCheckType {
		CREATE,
		UPDATE,
		DELETE
	}

	public void checkNullability(
			final Object[] values,
			final EntityPersister persister,
			final NullabilityCheckType checkType) {

		/*
		 * Typically when Bean Validation is on, we don't want to validate null values
		 * at the Hibernate Core level. Hence the checkNullability setting.
		 */
		if ( checkNullability ) {
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

			final boolean[] nullability = persister.getPropertyNullability();
			final boolean[] checkability = checkType == NullabilityCheckType.CREATE
					? persister.getPropertyInsertability()
					: persister.getPropertyUpdateability();
			final Type[] propertyTypes = persister.getPropertyTypes();
			final InMemoryValueGenerationStrategy[] inMemoryValueGenerationStrategies =
					persister.getEntityMetamodel().getInMemoryValueGenerationStrategies();

			for ( int i = 0; i < values.length; i++ ) {

				if ( checkability[i] &&
						values[i] != LazyPropertyInitializer.UNFETCHED_PROPERTY &&
						GenerationTiming.NEVER == inMemoryValueGenerationStrategies[i].getGenerationTiming() ) {
					final Object value = values[i];
					if ( !nullability[i] && value == null ) {
						//check basic level one nullablilty
						throw new PropertyValueException(
								"not-null property references a null or transient value",
								persister.getEntityName(),
								persister.getPropertyNames()[i]
							);

					}
					else if ( value != null ) {
						//values is not null and is checkable, we'll look deeper
						final String breakProperties = checkSubElementsNullability( propertyTypes[i], value );
						if ( breakProperties != null ) {
							throw new PropertyValueException(
								"not-null property references a null or transient value",
								persister.getEntityName(),
								buildPropertyPath( persister.getPropertyNames()[i], breakProperties )
							);
						}

					}
				}

			}
		}
	}

	/**
	 * check sub elements-nullability. Returns property path that break
	 * nullability or null if none
	 *
	 * @param propertyType type to check
	 * @param value value to check
	 *
	 * @return property path
	 * @throws HibernateException error while getting subcomponent values
	 */
	private String checkSubElementsNullability(Type propertyType, Object value) throws HibernateException {
		if ( propertyType.isComponentType() ) {
			return checkComponentNullability( value, (CompositeType) propertyType );
		}

		if ( propertyType.isCollectionType() ) {
			// persistent collections may have components
			final CollectionType collectionType = (CollectionType) propertyType;
			final Type collectionElementType = collectionType.getElementType( session.getFactory() );

			if ( collectionElementType.isComponentType() ) {
				// check for all components values in the collection
				final CompositeType componentType = (CompositeType) collectionElementType;
				final Iterator itr = CascadingActions.getLoadedElementsIterator( session, collectionType, value );
				while ( itr.hasNext() ) {
					final Object compositeElement = itr.next();
					if ( compositeElement != null ) {
						return checkComponentNullability( compositeElement, componentType );
					}
				}
			}
		}

		return null;
	}

	/**
	 * check component nullability. Returns property path that break
	 * nullability or null if none
	 *
	 * @param value component properties
	 * @param compositeType component not-nullable type
	 *
	 * @return property path
	 * @throws HibernateException error while getting subcomponent values
	 */
	private String checkComponentNullability(Object value, CompositeType compositeType) throws HibernateException {
		// IMPL NOTE : we currently skip checking "any" and "many to any" mappings.
		//
		// This is not the best solution.  But atm there is a mismatch between AnyType#getPropertyNullability
		// and the fact that cascaded-saves for "many to any" mappings are not performed until after this nullability
		// check.  So the nullability check fails for transient entity elements with generated identifiers because
		// the identifier is not yet generated/assigned (is null)
		//
		// The more correct fix would be to cascade saves of the many-to-any elements before the Nullability checking

		if ( compositeType.isAnyType() ) {
			return null;
		}

		final boolean[] nullability = compositeType.getPropertyNullability();
		if ( nullability != null ) {
			//do the test
			final Object[] subValues = compositeType.getPropertyValues( value, session );
			final Type[] propertyTypes = compositeType.getSubtypes();
			for ( int i = 0; i < subValues.length; i++ ) {
				final Object subValue = subValues[i];
				if ( !nullability[i] && subValue==null ) {
					return compositeType.getPropertyNames()[i];
				}
				else if ( subValue != null ) {
					final String breakProperties = checkSubElementsNullability( propertyTypes[i], subValue );
					if ( breakProperties != null ) {
						return buildPropertyPath( compositeType.getPropertyNames()[i], breakProperties );
					}
				}
			}
		}
		return null;
	}

	/**
	 * Return a well formed property path. Basically, it will return parent.child
	 *
	 * @param parent parent in path
	 * @param child child in path
	 *
	 * @return parent-child path
	 */
	private static String buildPropertyPath(String parent, String child) {
		return parent + '.' + child;
	}

}
