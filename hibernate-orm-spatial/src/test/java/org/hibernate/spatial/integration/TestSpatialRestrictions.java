/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.integration;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.jboss.logging.Logger;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Criterion;
import org.hibernate.spatial.HSMessageLogger;
import org.hibernate.spatial.SpatialFunction;
import org.hibernate.spatial.criterion.SpatialRestrictions;
import org.hibernate.spatial.integration.jts.GeomEntity;
import org.hibernate.spatial.testing.SpatialDialectMatcher;
import org.hibernate.spatial.testing.SpatialFunctionalTestCase;

import org.junit.Test;

import org.hibernate.testing.Skip;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.fail;

@Skip(condition = SpatialDialectMatcher.class, message = "No Spatial Dialect")
public class TestSpatialRestrictions extends SpatialFunctionalTestCase {

	private static HSMessageLogger LOG = Logger.getMessageLogger(
			HSMessageLogger.class,
			TestSpatialRestrictions.class.getName()
	);

	protected HSMessageLogger getLogger() {
		return LOG;
	}

	@Test
	public void within() throws SQLException {
		if ( !isSupportedByDialect( SpatialFunction.within ) ) {
			return;
		}
		Map<Integer, Boolean> dbexpected = expectationsFactory.getWithin( expectationsFactory.getTestPolygon() );
		Criterion spatialCriterion = SpatialRestrictions.within( "geom", expectationsFactory.getTestPolygon() );
		retrieveAndCompare( dbexpected, spatialCriterion );
	}

	@Test
	public void filter() throws SQLException {
		if ( !dialectSupportsFiltering() ) {
			return;
		}
		Map<Integer, Boolean> dbexpected = expectationsFactory.getFilter( expectationsFactory.getTestPolygon() );
		Criterion spatialCriterion = SpatialRestrictions.filter( "geom", expectationsFactory.getTestPolygon() );
		retrieveAndCompare( dbexpected, spatialCriterion );
	}

	@Test
	public void contains() throws SQLException {
		if ( !isSupportedByDialect( SpatialFunction.contains ) ) {
			return;
		}
		Map<Integer, Boolean> dbexpected = expectationsFactory.getContains( expectationsFactory.getTestPolygon() );
		Criterion spatialCriterion = SpatialRestrictions.contains( "geom", expectationsFactory.getTestPolygon() );
		retrieveAndCompare( dbexpected, spatialCriterion );
	}

	@Test
	public void crosses() throws SQLException {
		if ( !isSupportedByDialect( SpatialFunction.crosses ) ) {
			return;
		}
		Map<Integer, Boolean> dbexpected = expectationsFactory.getCrosses( expectationsFactory.getTestPolygon() );
		Criterion spatialCriterion = SpatialRestrictions.crosses( "geom", expectationsFactory.getTestPolygon() );
		retrieveAndCompare( dbexpected, spatialCriterion );
	}

	@Test
	public void touches() throws SQLException {
		if ( !isSupportedByDialect( SpatialFunction.touches ) ) {
			return;
		}
		Map<Integer, Boolean> dbexpected = expectationsFactory.getTouches( expectationsFactory.getTestPolygon() );
		Criterion spatialCriterion = SpatialRestrictions.touches( "geom", expectationsFactory.getTestPolygon() );
		retrieveAndCompare( dbexpected, spatialCriterion );
	}

	@Test
	public void disjoint() throws SQLException {
		if ( !isSupportedByDialect( SpatialFunction.disjoint ) ) {
			return;
		}
		Map<Integer, Boolean> dbexpected = expectationsFactory.getDisjoint( expectationsFactory.getTestPolygon() );
		Criterion spatialCriterion = SpatialRestrictions.disjoint( "geom", expectationsFactory.getTestPolygon() );
		retrieveAndCompare( dbexpected, spatialCriterion );
	}

	@Test
	public void eq() throws SQLException {
		if ( !isSupportedByDialect( SpatialFunction.equals ) ) {
			return;
		}
		Map<Integer, Boolean> dbexpected = expectationsFactory.getEquals( expectationsFactory.getTestPolygon() );
		Criterion spatialCriterion = SpatialRestrictions.eq( "geom", expectationsFactory.getTestPolygon() );
		retrieveAndCompare( dbexpected, spatialCriterion );
	}

