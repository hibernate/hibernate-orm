/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.internal.hql.navigable;

import org.hibernate.query.sqm.produce.spi.ResolutionContext;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableSourceReference;

/**
 * @author Steve Ebersole
 */
public class PathResolverSelectClauseImpl extends PathResolverBasicImpl {
	public PathResolverSelectClauseImpl(ResolutionContext context) {
		super( context );
	}

	@Override
	protected boolean shouldRenderTerminalAttributeBindingAsJoin() {
		return true;
	}

	@Override
	protected SqmNavigableReference resolveTerminalAttributeBinding(SqmNavigableSourceReference sourceBinding, String terminalName) {
		SqmNavigableReference attrBinding = context().getParsingContext().findOrCreateNavigableBinding( sourceBinding, terminalName );
		resolveAttributeJoinIfNot( attrBinding );
		return attrBinding;
	}
}
