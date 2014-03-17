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

import org.hibernate.mapping.PropertyGeneration;
import org.hibernate.metamodel.source.internal.annotations.attribute.AbstractPersistentAttribute;
import org.hibernate.metamodel.source.spi.SingularAttributeSource;

import static org.hibernate.metamodel.spi.binding.SingularAttributeBinding.NaturalIdMutability;

/**
 * Common support for singular, non-composite persistent attributes.
 *
 * @author Steve Ebersole
 * @author Hardy Ferentschik
 */
public abstract class AbstractSingularAttributeSource
		extends AbstractAttributeSource
		implements SingularAttributeSource {
	private final AttributeConversionInfo conversionInfo;

	protected AbstractSingularAttributeSource(
			AbstractManagedTypeSource container,
			AbstractPersistentAttribute attribute) {
		super( container, attribute );

		this.conversionInfo = container.locateConversionInfo( attribute.getName() );
		validateConversionInfo( conversionInfo );
	}

	protected abstract void validateConversionInfo(AttributeConversionInfo conversionInfo);

	public AttributeConversionInfo getConversionInfo() {
		return conversionInfo;
	}

	@Override
	public boolean isSingular() {
		return true;
	}

	@Override
	public boolean isVirtualAttribute() {
		return false;
	}

	@Override
	public PropertyGeneration getGeneration() {
		return getPersistentAttribute().getPropertyGeneration();
	}

	@Override
	public boolean isLazy() {
		return getPersistentAttribute().isLazy();
	}

	@Override
	public NaturalIdMutability getNaturalIdMutability() {
		return getPersistentAttribute().getNaturalIdMutability();
	}

	@Override
	public boolean areValuesIncludedInInsertByDefault() {
		return getPersistentAttribute().isInsertable();
	}

	@Override
	public boolean areValuesIncludedInUpdateByDefault() {
		return !getPersistentAttribute().isId() && getPersistentAttribute().isUpdatable();
	}

	@Override
	public boolean areValuesNullableByDefault() {
		return !getPersistentAttribute().isId() && getPersistentAttribute().isOptional();
	}
}
