// $Id: QueryTranslatorTestCase.java 11361 2007-03-29 12:48:35Z steve.ebersole@jboss.com $
package org.hibernate.test.hql;

import java.sql.Connection;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;

import junit.framework.ComparisonFailure;

import org.hibernate.EntityMode;
import org.hibernate.MappingException;
import org.hibernate.QueryException;
import org.hibernate.testing.junit.functional.FunctionalTestCase;
import org.hibernate.classic.Session;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.query.HQLQueryPlan;
import org.hibernate.hql.QueryTranslator;
import org.hibernate.hql.QueryTranslatorFactory;
import org.hibernate.hql.ast.ASTQueryTranslatorFactory;
import org.hibernate.hql.ast.HqlToken;
import org.hibernate.hql.ast.QueryTranslatorImpl;
import org.hibernate.hql.ast.util.ASTPrinter;
import org.hibernate.hql.classic.ClassicQueryTranslatorFactory;
import org.hibernate.type.Type;
import org.hibernate.util.StringHelper;

/**
 * Test case superclass for testing QueryTranslator implementations.
 *
 * @author josh Dec 6, 2004 8:21:21 AM
 */
public abstract class QueryTranslatorTestCase extends FunctionalTestCase {

	public QueryTranslatorTestCase(String x) {
		super( x );
		// Create an instance of HqlToken, so that it will have an entry point outside the package.  This
		// will stop IDEA's code inspector from suggesting that HqlToken should be package local.
		new HqlToken();
	}

	public String[] getMappings() {
		return new String[] {
				"hql/Animal.hbm.xml",
				"hql/EntityWithCrazyCompositeKey.hbm.xml",
				"hql/CrazyIdFieldNames.hbm.xml",
				"hql/SimpleEntityWithAssociation.hbm.xml",
				"hql/ComponentContainer.hbm.xml",
				"batchfetch/ProductLine.hbm.xml",
				"cid/Customer.hbm.xml",
				"cid/Order.hbm.xml",
				"cid/LineItem.hbm.xml",
				"cid/Product.hbm.xml",
				"legacy/Baz.hbm.xml",
				"legacy/Category.hbm.xml",
				"legacy/Commento.hbm.xml",
				"legacy/Container.hbm.xml",
				"legacy/Custom.hbm.xml",
				"legacy/Eye.hbm.xml",
				"legacy/Fee.hbm.xml",
				"legacy/FooBar.hbm.xml",
				"legacy/Fum.hbm.xml",
				"legacy/Glarch.hbm.xml",
				"legacy/Holder.hbm.xml",
				"legacy/Many.hbm.xml",
				"legacy/Marelo.hbm.xml",
				"legacy/MasterDetail.hbm.xml",
				"legacy/Middle.hbm.xml",
				"legacy/Multi.hbm.xml",
				"legacy/Nameable.hbm.xml",
				"legacy/One.hbm.xml",
				"legacy/Qux.hbm.xml",
				"legacy/Simple.hbm.xml",
				"legacy/SingleSeveral.hbm.xml",
				"legacy/WZ.hbm.xml",
				"legacy/UpDown.hbm.xml",
				"compositeelement/Parent.hbm.xml",
				"onetoone/joined/Person.hbm.xml",
				"any/Properties.hbm.xml"
		};
	}

	public boolean createSchema() {
		return false;
	}

	public boolean recreateSchemaAfterFailure() {
		return false;
	}

	public void assertTranslation(String hql) throws QueryException, MappingException {
		assertTranslation( hql, null );
	}

	public void assertTranslation(String hql, boolean scalar) throws QueryException, MappingException {
		assertTranslation( hql, null, scalar, null );
	}

	protected void assertTranslation(String hql, Map replacements) {
		ComparisonFailure cf = null;
		try {
			assertTranslation( hql, replacements, false, null );
		}
		catch ( ComparisonFailure e ) {
			e.printStackTrace();
			cf = e;
		}
		if ("false".equals(System.getProperty("org.hibernate.test.hql.SkipScalarQuery","false"))) {
			// Run the scalar translation anyway, even if there was a comparison failure.
			assertTranslation( hql, replacements, true, null );
		}
		if (cf != null)
			throw cf;
	}

