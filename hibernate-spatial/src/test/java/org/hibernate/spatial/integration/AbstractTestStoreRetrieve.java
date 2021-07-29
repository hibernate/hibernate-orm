/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.integration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.Query;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.spatial.testing.GeometryEquality;
import org.hibernate.spatial.testing.datareader.TestDataElement;
import org.hibernate.spatial.testing.domain.GeomEntity;
import org.hibernate.spatial.testing.domain.GeomEntityLike;
import org.hibernate.spatial.testing.domain.SpatialDomainModel;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;


/**
 * Created by Karel Maesen, Geovise BVBA on 15/02/2018.
 */
@DomainModel(modelDescriptorClasses = SpatialDomainModel.class)
@SessionFactory
public abstract class AbstractTestStoreRetrieve<G, E extends GeomEntityLike<G>>
		extends SpatialTestDataProvider {

	protected abstract GeometryEquality<G> getGeometryEquality();

	protected abstract Class<E> getGeomEntityClass();

	protected abstract E createFrom(TestDataElement element, Dialect dialect);

	private Map<Integer, E> stored = new HashMap<>();

	@Test
	public void testStoringGeomEntity(SessionFactoryScope scope) {

		//check whether we retrieve exactly what we store
		scope.inTransaction( this::storeTestObjects );
		scope.inTransaction( this::retrieveAndCompare );
	}

	@AfterEach
	public void cleanTables(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createQuery( "delete from " + this.getGeomEntityClass()
				.getCanonicalName() ).executeUpdate() );
	}

	@SuppressWarnings("unchecked")
	private void retrieveAndCompare(SessionImplementor session) {
		Query query = session.createQuery( "from " + this.getGeomEntityClass().getCanonicalName() );
		List<E> results = (List<E>) query.getResultList();
		results.stream().forEach( this::isInStored );
	}

	private void isInStored(E entity) {
		E input = stored.get( entity.getId() );
		assertEquals( entity, input );
	}

	@Test
	public void testStoringNullGeometries(SessionFactoryScope scope) {
		scope.inTransaction( this::storeNullGeometry );
		scope.inTransaction( this::retrieveAndCompareNullGeometry );
	}

	private void storeTestObjects(SessionImplementor session) {
		for ( TestDataElement element : testData ) {
			E entity = createFrom( element, session.getJdbcServices().getDialect() );
			stored.put( entity.getId(), entity );
			session.save( entity );
		}
	}

	private void storeNullGeometry(SessionImplementor session) {
		GeomEntity entity = new GeomEntity();
		entity.setId( 1 );
		entity.setType( "NULL Test" );
		session.save( entity );
	}

	private void retrieveAndCompareNullGeometry(SessionImplementor session) {
		GeomEntity entity = session.createQuery( "from GeomEntity", GeomEntity.class ).getResultList().get( 0 );
		assertEquals( "NULL Test", entity.getType() );
		assertEquals( 1, entity.getId() );
		assertNull( entity.getGeom() );
	}
}
