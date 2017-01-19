/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.common.spi;

import java.util.Map;
import java.util.function.Consumer;
import javax.persistence.metamodel.ManagedType;

import org.hibernate.persister.embedded.spi.EmbeddedContainer;
import org.hibernate.type.descriptor.java.spi.ManagedJavaDescriptor;

/**
 * @author Steve Ebersole
 */
public interface ManagedTypeImplementor<T>
		extends ManagedType<T>, NavigableSource<T>, EmbeddedContainer<T>, ExpressableType<T> {
	ManagedTypeImplementor<? super T> getSuperType();

	ManagedJavaDescriptor<T> getJavaTypeDescriptor();

	@Override
	NavigableSource getSource();

	PersistentAttribute findDeclaredAttribute(String name);
	PersistentAttribute findDeclaredAttribute(String name, Class resultType);

	PersistentAttribute findAttribute(String name);

	Map<String, PersistentAttribute> getDeclaredAttributesByName();

	<A extends javax.persistence.metamodel.Attribute> void collectDeclaredAttributes(Consumer<A> collector, Class<A> restrictionType);

	<A extends javax.persistence.metamodel.Attribute> void collectAttributes(Consumer<A> collector, Class<A> restrictionType);

	Map<String, PersistentAttribute> getAttributesByName();
}
