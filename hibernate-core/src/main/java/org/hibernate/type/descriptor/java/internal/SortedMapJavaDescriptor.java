/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java.internal;

import java.util.SortedMap;

import org.hibernate.type.descriptor.java.spi.AbstractBasicJavaDescriptor;
import org.hibernate.type.descriptor.spi.JdbcRecommendedSqlTypeMappingContext;
import org.hibernate.type.descriptor.spi.WrapperOptions;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

/**
 * @author Chris Cranford
 */
public class SortedMapJavaDescriptor extends AbstractBasicJavaDescriptor<SortedMap> {
	/**
	 * Singleton access
	 */
	public static final SortedMapJavaDescriptor INSTANCE = new SortedMapJavaDescriptor();

	private SortedMapJavaDescriptor() {
		super( SortedMap.class );
	}

	@Override
	public Class<SortedMap> getJavaType() {
		return SortedMap.class;
	}

	@Override
	public String getTypeName() {
		return getJavaType().getName();
	}

	@Override
	public SqlTypeDescriptor getJdbcRecommendedSqlType(JdbcRecommendedSqlTypeMappingContext context) {
		// none
		return null;
	}

	@Override
	public int extractHashCode(SortedMap value) {
		return value.hashCode();
	}

	@Override
	public boolean areEqual(SortedMap one, SortedMap another) {
		return one == another
				|| ( one != null && one.equals( another ) );
	}

	@Override
	public String extractLoggableRepresentation(SortedMap value) {
		return "{sortedmap}";
	}

	@Override
	public String toString(SortedMap value) {
		return "{sortedmap}";
	}

	@Override
	public SortedMap fromString(String value) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public <X> X unwrap(SortedMap value, Class<X> type, WrapperOptions options) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public <X> SortedMap wrap(X value, WrapperOptions options) {
		throw new UnsupportedOperationException(  );
	}
}
