/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.predicate;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;

import org.geolatte.geom.Envelope;
import org.geolatte.geom.Geometry;
import org.geolatte.geom.Polygon;
import org.geolatte.geom.Position;
import org.geolatte.geom.PositionSequence;
import org.geolatte.geom.PositionSequenceBuilders;
import org.geolatte.geom.crs.CoordinateReferenceSystem;

/**
 * {@link JTSFilterPredicate}, but for geolatte-geom.
 *
 * @deprecated Use {@link org.hibernate.spatial.criteria.GeolatteSpatialCriteriaBuilder GeolatteSpatialCriteriaBuilder} instead
 */
@Deprecated(since = "6.2")
public class GeolatteFilterPredicate {

	private final Expression<? extends Geometry> geometry;
	private final Expression<? extends Geometry> filter;

	public GeolatteFilterPredicate(
			CriteriaBuilder criteriaBuilder, Expression<? extends Geometry> geometry,
			Expression<? extends Geometry> filter) {

		this.geometry = geometry;
		this.filter = filter;
	}

	public GeolatteFilterPredicate(
			CriteriaBuilder criteriaBuilder, Expression<? extends Geometry> geometry,
			Geometry filter) {
		this( criteriaBuilder, geometry, criteriaBuilder.literal( filter )
		);
	}

	public GeolatteFilterPredicate(
			CriteriaBuilder criteriaBuilder, Expression<? extends Geometry> geometry,
			Envelope envelope) {
		this( criteriaBuilder, geometry, fromEnvelope( envelope )
		);
	}

//	@Override
//	public void registerParameters(ParameterRegistry registry) {
//		Helper.possibleParameter( geometry, registry );
//		Helper.possibleParameter( filter, registry );
//	}
//
//	@Override
//	public String render(boolean isNegated, RenderingContext renderingContext) {
//		String geometryParameter = ( (Renderable) geometry ).render( renderingContext );
//		String filterParameter = ( (Renderable) filter ).render( renderingContext );
//		Dialect dialect = renderingContext.getDialect();
//		if ( !( dialect instanceof SpatialDialect ) ) {
//			throw new IllegalStateException( "Dialect must be spatially enabled dialect" );
//		}
//		if ( dialect instanceof WithCustomJPAFilter ) {
//			return ( (WithCustomJPAFilter) dialect ).filterExpression( geometryParameter, filterParameter );
//		}
//		else {
//			return SpatialFunction.filter.name() + "(" + geometryParameter + ", " + filterParameter + ") = true";
//		}
//	}

	private static <P extends Position> Polygon<P> fromEnvelope(Envelope<P> envelope) {
		CoordinateReferenceSystem<P> crs = envelope.getCoordinateReferenceSystem();

		P lowerLeft = envelope.lowerLeft();
		P upperLeft = envelope.upperLeft();
		P upperRight = envelope.upperRight();
		P lowerRight = envelope.lowerRight();

		PositionSequence<P> positionSequence = PositionSequenceBuilders.fixedSized(
				5,
				crs.getPositionClass()
		)
				.add( lowerLeft )
				.add( upperLeft )
				.add( upperRight )
				.add( lowerRight )
				.add( lowerLeft )
				.toPositionSequence();

		return new Polygon<>( positionSequence, crs );
	}
}
