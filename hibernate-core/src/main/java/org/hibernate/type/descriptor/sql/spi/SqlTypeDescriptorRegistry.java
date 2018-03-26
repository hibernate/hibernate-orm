/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.sql.spi;

import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Basically a map from JDBC type code (int) -> {@link SqlTypeDescriptor}
 *
 * @author Steve Ebersole
 * @author Andrea Boriero
 *
 * @since 5.3
 */
public class SqlTypeDescriptorRegistry extends org.hibernate.type.descriptor.sql.SqlTypeDescriptorRegistry {
	private TypeConfiguration typeConfiguration;

	public SqlTypeDescriptorRegistry(TypeConfiguration typeConfiguration) {
		this.typeConfiguration = typeConfiguration;
	}

	@Override
	public void addDescriptor(SqlTypeDescriptor sqlTypeDescriptor) {
		super.addDescriptor( sqlTypeDescriptor );
	}

	@Override
	public SqlTypeDescriptor getDescriptor(int jdbcTypeCode) {
		return super.getDescriptor( jdbcTypeCode );
	}
}
