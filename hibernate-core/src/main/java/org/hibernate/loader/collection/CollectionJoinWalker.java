/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 *
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
			return super.whereString(alias, columnNames, batchSize);
		}
		else {
			StringBuilder buf = new StringBuilder();
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
