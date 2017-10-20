/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.internal;

import java.util.Iterator;
import java.util.List;
import javax.persistence.metamodel.Attribute.PersistentAttributeType;

import org.hibernate.HibernateException;
import org.hibernate.PropertyValueException;
import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.hibernate.engine.spi.CascadingActions;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.domain.internal.SingularPersistentAttributeEmbedded;
import org.hibernate.metamodel.model.domain.spi.CollectionElement;
import org.hibernate.metamodel.model.domain.spi.CollectionElementEmbedded;
import org.hibernate.metamodel.model.domain.spi.EmbeddedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.metamodel.model.domain.spi.PersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.metamodel.model.domain.spi.PluralAttributeCollection;
import org.hibernate.type.descriptor.java.internal.AnyTypeJavaDescriptor;

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
			final EntityDescriptor entityDescriptor,
			final boolean isUpdate) throws HibernateException {
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
			  * Look at Collections contraining component to
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

			final boolean[] nullability = entityDescriptor.getPropertyNullability();
			final boolean[] checkability = isUpdate ?
				entityDescriptor.getPropertyUpdateability() :
				entityDescriptor.getPropertyInsertability();
			final List<PersistentAttribute> persistentAttributes = entityDescriptor.getPersistentAttributes();

			for ( int i = 0; i < values.length; i++ ) {

				if ( checkability[i] && values[i]!= LazyPropertyInitializer.UNFETCHED_PROPERTY ) {
					final Object value = values[i];
					if ( !nullability[i] && value == null ) {

						//check basic level one nullablilty
						throw new PropertyValueException(
								"not-null property references a null or transient value",
								entityDescriptor.getEntityName(),
								entityDescriptor.getPropertyNames()[i]
							);

					}
					else if ( value != null ) {
						//values is not null and is checkable, we'll look deeper
						final String breakProperties = checkSubElementsNullability(
								persistentAttributes.get( i ),
								value
						);
						if ( breakProperties != null ) {
							throw new PropertyValueException(
								"not-null property references a null or transient value",
								entityDescriptor.getEntityName(),
								buildPropertyPath( entityDescriptor.getPropertyNames()[i], breakProperties )
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
	 * @param attribute the attribute to check
	 * @param value value to check
	 *
	 * @return property path
	 * @throws HibernateException error while getting subcomponent values
	 */
	private String checkSubElementsNullability(PersistentAttribute attribute, Object value) throws HibernateException {
		final PersistentAttributeType persistentAttributeType = attribute.getPersistentAttributeType();
		if ( persistentAttributeType == PersistentAttributeType.EMBEDDED ) {
			return checkComponentNullability(
					value,
					( (SingularPersistentAttributeEmbedded) attribute ).getEmbeddedDescriptor()
			);
		}

		if ( persistentAttributeType == PersistentAttributeType.ELEMENT_COLLECTION ) {
			// persistent collections may have components
			final PersistentCollectionDescriptor collectionDescriptor = ( (PluralAttributeCollection) attribute ).getPersistentCollectionDescriptor();
			final CollectionElement elementDescriptor = collectionDescriptor.getElementDescriptor();

			if ( elementDescriptor.getClassification() == CollectionElement.ElementClassification.EMBEDDABLE ) {
				// check for all components values in the collection
				final Iterator itr = CascadingActions.getLoadedElementsIterator( session, collectionDescriptor, value );
				while ( itr.hasNext() ) {
					final Object compositeElement = itr.next();
					if ( compositeElement != null ) {
						return checkComponentNullability(
								compositeElement,
								( (CollectionElementEmbedded) collectionDescriptor ).getEmbeddedDescriptor()
						);
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
	 * @param embeddedTypeDescriptor component Descriptor
	 *
	 * @return property path
	 * @throws HibernateException error while getting subcomponent values
	 */
	private String checkComponentNullability(Object value, EmbeddedTypeDescriptor embeddedTypeDescriptor) throws HibernateException {
		// IMPL NOTE : we currently skip checking "any" and "many to any" mappings.
		//
		// This is not the best solution.  But atm there is a mismatch between AnyType#getPropertyNullability
		// and the fact that cascaded-saves for "many to any" mappings are not performed until afterQuery this nullability
		// check.  So the nullability check fails for transient entity elements with generated identifiers because
		// the identifier is not yet generated/assigned (is null)
		//
		// The more correct fix would be to cascade saves of the many-to-any elements beforeQuery the Nullability checking

		if ( embeddedTypeDescriptor.getJavaTypeDescriptor() instanceof AnyTypeJavaDescriptor ) {
			return null;
		}

		final boolean[] nullability = embeddedTypeDescriptor.getPropertyNullability();
		if ( nullability != null ) {
			//do the test
			final Object[] propertyValues = embeddedTypeDescriptor.getPropertyValues( value );
			final List<PersistentAttribute> persistentAttributes = embeddedTypeDescriptor.getPersistentAttributes();
			for ( int i = 0; i < propertyValues.length; i++ ) {
				final Object propertyValue = propertyValues[i];
				if ( !nullability[i] && propertyValue == null ) {
					return ( persistentAttributes.get( i ) ).getAttributeName();
				}
				else if ( propertyValue != null ) {
					final PersistentAttribute attribute = persistentAttributes.get( i );
					final String breakProperties = checkSubElementsNullability( attribute, propertyValue );
					if ( breakProperties != null ) {
						return buildPropertyPath( attribute.getAttributeName(), breakProperties );
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
