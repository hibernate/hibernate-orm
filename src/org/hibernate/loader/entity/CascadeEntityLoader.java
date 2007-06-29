//$Id$
package org.hibernate.loader.entity;

import org.hibernate.MappingException;
import org.hibernate.engine.CascadingAction;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.loader.JoinWalker;
import org.hibernate.persister.entity.OuterJoinLoadable;
import org.hibernate.util.CollectionHelper;

public class CascadeEntityLoader extends AbstractEntityLoader {
	
	public CascadeEntityLoader(
			OuterJoinLoadable persister,
			CascadingAction action,
			SessionFactoryImplementor factory) 
	throws MappingException {
		super(
				persister, 
				persister.getIdentifierType(), 
				factory, 
				CollectionHelper.EMPTY_MAP
			);

		JoinWalker walker = new CascadeEntityJoinWalker(
				persister, 
				action,
				factory
			);
		initFromWalker( walker );

		postInstantiate();
		
		log.debug( "Static select for action " + action + " on entity " + entityName + ": " + getSQLString() );

	}

}
