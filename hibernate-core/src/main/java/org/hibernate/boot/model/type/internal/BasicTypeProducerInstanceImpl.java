/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.type.internal;

import org.hibernate.boot.model.type.spi.BasicTypeProducer;
import org.hibernate.boot.model.type.spi.BasicTypeResolver;
import org.hibernate.type.spi.BasicType;

/**
 * BasicTypeProducer implementation for cases where we are handed a {@link BasicType} directly.
 * <p/>
 * The return from calling this implementation's {@link #produceBasicType} is the exact
 * {@link BasicType} instance we were handled initially.  This implementation always produces
 * back the same instance it was handed initially.
 *
 * @author Steve Ebersole
 */
public class BasicTypeProducerInstanceImpl implements BasicTypeProducer {
	private final String name;
	private final BasicType basicType;

	public BasicTypeProducerInstanceImpl(String name, BasicType basicType) {
		this.name = name;
		this.basicType = basicType;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public BasicTypeProducer injectBasicTypeSiteContext(BasicTypeResolver context) {
		// nothing to do here
		return this;
	}

	@Override
	public BasicType produceBasicType() {
		// return the same BasicType instance we were handed at initialization
		return basicType;
	}
}
