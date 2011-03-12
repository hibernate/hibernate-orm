package org.hibernate.test.legacy;

import java.util.Iterator;

import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.DefaultNamingStrategy;
import org.hibernate.cfg.Environment;
import org.hibernate.hql.classic.ClassicQueryTranslatorFactory;
import org.hibernate.testing.junit.functional.FunctionalTestCase;
import org.hibernate.util.StringHelper;
import org.hibernate.dialect.Dialect;
import org.hibernate.classic.Session;
import org.hibernate.type.Type;
import org.hibernate.Query;

/**
 * @author Steve Ebersole
 */
public abstract class LegacyTestCase extends FunctionalTestCase {

	public static final String USE_ANTLR_PARSER_PROP = "legacy.use_antlr_hql_parser";
	private final boolean useAntlrParser;

	public LegacyTestCase(String x) {
		super( x );
		useAntlrParser = Boolean.valueOf( extractFromSystem( USE_ANTLR_PARSER_PROP ) ).booleanValue();
	}

	protected static String extractFromSystem(String systemPropertyName) {
		try {
			return System.getProperty( systemPropertyName );
		}
		catch( Throwable t ) {
			return null;
		}
	}

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

	protected int doDelete(Session session, String queryString, Object[] params, Type[] paramTypes) {
		Query query = session.createQuery( queryString );
		if ( params != null ) {
			for ( int i = 0; i < params.length; i++ ) {
				query.setParameter( i, params[i], paramTypes[i] );
			}
		}
		return doDelete( session, query );
	}

	protected int doDelete(Session session, Query selectQuery) {
		int count = 0;
		Iterator itr = selectQuery.list().iterator();
		while ( itr.hasNext() ) {
			session.delete( itr.next() );
			count++;
		}
		return count;
	}
}
