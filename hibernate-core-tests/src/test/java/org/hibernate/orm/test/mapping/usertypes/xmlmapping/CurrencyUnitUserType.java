package org.hibernate.orm.test.mapping.usertypes.xmlmapping;

import jakarta.annotation.Nonnull;

import java.util.function.BiConsumer;

import org.hibernate.type.descriptor.java.BasicJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.VarcharJdbcType;
import org.hibernate.usertype.BaseUserTypeSupport;

public class CurrencyUnitUserType extends BaseUserTypeSupport<CurrencyUnit> {

	@Override
	protected void resolve(@Nonnull BiConsumer<BasicJavaType<CurrencyUnit>, JdbcType> resolutionConsumer) {
		resolutionConsumer.accept( CurrencyUnitTypeDescriptor.INSTANCE, VarcharJdbcType.INSTANCE );
	}
}
