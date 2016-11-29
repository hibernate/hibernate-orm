/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.util.UUID;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.spi.JdbcLiteralFormatter;
import org.hibernate.type.spi.basic.BasicTypeImpl;
import org.hibernate.type.spi.descriptor.java.UUIDTypeDescriptor;
import org.hibernate.type.spi.descriptor.sql.VarcharTypeDescriptor;

/**
 * A type mapping {@link java.sql.Types#CHAR} (or {@link java.sql.Types#VARCHAR}) and {@link java.util.UUID}
 *
 * @author Steve Ebersole
 */
public class UUIDCharType extends BasicTypeImpl<UUID> implements JdbcLiteralFormatter<UUID> {
	public static final UUIDCharType INSTANCE = new UUIDCharType();

	public UUIDCharType() {
		super( UUIDTypeDescriptor.INSTANCE, VarcharTypeDescriptor.INSTANCE );
	}

	public String getName() {
		return "uuid-char";
	}

	@Override
	public JdbcLiteralFormatter<UUID> getJdbcLiteralFormatter() {
		return this;
	}

	@Override
	public String toJdbcLiteral(UUID value, Dialect dialect) {
		return StringType.INSTANCE.toJdbcLiteral( toString( value ), dialect );
	}
}
