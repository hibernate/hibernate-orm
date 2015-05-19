/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.plan.build.internal.returns;

import org.hibernate.loader.PropertyPath;
import org.hibernate.loader.plan.spi.BidirectionalEntityReference;
import org.hibernate.loader.plan.spi.Fetch;
import org.hibernate.loader.plan.spi.FetchSource;

/**
 * @author Gail Badner
 */
public abstract class AbstractAnyReference implements FetchSource {
	/**
	 * Convenient constant for returning no fetches from {@link #getFetches()}
	 */
	private static final Fetch[] NO_FETCHES = new Fetch[0];

	/**
	 * Convenient constant for returning no fetches from {@link #getFetches()}
	 */
	private static final BidirectionalEntityReference[] NO_BIDIRECTIONAL_ENTITY_REFERENCES =
			new BidirectionalEntityReference[0];

	private final PropertyPath propertyPath;

	public AbstractAnyReference(PropertyPath propertyPath) {
		this.propertyPath = propertyPath;
	}

	@Override
	public PropertyPath getPropertyPath() {
		return propertyPath;
	}

	@Override
	public Fetch[] getFetches() {
		return NO_FETCHES;
	}

	@Override
	public BidirectionalEntityReference[] getBidirectionalEntityReferences() {
		return NO_BIDIRECTIONAL_ENTITY_REFERENCES;
	}

	@Override
	public String getQuerySpaceUid() {
		// TODO: should this throw UnsupportedOperationException?
		return null;
	}

}
