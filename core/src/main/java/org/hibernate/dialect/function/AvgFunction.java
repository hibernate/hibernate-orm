/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.dialect.function;

import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.MappingException;
import org.hibernate.QueryException;
import org.hibernate.engine.Mapping;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.type.Type;

/**
 * The basic JPA spec compliant definition po<tt>AVG</tt> aggregation function.
 *
 * @author Steve Ebersole
 */
public class AvgFunction implements SQLFunction {
	public final Type getReturnType(Type columnType, Mapping mapping) throws QueryException {
		int[] sqlTypes;
		try {
			sqlTypes = columnType.sqlTypes( mapping );
		}
		catch ( MappingException me ) {
			throw new QueryException( me );
		}
		if ( sqlTypes.length != 1 ) {
			throw new QueryException( "multiple-column type in avg()" );
		}
		return Hibernate.DOUBLE;
	}

	public final boolean hasArguments() {
		return true;
	}

	public final boolean hasParenthesesIfNoArguments() {
		return true;
	}

	public String render(List args, SessionFactoryImplementor factory) throws QueryException {
		return "avg(" + args.get( 0 ) + ")";
	}

	public final String toString() {
		return "avg";
	}
}
