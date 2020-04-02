/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id.factory.spi;

import org.hibernate.id.factory.IdentifierGeneratorFactory;
import org.hibernate.service.Service;

/**
 * Let people register strategies
 *
 * @author <a href="mailto:emmanuel@hibernate.org">Emmanuel Bernard</a>
 */
public interface MutableIdentifierGeneratorFactory extends IdentifierGeneratorFactory, Service {
	public void register(String strategy, Class generatorClass);
}
