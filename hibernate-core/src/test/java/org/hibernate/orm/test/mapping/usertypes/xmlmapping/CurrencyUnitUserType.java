/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
