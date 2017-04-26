/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.domain;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.query.sqm.domain.SqmExpressableTypeEntity;
import org.hibernate.query.sqm.produce.internal.hql.navigable.TreatedNavigableReference;
import org.hibernate.query.sqm.tree.from.SqmDowncast;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSqmNavigableReference implements SqmNavigableReference {
	private Map<SqmExpressableTypeEntity,SqmDowncast> downcastMap;

	@Override
	public SqmNavigableReference treatAs(SqmExpressableTypeEntity target) {
		SqmDowncast downcast = null;
		if ( downcastMap == null ) {
			downcastMap = new HashMap<>();
		}
		else {
			downcast = downcastMap.get( target );
		}

		if ( downcast == null ) {
			downcast = new SqmDowncast( target );
			downcastMap.put( target, downcast );
		}

		return new TreatedNavigableReference( this, target );
	}

	@Override
	public void addDowncast(SqmDowncast downcast) {
		SqmDowncast existing = null;
		if ( downcastMap == null ) {
			downcastMap = new HashMap<>();
		}
		else {
			existing = downcastMap.get( downcast.getTargetType() );
		}

		final SqmDowncast toPut;
		if ( existing == null ) {
			toPut = downcast;
		}
		else {
			// todo : depending on how we ultimately define TreatedAsInformation defines what exactly needs to happen here..
			//		for now, just keep the existing...
			toPut = existing;

			// but set its intrinsic flag if needed...
			if ( downcast.isIntrinsic() ) {
				toPut.makeIntrinsic();
			}
		}

		downcastMap.put( downcast.getTargetType(), toPut );
	}

	@Override
	public Collection<SqmDowncast> getDowncasts() {
		if ( downcastMap == null ) {
			return Collections.emptySet();
		}
		else {
			return downcastMap.values();
		}
	}
}