	@Test
	public void intersects() throws SQLException {
		if ( !isSupportedByDialect( SpatialFunction.intersects ) ) {
			return;
		}
		Map<Integer, Boolean> dbexpected = expectationsFactory.getIntersects( expectationsFactory.getTestPolygon() );
		Criterion spatialCriterion = SpatialRestrictions.intersects( "geom", expectationsFactory.getTestPolygon() );
		retrieveAndCompare( dbexpected, spatialCriterion );
	}

	@Test
	public void overlaps() throws SQLException {
		if ( !isSupportedByDialect( SpatialFunction.overlaps ) ) {
			return;
		}
		Map<Integer, Boolean> dbexpected = expectationsFactory.getOverlaps( expectationsFactory.getTestPolygon() );
		Criterion spatialCriterion = SpatialRestrictions.overlaps( "geom", expectationsFactory.getTestPolygon() );
		retrieveAndCompare( dbexpected, spatialCriterion );
	}

	@Test
	public void dwithin() throws SQLException {
		if ( !isSupportedByDialect( SpatialFunction.dwithin ) ) {
			return;
		}
		Map<Integer, Boolean> dbexpected = expectationsFactory.getDwithin( expectationsFactory.getTestPoint(), 30.0 );
		Criterion spatialCriterion = SpatialRestrictions.distanceWithin(
				"geom",
				expectationsFactory.getTestPoint(),
				30.0
		);
		retrieveAndCompare( dbexpected, spatialCriterion );
	}

	@Test
	public void isEmpty() throws SQLException {
		if ( !isSupportedByDialect( SpatialFunction.isempty ) ) {
			return;
		}
		Map<Integer, Boolean> dbexpected = expectationsFactory.getIsEmpty();
		Criterion spatialCriterion = SpatialRestrictions.isEmpty( "geom" );
		retrieveAndCompare( dbexpected, spatialCriterion );
	}

	@Test
	public void isNotEmpty() throws SQLException {
		if ( !isSupportedByDialect( SpatialFunction.isempty ) ) {
			return;
		}
		Map<Integer, Boolean> dbexpected = expectationsFactory.getIsNotEmpty();
		Criterion spatialCriterion = SpatialRestrictions.isNotEmpty( "geom" );
		retrieveAndCompare( dbexpected, spatialCriterion );
	}

	@Test
	public void havingSRID() throws SQLException {
		if ( !isSupportedByDialect( SpatialFunction.srid ) ) {
			return;
		}
		Map<Integer, Boolean> dbexpected = expectationsFactory.havingSRID( 4326 );
		Criterion spatialCriterion = SpatialRestrictions.havingSRID( "geom", 4326 );
		retrieveAndCompare( dbexpected, spatialCriterion );
		dbexpected = expectationsFactory.havingSRID( 31370 );
		spatialCriterion = SpatialRestrictions.havingSRID( "geom", 31370 );
		retrieveAndCompare( dbexpected, spatialCriterion );
	}

	private void retrieveAndCompare(Map<Integer, Boolean> dbexpected, Criterion spatialCriterion) {
		Session session = null;
		Transaction tx = null;
		try {
			session = openSession();
			tx = session.beginTransaction();
			Criteria criteria = session.createCriteria( GeomEntity.class );
			criteria.add( spatialCriterion );
			compare( dbexpected, criteria.list() );
		}
		finally {
			if ( tx != null ) {
				tx.rollback();
			}
			if ( session != null ) {
				session.close();
			}
		}
	}

	private void compare(Map<Integer, Boolean> dbexpected, List list) {
		int cnt = 0;
		for ( Map.Entry<Integer, Boolean> entry : dbexpected.entrySet() ) {
			if ( entry.getValue() ) {
				cnt++;
				if ( !findInList( entry.getKey(), (List<GeomEntity>) list ) ) {
					fail( String.format( "Expected object with id= %d, but not found in result", entry.getKey() ) );
				}
			}
		}
		assertEquals( cnt, list.size() );
		LOG.info( String.format( "Found %d objects within testsuite-suite polygon.", cnt ) );
	}

	private boolean findInList(Integer id, List<GeomEntity> list) {
		for ( GeomEntity entity : list ) {
			if ( entity.getId().equals( id ) ) {
				return true;
			}
		}
		return false;
	}
}
