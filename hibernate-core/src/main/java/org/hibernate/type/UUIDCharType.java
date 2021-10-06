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
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.VarcharJdbcTypeDescriptor;

/**
 * A type mapping {@link java.sql.Types#CHAR} (or {@link java.sql.Types#VARCHAR}) and {@link UUID}
 *
 * @author Steve Ebersole
 */
public class UUIDCharType extends AbstractSingleColumnStandardBasicType<UUID> {
	public static final UUIDCharType INSTANCE = new UUIDCharType();

	public UUIDCharType() {
		super( VarcharJdbcTypeDescriptor.INSTANCE, new UUIDJavaTypeDescriptor() {
			@Override
			public long getDefaultSqlLength(
					Dialect dialect,
					JdbcTypeDescriptor jdbcType) {
				return 36;
			}
		} );
	}

	public String getName() {
		return "uuid-char";
	}

}
