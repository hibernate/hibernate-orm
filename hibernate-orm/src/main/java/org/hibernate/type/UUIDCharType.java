/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.util.UUID;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.descriptor.java.UUIDTypeDescriptor;
import org.hibernate.type.descriptor.sql.VarcharTypeDescriptor;

/**
 * A type mapping {@link java.sql.Types#CHAR} (or {@link java.sql.Types#VARCHAR}) and {@link java.util.UUID}
 *
 * @author Steve Ebersole
 */
public class UUIDCharType extends AbstractSingleColumnStandardBasicType<UUID> implements LiteralType<UUID> {
	public static final UUIDCharType INSTANCE = new UUIDCharType();

	public UUIDCharType() {
		super( VarcharTypeDescriptor.INSTANCE, UUIDTypeDescriptor.INSTANCE );
	}

	public String getName() {
		return "uuid-char";
	}

	public String objectToSQLString(UUID value, Dialect dialect) throws Exception {
		return StringType.INSTANCE.objectToSQLString( value.toString(), dialect );
	}
}
