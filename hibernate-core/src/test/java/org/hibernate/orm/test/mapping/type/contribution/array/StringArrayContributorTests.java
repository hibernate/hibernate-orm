/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.type.contribution.array;

import org.hibernate.boot.model.TypeContributor;
import org.hibernate.dialect.H2Dialect;

import org.hibernate.testing.orm.junit.BootstrapServiceRegistry;
import org.hibernate.testing.orm.junit.BootstrapServiceRegistry.JavaService;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.assertj.core.util.Arrays;

/**
 * @author Steve Ebersole
 */
@BootstrapServiceRegistry(
		javaServices = @JavaService( role = TypeContributor.class, impl = StringArrayTypeContributor.class )
)
@DomainModel(annotatedClasses = Post.class)
@SessionFactory
@RequiresDialect(H2Dialect.class)
public class StringArrayContributorTests {
	@Test
	public void simpleTest(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			// save one with tags
			session.save( new Post( 1, "with tags", "tag1", "tag2" ) );
			// and one without (null)
			session.save( new Post( 2, "no tags" ) );
		} );

		scope.inTransaction( (session) -> {
			session.createQuery( "select p.tags from Post p" ).list();
		} );
	}

	@Test
	public void testAsQueryParameter(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.createQuery( "select p  from Post p where array_contains(:arr, p.title) = true" )
					.setParameter( "arr", Arrays.array( "a", "b" ) )
					.list();
		});
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> session.createQuery( "delete Post" ).executeUpdate() );
	}
}
