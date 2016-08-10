/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.type.spi;

import org.hibernate.type.spi.BasicType;

/**
 * A "producer" for a BasicType, scoped by a {@link BasicTypeProducerRegistry}.
 *
 * @author Steve Ebersole
 */
public interface BasicTypeProducer {
	String getName();

	BasicTypeProducer injectBasicTypeSiteContext(BasicTypeSiteContext context);

	BasicType produceBasicType();
}
