/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.sql.hand.hbm;

import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.query.sql.spi.NativeQueryImplementor;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.NotImplementedYet;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.anyOf;
import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(
		annotatedClasses = { Group.class, Employee.class },
		xmlMappings = "mappings/sql/hbm/resultset/PersonResults.hbm.xml"
)
@SessionFactory
public class HbmResultSetMappingTests {

	@Test
	@NotImplementedYet( strict = false )
	public void testImplicitUsage(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final NativeQueryImplementor namedNativeQuery = session.getNamedNativeQuery( "group-members" );
			namedNativeQuery.list();
		} );
	}

	@Test
	@NotImplementedYet( strict = false )
	public void testExplicitUsage(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			//noinspection rawtypes
			final NativeQueryImplementor namedNativeQuery = session.getNamedNativeQuery( "group-members", "explicit-results-mapping" );
			namedNativeQuery.list();
		} );
	}
}
