/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javax.persistence.metamodel.ManagedType;

import org.hibernate.HibernateException;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.boot.model.domain.spi.ManagedTypeMappingImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.type.descriptor.java.spi.ManagedJavaDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Hibernate extension SPI for working with {@link ManagedType} implementations.  All
 * concrete ManagedType implementations (entity and embedded) are modelled as a
 * "descriptor" (see {@link EntityDescriptor} and {@link EmbeddedTypeDescriptor}
 *
 * NOTE : Hibernate additionally classifies plural attributes via a "descriptor" :
 * {@link PersistentCollectionDescriptor}.
 *
 * @todo (6.0) : describe what is available after each initialization phase (and therefore what is "undefined" in terms of access earlier).
 *
 * @author Steve Ebersole
 */
public interface ManagedTypeDescriptor<T>
		extends ManagedType<T>, NavigableContainer<T>, EmbeddedContainer<T>, ExpressableType<T> {

	/**
	 * Opportunity to perform any final tasks as part of initialization of the
	 * runtime model.  At this point...
	 *
	 * todo (6.0) : document the expectations of "at this point"
	 */
	void finishInitialization(
			ManagedTypeMappingImplementor bootDescriptor,
			RuntimeModelCreationContext creationContext);

	TypeConfiguration getTypeConfiguration();

	ManagedJavaDescriptor<T> getJavaTypeDescriptor();

	ManagedTypeRepresentationStrategy getRepresentationStrategy();

	List<StateArrayContributor> getStateArrayContributors();

	/**
	 * Return this managed type's persistent attributes, including those
	 * declared on super types.
	 */
	NonIdPersistentAttribute<? super T, ?> findPersistentAttribute(String name);

	/**
	 * Return this managed type's persistent attributes, excluding those
	 * declared on super types.
	 *
	 * @apiNote See the api-note on {@link #findPersistentAttribute}
	 */
	NonIdPersistentAttribute<? super T, ?> findDeclaredPersistentAttribute(String name);

	@SuppressWarnings("unchecked")
	default <R> NonIdPersistentAttribute<? super T, R> findDeclaredPersistentAttribute(String name, Class<R> resultType) {
		return (NonIdPersistentAttribute<? super T, R>) findDeclaredPersistentAttribute( name );
	}

	List<NonIdPersistentAttribute> getPersistentAttributes();

	List<NonIdPersistentAttribute> getDeclaredPersistentAttributes();

	default void visitAttributes(Consumer<NonIdPersistentAttribute> consumer) {
		for ( NonIdPersistentAttribute attribute : getPersistentAttributes() ) {
			consumer.accept( attribute );
		}
	}

	default void visitStateArrayNavigables(Consumer<StateArrayContributor<?>> consumer) {
		for ( StateArrayContributor contributor : getStateArrayContributors() ) {
			consumer.accept( contributor );
		}
	}


	/**
	 * Reduce an instance of the described entity into its "values array" -
	 * an array whose length is equal to the number of attributes where the
	 * `includeCondition` tests {@code true}.  Each element corresponds to either:
	 *
	 * 		* if the passed `swapCondition` tests {@code true}, then
	 * 			the value passed as `swapValue`
	 * 		* otherwise the attribute's extracted value
	 *
	 * In more specific terms, this method is responsible for extracting the domain
	 * object's value state array - which is the form we use in many places such
	 * EntityEntry#loadedState, L2 cache entry, etc.
	 *
	 * @param instance An instance of the described type (this)
	 * @param includeCondition Predicate to see if the given sub-Navigable should create
	 * an index in the array being built.
	 * @param swapCondition Predicate to see if the sub-Navigable's reduced state or
	 * the passed `swapValue` should be used for that sub-Navigable's value as its
	 * element in the array being built
	 * @param swapValue The value to use if the passed `swapCondition` tests {@code true}
	 * @param session The session :)
	 */
	default Object[] reduceToValuesArray(
			T instance,
			Predicate<NonIdPersistentAttribute> includeCondition,
			Predicate<NonIdPersistentAttribute> swapCondition,
			Object swapValue,
			SharedSessionContractImplementor session) {
		final ArrayList<Object> values = new ArrayList<>();

		// todo (6.0) : the real trick here is deciding which values to put in the array.
		//		specifically how to handle values like version, discriminator, etc
		//
		//		do we put that onus on the `includeCondition` completely (external)?
		//		or is this something that the descriptor should handle? maybe a method
		//
		//		generally speaking callers only care about the `swapCondition` which is
		//		where the "insertability", "laziness", etc comes into play

		// todo (6.0) : one option for this (^^) is to define `includeCondition` and `swapCondition` as `Predicate<Navigable` instead
		//		ManagedTypeDescriptor's implementation of that would walk these Navigables[1]
		//
		// [1] whichever Navigables we decide needs to be there in whatever order we decide.. it just needs to be consistent in usage[2]
		// [2] possibly (hopefully!)this (^^) can hold true for our enhancement needs as well.  A possible solution for would be
		// 		to just "reserve" the first few elements of this array for root-entity state such as id, version, discriminator, etc

		// todo (7.0) : bytecode enhancement should use some facilities to build a `org.hibernate.boot.Metadata` reference to determine its strategy for enhancement.
		//		this is related to the 2 6.0 todo comments above
		//
		//		drawback to this approach is that it would miss any provided XML overrides/additions.  - is that reasonable?
		//		maybe a comprise is to say that we can enhance anything for which there are XML *overrides*, but not additions
		// 		such as adding new entity definitions (the new ones would not be hooked

		visitAttributes(
				attribute -> {
					if ( includeCondition.test( attribute ) ) {
						values.add(
								swapCondition.test( attribute )
										? swapValue
										: attribute.getPropertyAccess().getGetter().get( instance )
						);
					}
				}
		);
		return values.toArray();
	}

	default Object extractAttributeValue(T instance, NonIdPersistentAttribute attribute) {
		return attribute.getPropertyAccess().getGetter().get( instance );
	}

	default void injectAttributeValue(T instance, NonIdPersistentAttribute attribute, Object value) {
		attribute.getPropertyAccess().getSetter().set( instance, value, getTypeConfiguration().getSessionFactory() );
	}

	default boolean hasMutableProperties() {
		throw new NotYetImplementedFor6Exception();
	}


	/**
	 * Set the given values to the mapped properties of the given object
	 */
	default void setPropertyValues(Object object, Object[] values) {
		visitStateArrayNavigables(
				contributor -> {
					if ( PersistentAttribute.class.isInstance( contributor ) ) {
						final Object value = values[ contributor.getStateArrayPosition() ];
						PersistentAttribute.class.cast( contributor ).getPropertyAccess()
								.getSetter()
								.set( object, value, getTypeConfiguration().getSessionFactory() );
					}
				}
		);
	}

	/**
	 * Return the (loaded) values of the mapped properties of the object (not including backrefs)
	 */
	default Object[] getPropertyValues(Object object) {
		throw new NotYetImplementedFor6Exception();
	}


	/**
	 * @deprecated Use the attribute's {@link org.hibernate.property.access.spi.PropertyAccess} instead
	 */
	@Deprecated
	default void setPropertyValue(Object object, int i, Object value) {
		throw new NotYetImplementedFor6Exception();
	}

	/**
	 * @deprecated Use the attribute's {@link org.hibernate.property.access.spi.PropertyAccess} instead
	 */
	@Deprecated
	default Object getPropertyValue(Object object, int i) throws HibernateException {
		throw new NotYetImplementedFor6Exception();
	}

	/**
	 * @deprecated Use the attribute's {@link org.hibernate.property.access.spi.PropertyAccess} instead
	 */
	@Deprecated
	default Object getPropertyValue(Object object, String propertyName) {
		throw new NotYetImplementedFor6Exception();
	}
}
