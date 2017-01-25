/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.type.internal;

import org.hibernate.boot.model.type.spi.AbstractBasicTypeProducer;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * A BasicTypeProducer implementation representing cases where the site did
 * not refer to a specific BasicType/BasicTypeProducer by name.  These are
 * "site-specific" BasicTypeProducer instances, meaning they will not be
 * registered with the BasicTypeProducerRegistry
 *
 * @author Steve Ebersole
 */
public class BasicTypeProducerUnregisteredImpl extends AbstractBasicTypeProducer {
	public BasicTypeProducerUnregisteredImpl(TypeConfiguration typeConfiguration) {
		super( typeConfiguration );
	}

	@Override
	public String getName() {
		return null;
	}
}
