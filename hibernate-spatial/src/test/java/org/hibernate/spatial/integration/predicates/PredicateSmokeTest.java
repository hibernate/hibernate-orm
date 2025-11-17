/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.integration.predicates;

import java.util.List;

import org.hibernate.dialect.OracleDialect;
import org.hibernate.spatial.predicate.GeolatteSpatialPredicates;
import org.hibernate.spatial.testing.IsSupportedBySpatial;
import org.hibernate.spatial.testing.SpatialSessionFactoryAware;
import org.hibernate.spatial.testing.domain.GeomEntity;
import org.hibernate.spatial.testing.domain.SpatialDomainModel;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Test;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.geolatte.geom.G2D;
import org.geolatte.geom.Polygon;

import static org.geolatte.geom.builder.DSL.g;
import static org.geolatte.geom.builder.DSL.polygon;
import static org.geolatte.geom.builder.DSL.ring;
import static org.geolatte.geom.crs.CoordinateReferenceSystems.WGS84;

@DomainModel(modelDescriptorClasses = SpatialDomainModel.class)
@SessionFactory
@RequiresDialectFeature(feature = IsSupportedBySpatial.class)
@SkipForDialect(dialectClass = OracleDialect.class, majorVersion = 11, reason = "See https://hibernate.atlassian.net/browse/HHH-15669")
public class PredicateSmokeTest extends SpatialSessionFactoryAware {

	Polygon<G2D> poly = polygon(
			WGS84,
			ring( g( 0, 0 ), g( 0, 10 ), g( 10, 10 ), g( 10, 0 ), g( 0, 0 ) )
	);

	@Test
	public void test() {
		scope.inTransaction( (session) -> {
			CriteriaBuilder cb = session.getCriteriaBuilder();
			CriteriaQuery<GeomEntity> cr = cb.createQuery( GeomEntity.class );
			Root<GeomEntity> root = cr.from( GeomEntity.class );
			cr.select( root ).where(
					GeolatteSpatialPredicates.intersects( cb, root.get("geom"), poly )
			);
			List<GeomEntity> resultList = session.createQuery( cr ).getResultList();
		} );
	}

}
