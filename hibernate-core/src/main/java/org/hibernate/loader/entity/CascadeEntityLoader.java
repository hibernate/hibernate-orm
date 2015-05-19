/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.entity;
import org.hibernate.MappingException;
import org.hibernate.engine.spi.CascadingAction;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.loader.JoinWalker;
import org.hibernate.persister.entity.OuterJoinLoadable;

public class CascadeEntityLoader extends AbstractEntityLoader {

	public CascadeEntityLoader(
			OuterJoinLoadable persister,
			CascadingAction action,
			SessionFactoryImplementor factory) throws MappingException {
		super(
				persister,
				persister.getIdentifierType(),
				factory,
				LoadQueryInfluencers.NONE
		);

		JoinWalker walker = new CascadeEntityJoinWalker(
				persister,
				action,
				factory
		);
		initFromWalker( walker );

		postInstantiate();

		if ( LOG.isDebugEnabled() ) {
			LOG.debugf( "Static select for action %s on entity %s: %s", action, entityName, getSQLString() );
		}
	}

}
