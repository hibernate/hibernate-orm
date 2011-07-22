/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.source.binder;

/**
 * Further contract for sources of {@code *-to-one} style associations.
 *
 * @author Steve Ebersole
 */
public interface ToOneAttributeSource extends SingularAttributeSource, AssociationAttributeSource {
	/**
	 * Obtain the name of the referenced entity.
	 *
	 * @return The name of the referenced entity
	 */
	public String getReferencedEntityName();

	/**
	 * Obtain the name of the referenced attribute.  Typically the reference is built based on the identifier
	 * attribute of the {@link #getReferencedEntityName() referenced entity}, but this value allows using a different
	 * attribute instead.
	 *
	 * @return The name of the referenced attribute; {@code null} indicates the identifier attribute.
	 */
	public String getReferencedEntityAttributeName();
}
