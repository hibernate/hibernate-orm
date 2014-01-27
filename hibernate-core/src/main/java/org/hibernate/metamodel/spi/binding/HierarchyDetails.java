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
package org.hibernate.metamodel.spi.binding;

import org.hibernate.EntityMode;
import org.hibernate.engine.OptimisticLockStyle;

/**
 * @author Steve Ebersole
 */
public class HierarchyDetails {
	private final EntityBinding rootEntityBinding;
	private final InheritanceType inheritanceType;
	private final TenantDiscrimination tenantDiscrimination;
	private final EntityMode entityMode;
	private final EntityIdentifier entityIdentifier;

	private EntityDiscriminator entityDiscriminator;

	private OptimisticLockStyle optimisticLockStyle;
	private EntityVersion entityVersion;

	private Caching caching;
	private Caching naturalIdCaching;

	private boolean explicitPolymorphism;

	public HierarchyDetails(EntityBinding rootEntityBinding, InheritanceType inheritanceType, EntityMode entityMode) {
		this.rootEntityBinding = rootEntityBinding;
		this.inheritanceType = inheritanceType;
		this.entityMode = entityMode;
		this.entityIdentifier = new EntityIdentifier( rootEntityBinding );
		this.entityVersion = new EntityVersion( rootEntityBinding );
		this.tenantDiscrimination = new TenantDiscrimination();
	}

	public EntityBinding getRootEntityBinding() {
		return rootEntityBinding;
	}

	public InheritanceType getInheritanceType() {
		return inheritanceType;
	}

	public TenantDiscrimination getTenantDiscrimination() {
		return tenantDiscrimination;
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

	public EntityVersion getEntityVersion() {
		return entityVersion;
	}

	public void setEntityVersion(EntityVersion entityVersion) {
		this.entityVersion = entityVersion;
	}

	public Caching getCaching() {
		return caching;
	}

	public void setCaching(Caching caching) {
		this.caching = caching;
	}

	public Caching getNaturalIdCaching() {
		return naturalIdCaching;
	}

	public void setNaturalIdCaching(Caching naturalIdCaching) {
		this.naturalIdCaching = naturalIdCaching;
	}

	public boolean isExplicitPolymorphism() {
		return explicitPolymorphism;
	}

	public void setExplicitPolymorphism(boolean explicitPolymorphism) {
		this.explicitPolymorphism = explicitPolymorphism;
	}
}
