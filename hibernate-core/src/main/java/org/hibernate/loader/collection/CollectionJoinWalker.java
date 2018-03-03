/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.collection;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.loader.JoinWalker;

/**
 * Superclass of walkers for collection initializers
 * 
 * @see CollectionLoader
 * @see OneToManyJoinWalker
 * @see BasicCollectionJoinWalker
 * @author Gavin King
 */
public abstract class CollectionJoinWalker extends JoinWalker {
	public CollectionJoinWalker(SessionFactoryImplementor factory, LoadQueryInfluencers loadQueryInfluencers) {
		super( factory, loadQueryInfluencers );
	}

	protected StringBuilder whereString(String alias, String[] columnNames, String subselect, int batchSize) {
		if (subselect==null) {
			return whereString(alias, columnNames, batchSize);
		}
		else {
			StringBuilder buf = new StringBuilder();
			if (columnNames.length>1) {
				buf.append('(');
			}
			buf.append( String.join(", ", StringHelper.qualify(alias, columnNames) ) );
			if (columnNames.length>1) {
				buf.append(')');
			}
			buf.append(" in ")
				.append('(')
				.append(subselect) 
				.append(')');
			return buf;
		}
	}
}
