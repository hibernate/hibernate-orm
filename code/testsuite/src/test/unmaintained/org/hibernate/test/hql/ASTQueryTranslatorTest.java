// $Id: ASTQueryTranslatorTest.java 8889 2005-12-20 17:35:54Z steveebersole $
package org.hibernate.test.hql;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests cases where the AST based QueryTranslator does not generate identical SQL.
 *
 * @author josh Dec 6, 2004 9:07:58 AM
 */
public class ASTQueryTranslatorTest extends QueryTranslatorTestCase {

	public ASTQueryTranslatorTest(String x) {
		super( x );
	}

	public static Test suite() {
		return new TestSuite( ASTQueryTranslatorTest.class );
	}

	protected boolean dropAfterFailure() {
		return false;
	}

	// ##### TESTS THAT DON'T PASS BECAUSE THEY GENERATE DIFFERENT, POSSIBLY VALID SQL #####

	public void testSelectManyToOne() {
		assertTranslation("select distinct a.zoo from Animal a where a.zoo is not null");
		assertTranslation("select a.zoo from Animal a");
	}

	public void testSelectExpression() {
		//old qt cant handle select-clause expressions
		assertTranslation("select a.bodyWeight + m.bodyWeight from Animal a join a.mother m");
	}
	
	public void testFetchProperties() {
		//not implemented in old qt
		assertTranslation("from Animal a fetch all properties join a.offspring o fetch all properties");
	}
	
	public void testOldSyntax() {
		//generates ANSI join instead of theta join
		assertTranslation("from a in class Animal, o in elements(a.offspring), h in class Human");
	}
	
	public void testImplicitJoinInsideOutsideSubselect() {
		// new qt re-uses the joins defined by 's.other.count' path when referenced in the subquery,
		// whereas the old qt reuses only the "root table" (not the already defined join to 'other'):
		//   OLD SQL :
		//      select  simple0_.id_ as col_0_0_
		//      from    Simple simple0_,
		//              Simple simple1_
		//      where   (simple1_.count_>0
		//      and     simple0_.other=simple1_.id_)
		//      and     (simple0_.id_=some(
		//                  select  simple2_.id_
		//                  from    Simple simple2_,
		//                          Simple simple3_,
		//                          Simple simple4_
		//                  where   (simple3_.count_=simple4_.count_
		//                  and     simple2_.other=simple3_.id_
		//                  and     simple0_.other=simple4_.id_)
		//              ))
		//   NEW SQL :
		//      select  simple0_.id_ as col_0_0_
		//      from    Simple simple0_,
		//              Simple simple1_
		//      where   (simple1_.count_>0
		//      and     simple0_.id_=some(
		//                  select  simple2_.id_
		//                  from    Simple simple2_,
		//                          Simple simple3_
		//                  where   (simple3_.count_=simple1_.count_
		//                  and     simple2_.other=simple3_.id_)
		//            )
		//        and simple0_.other=simple1_.id_)
		assertTranslation( "from Simple s where s = some( from Simple sim where sim.other.count=s.other.count ) and s.other.count > 0" );
		assertTranslation( "from Simple s where s.other.count > 0 and s = some( from Simple sim where sim.other.count=s.other.count )" );
	}

	public void testNakedPropertyRef() {
		// this is needed for ejb3 selects and bulk statements
		//      Note: these all fail because the old parser did not have this
		//      feature, it just "passes the tokens through" to the SQL.
		assertTranslation( "from Animal where bodyWeight = bodyWeight" );
		assertTranslation( "select bodyWeight from Animal" );
		assertTranslation( "select max(bodyWeight) from Animal" );
	}

	public void testNakedComponentPropertyRef() {
		// this is needed for ejb3 selects and bulk statements
		//      Note: these all fail because the old parser did not have this
		//      feature, it just "passes the tokens through" to the SQL.
		assertTranslation( "from Human where name.first = 'Gavin'" );
		assertTranslation( "select name from Human" );
		assertTranslation( "select upper(h.name.first) from Human as h" );
		assertTranslation( "select upper(name.first) from Human" );
	}

	public void testNakedMapIndex() throws Exception {
		assertTranslation( "from Zoo where mammals['dog'].description like '%black%'" );
	}

	public void testNakedImplicitJoins() {
		assertTranslation( "from Animal where mother.father = ?" );
	}

	public void testDuplicateImplicitJoinInWhere() {
		//new qt has more organized joins
		assertTranslation("from Human h where h.mother.bodyWeight>10 and h.mother.bodyWeight<10");
	}
	
	public void testWhereExpressions() {
		assertTranslation("from User u where u.userName='gavin' and u.human.name.first='Gavin'");
		//new qt has more organized joins
		assertTranslation("from User u where u.human.name.last='King' and u.human.name.first='Gavin'");
		assertTranslation("from Bar bar where bar.baz.name='josh'");
		assertTranslation("from Bar bar where bar.baz.name='josh' and not bar.baz.name='gavin'");
	}
	
	public void testImplicitJoinInSelect() {
		//slightly diff select clause, both correct
		assertTranslation("select foo.long, foo.foo from Foo foo");
	}
	
	public void testSelectStandardFunctions() throws Exception {
		//old parser throws an exception
		assertTranslation( "select current_date(), current_time(), current_timestamp() from Animal" );
	}

	public void testSelectClauseImplicitJoin() throws Exception {
		//the old query translator has a bug which results in the wrong return type!
		assertTranslation( "select d.owner.mother from Dog d" );
	}

	public void testComplexWhereExpression() throws Exception {
		// classic QT generates lots of extra parens and some extra theta joins.
		assertTranslation( "select distinct s from Simple s\n" +
				"where ( ( s.other.count + 3 ) = (15*2)/2 and s.count = 69) or ( ( s.other.count + 2 ) / 7 ) = 2" );
	}

	public void testDuplicateImplicitJoin() throws Exception {
		// old qt generates an extra theta join
		assertTranslation( "from Animal an where an.mother.bodyWeight > 10 and an.mother.bodyWeight < 20" );
	}

	public void testKeywordClassNameAndAlias() throws Exception {
		// The old QT throws an exception, the new one doesn't, which is better
		assertTranslation( "from Order order" );
	}

	public void testComponent3() throws Exception {
		// The old translator generates duplicate inner joins *and* duplicate theta join clauses in the where statement in this case.
		assertTranslation( "from Dog dog where dog.owner.name.first = 'Gavin' and dog.owner.name.last='King' and dog.owner.bodyWeight<70" );
	}

	public void testUncorrelatedSubselectWithJoin() throws Exception {
		// The old translator generates unnecessary inner joins for the Animal subclass of zoo.mammals
		// The new one is working fine now!
		assertTranslation( "from Animal a where a in (select mam from Zoo zoo join zoo.mammals mam)" );
	}

	public void testFetch() throws Exception {
		//SQL is correct, new qt is not throwing an exception when it should be (minor issue)
		assertTranslation("from Customer cust left join fetch cust.billingAddress where cust.customerId='abc123'");
	}

	public void testImplicitJoin() throws Exception {
		//old qt generates an exception, the new one doesnt 
		//this is actually invalid HQL, an implicit join on a many-valued association
		assertTranslation( "from Animal an where an.offspring.mother.bodyWeight > 10" );
	}

}
