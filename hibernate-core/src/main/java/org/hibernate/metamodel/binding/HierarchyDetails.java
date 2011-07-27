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
package org.hibernate.metamodel.binding;

import org.hibernate.EntityMode;
import org.hibernate.engine.OptimisticLockStyle;

/**
 * @author Steve Ebersole
 */
public class HierarchyDetails {
	private final EntityBinding rootEntityBinding;
	private final InheritanceType inheritanceType;
	private final EntityMode entityMode;

	private final EntityIdentifier entityIdentifier;

	private EntityDiscriminator entityDiscriminator;

	private OptimisticLockStyle optimisticLockStyle;
	private BasicAttributeBinding versioningAttributeBinding;

	private Caching caching;

	private boolean explicitPolymorphism;

	public HierarchyDetails(EntityBinding rootEntityBinding, InheritanceType inheritanceType, EntityMode entityMode) {
		this.rootEntityBinding = rootEntityBinding;
		this.inheritanceType = inheritanceType;
		this.entityMode = entityMode;
		this.entityIdentifier = new EntityIdentifier( rootEntityBinding );
	}

	public EntityBinding getRootEntityBinding() {
		return rootEntityBinding;
	}

	public InheritanceType getInheritanceType() {
		return inheritanceType;
	}

	public EntityMode getEntityMode() {
		return entityMode;
	}

	public EntityIdentifier getEntityIdentifier() {
		return entityIdentifier;
	}

	public EntityDiscriminator getEntityDiscriminator() {
		return entityDiscriminator;
	}

	public OptimisticLockStyle getOptimisticLockStyle() {
		return optimisticLockStyle;
	}

	public void setOptimisticLockStyle(OptimisticLockStyle optimisticLockStyle) {
		this.optimisticLockStyle = optimisticLockStyle;
	}

	public void setEntityDiscriminator(EntityDiscriminator entityDiscriminator) {
		this.entityDiscriminator = entityDiscriminator;
	}

	public BasicAttributeBinding getVersioningAttributeBinding() {
		return versioningAttributeBinding;
	}

	public void setVersioningAttributeBinding(BasicAttributeBinding versioningAttributeBinding) {
		this.versioningAttributeBinding = versioningAttributeBinding;
	}

	public Caching getCaching() {
		return caching;
	}

	public void setCaching(Caching caching) {
		this.caching = caching;
	}

	public boolean isExplicitPolymorphism() {
		return explicitPolymorphism;
	}

	public void setExplicitPolymorphism(boolean explicitPolymorphism) {
		this.explicitPolymorphism = explicitPolymorphism;
	}
}
