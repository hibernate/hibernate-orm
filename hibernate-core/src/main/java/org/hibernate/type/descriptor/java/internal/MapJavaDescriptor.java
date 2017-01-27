/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java.internal;

import java.util.Comparator;
import java.util.Map;

import org.hibernate.type.descriptor.java.spi.AbstractBasicJavaDescriptor;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.spi.JdbcRecommendedSqlTypeMappingContext;
import org.hibernate.type.descriptor.spi.WrapperOptions;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class MapJavaDescriptor extends AbstractBasicJavaDescriptor<Map> {
	/**
	 * Singleton access
	 */
	public static final MapJavaDescriptor INSTANCE = new MapJavaDescriptor();

	private MapJavaDescriptor() {
		super( Map.class );
	}

	@Override
	public Class<Map> getJavaType() {
		return Map.class;
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
	public MutabilityPlan<Map> getMutabilityPlan() {
		// no clue what this should be, if anything.  null for now...
		return null;
	}

	@Override
	public Comparator<Map> getComparator() {
		// we'd need this for SortedMap support,..
		//		but thats irrelevant for the sqm tests
		return null;
	}

	@Override
	public int extractHashCode(Map value) {
		return value.hashCode();
	}

	@Override
	public boolean areEqual(Map one, Map another) {
		// how deep do we want this to go?  for now I'll leave it to
		//		the Map#equals implementation
		return one == another
				|| ( one != null && one.equals( another ) );
	}

	@Override
	public String extractLoggableRepresentation(Map value) {
		return "{map}";
	}

	@Override
	public String toString(Map value) {
		return value == null ? "<null>" : value.toString();
	}

	@Override
	public Map fromString(String string) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public <X> X unwrap(Map value, Class<X> type, WrapperOptions options) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public <X> Map wrap(X value, WrapperOptions options) {
		throw new UnsupportedOperationException(  );
	}
}
