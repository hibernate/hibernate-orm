/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.type.spi;

import org.hibernate.boot.model.TypeDefinition;
import org.hibernate.type.spi.BasicType;

/**
 * Defines a registry for BasicType "producers" based on registration keys
 *
 * @author Steve Ebersole
 */
public interface BasicTypeProducerRegistry {
	enum DuplicationStrategy {
		KEEP,
		OVERWRITE,
		DISALLOW
	}

	BasicTypeProducerRegistry register(TypeDefinition typeDefinition);
	BasicTypeProducerRegistry register(TypeDefinition typeDefinition, DuplicationStrategy duplicationStrategy);

	BasicTypeProducerRegistry register(BasicType basicTypeInstance, String... keys);
	BasicTypeProducerRegistry register(BasicType basicTypeInstance, DuplicationStrategy duplicationStrategy, String... keys);

	/**
	 * Look up a BasicTypeProducer by name.  May return {@code null} if that name
	 * is not (yet) recognized.
	 *
	 * @param name
	 *
	 * @return
	 */
	BasicTypeProducer resolve(String name);

	BasicTypeProducer makeUnregisteredProducer();

	/**
	 * Releases this BasicTypeProducerRegistry and its hold resources.
	 * </p>
	 * Applications should never call this.  Not overly happy about exposing this.  But
	 * given the way TypeConfiguration and BasicTypeProducerRegistry get instantiated
	 * currently I do not see an alternative.
	 */
	void release();
}
