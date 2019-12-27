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

/**
 * JPA Criteria API {@link Predicate} equivalent of {@link SpatialFilter}.
 */
public class FilterPredicate extends AbstractSimplePredicate implements Serializable {
	private static final Pattern REGEX_PATTERN_PARAMETER_PLACEHOLDER = Pattern.compile( "\\?" );

	private final Expression<? extends Geometry> geometry;
	private final Expression<? extends Geometry> filter;

	public FilterPredicate(
			CriteriaBuilder criteriaBuilder, Expression<? extends Geometry> geometry,
			Expression<? extends Geometry> filter) {
		super( (CriteriaBuilderImpl) criteriaBuilder );
		this.geometry = geometry;
		this.filter = filter;
	}

	public FilterPredicate(
			CriteriaBuilder criteriaBuilder, Expression<? extends Geometry> geometry,
			Geometry filter) {
		this( criteriaBuilder, geometry,
				criteriaBuilder.literal( filter )
		);
	}

	public FilterPredicate(
			CriteriaBuilder criteriaBuilder, Expression<? extends Geometry> geometry,
			Envelope envelope, int srid) {
		this( criteriaBuilder, geometry,
				EnvelopeAdapter.toPolygon( envelope, srid )
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
		if ( dialect instanceof SpatialDialect ) {
			final SpatialDialect seDialect = (SpatialDialect) dialect;
			String sql = seDialect.getSpatialFilterExpression( geometryParameter );
			return REGEX_PATTERN_PARAMETER_PLACEHOLDER.matcher( sql )
					.replaceFirst( filterParameter );
		}
		else {
			throw new IllegalStateException( "Dialect must be spatially enabled dialect" );
		}
	}
}
