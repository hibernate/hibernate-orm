/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.jdbc;
import java.sql.Types;

import org.hibernate.type.descriptor.java.BasicJavaDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Descriptor for {@link Types#CHAR CHAR} handling.
 *
 * @author Steve Ebersole
 */
public class CharTypeDescriptor extends VarcharTypeDescriptor {
	public static final CharTypeDescriptor INSTANCE = new CharTypeDescriptor();

	public CharTypeDescriptor() {
	}

	@Override
	public String toString() {
		return "CharTypeDescriptor";
	}

	@Override
	public int getJdbcTypeCode() {
		return Types.CHAR;
	}

	@Override
	public <T> BasicJavaDescriptor<T> getJdbcRecommendedJavaTypeMapping(TypeConfiguration typeConfiguration) {
		return super.getJdbcRecommendedJavaTypeMapping( typeConfiguration );
	}
}
