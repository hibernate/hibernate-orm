//$Id: CollectionJoinWalker.java 7627 2005-07-24 06:53:06Z oneovthafew $
package org.hibernate.loader.collection;

import java.util.Map;

import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.loader.JoinWalker;
import org.hibernate.util.StringHelper;

/**
 * Superclass of walkers for collection initializers
 * 
 * @see CollectionLoader
 * @see OneToManyJoinWalker
 * @see BasicCollectionJoinWalker
 * @author Gavin King
 */
public abstract class CollectionJoinWalker extends JoinWalker {
	
	public CollectionJoinWalker(SessionFactoryImplementor factory, Map enabledFilters) {
		super( factory, enabledFilters );
	}

	protected StringBuffer whereString(String alias, String[] columnNames, String subselect, int batchSize) {
		if (subselect==null) {
			return super.whereString(alias, columnNames, batchSize);
		}
		else {
			StringBuffer buf = new StringBuffer();
			if (columnNames.length>1) buf.append('(');
			buf.append( StringHelper.join(", ", StringHelper.qualify(alias, columnNames) ) );
			if (columnNames.length>1) buf.append(')');
			buf.append(" in ")
				.append('(')
				.append(subselect) 
				.append(')');
			return buf;
		}
	}
}
