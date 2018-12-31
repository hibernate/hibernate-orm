/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import javax.persistence.metamodel.CollectionAttribute;
import javax.persistence.metamodel.ListAttribute;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.MapAttribute;
import javax.persistence.metamodel.SetAttribute;
import javax.persistence.metamodel.SingularAttribute;

import org.hibernate.graph.Graph;
import org.hibernate.graph.spi.SubGraphImplementor;
import org.hibernate.metamodel.model.domain.ManagedDomainType;

/**
 * Hibernate extension to the JPA {@link ManagedType} descriptor
 *
 * @author Steve Ebersole
 */
public interface ManagedTypeDescriptor<J> extends SimpleTypeDescriptor<J>, ManagedDomainType<J> {
	/**
	 * Get this ManagedType's super type descriptor.  ATM only supported for the
	 * {@link IdentifiableTypeDescriptor} branch of the ManagedType tree
	 */
	ManagedTypeDescriptor<? super J> getSuperType();

	/**
	 * The Hibernate "type name" ("entity name" - for non-POJO representations)
	 */
	String getName();

	/**
	 * Make an empty sub-graph based on this type.
	 *
	 * @apiNote Note that this is *not* the same as the type's
	 * {@linkplain #getDefaultGraph "default" graph}
	 */
	SubGraphImplementor<J> makeSubGraph();

	/**
	 * The default graph for this type.  Generally this is used to
	 * implement JPA's notion of a "load graph" for undefined sub-graphs.
	 *
	 * @apiNote The return is immutable ({@link Graph#isMutable()} == {@code false}) -
	 * use a {@linkplain Graph#makeCopy mutable copy}
	 */
	default SubGraphImplementor<J> getDefaultGraph() {
		throw new UnsupportedOperationException( "Not yet implemented - " + getClass().getName() );
	}

	/**
	 * Make a sub-graph based on one of this type's sub-types
	 */
	<S extends J> SubGraphImplementor<S> makeSubGraph(Class<S> subType);

	<S extends J> ManagedTypeDescriptor<S> findSubType(String subTypeName);

	<S extends J> ManagedTypeDescriptor<S> findSubType(Class<S> type);

	/**
	 * In-flight access to the managed type.  Used to add attributes, etc.
	 * Valid only during boot.
	 */
	InFlightAccess<J> getInFlightAccess();

	/**
	 * Used during creation of the managed type object to add its attributes
	 */
	interface InFlightAccess<J> {
		void addAttribute(PersistentAttributeDescriptor<J, ?> attribute);

		/**
		 * Called when configuration of the managed-type is complete
		 */
		void finishUp();
	}

	PersistentAttributeDescriptor<J, ?> findDeclaredAttribute(String name);

	PersistentAttributeDescriptor<? super J, ?> findAttribute(String name);

	@Override
	PersistentAttributeDescriptor<J, ?> getDeclaredAttribute(String name);

	@Override
	PersistentAttributeDescriptor<? super J, ?> getAttribute(String name);

	@Override
	<Y> SingularPersistentAttribute<? super J, Y> getSingularAttribute(String name, Class<Y> type);

	@Override
	<Y> SingularPersistentAttribute<J,Y> getDeclaredSingularAttribute(String name, Class<Y> type);

	<C,E> PluralPersistentAttribute<J,C,E> getPluralAttribute(String name);

	@Override
	<E> BagPersistentAttribute<? super J, E> getCollection(String name, Class<E> elementType);

	@Override
	default <E> CollectionAttribute<J, E> getDeclaredCollection(
			String name, Class<E> elementType) {
		return null;
	}

	@Override
	default <E> SetAttribute<? super J, E> getSet(String name, Class<E> elementType) {
		return null;
	}

	@Override
	default <E> SetAttribute<J, E> getDeclaredSet(String name, Class<E> elementType) {
		return null;
	}

	@Override
	default <E> ListAttribute<? super J, E> getList(String name, Class<E> elementType) {
		return null;
	}

	@Override
	default <E> ListAttribute<J, E> getDeclaredList(String name, Class<E> elementType) {
		return null;
	}

	@Override
	default <K, V> MapAttribute<? super J, K, V> getMap(
			String name, Class<K> keyType, Class<V> valueType) {
		return null;
	}

	@Override
	default <K, V> MapAttribute<J, K, V> getDeclaredMap(
			String name, Class<K> keyType, Class<V> valueType) {
		return null;
	}

	@Override
	default SingularAttribute<? super J, ?> getSingularAttribute(String name) {
		return null;
	}

	@Override
	default SingularAttribute<J, ?> getDeclaredSingularAttribute(String name) {
		return null;
	}

	@Override
	default CollectionAttribute<? super J, ?> getCollection(String name) {
		return null;
	}

	@Override
	default CollectionAttribute<J, ?> getDeclaredCollection(String name) {
		return null;
	}

	@Override
	default SetPersistentAttribute<? super J, ?> getSet(String name) {
		return null;
	}

	@Override
	default SetPersistentAttribute<J, ?> getDeclaredSet(String name) {
		return null;
	}

	@Override
	default ListPersistentAttribute<? super J, ?> getList(String name) {
		return null;
	}

	@Override
	default ListPersistentAttribute<J, ?> getDeclaredList(String name) {
		return null;
	}

	@Override
	default MapPersistentAttribute<? super J, ?, ?> getMap(String name) {
		return null;
	}

	@Override
	default MapPersistentAttribute<J, ?, ?> getDeclaredMap(String name) {
		return null;
	}
}
