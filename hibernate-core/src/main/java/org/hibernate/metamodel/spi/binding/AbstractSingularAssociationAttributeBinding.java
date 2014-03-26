/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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

import java.util.List;

import org.hibernate.AssertionFailure;
import org.hibernate.FetchMode;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.metamodel.source.spi.MetaAttributeContext;
import org.hibernate.metamodel.spi.AttributePath;
import org.hibernate.metamodel.spi.AttributeRole;
import org.hibernate.metamodel.spi.NaturalIdMutability;
import org.hibernate.metamodel.spi.domain.SingularAttribute;
import org.hibernate.metamodel.spi.relational.ForeignKey;
import org.hibernate.metamodel.spi.relational.TableSpecification;
import org.hibernate.metamodel.spi.relational.Value;

/**
 * @author Gail Badner
 */
public abstract class AbstractSingularAssociationAttributeBinding extends AbstractSingularAttributeBinding
		implements SingularAssociationAttributeBinding {
	private final EntityBinding referencedEntityBinding;
	private final SingularAttributeBinding referencedAttributeBinding;
	private JoinRelationalValueBindingContainer relationalValueBindingContainer;
	private CascadeStyle cascadeStyle;
	private FetchTiming fetchTiming;
	private FetchStyle fetchStyle;
	private boolean isUnWrapProxy;
	private final boolean isIgnoreNotFound;

	public AbstractSingularAssociationAttributeBinding(
			AttributeBindingContainer container,
			SingularAttribute attribute,
			String propertyAccessorName,
			boolean includedInOptimisticLocking,
			boolean isLazy,
			boolean isIgnoreNotFound,
			NaturalIdMutability naturalIdMutability,
			MetaAttributeContext metaAttributeContext,
			AttributeRole attributeRole,
			AttributePath attributePath,
			EntityBinding referencedEntityBinding,
			SingularAttributeBinding referencedAttributeBinding) {
		super(
				container,
				attribute,
				propertyAccessorName,
				includedInOptimisticLocking,
				isLazy,
				naturalIdMutability,
				metaAttributeContext,
				attributeRole,
				attributePath
		);
		if ( referencedEntityBinding == null ) {
			throw new IllegalArgumentException( "referencedEntityBinding must be non-null." );
		}
		if ( referencedAttributeBinding == null ) {
			throw new IllegalArgumentException( "referencedAttributeBinding must be non-null." );
		}
		this.referencedEntityBinding = referencedEntityBinding;
		this.referencedAttributeBinding = referencedAttributeBinding;
		this.isIgnoreNotFound = isIgnoreNotFound;
	}

	@Override
	public boolean isIgnoreNotFound() {
		return isIgnoreNotFound;
	}

	@Override
	public boolean isAssociation() {
		return true;
	}

	@Override
	public final String getReferencedEntityName() {
		return referencedEntityBinding.getEntityName();
	}

	@Override
	public CascadeStyle getCascadeStyle() {
		return cascadeStyle;
	}

	@Override
	public void setCascadeStyle(CascadeStyle cascadeStyle) {
		this.cascadeStyle = cascadeStyle;
	}

	@Override
	public FetchTiming getFetchTiming() {
		return fetchTiming;
	}

	@Override
	public void setFetchTiming(FetchTiming fetchTiming) {
		this.fetchTiming = fetchTiming;
	}

	@Override
	public FetchStyle getFetchStyle() {
		return fetchStyle;
	}

	@Override
	public void setFetchStyle(FetchStyle fetchStyle) {
		if ( fetchStyle == FetchStyle.SUBSELECT ) {
			throw new AssertionFailure( "Subselect fetching not yet supported for singular associations" );
		}
		this.fetchStyle = fetchStyle;
	}

	@Override
	public FetchMode getFetchMode() {
		switch ( fetchStyle ){
			case JOIN:
				return FetchMode.JOIN;
			case SELECT:
				return FetchMode.SELECT;
			case BATCH:
				// we need the subsequent select...
				return FetchMode.SELECT;
			default:
				throw new AssertionFailure( "Unexpected fetch style : " + fetchStyle.name() );
		}
	}

	@Override
	public final EntityBinding getReferencedEntityBinding() {
		return referencedEntityBinding;
	}

	@Override
	public SingularAttributeBinding getReferencedAttributeBinding() {
		return referencedAttributeBinding;
	}

	public void setJoinRelationalValueBindings(
			List<RelationalValueBinding> relationalValueBindings,
			ForeignKey foreignKey) {
		this.relationalValueBindingContainer =
				new JoinRelationalValueBindingContainer( relationalValueBindings, foreignKey );
	}

	@Override
	public TableSpecification getTable() {
		return relationalValueBindingContainer.getTable();
	}

	@Override
	public ForeignKey getForeignKey() {
		return relationalValueBindingContainer.getForeignKey();
	}

	@Override
	public List<Value> getValues() {
		return getRelationalValueBindingContainer().values();
	}

	@Override
	protected RelationalValueBindingContainer getRelationalValueBindingContainer() {
		return relationalValueBindingContainer;
	}

	@Override
	protected void collectRelationalValueBindings(RelationalValueBindingContainer relationalValueBindingContainer) {
		relationalValueBindingContainer.addRelationalValueBindings( this.relationalValueBindingContainer );
	}
}