	protected void runClassicTranslator(String hql) throws Exception {
		SessionFactoryImplementor factory = getSessionFactoryImplementor();
		Map replacements = new HashMap();
		QueryTranslator oldQueryTranslator = null;
		try {
			QueryTranslatorFactory classic = new ClassicQueryTranslatorFactory();
			oldQueryTranslator = classic.createQueryTranslator( hql, hql, Collections.EMPTY_MAP, factory );
			oldQueryTranslator.compile( replacements, false );
		}
		catch ( Exception e ) {
			e.printStackTrace();
			throw e;
		}
		String oldsql = oldQueryTranslator.getSQLString();
		System.out.println( "HQL    : " + hql );
		System.out.println( "OLD SQL: " + oldsql );
	}

	protected void assertTranslation(String hql, Map replacements, boolean scalar, String sql) {
		SessionFactoryImplementor factory = getSessionFactoryImplementor();

		// Create an empty replacements map if we don't have one.
		if ( replacements == null ) {
			replacements = new HashMap();
		}

		// steve -> note that the empty maps here represent the currently enabled filters...
		QueryTranslator oldQueryTranslator = null;
		Exception oldException = null;
		try {
			System.out.println("Compiling with classic QueryTranslator...");
			QueryTranslatorFactory classic = new ClassicQueryTranslatorFactory();
			oldQueryTranslator = classic.createQueryTranslator( hql, hql, Collections.EMPTY_MAP, factory );
			oldQueryTranslator.compile( replacements, scalar );
		}
		catch ( QueryException e ) {
			oldException = e;
		}
		catch ( MappingException e ) {
			oldException = e;
		}

		QueryTranslator newQueryTranslator = null;
		Exception newException = null;
		try {
			System.out.println("Compiling with AST QueryTranslator...");
			newQueryTranslator = createNewQueryTranslator( hql, replacements, scalar );
		}
		catch ( QueryException e ) {
			newException = e;
		}
		catch ( MappingException e ) {
			newException = e;
		}

		// If the old QT threw an exception, the new one should too.
		if ( oldException != null ) {
//			oldException.printStackTrace();
			assertNotNull( "New query translator did *NOT* throw an exception, the old one did : " + oldException, newException );
			assertEquals( oldException.getMessage(), newException.getMessage() );
			return;	// Don't bother with the rest of the assertions.
		}
		else if ( newException != null ) {
			newException.printStackTrace();
			assertNull( "Old query translator did not throw an exception, the new one did", newException );
		}

		// -- check all of the outputs --
		checkSql( oldQueryTranslator, newQueryTranslator, hql, scalar, sql );
		checkQuerySpaces( oldQueryTranslator, newQueryTranslator );
		checkReturnedTypes( oldQueryTranslator, newQueryTranslator );
		checkColumnNames( oldQueryTranslator, newQueryTranslator );

	}

	protected QueryTranslatorImpl createNewQueryTranslator(String hql, Map replacements, boolean scalar) {
		SessionFactoryImplementor factory = getSessionFactoryImplementor();
		return createNewQueryTranslator( hql, replacements, scalar, factory );
	}

	private QueryTranslatorImpl createNewQueryTranslator(String hql, Map replacements, boolean scalar, SessionFactoryImplementor factory) {
		QueryTranslatorFactory ast = new ASTQueryTranslatorFactory();
		QueryTranslatorImpl newQueryTranslator = ( QueryTranslatorImpl ) ast.createQueryTranslator( hql, hql, Collections.EMPTY_MAP, factory );
		newQueryTranslator.compile( replacements, scalar );
		return newQueryTranslator;
	}

	protected QueryTranslatorImpl createNewQueryTranslator(String hql) {
		return createNewQueryTranslator( hql, new HashMap(), false );
	}

	protected QueryTranslatorImpl createNewQueryTranslator(String hql, SessionFactoryImplementor sfimpl) {
		return createNewQueryTranslator( hql, new HashMap(), false, sfimpl );
	}

