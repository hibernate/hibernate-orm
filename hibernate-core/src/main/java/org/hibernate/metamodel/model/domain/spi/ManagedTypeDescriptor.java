/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import javax.persistence.metamodel.ManagedType;

import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.type.descriptor.java.spi.ManagedJavaDescriptor;

/**
 * Hibernate extension SPI for working with {@link ManagedType} implementations.  All
 * "concrete ManagedType" implementations (entity and embedded) are modelled as a
 * "persister" (see {@link EntityDescriptor} and
 * {@link EmbeddedTypeDescriptor}
 *
 * NOTE : Hibernate additionally classifies plural attributes via a "persister" :
 * {@link PersistentCollectionDescriptor}.
 *
 * @todo describe what is available after each initialization phase (and therefore what is "undefined" in terms of access earlier).
 *
 * @author Steve Ebersole
 */
public interface ManagedTypeDescriptor<T>
		extends ManagedType<T>, NavigableContainer<T>, EmbeddedContainer<T>, ExpressableType<T> {

	ManagedJavaDescriptor<T> getJavaTypeDescriptor();

	Instantiator getInstantiator();

	PersistentAttribute<? super T, ?> findAttribute(String name);
	PersistentAttribute<? super T, ?> findDeclaredAttribute(String name);
	PersistentAttribute<? super T, ?> findDeclaredAttribute(String name, Class resultType);

	default Set<PersistentAttribute<? super T,?>> getPersistentAttributes() {
		final HashSet<PersistentAttribute<? super T,?>> attributes = new HashSet<>();
		collectAttributes( attributes::add, PersistentAttribute.class );
		return attributes;
	}

	default Set<PersistentAttribute<? super T, ?>> getDeclaredPersistentAttributes() {
		final HashSet<PersistentAttribute<? super T,?>> attributes = new HashSet<>();
		collectDeclaredAttributes( attributes::add, PersistentAttribute.class );
		return attributes;
	}

	Map<String, PersistentAttribute> getAttributesByName();
	Map<String, PersistentAttribute> getDeclaredAttributesByName();

	<A extends javax.persistence.metamodel.Attribute> void collectAttributes(Consumer<A> collector, Class<A> restrictionType);
	<A extends javax.persistence.metamodel.Attribute> void collectDeclaredAttributes(Consumer<A> collector, Class<A> restrictionType);
}
