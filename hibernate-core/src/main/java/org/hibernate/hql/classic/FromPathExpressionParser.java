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
package org.hibernate.hql.classic;

import org.hibernate.QueryException;
import org.hibernate.persister.collection.CollectionPropertyNames;
import org.hibernate.type.Type;

public class FromPathExpressionParser extends PathExpressionParser {

	public void end(QueryTranslatorImpl q) throws QueryException {
		if ( !isCollectionValued() ) {
			Type type = getPropertyType();
			if ( type.isEntityType() ) {
				// "finish off" the join
				token( ".", q );
				token( null, q );
			}
			else if ( type.isCollectionType() ) {
				// default to element set if no elements() specified
				token( ".", q );
				token( CollectionPropertyNames.COLLECTION_ELEMENTS, q );
			}
		}
		super.end( q );
	}

	protected void setExpectingCollectionIndex() throws QueryException {
		throw new QueryException( "illegal syntax near collection-valued path expression in from: "  + getCollectionName() );
	}


}
