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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.metamodel.spi.domain.AttributeContainer;
import org.hibernate.metamodel.spi.domain.SingularAttribute;
import org.hibernate.metamodel.spi.source.MetaAttributeContext;

/**
 * @author Gail Badner
 */
public class NonAggregatedCompositeAttributeBinding extends AbstractCompositeAttributeBinding {
	private final Map<String, AttributeBinding> attributeBindingMap;

	public NonAggregatedCompositeAttributeBinding(
			AttributeBindingContainer container,
			SingularAttribute attribute,
			String propertyAccessorName,
			NaturalIdMutability naturalIdMutability,
			MetaAttributeContext metaAttributeContext,
			List<SingularAttributeBinding> subAttributeBindings) {
		super(
				container,
				attribute,
				propertyAccessorName,
				false,
				false,
				naturalIdMutability,
				metaAttributeContext
		);
		if ( !AttributeContainer.class.isInstance( attribute.getSingularAttributeType() ) ||
				attribute.getSingularAttributeType().isComposite() ) {
			throw new IllegalArgumentException(
					"Expected the attribute type to be an non-component attribute container"
			);
		}
		Map<String, AttributeBinding> map = new LinkedHashMap<String, AttributeBinding>();
		for ( SingularAttributeBinding attributeBinding : subAttributeBindings ) {
			map.put( attributeBinding.getAttribute().getName(), attributeBinding );
		}
		attributeBindingMap = Collections.unmodifiableMap( map );
	}

	@Override
	public boolean isAggregated() {
		return false;
	}

	@Override
	protected Map<String, AttributeBinding> attributeBindingMapInternal() {
		return attributeBindingMap;
	}
}
