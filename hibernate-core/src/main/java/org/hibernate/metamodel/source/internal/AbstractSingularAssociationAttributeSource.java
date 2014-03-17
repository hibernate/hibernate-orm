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
package org.hibernate.metamodel.source.internal;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.CascadeStyles;
import org.hibernate.metamodel.source.internal.annotations.attribute.SingularAssociationAttribute;
import org.hibernate.metamodel.source.internal.annotations.util.EnumConversionHelper;
import org.hibernate.metamodel.source.spi.AttributeSource;
import org.hibernate.metamodel.source.spi.MappedByAssociationSource;
import org.hibernate.metamodel.source.spi.SingularAttributeSource;
import org.hibernate.metamodel.source.spi.ToOneAttributeSource;
import org.hibernate.type.ForeignKeyDirection;

/**
 * Common support for singular association (*-to-one) persistent attributes.
 *
 * @author Steve Ebersole
 * @author Gail Badner
 */
public abstract class AbstractSingularAssociationAttributeSource
		extends AbstractSingularAttributeSource
		implements ToOneAttributeSource {

	private final Set<CascadeStyle> cascadeStyles;
	private final Set<MappedByAssociationSource> ownedAssociationSources = new HashSet<MappedByAssociationSource>();

	private Nature nature;

	public AbstractSingularAssociationAttributeSource(
			AbstractManagedTypeSource container,
			SingularAssociationAttribute attribute) {
		super( container, attribute );
		this.cascadeStyles = determineCascadeStyles( attribute );
	}

	private static Set<CascadeStyle> determineCascadeStyles(SingularAssociationAttribute associationAttribute) {
		final Set<CascadeStyle> cascadeStyles = EnumConversionHelper.cascadeTypeToCascadeStyleSet(
				associationAttribute.getJpaCascadeTypes(),
				associationAttribute.getHibernateCascadeTypes(),
				associationAttribute.getContext()
		);
		if ( associationAttribute.isOrphanRemoval() ) {
			cascadeStyles.add( CascadeStyles.DELETE_ORPHAN );
		}
		return cascadeStyles;
	}

	@Override
	protected void validateConversionInfo(AttributeConversionInfo conversionInfo) {
		throw getContainer().getLocalBindingContext().makeMappingException(
				"Illegal attempt to apply AttributeConverter to non-basic attribute : "
						+ getPersistentAttribute().getBackingMember().toString()
		);
	}

	protected void setNature(Nature nature) {
		assert this.nature == null;
		this.nature = nature;
	}

	@Override
	public SingularAssociationAttribute getPersistentAttribute() {
		return (SingularAssociationAttribute) super.getPersistentAttribute();
	}

	@Override
	public SingularAttributeSource.Nature getNature() {
		return nature;
	}


	@Override
	public AttributeSource getAttributeSource() {
		return this;
	}

	@Override
	public String getReferencedEntityName() {
		return getPersistentAttribute().getTargetTypeName();
	}

	@Override
	public boolean isUnique() {
		return nature == Nature.ONE_TO_ONE;
	}

	@Override
	public boolean isIgnoreNotFound() {
		return getPersistentAttribute().isIgnoreNotFound();
	}

	@Override
	public Set<MappedByAssociationSource> getOwnedAssociationSources() {
		return ownedAssociationSources;
	}

	@Override
	public void addMappedByAssociationSource(MappedByAssociationSource attributeSource) {
		if ( attributeSource == null ) {
			throw new IllegalArgumentException( "attributeSource must be non-null." );
		}
		ownedAssociationSources.add( attributeSource );
	}

	@Override
	public boolean isMappedBy() {
		return false;
	}

	@Override
	public Set<CascadeStyle> getCascadeStyles() {
		return cascadeStyles;
	}

	@Override
	public FetchTiming getFetchTiming() {
		return getPersistentAttribute().isLazy()
				? FetchTiming.DELAYED
				: FetchTiming.IMMEDIATE;
	}

	@Override
	public FetchStyle getFetchStyle() {
		if ( getPersistentAttribute().getFetchStyle() != null ) {
			return getPersistentAttribute().getFetchStyle();
		}
		else {
			return getPersistentAttribute().isLazy()
					? FetchStyle.SELECT
					: FetchStyle.JOIN;
		}
	}

	@Override
	public boolean isUnWrapProxy() {
		return getPersistentAttribute().isUnWrapProxy();
	}

	@Override
	public String toString() {
		return "ToOneAttributeSourceImpl{attribute=" + getPersistentAttribute()
				+ ", cascadeStyles=" + cascadeStyles + '}';
	}

	@Override
	public ForeignKeyDirection getForeignKeyDirection() {
		return nature == Nature.ONE_TO_ONE && !getPersistentAttribute().isOptional()
				? ForeignKeyDirection.FROM_PARENT
				: ForeignKeyDirection.TO_PARENT;
	}
}
