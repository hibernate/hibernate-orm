/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.usertypes.xmlmapping;

import java.util.function.BiConsumer;

import org.hibernate.type.descriptor.java.BasicJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.VarcharJdbcType;
import org.hibernate.usertype.BaseUserTypeSupport;

public class CurrencyUnitUserType extends BaseUserTypeSupport<CurrencyUnit> {

	@Override
	protected void resolve(BiConsumer<BasicJavaType<CurrencyUnit>, JdbcType> resolutionConsumer) {
		resolutionConsumer.accept( CurrencyUnitTypeDescriptor.INSTANCE, VarcharJdbcType.INSTANCE );
	}
}