	protected HQLQueryPlan createQueryPlan(String hql, boolean scalar) {
		return new HQLQueryPlan( hql, scalar, Collections.EMPTY_MAP, getSessionFactoryImplementor() );
	}

	protected HQLQueryPlan createQueryPlan(String hql) {
		return createQueryPlan( hql, false );
	}

	protected SessionFactoryImplementor getSessionFactoryImplementor() {
		SessionFactoryImplementor factory = ( SessionFactoryImplementor ) getSessions();
		if ( factory == null ) {
			throw new NullPointerException( "Unable to create factory!" );
		}
		return factory;
	}

	private void checkColumnNames(QueryTranslator oldQueryTranslator, QueryTranslator newQueryTranslator) {
		// Check the column names.

		String[][] oldColumnNames = oldQueryTranslator.getColumnNames();
		String[][] newColumnNames = newQueryTranslator.getColumnNames();
		/*assertEquals( "Column name array is not the right length!", oldColumnNames.length, newColumnNames.length );
		for ( int i = 0; i < oldColumnNames.length; i++ ) {
			assertEquals( "Column name array [" + i + "] is not the right length!", oldColumnNames[i].length, newColumnNames[i].length );
			for ( int j = 0; j < oldColumnNames[i].length; j++ ) {
				assertEquals( "Column name [" + i + "," + j + "]", oldColumnNames[i][j], newColumnNames[i][j] );
			}
		}*/
	}

	private void checkReturnedTypes(QueryTranslator oldQueryTranslator, QueryTranslator newQueryTranslator) {
		// Check the returned types for a regression.
		Type[] oldReturnTypes = oldQueryTranslator.getReturnTypes();
		Type[] returnTypes = newQueryTranslator.getReturnTypes();
		assertEquals( "Return types array is not the right length!", oldReturnTypes.length, returnTypes.length );
		for ( int i = 0; i < returnTypes.length; i++ ) {
			assertNotNull( returnTypes[i] );
			assertNotNull( oldReturnTypes[i] );
			assertEquals( "Returned types did not match!", oldReturnTypes[i].getReturnedClass(), returnTypes[i].getReturnedClass() );
			System.out.println("returnedType[" + i + "] = " + returnTypes[i] + " oldReturnTypes[" + i + "] = " + oldReturnTypes[i]);
		}
	}

	private void checkQuerySpaces(QueryTranslator oldQueryTranslator, QueryTranslator newQueryTranslator) {
		// Check the query spaces for a regression.
		Set oldQuerySpaces = oldQueryTranslator.getQuerySpaces();
		Set querySpaces = newQueryTranslator.getQuerySpaces();
		assertEquals( "Query spaces is not the right size!", oldQuerySpaces.size(), querySpaces.size() );
		for ( Iterator iterator = oldQuerySpaces.iterator(); iterator.hasNext(); ) {
			Object o = iterator.next();
			assertTrue( "New query space does not contain " + o + "!", querySpaces.contains( o ) );
		}
	}

	protected Exception compileBadHql(String hql, boolean scalar) {
		QueryTranslator newQueryTranslator;
		Map replacements = null;
		Exception newException = null;
		SessionFactoryImplementor factory = getSessionFactoryImplementor();
		try {
			QueryTranslatorFactory ast = new ASTQueryTranslatorFactory();
			newQueryTranslator = ast.createQueryTranslator( hql, hql, Collections.EMPTY_MAP, factory );
			newQueryTranslator.compile( replacements, scalar );
		}
		catch ( QueryException e ) {
			newException = e;
		}
		catch ( MappingException e ) {
			newException = e;
		}
		assertNotNull( "Expected exception from compilation of '" + hql + "'!", newException );
		return newException;
	}

