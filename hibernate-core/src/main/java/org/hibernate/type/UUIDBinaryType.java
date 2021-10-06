/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.util.UUID;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.descriptor.java.UUIDJavaTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.BinaryJdbcTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptor;

/**
 * A type mapping {@link java.sql.Types#BINARY} and {@link UUID}
 *
 * @author Steve Ebersole
 */
public class UUIDBinaryType extends AbstractSingleColumnStandardBasicType<UUID> {
	public static final UUIDBinaryType INSTANCE = new UUIDBinaryType();

	public UUIDBinaryType() {
		super( BinaryJdbcTypeDescriptor.INSTANCE, new UUIDJavaTypeDescriptor() {
			@Override
			public long getDefaultSqlLength(
					Dialect dialect,
					JdbcTypeDescriptor jdbcType) {
				return 16;
			}
		} );
	}

	public String getName() {
		return "uuid-binary";
	}

	@Override
	protected boolean registerUnderJavaType() {
		return true;
	}
}
