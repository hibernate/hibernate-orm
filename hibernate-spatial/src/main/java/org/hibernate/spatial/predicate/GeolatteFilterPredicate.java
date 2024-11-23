/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial.predicate;

import java.io.Serializable;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;

import org.hibernate.dialect.Dialect;
import org.hibernate.query.criteria.internal.CriteriaBuilderImpl;
import org.hibernate.query.criteria.internal.ParameterRegistry;
import org.hibernate.query.criteria.internal.Renderable;
import org.hibernate.query.criteria.internal.compile.RenderingContext;
import org.hibernate.query.criteria.internal.predicate.AbstractSimplePredicate;
import org.hibernate.spatial.SpatialDialect;
import org.hibernate.spatial.SpatialFunction;
import org.hibernate.spatial.dialect.WithCustomJPAFilter;

import org.geolatte.geom.Envelope;
import org.geolatte.geom.Geometry;
import org.geolatte.geom.Polygon;
import org.geolatte.geom.Position;
import org.geolatte.geom.PositionSequence;
import org.geolatte.geom.PositionSequenceBuilders;
import org.geolatte.geom.crs.CoordinateReferenceSystem;

/**
 * {@link JTSFilterPredicate}, but for geolatte-geom.
 */
public class GeolatteFilterPredicate extends AbstractSimplePredicate implements Serializable {

	private final Expression<? extends Geometry> geometry;
	private final Expression<? extends Geometry> filter;

	public GeolatteFilterPredicate(
			CriteriaBuilder criteriaBuilder, Expression<? extends Geometry> geometry,
			Expression<? extends Geometry> filter) {
		super( (CriteriaBuilderImpl) criteriaBuilder );
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

	@Override
	public void registerParameters(ParameterRegistry registry) {
		Helper.possibleParameter( geometry, registry );
		Helper.possibleParameter( filter, registry );
	}

	@Override
	public String render(boolean isNegated, RenderingContext renderingContext) {
		String geometryParameter = ( (Renderable) geometry ).render( renderingContext );
		String filterParameter = ( (Renderable) filter ).render( renderingContext );
		Dialect dialect = renderingContext.getDialect();
		if ( !( dialect instanceof SpatialDialect ) ) {
			throw new IllegalStateException( "Dialect must be spatially enabled dialect" );
		}
		if ( dialect instanceof WithCustomJPAFilter ) {
			return ( (WithCustomJPAFilter) dialect ).filterExpression( geometryParameter, filterParameter );
		}
		else {
			return SpatialFunction.filter.name() + "(" + geometryParameter + ", " + filterParameter + ") = true";
		}
	}

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