	private void checkSql(QueryTranslator oldQueryTranslator, QueryTranslator newQueryTranslator, String hql, boolean scalar, String sql) {

		String oldsql = oldQueryTranslator.getSQLString();
		String newsql = newQueryTranslator.getSQLString();
		System.out.println( "HQL    : " + ASTPrinter.escapeMultibyteChars(hql) );
		System.out.println( "OLD SQL: " + ASTPrinter.escapeMultibyteChars(oldsql) );
		System.out.println( "NEW SQL: " + ASTPrinter.escapeMultibyteChars(newsql) );
		if ( sql == null ) {
			// Check the generated SQL.                                          ASTPrinter.escapeMultibyteChars(
			assertSQLEquals( "SQL is not the same as the old SQL (scalar=" + scalar + ")", oldsql, newsql );
		}
		else {
			assertSQLEquals( "SQL is not the same as the expected SQL (scalar=" + scalar + ")", sql, newsql );
		}
	}

	private void assertSQLEquals(String message, String oldsql, String newsql) {
		Map oldMap = getTokens(oldsql);
		Map newMap = getTokens(newsql);
		if ( !oldMap.equals(newMap) ) {
			assertEquals(message, oldsql, newsql);			
		}
		
		//String oldsqlStripped = stripExtraSpaces( oldsql );
		//String newsqlStripped = stripExtraSpaces( newsql );
		//assertEquals( message, oldsqlStripped, newsqlStripped );
	}

	
	private Map getTokens(String sql) {
		Map result = new TreeMap();
		if (sql==null) return result;
		result.put( "=", new Integer( StringHelper.countUnquoted(sql, '=') ) );
		StringTokenizer tokenizer = new StringTokenizer( sql, "(),= " );
		while ( tokenizer.hasMoreTokens() ) {
			String fragment = tokenizer.nextToken();
			/*if ( "on".equals(fragment) ) fragment = "and";
			if ( "join".equals(fragment) || "inner".equals(fragment) ) continue;*/
			Integer count = (Integer) result.get(fragment);
			if ( count==null ) {
				count = new Integer(1);
			}
			else {
				count = new Integer( count.intValue() + 1 );
			}
			result.put(fragment, count);
		}
		return result;
	}
	
	private String stripExtraSpaces(String string) {
		if ( string == null ) {
			return null;
		}

		StringBuffer buffer = new StringBuffer( string.length() );
		char[] chars = string.toCharArray();
		int length = chars.length;
		boolean wasSpace = false;
		for ( int i = 0; i < length; i++ ) {
			boolean isSpace = chars[i] == ' ';
			if ( wasSpace && isSpace ) {
				continue;
			}
			else {
				buffer.append( chars[i] );
			}

			wasSpace = isSpace;
		}
//		StringTokenizer tokenizer = new StringTokenizer( string.trim(), " " );
//		while ( tokenizer.hasMoreTokens() ) {
//			final String fragment = tokenizer.nextToken();
//			buffer.append( fragment );
//			buffer.append( " " );
//		}
//
		return buffer.toString();
	}

	private void checkSqlByResultSet(
	        QueryTranslator oldQueryTranslator,
	        QueryTranslator newQueryTranslator,
	        Object[] binds
	) {
		String oldsql = oldQueryTranslator.getSQLString();
		String newsql = newQueryTranslator.getSQLString();

		Session session = openSession();
		Connection connection = session.connection();

		PreparedStatement oldps = null;
		PreparedStatement newps = null;
		ResultSet oldrs = null;
		ResultSet newrs = null;

		try {
			try {
				oldps = connection.prepareStatement( oldsql );
			}
			catch( Throwable t ) {
				fail( "Unable to prepare sql generated by old parser : " + t );
			}
			try {
				newps = connection.prepareStatement( newsql );
			}
			catch( Throwable t ) {
				fail( "Unable to prepare sql generated by new parser : " + t );
			}

			checkBinds(oldps, newps, binds);

			try {
				oldrs = executeQuery( oldps, binds );
			}
			catch( Throwable t ) {
				fail( "Unable to execute sql generated by old parser : " + t );
			}

			try {
				newrs = executeQuery( newps, binds );
			}
			catch( Throwable t ) {
				fail( "Unable to execute sql generated by new parser : " + t );
			}

			checkResults( oldrs, newrs );
		}
		finally {
			// make *sure* the sql resources get cleaned up
			release(oldrs);
			release(newrs);
			release(oldps);
			release(newps);
			release(session);
		}
	}

