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
import org.hibernate.type.spi.descriptor.sql.NClobTypeDescriptor;

/**
 * A type that maps between {@link java.sql.Types#NCLOB NCLOB} and {@link String}
 *
 * @author Gavin King
 * @author Gail Badner
 * @author Steve Ebersole
 */
public class MaterializedNClobType extends BasicTypeImpl<String> {
	public static final MaterializedNClobType INSTANCE = new MaterializedNClobType();

	public MaterializedNClobType() {
		super( StringJavaDescriptor.INSTANCE, NClobTypeDescriptor.DEFAULT );
	}

	public String getName() {
		return "materialized_nclob";
	}

	@Override
	public JdbcLiteralFormatter<String> getJdbcLiteralFormatter() {
		// no literal support for NCLOB
		return null;
	}
}
