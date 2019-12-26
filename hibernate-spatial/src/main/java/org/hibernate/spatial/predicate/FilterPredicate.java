/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial.predicate;

import java.io.Serializable;
import java.util.regex.Pattern;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;

import org.hibernate.dialect.Dialect;
import org.hibernate.query.criteria.internal.CriteriaBuilderImpl;
import org.hibernate.query.criteria.internal.ParameterRegistry;
import org.hibernate.query.criteria.internal.Renderable;
import org.hibernate.query.criteria.internal.compile.RenderingContext;
import org.hibernate.query.criteria.internal.predicate.AbstractSimplePredicate;
import org.hibernate.spatial.SpatialDialect;
import org.hibernate.spatial.criterion.SpatialFilter;
import org.hibernate.spatial.jts.EnvelopeAdapter;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;

/**
 * JPA Criteria API {@link Predicate} equivalent of {@link SpatialFilter}.
 */
public class FilterPredicate extends AbstractSimplePredicate implements Serializable {
	private static final Pattern REGEX_PATTERN_PARAMETER_PLACEHOLDER = Pattern.compile( "\\?" );

	private final Expression<? extends Geometry> geometryLeftHandSide;

	private final Expression<? extends Geometry> geometryRightHandSide;

	private final Expression<? extends Polygon> polygon;

	private FilterPredicate(
			CriteriaBuilder criteriaBuilder, Expression<? extends Geometry> geometryLeftHandSide,
			Expression<? extends Geometry> geometryRightHandSide, Expression<? extends Polygon> polygon) {
		super( (CriteriaBuilderImpl) criteriaBuilder );
		this.geometryLeftHandSide = geometryLeftHandSide;
		this.geometryRightHandSide = geometryRightHandSide;
		this.polygon = polygon;
	}

	public static FilterPredicate byGeometry(
			CriteriaBuilder criteriaBuilder, Expression<? extends Geometry> geometryLeftHandSide,
			Expression<? extends Geometry> geometryRightHandSide) {
		return new FilterPredicate( criteriaBuilder, geometryLeftHandSide,
				geometryRightHandSide, null
		);
	}

	public static FilterPredicate byGeometry(
			CriteriaBuilder criteriaBuilder, Expression<? extends Geometry> geometryLeftHandSide,
			Geometry geometryRightHandSide) {
		return byGeometry( criteriaBuilder, geometryLeftHandSide,
				criteriaBuilder.literal( geometryRightHandSide )
		);
	}

	public static FilterPredicate byPolygon(
			CriteriaBuilder criteriaBuilder, Expression<? extends Geometry> geometryLeftHandSide,
			Expression<? extends Polygon> polygon) {
		return new FilterPredicate( criteriaBuilder, geometryLeftHandSide,
				null, polygon
		);
	}

	public static FilterPredicate byPolygon(
			CriteriaBuilder criteriaBuilder, Expression<? extends Geometry> geometryLeftHandSide,
			Polygon polygon) {
		return byPolygon( criteriaBuilder, geometryLeftHandSide,
				criteriaBuilder.literal( polygon )
		);
	}

	public static FilterPredicate byPolygon(
			CriteriaBuilder criteriaBuilder, Expression<? extends Geometry> geometryLeftHandSide,
			Envelope envelope, int srid) {
		return byPolygon( criteriaBuilder, geometryLeftHandSide,
				EnvelopeAdapter.toPolygon( envelope, srid )
		);
	}

	@Override
	public void registerParameters(ParameterRegistry registry) {
		Helper.possibleParameter( geometryLeftHandSide, registry );
		if ( geometryRightHandSide != null ) {
			Helper.possibleParameter( geometryRightHandSide, registry );
		}
		if ( polygon != null ) {
			Helper.possibleParameter( polygon, registry );
		}
	}

	@Override
	public String render(boolean isNegated, RenderingContext renderingContext) {
		String geometryLeftHandSideColumn = ( (Renderable) geometryLeftHandSide ).render( renderingContext );
		Dialect dialect = renderingContext.getDialect();
		if ( dialect instanceof SpatialDialect ) {
			final SpatialDialect seDialect = (SpatialDialect) dialect;
			String sql = seDialect.getSpatialFilterExpression( geometryLeftHandSideColumn );
			if ( geometryRightHandSide != null ) {
				String geometryRightHandSideParameter = ( (Renderable) geometryRightHandSide ).render( renderingContext );
				sql = REGEX_PATTERN_PARAMETER_PLACEHOLDER.matcher( sql )
						.replaceFirst( geometryRightHandSideParameter );
			}
			if ( polygon != null ) {
				String polygonParameter = ( (Renderable) polygon ).render( renderingContext );
				sql = REGEX_PATTERN_PARAMETER_PLACEHOLDER.matcher( sql )
						.replaceFirst( polygonParameter );
			}
			return sql;
		}
		else {
			throw new IllegalStateException( "Dialect must be spatially enabled dialect" );
		}
	}
}
