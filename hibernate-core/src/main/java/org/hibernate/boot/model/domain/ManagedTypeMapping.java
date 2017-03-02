/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.model.domain;

import java.util.List;
import java.util.Set;

import org.hibernate.persister.common.spi.PersistentAttribute;

/**
 * Corollary to what JPA calls a "managed type" at the boot-time metamodel.
 * Essentially a base class describing the commonality between an entity, a
 * mapped-superclass and an embeddable.
 *
 * @param <I> The ManagedTypeMapping sub-type for better return typing (collections)
 *
 * @author Steve Ebersole
 */
public interface ManagedTypeMapping<I> {
	/**
	 * The name of this managed type.  Generally the class name.
	 */
	String getName();

	I getSuperType();
	void setSuperType(I superType);

	/**
	 * @todo (6.0) Should we order these as discussed below for attributes?
	 * 		I'm just not sure there is a clear benefit here, so at the moment
	 * 		I'd lean towards no.
	 */
	Set<I> getSubTypes();

	void addSubType(I subType);

	/**
	 * The ordering here is defined by the alphabetical ordering of the
	 * attributes' names
	 */
	List<PersistentAttributeMapping> getDeclaredPersistentAttributes();

	void addDeclaredPersistentAttribute(PersistentAttribute attribute);
}
