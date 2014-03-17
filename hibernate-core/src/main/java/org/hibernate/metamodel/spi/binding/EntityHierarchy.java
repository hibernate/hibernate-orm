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
package org.hibernate.metamodel.spi.binding;

import org.hibernate.EntityMode;
import org.hibernate.engine.OptimisticLockStyle;

/**
 * Represents an Entity inheritance hierarchy in the binding model.
 *
 * @author Steve Ebersole
 */
public interface EntityHierarchy {
	public EntityBinding getRootEntityBinding();

	public InheritanceType getInheritanceType();

	public EntityMode getEntityMode();

	public EntityIdentifier getEntityIdentifier();

	public EntityDiscriminator getEntityDiscriminator();

	public EntityVersion getEntityVersion();

	public OptimisticLockStyle getOptimisticLockStyle();

	public TenantDiscrimination getTenantDiscrimination();

	public Caching getCaching();

	public Caching getNaturalIdCaching();

	public boolean isMutable();

	public boolean isExplicitPolymorphism();

	/**
	 * Obtain the specified extra where condition to be applied to this entity.
	 *
	 * @return The extra where condition
	 */
	public String getWhere();
}
