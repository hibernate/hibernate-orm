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
import org.hibernate.spatial.testing.domain.SpatialDomainModel;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


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
	public void testStoringNullGeometries() {
		storeNullGeometry();
		retrieveNullGeometry();
	}


	private String createFailureMessage(int id, G storedGeometry, G retrievedGeometry) {
		String expectedText = ( storedGeometry != null ? storedGeometry.toString() : "NULL" );
		String retrievedText = ( retrievedGeometry != null ? retrievedGeometry.toString() : "NULL" );
		return String.format(
				"Equality testsuite-suite failed for %d.%nExpected: %s%nReceived:%s",
				id,
				expectedText,
				retrievedText
		);
	}

	private void storeTestObjects(SessionImplementor session) {
		// Every testsuite-suite instance is committed seperately
		// to improve feedback in case of failure
		for ( TestDataElement element : testData ) {
			E entity = createFrom( element, session.getJdbcServices().getDialect() );
			stored.put( entity.getId(), entity );
			session.save( entity );
		}
	}


	private void storeNullGeometry() {

	}

	private void retrieveNullGeometry() {
	}
}
