/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.sql.spi;

import java.sql.Types;

import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Descriptor for {@link Types#CHAR CHAR} handling.
 *
 * @author Steve Ebersole
 */
public class CharSqlDescriptor extends VarcharSqlDescriptor {
	public static final CharSqlDescriptor INSTANCE = new CharSqlDescriptor();

	public CharSqlDescriptor() {
	}

	@Override
	public <T> BasicJavaDescriptor<T> getJdbcRecommendedJavaTypeMapping(TypeConfiguration typeConfiguration) {
		return (BasicJavaDescriptor<T>) typeConfiguration.getJavaTypeDescriptorRegistry().getDescriptor( Character.class );
	}

	@Override
	public int getJdbcTypeCode() {
		return Types.CHAR;
	}
}
