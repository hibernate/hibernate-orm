/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.procedure.internal;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.hibernate.LockMode;
import org.hibernate.MappingException;
import org.hibernate.engine.ResultSetMappingDefinition;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryReturn;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryRootReturn;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.loader.custom.sql.SQLQueryReturnProcessor;
import org.hibernate.persister.entity.EntityPersister;

/**
 * @author Steve Ebersole
 */
public class Util {
	private Util() {
	}

	/**
	 * Makes a copy of the given query return array.
	 *
	 * @param queryReturns The returns to copy
	 *
	 * @return The copy
	 */
	static NativeSQLQueryReturn[] copy(NativeSQLQueryReturn[] queryReturns) {
		if ( queryReturns == null ) {
			return new NativeSQLQueryReturn[0];
		}

		final NativeSQLQueryReturn[] copy = new NativeSQLQueryReturn[ queryReturns.length ];
		System.arraycopy( queryReturns, 0, copy, 0, queryReturns.length );
		return copy;
	}

	public static Set<String> copy(Set<String> synchronizedQuerySpaces) {
		return CollectionHelper.makeCopy( synchronizedQuerySpaces );
	}

	public static Map<String,Object> copy(Map<String, Object> hints) {
		return CollectionHelper.makeCopy( hints );
	}

	public static interface ResultSetMappingResolutionContext {
		public SessionFactoryImplementor getSessionFactory();
		public ResultSetMappingDefinition findResultSetMapping(String name);
		public void addQueryReturns(NativeSQLQueryReturn... queryReturns);
		public void addQuerySpaces(String... spaces);
	}

	public static void resolveResultSetMappings(ResultSetMappingResolutionContext context, String... resultSetMappingNames) {
		for ( String resultSetMappingName : resultSetMappingNames ) {
			final ResultSetMappingDefinition mapping = context.findResultSetMapping( resultSetMappingName );
			if ( mapping == null ) {
				throw new MappingException( "Unknown SqlResultSetMapping [" + resultSetMappingName + "]" );
			}

			context.addQueryReturns( mapping.getQueryReturns() );

			final SQLQueryReturnProcessor processor =
					new SQLQueryReturnProcessor( mapping.getQueryReturns(), context.getSessionFactory() );
			final SQLQueryReturnProcessor.ResultAliasContext processResult = processor.process();
			context.addQuerySpaces( processResult.collectQuerySpaces() );
		}
	}

	public static interface ResultClassesResolutionContext {
		public SessionFactoryImplementor getSessionFactory();
		public void addQueryReturns(NativeSQLQueryReturn... queryReturns);
		public void addQuerySpaces(String... spaces);
	}

	public static void resolveResultClasses(ResultClassesResolutionContext context, Class... resultClasses) {
		int i = 1;
		for ( Class resultClass : resultClasses ) {
			context.addQueryReturns(
					new NativeSQLQueryRootReturn( "alias" + i, resultClass.getName(), LockMode.READ )
			);
			try {
				final EntityPersister persister = context.getSessionFactory().getEntityPersister( resultClass.getName() );
				context.addQuerySpaces( (String[]) persister.getQuerySpaces() );
			}
			catch (Exception ignore) {

			}
		}
	}
}
