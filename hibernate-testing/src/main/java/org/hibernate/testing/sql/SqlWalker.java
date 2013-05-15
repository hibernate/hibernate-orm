/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
 */
package org.hibernate.testing.sql;

import java.lang.reflect.Field;
import java.util.List;

/**
 *
 */
public class SqlWalker {

	public static final SqlWalker INSTANCE = new SqlWalker();

	protected SqlWalker() {
	}

	public boolean walk( SqlVisitor visitor, Object object ) {
		return walk( visitor, object, null, null, -1 );
	}

	private boolean walk( SqlVisitor visitor, Object object, SqlObject parent, Field field, int index ) {
		if ( !visitor.visit( object, parent, field, index ) ) {
			return false;
		}
		if ( object instanceof List ) {
			List< Object > list = ( List< Object > ) object;
			if ( !visitor.preVisitElements( list, parent, field, index ) ) {
				return false;
			}
			for ( int ndx = 0; ndx < list.size(); ++ndx ) {
				if ( !walk( visitor, list.get( ndx ), parent, field, ndx ) ) {
					return false;
				}
			}
			if ( !visitor.postVisitElements( list, parent, field, index ) ) {
				return false;
			}
		} else if ( object instanceof SqlObject && !( object instanceof Reference ) ) {
			SqlObject sqlObj = ( SqlObject ) object;
			if ( !visitor.preVisitFields( sqlObj, parent, field, index ) ) {
				return false;
			}
			for ( Field fld : object.getClass().getFields() ) {
				try {
					if ( !walk( visitor, fld.get( object ), sqlObj, fld, -1 ) ) {
						return false;
					}
				} catch ( IllegalAccessException error ) {
					throw new RuntimeException( error );
				}
			}
			if ( !visitor.postVisitFields( sqlObj, parent, field, index ) ) {
				return false;
			}
		}
		return true;
	}
}
