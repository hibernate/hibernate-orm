//$Id: HQLTest.java 9873 2006-05-04 13:42:48Z max.andersen@jboss.com $
package org.hibernate.test.hql;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;

import junit.framework.Test;

import org.hibernate.Hibernate;
import org.hibernate.junit.functional.FunctionalTestClassTestSuite;
import org.hibernate.classic.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.hql.QueryTranslator;
import org.hibernate.hql.QueryTranslatorFactory;
import org.hibernate.hql.ast.QueryTranslatorImpl;
import org.hibernate.hql.ast.tree.SelectClause;
import org.hibernate.hql.classic.ClassicQueryTranslatorFactory;

/**
 * Tests cases for ensuring alignment between HQL and Criteria behavior. 
 *
 * @author Max Rydahl Andersen
 */
public class CriteriaHQLAlignmentTest extends QueryTranslatorTestCase {

	public CriteriaHQLAlignmentTest(String x) {
		super( x );
		SelectClause.VERSION2_SQL = true;
	}

	public boolean createSchema() {
		return true; // needed for the Criteria return type test
	}

	public boolean recreateSchemaAfterFailure() {
		return true;
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( CriteriaHQLAlignmentTest.class );
	}

	public void testHQLAggregationReturnType() {
		// EJB3: COUNT returns Long
		QueryTranslatorImpl translator = createNewQueryTranslator( "select count(*) from Human h" );
		assertEquals( "incorrect return type count", 1, translator.getReturnTypes().length );
		assertEquals( "incorrect return type", Hibernate.LONG, translator.getReturnTypes()[0] );
		
		translator = createNewQueryTranslator( "select count(h.height) from Human h" );
		assertEquals( "incorrect return type count", 1, translator.getReturnTypes().length );
		assertEquals( "incorrect return type", Hibernate.LONG, translator.getReturnTypes()[0] );
				
		// MAX, MIN return the type of the state-field to which they are applied. 
		translator = createNewQueryTranslator( "select max(h.height) from Human h" );
		assertEquals( "incorrect return type count", 1, translator.getReturnTypes().length );
		assertEquals( "incorrect return type", Hibernate.DOUBLE, translator.getReturnTypes()[0] );
		
		translator = createNewQueryTranslator( "select max(h.id) from Human h" );
		assertEquals( "incorrect return type count", 1, translator.getReturnTypes().length );
		assertEquals( "incorrect return type", Hibernate.LONG, translator.getReturnTypes()[0] );
		
		// AVG returns Double.
		translator = createNewQueryTranslator( "select avg(h.height) from Human h" );
		assertEquals( "incorrect return type count", 1, translator.getReturnTypes().length );
		assertEquals( "incorrect return type", Hibernate.DOUBLE, translator.getReturnTypes()[0] );
		
		translator = createNewQueryTranslator( "select avg(h.id) from Human h" );
		assertEquals( "incorrect return type count", 1, translator.getReturnTypes().length );
		assertEquals( "incorrect return type", Hibernate.DOUBLE, translator.getReturnTypes()[0] );
		
		translator = createNewQueryTranslator( "select avg(h.bigIntegerValue) from Human h" );
		assertEquals( "incorrect return type count", 1, translator.getReturnTypes().length );
		assertEquals( "incorrect return type", Hibernate.DOUBLE, translator.getReturnTypes()[0] );
		
        // SUM returns Long when applied to state-fields of integral types (other than BigInteger);
 	    translator = createNewQueryTranslator( "select sum(h.id) from Human h" );
		assertEquals( "incorrect return type count", 1, translator.getReturnTypes().length );
		assertEquals( "incorrect return type", Hibernate.LONG, translator.getReturnTypes()[0] );
		
		translator = createNewQueryTranslator( "select sum(h.intValue) from Human h" );
		assertEquals( "incorrect return type count", 1, translator.getReturnTypes().length );
		assertEquals( "incorrect return type", Hibernate.LONG, translator.getReturnTypes()[0] );
		
		// SUM returns Double when applied to state-fields of floating point types; 
		translator = createNewQueryTranslator( "select sum(h.height) from Human h" );
		assertEquals( "incorrect return type count", 1, translator.getReturnTypes().length );
		assertEquals( "incorrect return type", Hibernate.DOUBLE, translator.getReturnTypes()[0] );

		translator = createNewQueryTranslator( "select sum(h.floatValue) from Human h" );
		assertEquals( "incorrect return type count", 1, translator.getReturnTypes().length );
		assertEquals( "incorrect return type", Hibernate.DOUBLE, translator.getReturnTypes()[0] );
		
	    // SUM returns BigInteger when applied to state-fields of type BigInteger 
		translator = createNewQueryTranslator( "select sum(h.bigIntegerValue) from Human h" );
		assertEquals( "incorrect return type count", 1, translator.getReturnTypes().length );
		assertEquals( "incorrect return type", Hibernate.BIG_INTEGER, translator.getReturnTypes()[0] );
		
		// SUM and BigDecimal when applied to state-fields of type BigDecimal.
		translator = createNewQueryTranslator( "select sum(h.bigDecimalValue) from Human h" );
		assertEquals( "incorrect return type count", 1, translator.getReturnTypes().length );
		assertEquals( "incorrect return type", Hibernate.BIG_DECIMAL, translator.getReturnTypes()[0] );

		// special case to test classicquery special case handling of count(*)
		QueryTranslator oldQueryTranslator = null;
		String hql = "select count(*) from Human h";
		QueryTranslatorFactory classic = new ClassicQueryTranslatorFactory();
		oldQueryTranslator = classic.createQueryTranslator( hql, hql, Collections.EMPTY_MAP, getSessionFactoryImplementor() );
		oldQueryTranslator.compile( Collections.EMPTY_MAP, true);
		assertEquals( "incorrect return type count", 1, oldQueryTranslator.getReturnTypes().length );
		assertEquals( "incorrect return type", Hibernate.LONG, oldQueryTranslator.getReturnTypes()[0] );

	}
	
