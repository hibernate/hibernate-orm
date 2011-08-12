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
package org.hibernate.metamodel.source.annotations.attribute;

import java.util.Set;

import org.hibernate.FetchMode;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.metamodel.source.annotations.EnumConversionHelper;
import org.hibernate.metamodel.source.binder.SingularAttributeNature;
import org.hibernate.metamodel.source.binder.ToOneAttributeSource;

/**
 * @author Hardy Ferentschik
 */
public class ToOneAttributeSourceImpl extends SingularAttributeSourceImpl implements ToOneAttributeSource {
	private final AssociationAttribute associationAttribute;
	private final Set<CascadeStyle> cascadeStyles;

	public ToOneAttributeSourceImpl(AssociationAttribute associationAttribute) {
		super( associationAttribute );
		this.associationAttribute = associationAttribute;
		this.cascadeStyles = EnumConversionHelper.cascadeTypeToCascadeStyleSet( associationAttribute.getCascadeTypes() );
	}

	@Override
	public SingularAttributeNature getNature() {
		return SingularAttributeNature.MANY_TO_ONE;
	}

	@Override
	public String getReferencedEntityName() {
		return associationAttribute.getReferencedEntityType();
	}

	@Override
	public String getReferencedEntityAttributeName() {
		return associationAttribute.getMappedBy();
	}

	@Override
	public Iterable<CascadeStyle> getCascadeStyles() {
		return cascadeStyles;
	}

	@Override
	public FetchMode getFetchMode() {
		return associationAttribute.getFetchMode();
	}

	@Override
	public FetchTiming getFetchTiming() {
		  // todo : implement
		return FetchTiming.IMMEDIATE;
	}

	@Override
	public FetchStyle getFetchStyle() {
		// todo : implement
		return FetchStyle.JOIN;
	}
}


