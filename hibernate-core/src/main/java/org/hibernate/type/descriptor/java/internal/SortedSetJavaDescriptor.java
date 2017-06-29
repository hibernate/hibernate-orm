/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java.internal;

import java.util.SortedSet;

import org.hibernate.type.descriptor.java.spi.AbstractBasicJavaDescriptor;
import org.hibernate.type.descriptor.spi.JdbcRecommendedSqlTypeMappingContext;
import org.hibernate.type.descriptor.spi.WrapperOptions;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

/**
 * @author Chris Cranford
 */
public class SortedSetJavaDescriptor extends AbstractBasicJavaDescriptor<SortedSet> {
	/**
	 * Singleton access
	 */
	public static final SortedSetJavaDescriptor INSTANCE = new SortedSetJavaDescriptor();

	private SortedSetJavaDescriptor() {
		super( SortedSet.class );
	}

	@Override
	public Class<SortedSet> getJavaType() {
		return SortedSet.class;
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
	public int extractHashCode(SortedSet value) {
		return value.hashCode();
	}

	@Override
	public boolean areEqual(SortedSet one, SortedSet another) {
		return one == another
				|| ( one != null && one.equals( another ) );
	}

	@Override
	public String extractLoggableRepresentation(SortedSet value) {
		return "{sortedset}";
	}

	@Override
	public String toString(SortedSet value) {
		return "{sortedset}";
	}

	@Override
	public SortedSet fromString(String value) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public <X> X unwrap(SortedSet value, Class<X> type, WrapperOptions options) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public <X> SortedSet wrap(X value, WrapperOptions options) {
		throw new UnsupportedOperationException(  );
	}
}
