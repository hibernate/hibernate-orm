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
package org.hibernate.test.legacy;

import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.DefaultNamingStrategy;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.PostgreSQL81Dialect;
import org.hibernate.hql.internal.classic.ClassicQueryTranslatorFactory;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.type.Type;
import org.junit.Before;

/**
 * @author Steve Ebersole
 */
public abstract class LegacyTestCase extends BaseCoreFunctionalTestCase {
	public static final String USE_ANTLR_PARSER_PROP = "legacy.use_antlr_hql_parser";

	private boolean useAntlrParser;

	@Before
	public void checkAntlrParserSetting() {
		useAntlrParser = Boolean.valueOf( extractFromSystem( USE_ANTLR_PARSER_PROP ) );
	}

	protected boolean supportsLockingNullableSideOfJoin(Dialect dialect) {
		// db2 and pgsql do *NOT*
		return ! ( DB2Dialect.class.isInstance( dialect ) || PostgreSQL81Dialect.class.isInstance( dialect ));
	}

	protected static String extractFromSystem(String systemPropertyName) {
		try {
			return System.getProperty( systemPropertyName );
		}
		catch( Throwable t ) {
			return null;
		}
	}

	@Override
	protected void cleanupTestData() throws Exception {
		Session s = openSession();
		s.beginTransaction();
		List list = s.createQuery( "from java.lang.Object" ).list();
		for ( Object obj : list ) {
			s.delete( obj );
		}
		s.getTransaction().commit();
		s.close();
	}

	@Override
	public void configure(Configuration cfg) {
		super.configure( cfg );
		if ( !useAntlrParser ) {
			cfg.setProperty( Environment.QUERY_TRANSLATOR, ClassicQueryTranslatorFactory.class.getName() );
			try {
				String dialectTrueRepresentation = Dialect.getDialect().toBooleanValueString( true );
				// if this call succeeds, then the dialect is saying to represent true/false as int values...
				Integer.parseInt( dialectTrueRepresentation );
				String subs = cfg.getProperties().getProperty( Environment.QUERY_SUBSTITUTIONS );
				if ( subs == null ) {
					subs = "";
				}
				if ( StringHelper.isEmpty( subs ) ) {
					subs = "true=1, false=0";
				}
				else {
					subs += ", true=1, false=0";
				}
				cfg.getProperties().setProperty( Environment.QUERY_SUBSTITUTIONS, subs );
				cfg.setNamingStrategy( DefaultNamingStrategy.INSTANCE );
			}
			catch( NumberFormatException nfe ) {
				// the Integer#parseInt call failed...
			}
		}
	}

	protected int doDelete(Session session, String queryString) {
		return doDelete( session, session.createQuery( queryString ) );
	}

	protected int doDelete(Session session, String queryString, Object param, Type paramType) {
		Query query = session.createQuery( queryString )
				.setParameter( 0, param, paramType );
		return doDelete( session, query );
	}

	protected int doDelete(Session session, Query selectQuery) {
		int count = 0;
		for ( Object o : selectQuery.list() ) {
			session.delete( o );
			count++;
		}
		return count;
	}
}
