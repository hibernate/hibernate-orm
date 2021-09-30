/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.testing;

import java.util.Set;

import org.hibernate.spatial.CommonSpatialFunction;
import org.hibernate.spatial.integration.SpatialTestDataProvider;
import org.hibernate.spatial.testing.datareader.TestSupport;
import org.hibernate.spatial.testing.domain.GeomEntity;
import org.hibernate.spatial.testing.domain.JtsGeomEntity;
import org.hibernate.spatial.testing.domain.SpatialDomainModel;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SessionFactoryScopeAware;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

@DomainModel(modelDescriptorClasses = SpatialDomainModel.class)
abstract public class SpatialTestBase
		extends SpatialTestDataProvider implements SessionFactoryScopeAware {

	protected SessionFactoryScope scope;
	protected Set<String> supportedFunctions;

	public abstract TestSupport.TestDataPurpose purpose();

	@Override
	public void injectSessionFactoryScope(SessionFactoryScope scope) {
		this.scope = scope;
		//scope is set to null during test cleanup
		if ( scope != null ) {
			this.supportedFunctions = scope.getSessionFactory()
					.getQueryEngine()
					.getSqmFunctionRegistry()
					.getFunctions()
					.keySet();

		}
	}

	@BeforeEach
	public void beforeEach() {
		scope.inTransaction( session -> super.entities(
						JtsGeomEntity.class,
						purpose()
				)
				.forEach( session::save ) );
		scope.inTransaction( session -> super.entities(
				GeomEntity.class,
				purpose()
		).forEach( session::save ) );
	}

	@AfterEach
	public void cleanup() {
		scope.inTransaction( session -> session.createQuery( "delete from GeomEntity" ).executeUpdate() );
		scope.inTransaction( session -> session.createQuery( "delete from JtsGeomEntity" ).executeUpdate() );
	}

	public boolean isSupported(CommonSpatialFunction function) {
		return supportedFunctions.contains( function.name() );
	}

}
