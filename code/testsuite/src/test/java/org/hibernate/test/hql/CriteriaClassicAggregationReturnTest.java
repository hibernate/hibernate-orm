package org.hibernate.test.hql;

import java.util.Collections;

import junit.framework.Test;

import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.function.ClassicCountFunction;
import org.hibernate.dialect.function.ClassicAvgFunction;
import org.hibernate.dialect.function.ClassicSumFunction;
import org.hibernate.hql.ast.QueryTranslatorImpl;
import org.hibernate.hql.QueryTranslator;
import org.hibernate.hql.QueryTranslatorFactory;
import org.hibernate.hql.classic.ClassicQueryTranslatorFactory;
import org.hibernate.Hibernate;
import org.hibernate.junit.functional.FunctionalTestClassTestSuite;

/**
 * {@inheritDoc}
 *
 * @author Steve Ebersole
 */
public class CriteriaClassicAggregationReturnTest extends QueryTranslatorTestCase {

	public CriteriaClassicAggregationReturnTest(String x) {
		super( x );
	}

	public void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.addSqlFunction( "count", new ClassicCountFunction() );
		cfg.addSqlFunction( "avg", new ClassicAvgFunction() );
		cfg.addSqlFunction( "sum", new ClassicSumFunction() );
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( CriteriaClassicAggregationReturnTest.class );
	}

	public void testClassicHQLAggregationReturnTypes() {
		// EJB3: COUNT returns Long
		QueryTranslatorImpl translator = createNewQueryTranslator( "select count(*) from Human h", sfi() );
		assertEquals( "incorrect return type count", 1, translator.getReturnTypes().length );
		assertEquals( "incorrect return type", Hibernate.INTEGER, translator.getReturnTypes()[0] );

		translator = createNewQueryTranslator( "select count(h.height) from Human h", sfi() );
		assertEquals( "incorrect return type count", 1, translator.getReturnTypes().length );
		assertEquals( "incorrect return type", Hibernate.INTEGER, translator.getReturnTypes()[0] );

		// MAX, MIN return the type of the state-field to which they are applied.
		translator = createNewQueryTranslator( "select max(h.height) from Human h", sfi() );
		assertEquals( "incorrect return type count", 1, translator.getReturnTypes().length );
		assertEquals( "incorrect return type", Hibernate.DOUBLE, translator.getReturnTypes()[0] );

		translator = createNewQueryTranslator( "select max(h.id) from Human h", sfi() );
		assertEquals( "incorrect return type count", 1, translator.getReturnTypes().length );
		assertEquals( "incorrect return type", Hibernate.LONG, translator.getReturnTypes()[0] );

		// AVG returns Float integrals, and otherwise the field type.
		translator = createNewQueryTranslator( "select avg(h.height) from Human h", sfi() );
		assertEquals( "incorrect return type count", 1, translator.getReturnTypes().length );
		assertEquals( "incorrect return type", Hibernate.DOUBLE, translator.getReturnTypes()[0] );

		translator = createNewQueryTranslator( "select avg(h.id) from Human h", sfi() );
		assertEquals( "incorrect return type count", 1, translator.getReturnTypes().length );
		assertEquals( "incorrect return type", Hibernate.FLOAT, translator.getReturnTypes()[0] );

		translator = createNewQueryTranslator( "select avg(h.bigIntegerValue) from Human h", sfi() );
		assertEquals( "incorrect return type count", 1, translator.getReturnTypes().length );
		assertEquals( "incorrect return type", Hibernate.BIG_INTEGER, translator.getReturnTypes()[0] );

        // SUM returns underlying type sum
 	    translator = createNewQueryTranslator( "select sum(h.id) from Human h", sfi() );
		assertEquals( "incorrect return type count", 1, translator.getReturnTypes().length );
		assertEquals( "incorrect return type", Hibernate.LONG, translator.getReturnTypes()[0] );

		translator = createNewQueryTranslator( "select sum(h.intValue) from Human h", sfi() );
		assertEquals( "incorrect return type count", 1, translator.getReturnTypes().length );
		assertEquals( "incorrect return type", Hibernate.INTEGER, translator.getReturnTypes()[0] );

		translator = createNewQueryTranslator( "select sum(h.height) from Human h", sfi() );
		assertEquals( "incorrect return type count", 1, translator.getReturnTypes().length );
		assertEquals( "incorrect return type", Hibernate.DOUBLE, translator.getReturnTypes()[0] );

		translator = createNewQueryTranslator( "select sum(h.floatValue) from Human h", sfi() );
		assertEquals( "incorrect return type count", 1, translator.getReturnTypes().length );
		assertEquals( "incorrect return type", Hibernate.FLOAT, translator.getReturnTypes()[0] );

		translator = createNewQueryTranslator( "select sum(h.bigIntegerValue) from Human h", sfi() );
		assertEquals( "incorrect return type count", 1, translator.getReturnTypes().length );
		assertEquals( "incorrect return type", Hibernate.BIG_INTEGER, translator.getReturnTypes()[0] );

		translator = createNewQueryTranslator( "select sum(h.bigDecimalValue) from Human h", sfi() );
		assertEquals( "incorrect return type count", 1, translator.getReturnTypes().length );
		assertEquals( "incorrect return type", Hibernate.BIG_DECIMAL, translator.getReturnTypes()[0] );

		// special case to test classicquery special case handling of count(*)
		QueryTranslator oldQueryTranslator = null;
		String hql = "select count(*) from Human h";
		QueryTranslatorFactory classic = new ClassicQueryTranslatorFactory();
		oldQueryTranslator = classic.createQueryTranslator( hql, hql, Collections.EMPTY_MAP, sfi() );
		oldQueryTranslator.compile( Collections.EMPTY_MAP, true);
		assertEquals( "incorrect return type count", 1, oldQueryTranslator.getReturnTypes().length );
		assertEquals( "incorrect return type", Hibernate.INTEGER, oldQueryTranslator.getReturnTypes()[0] );

	}
}
