/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.util.UUID;

import org.hibernate.type.spi.JdbcLiteralFormatter;
import org.hibernate.type.spi.basic.BasicTypeImpl;
import org.hibernate.type.spi.descriptor.java.UUIDTypeDescriptor;
import org.hibernate.type.spi.descriptor.sql.BinaryTypeDescriptor;

/**
 * A type mapping {@link java.sql.Types#BINARY} and {@link UUID}
 *
 * @author Steve Ebersole
 */
public class UUIDBinaryType extends BasicTypeImpl<UUID> {
	public static final UUIDBinaryType INSTANCE = new UUIDBinaryType();

	public UUIDBinaryType() {
		super( UUIDTypeDescriptor.INSTANCE, BinaryTypeDescriptor.INSTANCE );
	}

	public String getName() {
		return "uuid-binary";
	}

	@Override
	public JdbcLiteralFormatter<UUID> getJdbcLiteralFormatter() {
		// no literal support for binary data
		return null;
	}
}
