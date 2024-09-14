/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.foreignkeys.sorting;

import java.math.BigDecimal;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@DomainModel(annotatedClasses = { A.class, B.class })
@SessionFactory
public class ForeignKeyColumnSortingTests {
	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-16514" )
	public void testDerivedCompositeFk(SessionFactoryScope sfScope) {

		sfScope.inTransaction( (session) -> {
			final B b = new B( "abc", new BigDecimal( 1 ), B.Type.ONE, 1L, "tester" );
			session.persist( b );
			session.persist( new A( b ) );
		} );

		sfScope.inTransaction( (session) -> {
			final A loaded = session.get( A.class, 1 );
			assertThat( loaded ).isNotNull();
			assertThat( loaded.b ).isNotNull();
			assertThat( loaded.b.name ).isEqualTo( "tester" );
		} );
	}
}
