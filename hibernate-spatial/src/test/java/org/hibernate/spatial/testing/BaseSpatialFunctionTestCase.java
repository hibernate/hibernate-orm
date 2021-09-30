/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.testing;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.hibernate.mapping.PersistentClass;
import org.hibernate.spatial.integration.SpatialTestDataProvider;
import org.hibernate.spatial.testing.domain.GeomEntity;
import org.hibernate.spatial.testing.domain.JtsGeomEntity;
import org.hibernate.spatial.testing.domain.SpatialDomainModel;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SessionFactoryScopeAware;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings("ALL")
@DomainModel(modelDescriptorClasses = SpatialDomainModel.class)
@SessionFactory
public abstract class BaseSpatialFunctionTestCase extends SpatialTestDataProvider
		implements SessionFactoryScopeAware {

	private SessionFactoryScope scope;
	List received;
	List expected;

	@Override
	public void injectSessionFactoryScope(SessionFactoryScope scope) {
		this.scope = scope;
	}

	@BeforeEach
	public void beforeEach() {
		scope.inTransaction( session -> super.entities( JtsGeomEntity.class ).forEach( session::save ) );
		scope.inTransaction( session -> super.entities( GeomEntity.class ).forEach( session::save ) );
	}

	@AfterEach
	public void cleanup() {
		scope.inTransaction( session -> session.createQuery( "delete from GeomEntity" ).executeUpdate() );
		scope.inTransaction( session -> session.createQuery( "delete from JtsGeomEntity" ).executeUpdate() );
	}

	@ParameterizedTest
	@ValueSource(classes = { GeomEntity.class, JtsGeomEntity.class })
	public void testFunction(Class entityClass) {
		PersistentClass geomEntity = scope.getMetadataImplementor().getEntityBinding( entityClass.getCanonicalName() );
		String table = geomEntity.getTable().getName();
		scope.inSession( session -> {
			expected = (List) session.createNativeQuery(
							sqlTemplate().mkNativeSQLString( table ) )
					.getResultList()
					.stream().map( mapper() )
					.collect( Collectors.toList() );
		} );

		scope.inSession( session -> {
			received = (List) session.createQuery( jqlQueryTemplate().mkHQLString( entityClass.getCanonicalName() ) )
					.getResultList()
					.stream()
					.map( mapper() )
					.collect( Collectors.toList() );
		} );
		assertEquals( expected, received );
	}

	protected abstract HQLTemplate jqlQueryTemplate();

	protected abstract NativeSQLTemplate sqlTemplate();

	protected abstract Function<Object, Object> mapper();

}
