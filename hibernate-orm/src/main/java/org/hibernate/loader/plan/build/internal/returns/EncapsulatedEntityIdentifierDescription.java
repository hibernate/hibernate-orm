/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
