/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.classic;
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
