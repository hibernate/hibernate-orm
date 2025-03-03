/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.integration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.spatial.testing.IsSupportedBySpatial;
import org.hibernate.spatial.testing.SpatialSessionFactoryAware;
import org.hibernate.spatial.testing.datareader.TestDataElement;
import org.hibernate.spatial.testing.domain.GeomEntity;
import org.hibernate.spatial.testing.domain.GeomEntityLike;
import org.hibernate.spatial.testing.domain.JtsGeomEntity;
import org.hibernate.spatial.testing.domain.SpatialDomainModel;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.persistence.Query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;


/**
 * Created by Karel Maesen, Geovise BVBA on 15/02/2018.
 */
@DomainModel(modelDescriptorClasses = SpatialDomainModel.class)
@RequiresDialectFeature(feature = IsSupportedBySpatial.class)
@SessionFactory
public class StoreAndRetrieveTests extends SpatialSessionFactoryAware {

	private Map<Integer, Object> stored = new HashMap<>();

	@ParameterizedTest
	@ValueSource(classes = { GeomEntity.class, JtsGeomEntity.class })
	public void testStoringGeomEntity(final Class entityClass) {

		//check whether we retrieve exactly what we store
		scope.inTransaction( session -> storeTestObjects( session, entityClass ) );
		scope.inTransaction( session -> retrieveAndCompare( session, entityClass ) );
	}

	@AfterEach
	public void cleanTables(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createQuery( "delete from GeomEntity" ).executeUpdate() );
		scope.inTransaction( session -> session.createQuery( "delete from JtsGeomEntity" ).executeUpdate() );
	}

	@SuppressWarnings("unchecked")
	private void retrieveAndCompare(SessionImplementor session, Class entityClass) {
		Query query = session.createQuery( "from " + entityClass.getCanonicalName() );
		List results = query.getResultList();
		results.stream().forEach( this::isInStored );
	}

	private void isInStored(Object entity) {
		Object input = stored.get( ( (GeomEntityLike) entity ).getId() );
		assertEquals( entity, input );
	}

	@Test
	public void testStoringNullGeometries(SessionFactoryScope scope) {
		scope.inTransaction( this::storeNullGeometry );
		scope.inTransaction( this::retrieveAndCompareNullGeometry );
	}

	private void storeTestObjects(SessionImplementor session, Class entityClass) {
		for ( TestDataElement element : testData ) {
			GeomEntityLike entity = createFrom( element, entityClass, session.getJdbcServices().getDialect() );
			stored.put( entity.getId(), entity );
			session.persist( entity );
		}
	}

	private GeomEntityLike createFrom(TestDataElement element, Class entityClass, Dialect dialect) {
		if ( entityClass.equals( GeomEntity.class ) ) {
			return GeomEntity.createFrom( element, dialect );
		}
		else {
			return JtsGeomEntity.createFrom( element, dialect );
		}
	}

	private void storeNullGeometry(SessionImplementor session) {
		GeomEntity entity = new GeomEntity();
		entity.setId( 1 );
		entity.setType( "NULL Test" );
		session.persist( entity );
	}

	private void retrieveAndCompareNullGeometry(SessionImplementor session) {
		GeomEntity entity = session.createQuery( "from GeomEntity", GeomEntity.class ).getResultList().get( 0 );
		assertEquals( "NULL Test", entity.getType() );
		assertEquals( 1, entity.getId() );
		assertNull( entity.getGeom() );
	}


}
