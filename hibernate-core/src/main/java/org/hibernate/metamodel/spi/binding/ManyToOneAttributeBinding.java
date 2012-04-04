/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.AssertionFailure;
import org.hibernate.FetchMode;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.metamodel.spi.domain.SingularAttribute;
import org.hibernate.metamodel.spi.source.MetaAttributeContext;

/**
 * TODO : javadoc
 *
 * @author Gail Badner
 * @author Steve Ebersole
 */
public class ManyToOneAttributeBinding
		extends AbstractSingularAttributeBinding
		implements SingularAssociationAttributeBinding {

	private final SingularAttributeBinding referencedAttributeBinding;
	private final List<RelationalValueBinding> relationalValueBindings;

	private CascadeStyle cascadeStyle;
	private FetchTiming fetchTiming;
	private FetchStyle fetchStyle;

	public ManyToOneAttributeBinding(
			AttributeBindingContainer container,
			SingularAttribute attribute,
			String propertyAccessorName,
			boolean includedInOptimisticLocking,
			boolean lazy,
			MetaAttributeContext metaAttributeContext,
			SingularAttributeBinding referencedAttributeBinding,
			List<RelationalValueBinding> relationalValueBindings) {
		super(
				container,
				attribute,
				propertyAccessorName,
				includedInOptimisticLocking,
				lazy,
				metaAttributeContext
		);

		if ( referencedAttributeBinding == null ) {
			throw new IllegalArgumentException( "referencedAttributeBinding must be non-null." );
		}
		if ( !EntityBinding.class.isInstance( referencedAttributeBinding.getContainer() ) ) {
			throw new AssertionFailure( "Illegal attempt to resolve many-to-one reference based on non-entity attribute" );
		}

		this.referencedAttributeBinding = referencedAttributeBinding;
		this.relationalValueBindings = Collections.unmodifiableList( relationalValueBindings );
	}

	@Override
	public List<RelationalValueBinding> getRelationalValueBindings() {
		return relationalValueBindings;
	}

	@Override
	public boolean hasDerivedValue() {
		// todo : not sure this is even relevant for many-to-one
		return false;
	}

	@Override
	public boolean isNullable() {
		// todo : not sure this is even relevant for many-to-one
		return false;
	}

	@Override
	public boolean isAssociation() {
		return true;
	}

	@Override
	public final String getReferencedEntityName() {
		return getReferencedEntityBinding().getEntity().getName();
	}

	@Override
	public CascadeStyle getCascadeStyle() {
		return cascadeStyle;
	}

	@Override
	public void setCascadeStyles(Iterable<CascadeStyle> cascadeStyles) {
		List<CascadeStyle> cascadeStyleList = new ArrayList<CascadeStyle>();
		for ( CascadeStyle style : cascadeStyles ) {
			if ( style != CascadeStyle.NONE ) {
				cascadeStyleList.add( style );
			}
		}
		if ( cascadeStyleList.isEmpty() ) {
			cascadeStyle = CascadeStyle.NONE;
		}
		else if ( cascadeStyleList.size() == 1 ) {
			cascadeStyle = cascadeStyleList.get( 0 );
		}
		else {
			cascadeStyle = new CascadeStyle.MultipleCascadeStyle(
					cascadeStyleList.toArray( new CascadeStyle[cascadeStyleList.size()] )
			);
		}
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
		if ( fetchStyle == FetchStyle.JOIN ) {
			return FetchMode.JOIN;
		}
		else if ( fetchStyle == FetchStyle.SELECT ) {
			return FetchMode.SELECT;
		}
		else if ( fetchStyle == FetchStyle.BATCH ) {
			// we need the subsequent select...
			return FetchMode.SELECT;
		}

		throw new AssertionFailure( "Unexpected fetch style : " + fetchStyle.name() );
	}

	@Override
	public final EntityBinding getReferencedEntityBinding() {
		return referencedAttributeBinding.getContainer().seekEntityBinding();
	}

	@Override
	protected void collectRelationalValueBindings(List<RelationalValueBinding> valueBindings) {
		valueBindings.addAll( relationalValueBindings );
	}
}