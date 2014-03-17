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

import org.hibernate.metamodel.source.internal.annotations.attribute.AssociationOverride;
import org.hibernate.metamodel.source.internal.annotations.attribute.AttributeOverride;
import org.hibernate.metamodel.source.internal.annotations.entity.ManagedTypeMetadata;
import org.hibernate.metamodel.source.spi.AttributeSourceContainer;
import org.hibernate.metamodel.spi.LocalBindingContext;

/**
 * Base class for sources of "managed type" (entity, mapped-superclass,
 * embeddable) information.
 *
 * @author Steve Ebersole
 * @author Hardy Ferentschik
 */
public abstract class AbstractManagedTypeSource implements AttributeSourceContainer {
	private final ManagedTypeMetadata metadata;

	protected AbstractManagedTypeSource(ManagedTypeMetadata metadata) {
		this.metadata = metadata;
	}

	@Override
	public LocalBindingContext getLocalBindingContext() {
		return metadata.getLocalBindingContext();
	}

	abstract AttributeOverride locateAttributeOverride(String path);

	abstract AssociationOverride locateAssociationOverride(String path);

	abstract AttributeConversionInfo locateConversionInfo(String path);
}
