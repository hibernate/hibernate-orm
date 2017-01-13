/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import org.hibernate.type.spi.JdbcLiteralFormatter;
import org.hibernate.type.spi.basic.BasicTypeImpl;
import org.hibernate.type.descriptor.java.internal.StringJavaDescriptor;
import org.hibernate.type.spi.descriptor.sql.ClobTypeDescriptor;

/**
 * A type that maps between {@link java.sql.Types#CLOB CLOB} and {@link String}
 *
 * @author Gavin King
 * @author Gail Badner
 * @author Steve Ebersole
 */
public class MaterializedClobType extends BasicTypeImpl<String> {
	public static final MaterializedClobType INSTANCE = new MaterializedClobType();

	public MaterializedClobType() {
		super( StringJavaDescriptor.INSTANCE, ClobTypeDescriptor.DEFAULT );
	}

	public String getName() {
		return "materialized_clob";
	}

	@Override
	public JdbcLiteralFormatter<String> getJdbcLiteralFormatter() {
		// no literal support for CLOB
		return null;
	}
}
