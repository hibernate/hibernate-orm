/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.loader.plan.build.internal.returns;

import org.hibernate.loader.PropertyPath;
import org.hibernate.loader.plan.build.spi.ExpandingCompositeQuerySpace;
import org.hibernate.loader.plan.build.spi.ExpandingEntityIdentifierDescription;
import org.hibernate.loader.plan.spi.EntityReference;
import org.hibernate.type.CompositeType;

/**
 * Models a composite entity identifier that is encapsulated (meaning there is a composite class and a single
 * attribute that encapsulates the composite value).
 *
 * @author Steve Ebersole
 */
public class EncapsulatedEntityIdentifierDescription
		extends AbstractCompositeEntityIdentifierDescription
		implements ExpandingEntityIdentifierDescription {

	/**
	 * Build an encapsulated version of a composite EntityIdentifierDescription
	 *
	 * @param entityReference The entity whose identifier we describe
	 * @param compositeQuerySpace The query space we are mapped to.
	 * @param compositeType The type representing this composition
	 * @param propertyPath The property path (informational)
	 */
	protected EncapsulatedEntityIdentifierDescription(
			EntityReference entityReference,
			ExpandingCompositeQuerySpace compositeQuerySpace,
			CompositeType compositeType,
			PropertyPath propertyPath) {
		super( entityReference, compositeQuerySpace, compositeType, propertyPath );
	}
}
