/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.spi;

import org.hibernate.boot.model.type.spi.BasicTypeProducerRegistry;

/**
 * Baseline set of {@link BasicType} implementations
 *
 * @author Steve Ebersole
 */
public class BasicTypesBaseline {
	public static void prime(TypeConfiguration typeConfiguration, BasicTypeProducerRegistry basicTypeProducerRegistry) {
		// need to decide what is going to drive this
		//
		// ideally that becomes this class
	}
}
