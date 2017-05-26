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
 * Descriptor for {@link Types#BINARY BINARY} handling.
 *
 * @author Steve Ebersole
 */
public class BinarySqlDescriptor extends VarbinarySqlDescriptor {
	public static final BinarySqlDescriptor INSTANCE = new BinarySqlDescriptor();

	public BinarySqlDescriptor() {
	}

	@Override
	public <T> BasicJavaDescriptor<T> getJdbcRecommendedJavaTypeMapping(TypeConfiguration typeConfiguration) {
		return super.getJdbcRecommendedJavaTypeMapping( typeConfiguration );
	}

	@Override
	public int getJdbcTypeCode() {
		return Types.BINARY;
	}
}
