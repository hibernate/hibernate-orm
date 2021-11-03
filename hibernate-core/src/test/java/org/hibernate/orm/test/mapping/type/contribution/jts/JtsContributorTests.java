/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.type.contribution.jts;

import org.hibernate.boot.model.TypeContributor;
import org.hibernate.orm.test.mapping.type.contribution.jts.infrastructure.CustomConnectionProviderInitiator;
import org.hibernate.orm.test.mapping.type.contribution.jts.infrastructure.CustomJdbcServicesInitiator;

import org.hibernate.testing.orm.junit.BootstrapServiceRegistry;
import org.hibernate.testing.orm.junit.BootstrapServiceRegistry.JavaService;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;

/**
 * @author Steve Ebersole
 */
@BootstrapServiceRegistry(
		javaServices = @JavaService( role = TypeContributor.class, impl = JtsTypeContributor.class )
)
@ServiceRegistry(
		initiators = { CustomJdbcServicesInitiator.class, CustomConnectionProviderInitiator.class }
)
@DomainModel( annotatedClasses = FirePoint.class )
@SessionFactory
public class JtsContributorTests {
	@Test
	public void simpleTest(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			// save one with a Point
			final GeometryFactory geometryFactory = new GeometryFactory();
			session.save( new FirePoint( 1, "alpha", geometryFactory.createPoint( new Coordinate( 1, 2, 3 ) ) ) );
			// and one without (null)
			session.save( new FirePoint( 2, "bravo", null ) );
		} );

		scope.inTransaction( (session) -> {
			session.createQuery( "select fp.coordinate from FirePoint fp" ).list();
		} );
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> session.createQuery( "delete FirePoint" ).executeUpdate() );
	}
}