	private void checkBinds(PreparedStatement oldps, PreparedStatement newps, Object[] binds) {
		// Make sure the binds "feel" ok
		try {
			ParameterMetaData oldBindMetaData = oldps.getParameterMetaData();
			ParameterMetaData newBindMetaData = newps.getParameterMetaData();

			assertEquals( "Different bind parameter count", oldBindMetaData.getParameterCount(), newBindMetaData.getParameterCount() );
			assertEquals( "Incorrect number of binds passed in", oldBindMetaData.getParameterCount(), binds == null ? 0 : binds.length );

			for ( int i = 0, max = oldBindMetaData.getParameterCount(); i < max; i++ ) {
				assertEquals( "Different bind types", oldBindMetaData.getParameterType(i), newBindMetaData.getParameterType(i) );
			}
		}
		catch( Throwable t ) {
			fail( "SQLException comparing binds : " + t );
		}
	}

	private ResultSet executeQuery(PreparedStatement ps, Object[] binds) throws SQLException {
		if ( binds != null ) {
			for ( int i = 0, max = binds.length; i < max; i++ ) {
				ps.setObject( i, binds[i] );
			}
		}

		return ps.executeQuery();
	}

	private void checkResults(ResultSet oldrs, ResultSet newrs) {
		ResultSetMetaData oldmeta = null;
		ResultSetMetaData newmeta = null;
		int colCount = 0;
		Type[] types = null;

		// first compare the metadata from the two results
		try {
			oldmeta = oldrs.getMetaData();
			newmeta = newrs.getMetaData();
			assertEquals( "Different column counts", oldmeta.getColumnCount(), newmeta.getColumnCount() );

			colCount = oldmeta.getColumnCount();
			types = new Type[colCount];

			for ( int i = 1, max = colCount; i < max; i++ ) {
				assertEquals( "Column names were different", oldmeta.getColumnName(i), newmeta.getColumnName(i) );
				assertEquals( "Column types were different", oldmeta.getColumnType(i), newmeta.getColumnType(i) );
				assertEquals( "Java types were different", oldmeta.getColumnClassName(i), newmeta.getColumnClassName(i) );
				types[i] = sfi().getTypeResolver().basic( oldmeta.getColumnClassName(i) );
			}
		}
		catch( Throwable t ) {
			fail( "Error comparing result set metadata" );
		}

		// Then compare the actual results
		try {
			while ( oldrs.next() & newrs.next() ) {
				for ( int i = 1; i < colCount; i++ ) {
					Object oldval = oldrs.getObject(i);
					if ( oldrs.wasNull() ) oldval = null;
					Object newval = newrs.getObject(i);
					if ( newrs.wasNull() ) newval = null;
					checkLogicalEquality( oldval, newval, types[i] );
				}
			}

			// for "better reporting" purposes, make sure both result sets are fully exhausted
			while ( oldrs.next() );
			while ( newrs.next() );

			assertEquals( "Different row counts", oldrs.getRow(), newrs.getRow() );
		}
		catch( Throwable t ) {
			fail( "Error comparing result set structure" );
		}
	}

	private void checkLogicalEquality(Object oldval, Object newval, Type type) {
		if ( oldval == null && newval == null ) {
			// two nulls are logically equivalent here...
			return;
		}
		else {
			assertTrue( "Different result values", type.isEqual(oldval, newval, EntityMode.POJO) );
		}
	}

	private void release(PreparedStatement ps) {
		if ( ps != null ) {
			try {
				ps.close();
			}
			catch( Throwable t ) {}
		}
	}

	private void release(ResultSet rs) {
		if ( rs != null ) {
			try {
				rs.close();
			}
			catch( Throwable t ) {}
		}
	}

	private void release(Session session) {
		if ( session != null ) {
			try {
				session.close();
			}
			catch( Throwable t ) {}
		}
	}
}
