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
public class HierarchyDetails implements EntityHierarchy {
	private final EntityBinding rootEntityBinding;
	private final InheritanceType inheritanceType;
	private final EntityIdentifier entityIdentifier;
	private final EntityDiscriminator entityDiscriminator;
	private final EntityVersion entityVersion;

	private final TenantDiscrimination tenantDiscrimination;

	private final EntityMode entityMode;
	private final OptimisticLockStyle optimisticLockStyle;

	private final Caching caching;
	private final Caching naturalIdCaching;

	private final boolean explicitPolymorphism;
	private final boolean mutable;
	private final String where;

	public HierarchyDetails(
			EntityMode entityMode,
			InheritanceType inheritanceType,
			boolean isDiscriminated,
			boolean isVersioned,
			boolean isTenancyDiscriminated,
			OptimisticLockStyle optimisticLockStyle,
			Caching caching,
			Caching naturalIdCaching,
			boolean explicitPolymorphism,
			boolean mutable,
			String where) {
		this.entityMode = entityMode;
		this.inheritanceType = inheritanceType;
		this.optimisticLockStyle = optimisticLockStyle;
		this.caching = caching;
		this.naturalIdCaching = naturalIdCaching;
		this.explicitPolymorphism = explicitPolymorphism;
		this.mutable = mutable;
		this.where = where;

		this.rootEntityBinding = new EntityBinding( this );
		this.entityIdentifier = new EntityIdentifier( rootEntityBinding );

		this.entityVersion = isVersioned ? new EntityVersion( rootEntityBinding ) : null;
		this.entityDiscriminator = isDiscriminated ? new EntityDiscriminator() : null;
		this.tenantDiscrimination = isTenancyDiscriminated ? new TenantDiscrimination() : null;
	}

	@Override
	public EntityBinding getRootEntityBinding() {
		return rootEntityBinding;
	}

	@Override
	public InheritanceType getInheritanceType() {
		return inheritanceType;
	}

	@Override
	public TenantDiscrimination getTenantDiscrimination() {
		return tenantDiscrimination;
	}

	@Override
	public EntityMode getEntityMode() {
		return entityMode;
	}

	@Override
	public EntityIdentifier getEntityIdentifier() {
		return entityIdentifier;
	}

	@Override
	public EntityDiscriminator getEntityDiscriminator() {
		return entityDiscriminator;
	}

	@Override
	public OptimisticLockStyle getOptimisticLockStyle() {
		return optimisticLockStyle;
	}


	public boolean isVersioned() {
		return getEntityVersion() != null;
	}

	@Override
	public EntityVersion getEntityVersion() {
		return entityVersion;
	}

	@Override
	public Caching getCaching() {
		return caching;
	}

	@Override
	public Caching getNaturalIdCaching() {
		return naturalIdCaching;
	}

	@Override
	public boolean isMutable() {
		return mutable;
	}

	@Override
	public boolean isExplicitPolymorphism() {
		return explicitPolymorphism;
	}

	@Override
	public String getWhere() {
		return where;
	}

	public static class Builder {
		private InheritanceType inheritanceType = InheritanceType.NO_INHERITANCE;
		private EntityMode entityMode = EntityMode.POJO;
		private OptimisticLockStyle optimisticLockStyle = OptimisticLockStyle.VERSION;
		private Caching caching;
		private Caching naturalIdCaching;
		private boolean explicitPolymorphism;
		private boolean mutable = true;
		private String where;

		private boolean isDiscriminated;
		private boolean isVersioned;
		private boolean isTenancyDiscriminated;

		public Builder setInheritanceType(InheritanceType inheritanceType) {
			this.inheritanceType = inheritanceType;
			return this;
		}

		public Builder setEntityMode(EntityMode entityMode) {
			this.entityMode = entityMode;
			return this;
		}

		public Builder setOptimisticLockStyle(OptimisticLockStyle optimisticLockStyle) {
			this.optimisticLockStyle = optimisticLockStyle;
			return this;
		}

		public Builder setCaching(Caching caching) {
			this.caching = caching;
			return this;
		}

		public Builder setNaturalIdCaching(Caching naturalIdCaching) {
			this.naturalIdCaching = naturalIdCaching;
			return this;
		}

		public Builder setExplicitPolymorphism(boolean explicitPolymorphism) {
			this.explicitPolymorphism = explicitPolymorphism;
			return this;
		}

		public Builder setMutable(boolean mutable) {
			this.mutable = mutable;
			return this;
		}

		public Builder setWhere(String where) {
			this.where = where;
			return this;
		}

		public Builder setDiscriminated(boolean isDiscriminated) {
			this.isDiscriminated = isDiscriminated;
			return this;
		}

		public Builder setVersioned(boolean isVersioned) {
			this.isVersioned = isVersioned;
			return this;
		}

		public Builder setTenancyDiscriminated(boolean isTenancyDiscriminated) {
			this.isTenancyDiscriminated = isTenancyDiscriminated;
			return this;
		}

		public HierarchyDetails createHierarchyDetails() {
			return new HierarchyDetails(
					entityMode,
					inheritanceType,
					isDiscriminated,
					isVersioned,
					isTenancyDiscriminated,
					optimisticLockStyle,
					caching,
					naturalIdCaching,
					explicitPolymorphism,
					mutable,
					where
			);
		}
	}
}
