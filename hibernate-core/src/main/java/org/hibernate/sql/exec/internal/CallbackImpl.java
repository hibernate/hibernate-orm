/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.exec.internal;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.ast.spi.AfterLoadAction;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.sql.exec.spi.Callback;

/**
 * @author Christian Beikov
 */
public class CallbackImpl implements Callback {

	private final List<AfterLoadAction> afterLoadActions;

	public CallbackImpl() {
		this.afterLoadActions = new ArrayList<>( 1 );
	}

	@Override
	public void registerAfterLoadAction(AfterLoadAction afterLoadAction) {
		afterLoadActions.add( afterLoadAction );
	}

	@Override
	public void invokeAfterLoadActions(Object entity, EntityMappingType entityMappingType, SharedSessionContractImplementor session) {
		for ( int i = 0; i < afterLoadActions.size(); i++ ) {
			afterLoadActions.get( i ).afterLoad( entity, entityMappingType, session );
		}
	}

	@Override
	public boolean hasAfterLoadActions() {
		return !afterLoadActions.isEmpty();
	}
}
