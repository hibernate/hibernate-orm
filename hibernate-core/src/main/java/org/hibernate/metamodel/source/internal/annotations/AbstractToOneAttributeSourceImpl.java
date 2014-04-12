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
import org.hibernate.metamodel.source.spi.ToOneAttributeSource;
import org.hibernate.metamodel.spi.AttributePath;
import org.hibernate.metamodel.spi.AttributeRole;
import org.hibernate.metamodel.spi.SingularAttributeNature;
import org.hibernate.type.ForeignKeyDirection;

public abstract class AbstractToOneAttributeSourceImpl extends SingularAttributeSourceImpl implements ToOneAttributeSource{
	private final SingularAssociationAttribute associationAttribute;
	private final Set<CascadeStyle> unifiedCascadeStyles;

	private SingularAttributeNature singularAttributeNature;
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
	public SingularAttributeNature getSingularAttributeNature() {
		return singularAttributeNature;
	}

	protected SingularAssociationAttribute associationAttribute() {
		return associationAttribute;
	}

	protected void setSingularAttributeNature(SingularAttributeNature singularAttributeNature) {
		if ( this.singularAttributeNature != null ) {
			throw new IllegalStateException( "nature is already initialized." );
		}
		this.singularAttributeNature = singularAttributeNature;
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
	public boolean isIgnoreNotFound() {
		return associationAttribute.isIgnoreNotFound();
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
		return singularAttributeNature == SingularAttributeNature.ONE_TO_ONE &&
				!associationAttribute.isOptional() &&
				associationAttribute.getMappedByAttributeName() == null
				? ForeignKeyDirection.FROM_PARENT
				: ForeignKeyDirection.TO_PARENT;
	}

	@Override
	public String toString() {
		return "ToOneAttributeSourceImpl{role=" + associationAttribute.getRole().getFullPath() + '}';
	}


	@Override
	public AttributePath getAttributePath() {
		return associationAttribute.getPath();
	}

	@Override
	public AttributeRole getAttributeRole() {
		return associationAttribute.getRole();
	}
}