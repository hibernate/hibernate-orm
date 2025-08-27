/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.internal;

import java.util.Iterator;

import org.hibernate.HibernateException;
import org.hibernate.PropertyValueException;
import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.generator.Generator;
import org.hibernate.type.AnyType;
import org.hibernate.type.CollectionType;
import org.hibernate.type.ComponentType;
import org.hibernate.type.CompositeType;
import org.hibernate.type.Type;

import static org.hibernate.engine.spi.CascadingActions.getLoadedElementsIterator;
import static org.hibernate.internal.util.StringHelper.qualify;

/**
 * Implements the algorithm for validating property values for illegal null values.
 * <p>
 * For example, a field or property does not accept null values if it is mapped
 * {@link jakarta.persistence.Basic#optional @Basic(optional=false)}.
 *
 * @author Gavin King
 */
public final class Nullability {
	private final SharedSessionContractImplementor session;
	private final boolean checkNullability;
	private NullabilityCheckType checkType;

	public enum NullabilityCheckType {
		CREATE,
		UPDATE,
		DELETE
	}

	public Nullability(SharedSessionContractImplementor session, NullabilityCheckType checkType) {
		this.session = session;
		this.checkNullability = session.getFactory().getSessionFactoryOptions().isCheckNullability();
		this.checkType = checkType;
	}

	@Deprecated(forRemoval = true, since = "7")
	public Nullability(SharedSessionContractImplementor session) {
		this.session = session;
		this.checkNullability = session.getFactory().getSessionFactoryOptions().isCheckNullability();
	}

	/**
	 * Check nullability of the entity properties
	 *
	 * @param values entity properties
	 * @param persister class persister
	 * @param isUpdate whether it is intended to be updated or saved
	 *
	 * @throws PropertyValueException Break the nullability of one property
	 * @throws HibernateException error while getting Component values
	 *
	 * @deprecated Use {@link #checkNullability(Object[], EntityPersister)}
	 */
	@Deprecated(forRemoval = true, since = "7")
	public void checkNullability(
			final Object[] values,
			final EntityPersister persister,
			final boolean isUpdate) {
		checkType =  isUpdate ? NullabilityCheckType.UPDATE : NullabilityCheckType.CREATE;
		checkNullability( values, persister );
	}

	/**
	 * Check nullability of the entity properties
	 *
	 * @param values entity properties
	 * @param persister class persister
	 *
	 * @throws PropertyValueException Break the nullability of one property
	 * @throws HibernateException error while getting Component values
	 */
	public void checkNullability(final Object[] values, final EntityPersister persister) {

		// Typically, when Bean Validation is present, we don't validate
		// not-null values here. Hence, the checkNullability setting.
		if ( checkNullability ) {
			// Algorithm:
			// Check for any level one nullability breaks
			// Look at non-null components to
			//   recursively check next level of nullability breaks
			// Look at Collections containing components to
			//   recursively check next level of nullability breaks
			//
			// In the previous implementation, not-null stuff was checked
			// filtering by level one only updatable or insertable columns.
			// So setting a subcomponent as update="false" has no effect on
			// not-null check if the main component had good checkability
			// In this implementation, we keep this feature.
			// However, I never see any documentation mentioning that, but
			// it's for sure a limitation.

			final boolean[] nullability = persister.getPropertyNullability();
			final boolean[] checkability = getCheckability( persister );
			final Type[] propertyTypes = persister.getPropertyTypes();
			final Generator[] generators = persister.getEntityMetamodel().getGenerators();
			for ( int i = 0; i < values.length; i++ ) {
				if ( checkability[i]
						&& !unfetched( values[i] )
						&& !generated( generators[i] ) ) {
					final Object value = values[i];
					if ( value == null ) {
						if ( !nullability[i] ) {
							// check basic level-one nullability
							throw new PropertyValueException(
									"not-null property references a null or transient value",
									persister.getEntityName(),
									persister.getPropertyNames()[i]
							);
						}
					}
					else {
						// values is not null and is checkable, we'll look deeper
						final String breakProperties = checkSubElementsNullability( propertyTypes[i], value );
						if ( breakProperties != null ) {
							throw new PropertyValueException(
									"not-null property references a null or transient value",
									persister.getEntityName(),
									qualify( persister.getPropertyNames()[i], breakProperties )
							);
						}
					}
				}
			}
		}
	}

	private boolean[] getCheckability(EntityPersister persister) {
		return checkType == NullabilityCheckType.CREATE
				? persister.getPropertyInsertability()
				: persister.getPropertyUpdateability();
	}

	private static boolean unfetched(Object value) {
		return value == LazyPropertyInitializer.UNFETCHED_PROPERTY;
	}

	private static boolean generated(Generator generator) {
		return generator != null && generator.generatesSometimes();
	}

	/**
	 * Check nullability of sub-elements.
	 * Returns property path that break nullability, or null if none.
	 *
	 * @param propertyType type to check
	 * @param value value to check
	 *
	 * @return property path
	 * @throws HibernateException error while getting subcomponent values
	 */
	private String checkSubElementsNullability(Type propertyType, Object value) {
		if ( propertyType instanceof AnyType anyType ) {
			return checkComponentNullability( value, anyType );
		}
		else if ( propertyType instanceof ComponentType componentType ) {
			return checkComponentNullability( value, componentType );
		}
		else if ( propertyType instanceof CollectionType collectionType ) {
			// persistent collections may have components
			if ( collectionType.getElementType( session.getFactory() ) instanceof CompositeType componentType ) {
				// check for all component's values in the collection
				final Iterator<?> iterator = getLoadedElementsIterator( collectionType, value );
				while ( iterator.hasNext() ) {
					final Object compositeElement = iterator.next();
					if ( compositeElement != null ) {
						final String path = checkComponentNullability( compositeElement, componentType );
						if ( path != null ) {
							return path;
						}
					}
				}
			}
			return null;
		}
		else {
			return null;
		}
	}

	/**
	 * Check component nullability.
	 * Returns property path that breaks nullability, or null if none.
	 *
	 * @param composite component properties
	 * @param compositeType component not-nullable type
	 *
	 * @return property path
	 * @throws HibernateException error while getting subcomponent values
	 */
	private String checkComponentNullability(Object composite, CompositeType compositeType) {
		// IMPL NOTE: we currently skip checking "any" and "many-to-any" mappings.
		//
		// This is not the best solution. But there's a mismatch between AnyType.getPropertyNullability()
		// and the fact that cascaded-saves for "many-to-any" mappings are not performed until after this
		// nullability check. So the nullability check fails for transient entity elements with generated
		// identifiers because the identifier is not yet generated/assigned (is null).
		//
		// The fix would be to cascade saves of the many-to-any elements before Nullability checking.

		if ( compositeType instanceof AnyType ) {
			return null;
		}
		else {
			final boolean[] nullability = compositeType.getPropertyNullability();
			if ( nullability != null ) {
				// do the test
				final Object[] values = compositeType.getPropertyValues( composite, session );
				final Type[] propertyTypes = compositeType.getSubtypes();
				final String[] propertyNames = compositeType.getPropertyNames();
				for ( int i = 0; i < values.length; i++ ) {
					final Object value = values[i];
					if ( value == null ) {
						if ( !nullability[i] ) {
							return propertyNames[i];
						}
					}
					else {
						final String breakProperties = checkSubElementsNullability( propertyTypes[i], value );
						if ( breakProperties != null ) {
							return qualify( propertyNames[i], breakProperties );
						}
					}
				}
			}
			return null;
		}
	}

}
