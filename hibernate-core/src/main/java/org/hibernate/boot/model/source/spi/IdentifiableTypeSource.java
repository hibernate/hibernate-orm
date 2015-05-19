/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.spi;

import java.util.Collection;
import java.util.List;

import org.hibernate.boot.jaxb.Origin;

/**
 * Common contract between Entity and MappedSuperclass sources.  The
 * terminology is taken from JPA's {@link javax.persistence.metamodel.IdentifiableType}
 *
 * @author Steve Ebersole
 */
public interface IdentifiableTypeSource extends AttributeSourceContainer {
	/**
	 * Obtain the origin of this source.
	 *
	 * @return The origin of this source.
	 */
	public Origin getOrigin();

	/**
	 * Get the hierarchy this belongs to.
	 *
	 * @return The hierarchy this belongs to.
	 */
	public EntityHierarchySource getHierarchy();

	/**
	 * Obtain the metadata-building context local to this entity source.
	 *
	 * @return The local binding context
	 */
	public LocalMetadataBuildingContext getLocalMetadataBuildingContext();

	/**
	 * Get the name of this type.
	 *
	 * @return The name of this type.
	 */
	public String getTypeName();

	public IdentifiableTypeSource getSuperType();

	/**
	 * Access the subtype sources for types extending from this type source,
	 *
	 * @return Sub-type sources
	 */
	public Collection<IdentifiableTypeSource> getSubTypes();

	/**
	 * Access to the sources describing JPA lifecycle callbacks.
	 *
	 * @return JPA lifecycle callback sources
	 */
	public List<JpaCallbackSource> getJpaCallbackClasses();
}

