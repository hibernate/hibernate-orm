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
 * A BasicTypeProducer intended for use in cases where the producer will not be registered.
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
