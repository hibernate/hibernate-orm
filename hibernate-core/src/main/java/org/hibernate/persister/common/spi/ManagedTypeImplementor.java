/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.common.spi;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import javax.persistence.metamodel.ManagedType;

import org.hibernate.mapping.ManagedTypeMapping;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.persister.embedded.spi.EmbeddedContainer;
import org.hibernate.persister.spi.PersisterCreationContext;
import org.hibernate.type.descriptor.java.spi.ManagedJavaDescriptor;

/**
 * Hibernate extension SPI for working with {@link ManagedType} implementations.  All
 * "concrete ManagedType" implementations (entity and embedded) are modelled as a
 * "persister" (see {@link org.hibernate.persister.entity.spi.EntityPersister} and
 * {@link org.hibernate.persister.embedded.spi.EmbeddedPersister}
 *
 * NOTE : Hibernate additionally classifies plural attributes via a "persister" :
 * {@link org.hibernate.persister.collection.spi.CollectionPersister}.
 *
 * @todo describe what is available after each initialization phase (and therefore what is "undefined" in terms of access earlier).
 *
 * @author Steve Ebersole
 */
public interface ManagedTypeImplementor<T>
		extends ManagedType<T>, NavigableSource<T>, EmbeddedContainer<T>, ExpressableType<T> {
	ManagedTypeImplementor<? super T> getSuperclassType();

	// todo : finishInitialization assumes that PersistentClass can also represent MappedSuperclass and Embeddables which is currently not true
	//		Given that tooling leverages "PersistentClass as entity", and PersistentClass is generally understood to
	//		model an entity I think the best approach here is to:
	//	`		1) Define a new org.hibernate.mapping.ManagedTypeMapping that represents mappings for any "managed type"
	//				in the normal JPA meaning of that term (mapped-superclass, entity, embeddable)
	//			2) Define a new org.hibernate.mapping.EmbeddedTypeMapping extending ManagedTypeMapping
	// 				(org.hibernate.mapping.Composite).  Or should we split EmbeddableTypeMapping and
	//				"EmbeddedMapping"?
	//.			3) Define a new org.hibernate.mapping.IdentifiableTypeMapping extending ManagedTypeMapping
	//			4) Define a new org.hibernate.mapping.MappedSuperclassTypeMapping extending IdentifiableTypeMapping
	//			5) Define a new org.hibernate.mapping.EntityTypeMapping extending IdentifiableTypeMapping
	//			6) Make PersistentClass extend EntityTypeMapping and deprecate
	//			7) Make Composite extend EmbeddedTypeMapping and deprecate
	//			8) Make MapppedSuperclass extend MappedSuperclassTypeMapping and deprecate
	//			9) Re-work the hierarchies here to better fit this new model

	/**
	 * Called after all EntityPersister instance have been created;
	 *
	 * @param superType The entity's super's EntityPersister
	 * @param mappingBinding Should be  the same reference (instance) originally passed to the
	 * 		ctor, but we want to not have to store that reference as instance state -
	 * 		so we pass it in again
	 * @param creationContext Access to the database model, etc
	 *
	 * @todo (6.0) Use ManagedTypeMapping here as super-type rather than PersistentClass
	 */
	void finishInitialization(ManagedTypeImplementor<? super T> superType, ManagedTypeMapping mappingBinding, PersisterCreationContext creationContext);

	/**
	 * Called after {@link #finishInitialization} has been called on all persisters.
	 */
	void postInstantiate();

	ManagedJavaDescriptor<T> getJavaTypeDescriptor();

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
