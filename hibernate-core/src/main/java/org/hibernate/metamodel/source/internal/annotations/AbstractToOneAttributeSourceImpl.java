package org.hibernate.metamodel.source.internal.annotations;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.CascadeStyles;
import org.hibernate.metamodel.source.internal.annotations.attribute.AbstractPersistentAttribute;
import org.hibernate.metamodel.source.internal.annotations.attribute.SingularAssociationAttribute;
import org.hibernate.metamodel.source.internal.annotations.util.EnumConversionHelper;
import org.hibernate.metamodel.source.spi.AttributeSource;
import org.hibernate.metamodel.source.spi.MappedByAssociationSource;
import org.hibernate.metamodel.source.spi.SingularAttributeSource;
import org.hibernate.metamodel.source.spi.ToOneAttributeSource;
import org.hibernate.type.ForeignKeyDirection;

public abstract class AbstractToOneAttributeSourceImpl extends SingularAttributeSourceImpl implements ToOneAttributeSource{
	private final SingularAssociationAttribute associationAttribute;
	private final Set<CascadeStyle> unifiedCascadeStyles;

	private SingularAttributeSource.Nature nature;
	private final Set<MappedByAssociationSource> ownedAssociationSources = new HashSet<MappedByAssociationSource>();

	public AbstractToOneAttributeSourceImpl(SingularAssociationAttribute associationAttribute) {
		super( associationAttribute );
		this.associationAttribute = associationAttribute;
		this.unifiedCascadeStyles = determineCascadeStyles( associationAttribute );
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
	public SingularAttributeSource.Nature getNature() {
		return nature;
	}

	protected SingularAssociationAttribute associationAttribute() {
		return associationAttribute;
	}

	protected void setNature(SingularAttributeSource.Nature nature) {
		if ( this.nature != null ) {
			throw new IllegalStateException( "nature is already initialized." );
		}
		this.nature = nature;
	}

	@Override
	public AttributeSource getAttributeSource() {
		return this;
	}

	@Override
	public String getReferencedEntityName() {
		return associationAttribute.getTargetTypeName();
	}

	@Override
	public boolean isUnique() {
		return AbstractPersistentAttribute.Nature.ONE_TO_ONE.equals( associationAttribute.getNature() );
	}

	@Override
	public boolean isNotFoundAnException() {
		return !associationAttribute.getShouldIgnoreNotFound();
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
		return unifiedCascadeStyles;
	}

	@Override
	public FetchTiming getFetchTiming() {
		return associationAttribute.isLazy() ? FetchTiming.DELAYED : FetchTiming.IMMEDIATE;
	}

	@Override
	public FetchStyle getFetchStyle() {
		if ( associationAttribute.getFetchStyle() != null ) {
			return associationAttribute.getFetchStyle();
		}
		else {
			return associationAttribute.isLazy() ? FetchStyle.SELECT : FetchStyle.JOIN;
		}
	}

	@Override
	public boolean isUnWrapProxy() {
		return associationAttribute.isUnWrapProxy();
	}

	@Override
	public ForeignKeyDirection getForeignKeyDirection() {
		return nature == Nature.ONE_TO_ONE && !associationAttribute.isOptional()
				? ForeignKeyDirection.FROM_PARENT
				: ForeignKeyDirection.TO_PARENT;
	}

	@Override
	public String toString() {
		return "ToOneAttributeSourceImpl{role=" + associationAttribute.getRole().getFullPath() + '}';
	}
}