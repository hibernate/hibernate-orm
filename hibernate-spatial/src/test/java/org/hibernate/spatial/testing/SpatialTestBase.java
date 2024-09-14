/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial.testing;

import org.hibernate.spatial.testing.datareader.TestSupport;
import org.hibernate.spatial.testing.domain.GeomEntity;
import org.hibernate.spatial.testing.domain.JtsGeomEntity;
import org.hibernate.spatial.testing.domain.SpatialDomainModel;

import org.hibernate.testing.orm.junit.DomainModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

@DomainModel(modelDescriptorClasses = SpatialDomainModel.class)
abstract public class SpatialTestBase
		extends SpatialSessionFactoryAware {

	public abstract TestSupport.TestDataPurpose purpose();

	@BeforeEach
	public void beforeEach() {
		scope.inTransaction( session -> super.entities(
						JtsGeomEntity.class,
						purpose()
				)
				.forEach( session::persist ) );
		scope.inTransaction( session -> super.entities(
				GeomEntity.class,
				purpose()
		).forEach( session::persist ) );
	}

	@AfterEach
	public void cleanup() {
		scope.inTransaction( session -> session.createMutationQuery( "delete from GeomEntity" )
				.executeUpdate() );
		scope.inTransaction( session -> session.createMutationQuery( "delete from JtsGeomEntity" )
				.executeUpdate() );
	}


}
