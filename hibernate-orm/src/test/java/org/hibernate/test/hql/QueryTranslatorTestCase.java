/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.hql;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;

import junit.framework.ComparisonFailure;

import org.hibernate.MappingException;
import org.hibernate.QueryException;
import org.hibernate.engine.query.spi.HQLQueryPlan;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.hql.internal.ast.ASTQueryTranslatorFactory;
import org.hibernate.hql.internal.ast.QueryTranslatorImpl;
import org.hibernate.hql.internal.ast.util.ASTPrinter;
import org.hibernate.hql.internal.classic.ClassicQueryTranslatorFactory;
import org.hibernate.hql.spi.QueryTranslator;
import org.hibernate.hql.spi.QueryTranslatorFactory;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.type.Type;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public abstract class QueryTranslatorTestCase extends BaseCoreFunctionalTestCase {
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

	@Override
	public boolean createSchema() {
		return false;
	}

	@Override
	public boolean rebuildSessionFactoryOnError() {
		return false;
	}

	public void assertTranslation(String hql) throws QueryException, MappingException {
		assertTranslation( hql, null );
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

	protected void assertTranslation(String hql, Map replacements, boolean scalar, String sql) {
		SessionFactoryImplementor factory = sessionFactory();

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
			oldQueryTranslator = classic.createQueryTranslator( hql, hql, Collections.EMPTY_MAP, factory, null );
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

	}

	protected QueryTranslatorImpl createNewQueryTranslator(String hql, Map replacements, boolean scalar) {
		SessionFactoryImplementor factory = sessionFactory();
		return createNewQueryTranslator( hql, replacements, scalar, factory );
	}

	private QueryTranslatorImpl createNewQueryTranslator(String hql, Map replacements, boolean scalar, SessionFactoryImplementor factory) {
		QueryTranslatorFactory ast = new ASTQueryTranslatorFactory();
		QueryTranslatorImpl newQueryTranslator = ( QueryTranslatorImpl ) ast.createQueryTranslator( hql, hql, Collections.EMPTY_MAP, factory, null );
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
		return new HQLQueryPlan( hql, scalar, Collections.EMPTY_MAP, sessionFactory() );
	}

	protected HQLQueryPlan createQueryPlan(String hql) {
		return createQueryPlan( hql, false );
	}

	@Deprecated
	protected SessionFactoryImplementor getSessionFactoryImplementor() {
		return sessionFactory();
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
		Set<Serializable> oldQuerySpaces = oldQueryTranslator.getQuerySpaces();
		Set<Serializable> querySpaces = newQueryTranslator.getQuerySpaces();
		assertEquals( "Query spaces is not the right size!", oldQuerySpaces.size(), querySpaces.size() );
		for ( Object o : oldQuerySpaces ) {
			assertTrue( "New query space does not contain " + o + "!", querySpaces.contains( o ) );
		}
	}

	protected Exception compileBadHql(String hql, boolean scalar) {
		QueryTranslator newQueryTranslator;
		Map replacements = null;
		Exception newException = null;
		SessionFactoryImplementor factory = sessionFactory();
		try {
			QueryTranslatorFactory ast = new ASTQueryTranslatorFactory();
			newQueryTranslator = ast.createQueryTranslator( hql, hql, Collections.EMPTY_MAP, factory, null );
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
	}

	
	private Map getTokens(String sql) {
		Map<String,Integer> result = new TreeMap<String,Integer>();
		if ( sql == null ) {
			return result;
		}
		result.put( "=", Integer.valueOf( StringHelper.countUnquoted( sql, '=' ) ) );
		StringTokenizer tokenizer = new StringTokenizer( sql, "(),= " );
		while ( tokenizer.hasMoreTokens() ) {
			String fragment = tokenizer.nextToken();
			/*if ( "on".equals(fragment) ) fragment = "and";
			if ( "join".equals(fragment) || "inner".equals(fragment) ) continue;*/
			Integer count = result.get(fragment);
			if ( count == null ) {
				count = Integer.valueOf(1);
			}
			else {
				count = Integer.valueOf( count.intValue() + 1 );
			}
			result.put(fragment, count);
		}
		return result;
	}
}
