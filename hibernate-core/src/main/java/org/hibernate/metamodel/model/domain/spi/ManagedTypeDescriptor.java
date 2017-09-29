/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javax.persistence.metamodel.ManagedType;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.type.descriptor.java.spi.ManagedJavaDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Hibernate extension SPI for working with {@link ManagedType} implementations.  All
 * "concrete ManagedType" implementations (entity and embedded) are modelled as a
 * "persister" (see {@link EntityDescriptor} and
 * {@link EmbeddedTypeDescriptor}
 *
 * NOTE : Hibernate additionally classifies plural attributes via a "persister" :
 * {@link PersistentCollectionDescriptor}.
 *
 * @todo (6.0) : describe what is available after each initialization phase (and therefore what is "undefined" in terms of access earlier).
 *
 * @author Steve Ebersole
 */
public interface ManagedTypeDescriptor<T>
		extends ManagedType<T>, NavigableContainer<T>, EmbeddedContainer<T>, ExpressableType<T> {

	TypeConfiguration getTypeConfiguration();

	ManagedJavaDescriptor<T> getJavaTypeDescriptor();

	RepresentationStrategy getRepresentationStrategy();

	PersistentAttribute<? super T, ?> findAttribute(String name);
	PersistentAttribute<? super T, ?> findDeclaredAttribute(String name);
	PersistentAttribute<? super T, ?> findDeclaredAttribute(String name, Class resultType);

	default List<PersistentAttribute<? super T,?>> getPersistentAttributes() {
		final List<PersistentAttribute<? super T,?>> attributes = new ArrayList<>();
		collectAttributes( attributes::add, PersistentAttribute.class );
		return attributes;
	}

	default List<PersistentAttribute<? super T, ?>> getDeclaredPersistentAttributes() {
		final List<PersistentAttribute<? super T,?>> attributes = new ArrayList<>();
		collectDeclaredAttributes( attributes::add, PersistentAttribute.class );
		return attributes;
	}

	Map<String, PersistentAttribute> getAttributesByName();
	Map<String, PersistentAttribute> getDeclaredAttributesByName();

	default void visitAttributes(Consumer<? extends PersistentAttribute> consumer) {
		throw new NotYetImplementedFor6Exception();
	}

	<A extends javax.persistence.metamodel.Attribute> void collectAttributes(Consumer<A> collector, Class<A> restrictionType);
	<A extends javax.persistence.metamodel.Attribute> void collectDeclaredAttributes(Consumer<A> collector, Class<A> restrictionType);

	/**
	 * Reduce an instance of the described type into an array of it's
	 * sub-Navigable state
	 *
	 * @apiNote The returned array is of length equal to the number of
	 * sub-Navigables.  Each element in that array represents the
	 * corresponding sub-Navigable's reduced state (see {@link Navigable#reduce}).
	 */
	default Object[] reduceToValuesArray(T instance, SharedSessionContractImplementor session) {
		return reduceToValuesArray(
				instance,
				o -> true,
				o -> false,
				null,
				session
		);
	}

	/**
	 * Reduce an instance of the described entity into its "values array" - whose
	 * length is equal to the number of attributes where the `includeCondition`
	 * tests {@code true}.  Each element corresponds to either:
	 *
	 * 		* if the passed `swapCondition` tests {@code true}, then
	 * 			the value passed as `swapValue`
	 * 		* otherwise the attribute's extracted - (see {@link PersistentAttribute#reduce})
	 *
	 * In more specific terms, this method is responsible for extracting the domain
	 * object's value state array - which is the form we use in many places such
	 * EntityEntry#loadedState, L2 cache entry, etc.
	 *
	 * @param instance An instance of the described type (this)
	 * @param includeCondition Predicate to see if the given sub-Navigable should create
	 * 		an index in the array being built.
	 * @param swapCondition Predicate to see if the sub-Navigable's reduced state or
	 * 		the passed `swapValue` should be used for that sub-Navigable's value as its
	 *		element in the array being built
	 * @param swapValue The value to use if the passed `swapCondition` tests {@code true}
	 * @param session The session :)
	 */
	Object[] reduceToValuesArray(
			T instance,
			Predicate includeCondition,
			Predicate swapCondition,
			Object swapValue,
			SharedSessionContractImplementor session);
}
