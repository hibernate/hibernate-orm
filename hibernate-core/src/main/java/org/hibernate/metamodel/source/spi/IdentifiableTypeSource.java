/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.metamodel.source.spi;

import java.util.Collection;
import java.util.List;

import org.hibernate.metamodel.spi.LocalBindingContext;
import org.hibernate.xml.spi.Origin;

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
	 * Obtain the binding context local to this entity source.
	 *
	 * @return The local binding context
	 */
	public LocalBindingContext getLocalBindingContext();

	/**
	 * Get the hierarchy this belongs to.
	 *
	 * @return The hierarchy this belongs to.
	 */
	public EntityHierarchySource getHierarchy();

	/**
	 * Get the name of this type.
	 * <p/>
	 * Ultimately this is the value used to resolve
	 * {@link org.hibernate.metamodel.reflite.spi.JavaTypeDescriptor}
	 * and {@link org.hibernate.metamodel.spi.domain.Type} references.  It
	 * requires a little bit of decoding on the source side of things to
	 * account for entity-mode, entity-name, class-name and contextual
	 * default package name.
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
