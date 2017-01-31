/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.type.spi;

import org.hibernate.type.spi.BasicType;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractBasicTypeProducer implements BasicTypeProducer {
	private final TypeConfiguration typeConfiguration;
	private BasicTypeResolver basicTypeResolver;

	protected AbstractBasicTypeProducer(TypeConfiguration typeConfiguration) {
		this.typeConfiguration = typeConfiguration;
	}

	@Override
	public BasicTypeProducer injectBasicTypeSiteContext(BasicTypeResolver basicTypeResolver) {
		this.basicTypeResolver = basicTypeResolver;
		return this;
	}

	protected BasicTypeResolver getBasicTypeResolver() {
		return basicTypeResolver;
	}

	@Override
	public BasicType produceBasicType() {
		if ( basicTypeResolver == null ) {
			return null;
		}

		return typeConfiguration.getBasicTypeRegistry().resolveBasicType(
				basicTypeResolver,
				basicTypeResolver
		);
	}
}
