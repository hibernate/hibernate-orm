/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial.predicate;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.predicate.AbstractNegatableSqmPredicate;
import org.hibernate.spatial.jts.EnvelopeAdapter;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;

/**
 * JPA Spatial Filter {@link Predicate}
 */
class JTSFilterPredicate {

	private Expression<?> geometry;
	private Expression<?> filter;

//require to completely re-implement


}
