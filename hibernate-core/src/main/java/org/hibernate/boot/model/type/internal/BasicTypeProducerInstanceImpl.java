/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.type.internal;

import java.util.Comparator;
import java.util.Map;

import org.hibernate.boot.model.type.spi.BasicTypeProducer;
import org.hibernate.boot.model.type.spi.BasicTypeSiteContext;
import org.hibernate.type.spi.BasicType;
import org.hibernate.type.spi.basic.AttributeConverterDefinition;
import org.hibernate.type.spi.descriptor.JdbcRecommendedSqlTypeMappingContext;
import org.hibernate.type.spi.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.spi.descriptor.java.MutabilityPlan;
import org.hibernate.type.spi.descriptor.sql.SqlTypeDescriptor;
import org.hibernate.usertype.DynamicParameterizedType;

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
	public BasicTypeProducer injectBasicTypeSiteContext(BasicTypeSiteContext context) {
		// nothing to do here
		return this;
	}

	@Override
	public BasicType produceBasicType() {
		// return the same BasicType instance we were handed at initialization
		return basicType;
	}
}