	// HHH-1724 Align Criteria with HQL aggregation return types.
	public void testCriteriaAggregationReturnTypeFailureExpected() {
		Session s = openSession();
		Human human = new Human();
		human.setBigIntegerValue( new BigInteger("42") );
		human.setBigDecimalValue( new BigDecimal(45) );
		s.save(human);
		
		// EJB3: COUNT returns Long
		Long longValue = (Long) s.createCriteria( Human.class ).setProjection( Projections.rowCount()).uniqueResult();
		assertEquals(longValue, new Long(1));
		longValue = (Long) s.createCriteria( Human.class ).setProjection( Projections.count("height")).uniqueResult();
		assertEquals(longValue, new Long(1));
		
		 // MAX, MIN return the type of the state-field to which they are applied. 		
		Double dblValue = (Double) s.createCriteria( Human.class ).setProjection( Projections.max( "height" )).uniqueResult();
		assertNotNull(dblValue);
		
		longValue = (Long) s.createCriteria( Human.class ).setProjection( Projections.max( "id" )).uniqueResult();
		assertNotNull(longValue);
		
		// AVG returns Double.
		dblValue = (Double) s.createCriteria( Human.class ).setProjection( Projections.avg( "height" )).uniqueResult();
		assertNotNull(dblValue);
		
		dblValue = (Double) s.createCriteria( Human.class ).setProjection( Projections.avg( "id" )).uniqueResult();
		assertNotNull(dblValue);
		
		dblValue = (Double) s.createCriteria( Human.class ).setProjection( Projections.avg( "bigIntegerValue" )).uniqueResult();
		assertNotNull(dblValue);
		
        // SUM returns Long when applied to state-fields of integral types (other than BigInteger);
		longValue = (Long) s.createCriteria( Human.class ).setProjection( Projections.sum( "id" )).uniqueResult();
		assertNotNull(longValue);
		
		longValue = (Long) s.createCriteria( Human.class ).setProjection( Projections.sum( "intValue" )).uniqueResult();
		assertNotNull(longValue);
		
		// SUM returns Double when applied to state-fields of floating point types; 
		dblValue = (Double) s.createCriteria( Human.class ).setProjection( Projections.sum( "height" )).uniqueResult();
		assertNotNull(dblValue);
		
		dblValue = (Double) s.createCriteria( Human.class ).setProjection( Projections.sum( "floatValue" )).uniqueResult();
		assertNotNull(dblValue);
		
	    // SUM returns BigInteger when applied to state-fields of type BigInteger 
		BigInteger bigIValue = (BigInteger) s.createCriteria( Human.class ).setProjection( Projections.sum( "bigIntegerValue" )).uniqueResult();
		assertNotNull(bigIValue);
		
		// SUM and BigDecimal when applied to state-fields of type BigDecimal.
		BigDecimal bigDValue = (BigDecimal) s.createCriteria( Human.class ).setProjection( Projections.sum( "bigDecimalValue" )).uniqueResult();
		assertNotNull(bigDValue);
		
		s.delete( human );
		s.flush();
		s.close();
	}

}
