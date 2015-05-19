/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

// $Id: HqlParserTest.java 10977 2006-12-12 23:28:04Z steve.ebersole@jboss.com $
package org.hibernate.test.hql;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Stack;

import org.hibernate.hql.internal.antlr.HqlTokenTypes;
import org.hibernate.hql.internal.ast.HqlParser;
import org.hibernate.hql.internal.ast.tree.Node;
import org.hibernate.hql.internal.ast.util.ASTIterator;
import org.hibernate.hql.internal.ast.util.ASTPrinter;

import org.junit.Test;

import antlr.RecognitionException;
import antlr.TokenStreamException;
import antlr.collections.AST;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests the HQL parser on various inputs, just makes sure that the first phase of the parser
 * works properly (i.e. no unexpected syntax errors).
 * todo this should be a unit test.
 */
public class HqlParserTest{


	@Test
    public void testUnion() throws Exception {
		parse("from Animal a where a in (from Cat union from Dog) ");
	}

	/**
	 * Section 9.2 - from *
	 */
	@Test public void testDocoExamples92() throws Exception {
		parse( "from eg.Cat" );
		parse( "from eg.Cat as cat" );
		parse( "from eg.Cat cat" );
		parse( "from Formula, Parameter" );
		parse( "from Formula as form, Parameter as param" );
	}

	/**
	 * Section 9.3 - Associations and joins *
	 */
	@Test public void testDocoExamples93() throws Exception {
		parse( "from eg.Cat as cat inner join cat.mate as mate left outer join cat.kittens as kitten" );
		parse( "from eg.Cat as cat left join cat.mate.kittens as kittens" );
		parse( "from Formula form full join form.parameter param" );
		parse( "from eg.Cat as cat join cat.mate as mate left join cat.kittens as kitten" );
		parse( "from eg.Cat as cat\ninner join fetch cat.mate\nleft join fetch cat.kittens" );
	}

	/**
	 * Section 9.4 - Select *
	 */
	@Test public void testDocoExamples94() throws Exception {
		parse( "select mate from eg.Cat as cat inner join cat.mate as mate" );
		parse( "select cat.mate from eg.Cat cat" );
		parse( "select elements(cat.kittens) from eg.Cat cat" );
		parse( "select cat.name from eg.DomesticCat cat where cat.name like 'fri%'" );
		parse( "select cust.name.firstName from Customer as cust" );
		parse( "select mother, offspr, mate.name from eg.DomesticCat\n"
				+ " as mother inner join mother.mate as mate left outer join\n"
				+ "mother.kittens as offspr" );
		parse( "select new Family(mother, mate, offspr)\n"
				+ "from eg.DomesticCat as mother\n"
				+ "join mother.mate as mate\n"
				+ "left join mother.kittens as offspr\n" );
	}

	/**
	 * Section 9.5 - Aggregate functions *
	 */
	@Test public void testDocoExamples95() throws Exception {
		parse( "select avg(cat.weight), sum(cat.weight), max(cat.weight), count(cat)\n"
				+ "from eg.Cat cat" );
		parse( "select cat, count( elements(cat.kittens) )\n"
				+ " from eg.Cat cat group by cat" );
		parse( "select distinct cat.name from eg.Cat cat" );
		parse( "select count(distinct cat.name), count(cat) from eg.Cat cat" );
	}

	/**
	 * Section 9.6 - Polymorphism *
	 */
	@Test public void testDocoExamples96() throws Exception {
		parse( "from eg.Cat as cat" );
		parse( "from java.lang.Object o" );
		parse( "from eg.Named n, eg.Named m where n.name = m.name" );
	}

	/**
	 * Section 9.7 - Where *
	 */
	@Test public void testDocoExamples97() throws Exception {
		parse( "from eg.Cat as cat where cat.name='Fritz'" );
		parse( "select foo\n"
				+ "from eg.Foo foo, eg.Bar bar\n"
				+ "where foo.startDate = bar.date\n" );
		parse( "from eg.Cat cat where cat.mate.name is not null" );
		parse( "from eg.Cat cat, eg.Cat rival where cat.mate = rival.mate" );
		parse( "select cat, mate\n"
				+ "from eg.Cat cat, eg.Cat mate\n"
				+ "where cat.mate = mate" );
		parse( "from eg.Cat as cat where cat.id = 123" );
		parse( "from eg.Cat as cat where cat.mate.id = 69" );
		parse( "from bank.Person person\n"
				+ "where person.id.country = 'AU'\n"
				+ "and person.id.medicareNumber = 123456" );
		parse( "from bank.Account account\n"
				+ "where account.owner.id.country = 'AU'\n"
				+ "and account.owner.id.medicareNumber = 123456" );
		parse( "from eg.Cat cat where cat.class = eg.DomesticCat" );
		parse( "from eg.AuditLog log, eg.Payment payment\n"
				+ "where log.item.class = 'eg.Payment' and log.item.id = payment.id" );
	}

	/**
	 * Section 9.8 - Expressions *
	 */
	@Test public void testDocoExamples98() throws Exception {
		parse( "from eg.DomesticCat cat where cat.name between 'A' and 'B'" );
		parse( "from eg.DomesticCat cat where cat.name in ( 'Foo', 'Bar', 'Baz' )" );
		parse( "from eg.DomesticCat cat where cat.name not between 'A' and 'B'" );
		parse( "from eg.DomesticCat cat where cat.name not in ( 'Foo', 'Bar', 'Baz' )" );
		parse( "from eg.Cat cat where cat.kittens.size > 0" );
		parse( "from eg.Cat cat where size(cat.kittens) > 0" );
// This is a little odd.  I'm not sure whether 'current' is a keyword.
//        parse("from Calendar cal where cal.holidays.maxElement > current date");
// Using the token 'order' as both a keyword and an identifier works now, but
// the second instance causes some problems because order is valid in the second instance.
//        parse("from Order order where maxindex(order.items) > 100");
//        parse("from Order order where minelement(order.items) > 10000");
		parse( "from Order ord where maxindex(ord.items) > 100" );
		parse( "from Order ord where minelement(ord.items) > 10000" );

		parse( "select mother from eg.Cat as mother, eg.Cat as kit\n"
				+ "where kit in elements(foo.kittens)" );
		parse( "select p from eg.NameList list, eg.Person p\n"
				+ "where p.name = some elements(list.names)" );
		parse( "from eg.Cat cat where exists elements(cat.kittens)" );
		parse( "from eg.Player p where 3 > all elements(p.scores)" );
		parse( "from eg.Show show where 'fizard' in indices(show.acts)" );

		// Yet another example of the pathological 'order' token.
//        parse("from Order order where order.items[0].id = 1234");
//        parse("select person from Person person, Calendar calendar\n"
//        + "where calendar.holidays['national day'] = person.birthDay\n"
//        + "and person.nationality.calendar = calendar");
//        parse("select item from Item item, Order order\n"
//        + "where order.items[ order.deliveredItemIndices[0] ] = item and order.id = 11");
//        parse("select item from Item item, Order order\n"
//        + "where order.items[ maxindex(order.items) ] = item and order.id = 11");

		parse( "from Order ord where ord.items[0].id = 1234" );
		parse( "select person from Person person, Calendar calendar\n"
				+ "where calendar.holidays['national day'] = person.birthDay\n"
				+ "and person.nationality.calendar = calendar" );
		parse( "select item from Item item, Order ord\n"
				+ "where ord.items[ ord.deliveredItemIndices[0] ] = item and ord.id = 11" );
		parse( "select item from Item item, Order ord\n"
				+ "where ord.items[ maxindex(ord.items) ] = item and ord.id = 11" );

		parse( "select item from Item item, Order ord\n"
				+ "where ord.items[ size(ord.items) - 1 ] = item" );

		parse( "from eg.DomesticCat cat where upper(cat.name) like 'FRI%'" );

		parse( "select cust from Product prod, Store store\n"
				+ "inner join store.customers cust\n"
				+ "where prod.name = 'widget'\n"
				+ "and store.location.name in ( 'Melbourne', 'Sydney' )\n"
				+ "and prod = all elements(cust.currentOrder.lineItems)" );

	}

	@Test public void testDocoExamples99() throws Exception {
		parse( "from eg.DomesticCat cat\n"
				+ "order by cat.name asc, cat.weight desc, cat.birthdate" );
	}

	@Test public void testDocoExamples910() throws Exception {
		parse( "select cat.color, sum(cat.weight), count(cat)\n"
				+ "from eg.Cat cat group by cat.color" );
		parse( "select foo.id, avg( elements(foo.names) ), max( indices(foo.names) )\n"
				+ "from eg.Foo foo group by foo.id" );
		parse( "select cat.color, sum(cat.weight), count(cat)\n"
				+ "from eg.Cat cat group by cat.color\n"
				+ "having cat.color in (eg.Color.TABBY, eg.Color.BLACK)" );
		parse( "select cat from eg.Cat cat join cat.kittens kitten\n"
				+ "group by cat having avg(kitten.weight) > 100\n"
				+ "order by count(kitten) asc, sum(kitten.weight) desc" );
	}

	@Test public void testDocoExamples911() throws Exception {
		parse( "from eg.Cat as fatcat where fatcat.weight > (\n"
				+ "select avg(cat.weight) from eg.DomesticCat cat)" );
		parse( "from eg.DomesticCat as cat where cat.name = some (\n"
				+ "select name.nickName from eg.Name as name)\n" );
		parse( "from eg.Cat as cat where not exists (\n"
				+ "from eg.Cat as mate where mate.mate = cat)" );
		parse( "from eg.DomesticCat as cat where cat.name not in (\n"
				+ "select name.nickName from eg.Name as name)" );
	}

	@Test public void testDocoExamples912() throws Exception {
		parse( "select ord.id, sum(price.amount), count(item)\n"
				+ "from Order as ord join ord.lineItems as item\n"
				+ "join item.product as product, Catalog as catalog\n"
				+ "join catalog.prices as price\n"
				+ "where ord.paid = false\n"
				+ "and ord.customer = :customer\n"
				+ "and price.product = product\n"
				+ "and catalog.effectiveDate < sysdate\n"
				+ "and catalog.effectiveDate >= all (\n"
				+ "select cat.effectiveDate from Catalog as cat where cat.effectiveDate < sysdate)\n"
				+ "group by ord\n"
				+ "having sum(price.amount) > :minAmount\n"
				+ "order by sum(price.amount) desc" );

		parse( "select ord.id, sum(price.amount), count(item)\n"
				+ "from Order as ord join ord.lineItems as item join item.product as product,\n"
				+ "Catalog as catalog join catalog.prices as price\n"
				+ "where ord.paid = false and ord.customer = :customer\n"
				+ "and price.product = product and catalog = :currentCatalog\n"
				+ "group by ord having sum(price.amount) > :minAmount\n"
				+ "order by sum(price.amount) desc" );

		parse( "select count(payment), status.name \n"
				+ "from Payment as payment \n"
				+ "    join payment.currentStatus as status\n"
				+ "    join payment.statusChanges as statusChange\n"
				+ "where payment.status.name <> PaymentStatus.AWAITING_APPROVAL\n"
				+ "    or (\n"
				+ "        statusChange.timeStamp = ( \n"
				+ "            select max(change.timeStamp) \n"
				+ "            from PaymentStatusChange change \n"
				+ "            where change.payment = payment\n"
				+ "        )\n"
				+ "        and statusChange.user <> :currentUser\n"
				+ "    )\n"
				+ "group by status.name, status.sortOrder\n"
				+ "order by status.sortOrder" );
		parse( "select count(payment), status.name \n"
				+ "from Payment as payment\n"
				+ "    join payment.currentStatus as status\n"
				+ "where payment.status.name <> PaymentStatus.AWAITING_APPROVAL\n"
				+ "    or payment.statusChanges[ maxIndex(payment.statusChanges) ].user <> :currentUser\n"
				+ "group by status.name, status.sortOrder\n"
				+ "order by status.sortOrder" );
		parse( "select account, payment\n"
				+ "from Account as account\n"
				+ "    left outer join account.payments as payment\n"
				+ "where :currentUser in elements(account.holder.users)\n"
				+ "    and PaymentStatus.UNPAID = isNull(payment.currentStatus.name, PaymentStatus.UNPAID)\n"
				+ "order by account.type.sortOrder, account.accountNumber, payment.dueDate" );
		parse( "select account, payment\n"
				+ "from Account as account\n"
				+ "    join account.holder.users as user\n"
				+ "    left outer join account.payments as payment\n"
				+ "where :currentUser = user\n"
				+ "    and PaymentStatus.UNPAID = isNull(payment.currentStatus.name, PaymentStatus.UNPAID)\n"
				+ "order by account.type.sortOrder, account.accountNumber, payment.dueDate" );
	}

	@Test public void testExamples1() throws Exception {
		parse( "select new org.hibernate.test.S(s.count, s.address)\n"
				+ "from s in class Simple" );
		parse( "select s.name, sysdate, trunc(s.pay), round(s.pay) from s in class Simple" );
		parse( "select round(s.pay, 2) from s" );
		parse( "select abs(round(s.pay)) from s in class Simple" );
		parse( "select trunc(round(sysdate)) from s in class Simple" );
	}

	@Test public void testArrayExpr() throws Exception {
		parse( "from Order ord where ord.items[0].id = 1234" );
	}

	@Test public void testMultipleActualParameters() throws Exception {
		parse( "select round(s.pay, 2) from s" );
	}

	@Test public void testMultipleFromClasses() throws Exception {
		parse( "FROM eg.mypackage.Cat qat, com.toadstool.Foo f" );
		parse( "FROM eg.mypackage.Cat qat, org.jabberwocky.Dipstick" );
	}

	@Test public void testFromWithJoin() throws Exception {
		parse( "FROM eg.mypackage.Cat qat, com.toadstool.Foo f join net.sf.blurb.Blurb" );
		parse( "FROM eg.mypackage.Cat qat  left join com.multijoin.JoinORama , com.toadstool.Foo f join net.sf.blurb.Blurb" );
	}

	@Test public void testSelect() throws Exception {
		parse( "SELECT f FROM eg.mypackage.Cat qat, com.toadstool.Foo f join net.sf.blurb.Blurb" );
		parse( "SELECT DISTINCT bar FROM eg.mypackage.Cat qat  left join com.multijoin.JoinORama as bar, com.toadstool.Foo f join net.sf.blurb.Blurb" );
		parse( "SELECT count(*) FROM eg.mypackage.Cat qat" );
		parse( "SELECT avg(qat.weight) FROM eg.mypackage.Cat qat" );
	}

	@Test public void testWhere() throws Exception {
		parse( "FROM eg.mypackage.Cat qat where qat.name like '%fluffy%' or qat.toes > 5" );
		parse( "FROM eg.mypackage.Cat qat where not qat.name like '%fluffy%' or qat.toes > 5" );
		parse( "FROM eg.mypackage.Cat qat where not qat.name not like '%fluffy%'" );
		parse( "FROM eg.mypackage.Cat qat where qat.name in ('crater','bean','fluffy')" );
		parse( "FROM eg.mypackage.Cat qat where qat.name not in ('crater','bean','fluffy')" );
		parse( "from Animal an where sqrt(an.bodyWeight)/2 > 10" );
		parse( "from Animal an where (an.bodyWeight > 10 and an.bodyWeight < 100) or an.bodyWeight is null" );
	}

	@Test public void testGroupBy() throws Exception {
		parse( "FROM eg.mypackage.Cat qat group by qat.breed" );
		parse( "FROM eg.mypackage.Cat qat group by qat.breed, qat.eyecolor" );
	}

	@Test public void testOrderBy() throws Exception {
		parse( "FROM eg.mypackage.Cat qat order by avg(qat.toes)" );
		parse( "from Animal an order by sqrt(an.bodyWeight)/2" );
	}

	@Test public void testDoubleLiteral() throws Exception {
		parse( "from eg.Cat as tinycat where fatcat.weight < 3.1415" );
		parse( "from eg.Cat as enormouscat where fatcat.weight > 3.1415e3" );
	}

	@Test public void testComplexConstructor() throws Exception {
		parse( "select new Foo(count(bar)) from bar" );
		parse( "select new Foo(count(bar),(select count(*) from doofus d where d.gob = 'fat' )) from bar" );
	}


	@Test public void testInNotIn() throws Exception {
		parse( "from foo where foo.bar in ('a' , 'b', 'c')" );
		parse( "from foo where foo.bar not in ('a' , 'b', 'c')" );
	}

	@Test public void testOperatorPrecedence() throws Exception {
		parse( "from foo where foo.bar = 123 + foo.baz * foo.not" );
		parse( "from foo where foo.bar like 'testzzz' || foo.baz or foo.bar in ('duh', 'gob')" );
	}

	/**
	 * Tests HQL generated by the other unit tests.
	 *
	 * @throws Exception if the HQL could not be parsed.
	 */
	@Test public void testUnitTestHql() throws Exception {
		parse( "select foo from foo in class org.hibernate.test.Foo, fee in class org.hibernate.test.Fee where foo.dependent = fee order by foo.string desc, foo.component.count asc, fee.id" );
		parse( "select foo.foo, foo.dependent from foo in class org.hibernate.test.Foo order by foo.foo.string desc, foo.component.count asc, foo.dependent.id" );
		parse( "select foo from foo in class org.hibernate.test.Foo order by foo.dependent.id, foo.dependent.fi" );
		parse( "SELECT one FROM one IN CLASS org.hibernate.test.One ORDER BY one.value ASC" );
		parse( "SELECT many.one FROM many IN CLASS org.hibernate.test.Many ORDER BY many.one.value ASC, many.one.id" );
		parse( "select foo.id from org.hibernate.test.Foo foo where foo.joinedProp = 'foo'" );
		parse( "from org.hibernate.test.Foo foo inner join fetch foo.foo" );
		parse( "from org.hibernate.test.Baz baz left outer join fetch baz.fooToGlarch" );
		parse( "select foo.foo.foo.string from foo in class org.hibernate.test.Foo where foo.foo = 'bar'" );
		parse( "select foo.foo.foo.foo.string from foo in class org.hibernate.test.Foo where foo.foo.foo = 'bar'" );
		parse( "select foo.foo.foo.string from foo in class org.hibernate.test.Foo where foo.foo.foo.foo.string = 'bar'" );
		parse( "select foo.string from foo in class org.hibernate.test.Foo where foo.foo.foo = 'bar' and foo.foo.foo.foo = 'baz'" );
		parse( "select foo.string from foo in class org.hibernate.test.Foo where foo.foo.foo.foo.string = 'a' and foo.foo.string = 'b'" );
		parse( "from org.hibernate.test.Foo as foo where foo.component.glarch.name is not null" );
		parse( "from org.hibernate.test.Foo as foo left outer join foo.component.glarch as glarch where glarch.name = 'foo'" );
		parse( "from org.hibernate.test.Foo" );
		parse( "from org.hibernate.test.Foo foo left outer join foo.foo" );
		parse( "from org.hibernate.test.Foo, org.hibernate.test.Bar" );
		parse( "from org.hibernate.test.Baz baz left join baz.fooToGlarch, org.hibernate.test.Bar bar join bar.foo" );
		parse( "from org.hibernate.test.Baz baz left join baz.fooToGlarch join baz.fooSet" );
		parse( "from org.hibernate.test.Baz baz left join baz.fooToGlarch join fetch baz.fooSet foo left join fetch foo.foo" );
		parse( "from foo in class org.hibernate.test.Foo where foo.string='osama bin laden' and foo.boolean = true order by foo.string asc, foo.component.count desc" );
		parse( "from foo in class org.hibernate.test.Foo where foo.string='osama bin laden' order by foo.string asc, foo.component.count desc" );
		parse( "select foo.foo from foo in class org.hibernate.test.Foo" );
		parse( "from foo in class org.hibernate.test.Foo where foo.component.count is null order by foo.component.count" );
		parse( "from foo in class org.hibernate.test.Foo where foo.component.name='foo'" );
		parse( "select distinct foo.component.name, foo.component.name from foo in class org.hibernate.test.Foo where foo.component.name='foo'" );
		parse( "select distinct foo.component.name, foo.id from foo in class org.hibernate.test.Foo where foo.component.name='foo'" );
		parse( "from foo in class org.hibernate.test.Foo where foo.id=?" );
		parse( "from foo in class org.hibernate.test.Foo where foo.key=?" );
		parse( "select foo.foo from foo in class org.hibernate.test.Foo where foo.string='fizard'" );
		parse( "from foo in class org.hibernate.test.Foo where foo.component.subcomponent.name='bar'" );
		parse( "select foo.foo from foo in class org.hibernate.test.Foo where foo.foo.id=?" );
		parse( "from foo in class org.hibernate.test.Foo where foo.foo = ?" );
		parse( "from bar in class org.hibernate.test.Bar where bar.string='a string' or bar.string='a string'" );
		parse( "select foo.component.name, elements(foo.component.importantDates) from foo in class org.hibernate.test.Foo where foo.foo.id=?" );
		parse( "select max(elements(foo.component.importantDates)) from foo in class org.hibernate.test.Foo group by foo.id" );
		parse( "select foo.foo.foo.foo from foo in class org.hibernate.test.Foo, foo2 in class org.hibernate.test.Foo where foo = foo2.foo and not not ( not foo.string='fizard' ) and foo2.string between 'a' and (foo.foo.string) and ( foo2.string in ( 'fiz', 'blah') or 1=1 )" );
		parse( "from foo in class org.hibernate.test.Foo where foo.string='from BoogieDown  -tinsel town  =!@#$^&*())'" );
		parse( "from foo in class org.hibernate.test.Foo where not foo.string='foo''bar'" ); // Added quote quote is an escape
		parse( "from foo in class org.hibernate.test.Foo where foo.component.glarch.next is null" );
		parse( " from bar in class org.hibernate.test.Bar where bar.baz.count=667 and bar.baz.count!=123 and not bar.baz.name='1-E-1'" );
		parse( " from i in class org.hibernate.test.Bar where i.baz.name='Bazza'" );
		parse( "select count(distinct foo.foo) from foo in class org.hibernate.test.Foo" );
		parse( "select count(foo.foo.boolean) from foo in class org.hibernate.test.Foo" );
		parse( "select count(*), foo.int from foo in class org.hibernate.test.Foo group by foo.int" );
		parse( "select sum(foo.foo.int) from foo in class org.hibernate.test.Foo" );
		parse( "select count(foo) from foo in class org.hibernate.test.Foo where foo.id=?" );
		parse( "from foo in class org.hibernate.test.Foo where foo.boolean = ?" );
		parse( "select new Foo(fo.x) from org.hibernate.test.Fo fo" );
		parse( "select new Foo(fo.integer) from org.hibernate.test.Foo fo" );
		parse( "select new Foo(fo.x) from org.hibernate.test.Foo fo" );
		parse( "select foo.long, foo.component.name, foo, foo.foo from foo in class org.hibernate.test.Foo" );
		parse( "select avg(foo.float), max(foo.component.name), count(distinct foo.id) from foo in class org.hibernate.test.Foo" );
		parse( "select foo.long, foo.component, foo, foo.foo from foo in class org.hibernate.test.Foo" );
		parse( "from o in class org.hibernate.test.MoreStuff" );
		parse( "from o in class org.hibernate.test.Many" );
		parse( "from o in class org.hibernate.test.Fee" );
		parse( "from o in class org.hibernate.test.Qux" );
		parse( "from o in class org.hibernate.test.Y" );
		parse( "from o in class org.hibernate.test.Fumm" );
		parse( "from o in class org.hibernate.test.X" );
		parse( "from o in class org.hibernate.test.Simple" );
		parse( "from o in class org.hibernate.test.Location" );
		parse( "from o in class org.hibernate.test.Holder" );
		parse( "from o in class org.hibernate.test.Part" );
		parse( "from o in class org.hibernate.test.Baz" );
		parse( "from o in class org.hibernate.test.Vetoer" );
		parse( "from o in class org.hibernate.test.Sortable" );
		parse( "from o in class org.hibernate.test.Contained" );
		parse( "from o in class org.hibernate.test.Stuff" );
		parse( "from o in class org.hibernate.test.Immutable" );
		parse( "from o in class org.hibernate.test.Container" );
		parse( "from o in class org.hibernate.test.X$XX" );
		parse( "from o in class org.hibernate.test.One" );
		parse( "from o in class org.hibernate.test.Foo" );
		parse( "from o in class org.hibernate.test.Fo" );
		parse( "from o in class org.hibernate.test.Glarch" );
		parse( "from o in class org.hibernate.test.Fum" );
		parse( "from n in class org.hibernate.test.Holder" );
		parse( "from n in class org.hibernate.test.Baz" );
		parse( "from n in class org.hibernate.test.Bar" );
		parse( "from n in class org.hibernate.test.Glarch" );
		parse( "from n in class org.hibernate.test.Holder where n.name is not null" );
		parse( "from n in class org.hibernate.test.Baz where n.name is not null" );
		parse( "from n in class org.hibernate.test.Bar where n.name is not null" );
		parse( "from n in class org.hibernate.test.Glarch where n.name is not null" );
		parse( "from n in class org.hibernate.test.Holder" );
		parse( "from n in class org.hibernate.test.Baz" );
		parse( "from n in class org.hibernate.test.Bar" );
		parse( "from n in class org.hibernate.test.Glarch" );
		parse( "from n0 in class org.hibernate.test.Holder, n1 in class org.hibernate.test.Holder where n0.name = n1.name" );
		parse( "from n0 in class org.hibernate.test.Baz, n1 in class org.hibernate.test.Holder where n0.name = n1.name" );
		parse( "from n0 in class org.hibernate.test.Bar, n1 in class org.hibernate.test.Holder where n0.name = n1.name" );
		parse( "from n0 in class org.hibernate.test.Glarch, n1 in class org.hibernate.test.Holder where n0.name = n1.name" );
		parse( "from n0 in class org.hibernate.test.Holder, n1 in class org.hibernate.test.Baz where n0.name = n1.name" );
		parse( "from n0 in class org.hibernate.test.Baz, n1 in class org.hibernate.test.Baz where n0.name = n1.name" );
		parse( "from n0 in class org.hibernate.test.Bar, n1 in class org.hibernate.test.Baz where n0.name = n1.name" );
		parse( "from n0 in class org.hibernate.test.Glarch, n1 in class org.hibernate.test.Baz where n0.name = n1.name" );
		parse( "from n0 in class org.hibernate.test.Holder, n1 in class org.hibernate.test.Bar where n0.name = n1.name" );
		parse( "from n0 in class org.hibernate.test.Baz, n1 in class org.hibernate.test.Bar where n0.name = n1.name" );
		parse( "from n0 in class org.hibernate.test.Bar, n1 in class org.hibernate.test.Bar where n0.name = n1.name" );
		parse( "from n0 in class org.hibernate.test.Glarch, n1 in class org.hibernate.test.Bar where n0.name = n1.name" );
		parse( "from n0 in class org.hibernate.test.Holder, n1 in class org.hibernate.test.Glarch where n0.name = n1.name" );
		parse( "from n0 in class org.hibernate.test.Baz, n1 in class org.hibernate.test.Glarch where n0.name = n1.name" );
		parse( "from n0 in class org.hibernate.test.Bar, n1 in class org.hibernate.test.Glarch where n0.name = n1.name" );
		parse( "from n0 in class org.hibernate.test.Glarch, n1 in class org.hibernate.test.Glarch where n0.name = n1.name" );
		parse( "from n in class org.hibernate.test.Holder where n.name = :name" );
		parse( "from o in class org.hibernate.test.MoreStuff" );
		parse( "from o in class org.hibernate.test.Many" );
		parse( "from o in class org.hibernate.test.Fee" );
		parse( "from o in class org.hibernate.test.Qux" );
		parse( "from o in class org.hibernate.test.Y" );
		parse( "from o in class org.hibernate.test.Fumm" );
		parse( "from o in class org.hibernate.test.X" );
		parse( "from o in class org.hibernate.test.Simple" );
		parse( "from o in class org.hibernate.test.Location" );
		parse( "from o in class org.hibernate.test.Holder" );
		parse( "from o in class org.hibernate.test.Part" );
		parse( "from o in class org.hibernate.test.Baz" );
		parse( "from o in class org.hibernate.test.Vetoer" );
		parse( "from o in class org.hibernate.test.Sortable" );
		parse( "from o in class org.hibernate.test.Contained" );
		parse( "from o in class org.hibernate.test.Stuff" );
		parse( "from o in class org.hibernate.test.Immutable" );
		parse( "from o in class org.hibernate.test.Container" );
		parse( "from o in class org.hibernate.test.X$XX" );
		parse( "from o in class org.hibernate.test.One" );
		parse( "from o in class org.hibernate.test.Foo" );
		parse( "from o in class org.hibernate.test.Fo" );
		parse( "from o in class org.hibernate.test.Glarch" );
		parse( "from o in class org.hibernate.test.Fum" );
		parse( "select baz.code, min(baz.count) from baz in class org.hibernate.test.Baz group by baz.code" );
		parse( "selecT baz from baz in class org.hibernate.test.Baz where baz.stringDateMap['foo'] is not null or baz.stringDateMap['bar'] = ?" );
		parse( "select baz from baz in class org.hibernate.test.Baz where baz.stringDateMap['now'] is not null" );
		parse( "select baz from baz in class org.hibernate.test.Baz where baz.stringDateMap['now'] is not null and baz.stringDateMap['big bang'] < baz.stringDateMap['now']" );
		parse( "select index(date) from org.hibernate.test.Baz baz join baz.stringDateMap date" );
		parse( "from foo in class org.hibernate.test.Foo where foo.integer not between 1 and 5 and foo.string not in ('cde', 'abc') and foo.string is not null and foo.integer<=3" );
		parse( "from org.hibernate.test.Baz baz inner join baz.collectionComponent.nested.foos foo where foo.string is null" );
		parse( "from org.hibernate.test.Baz baz inner join baz.fooSet where '1' in (from baz.fooSet foo where foo.string is not null)" );
		parse( "from org.hibernate.test.Baz baz where 'a' in elements(baz.collectionComponent.nested.foos) and 1.0 in elements(baz.collectionComponent.nested.floats)" );
		parse( "from org.hibernate.test.Foo foo join foo.foo where foo.foo in ('1','2','3')" );
		parse( "select foo.foo from org.hibernate.test.Foo foo where foo.foo in ('1','2','3')" );
		parse( "select foo.foo.string from org.hibernate.test.Foo foo where foo.foo in ('1','2','3')" );
		parse( "select foo.foo.string from org.hibernate.test.Foo foo where foo.foo.string in ('1','2','3')" );
		parse( "select foo.foo.long from org.hibernate.test.Foo foo where foo.foo.string in ('1','2','3')" );
		parse( "select count(*) from org.hibernate.test.Foo foo where foo.foo.string in ('1','2','3') or foo.foo.long in (1,2,3)" );
		parse( "select count(*) from org.hibernate.test.Foo foo where foo.foo.string in ('1','2','3') group by foo.foo.long" );
		parse( "from org.hibernate.test.Foo foo1 left join foo1.foo foo2 left join foo2.foo where foo1.string is not null" );
		parse( "from org.hibernate.test.Foo foo1 left join foo1.foo.foo where foo1.string is not null" );
		parse( "from org.hibernate.test.Foo foo1 left join foo1.foo foo2 left join foo1.foo.foo foo3 where foo1.string is not null" );
		parse( "select foo.formula from org.hibernate.test.Foo foo where foo.formula > 0" );
		parse( "from org.hibernate.test.Foo as foo join foo.foo as foo2 where foo2.id >'a' or foo2.id <'a'" );
		parse( "from org.hibernate.test.Holder" );
		parse( "from org.hibernate.test.Baz baz left outer join fetch baz.manyToAny" );
		parse( "from org.hibernate.test.Baz baz join baz.manyToAny" );
		parse( "select baz from org.hibernate.test.Baz baz join baz.manyToAny a where index(a) = 0" );
		parse( "select bar from org.hibernate.test.Bar bar where bar.baz.stringDateMap['now'] is not null" );
		parse( "select bar from org.hibernate.test.Bar bar join bar.baz b where b.stringDateMap['big bang'] < b.stringDateMap['now'] and b.stringDateMap['now'] is not null" );
		parse( "select bar from org.hibernate.test.Bar bar where bar.baz.stringDateMap['big bang'] < bar.baz.stringDateMap['now'] and bar.baz.stringDateMap['now'] is not null" );
		parse( "select foo.string, foo.component, foo.id from org.hibernate.test.Bar foo" );
		parse( "select elements(baz.components) from org.hibernate.test.Baz baz" );
		parse( "select bc.name from org.hibernate.test.Baz baz join baz.components bc" );
		parse( "from org.hibernate.test.Foo foo where foo.integer < 10 order by foo.string" );
		parse( "from org.hibernate.test.Fee" );
		parse( "from org.hibernate.test.Holder h join h.otherHolder oh where h.otherHolder.name = 'bar'" );
		parse( "from org.hibernate.test.Baz baz join baz.fooSet foo join foo.foo.foo foo2 where foo2.string = 'foo'" );
		parse( "from org.hibernate.test.Baz baz join baz.fooArray foo join foo.foo.foo foo2 where foo2.string = 'foo'" );
		parse( "from org.hibernate.test.Baz baz join baz.stringDateMap date where index(date) = 'foo'" );
		parse( "from org.hibernate.test.Baz baz join baz.topGlarchez g where index(g) = 'A'" );
		parse( "select index(g) from org.hibernate.test.Baz baz join baz.topGlarchez g" );
		parse( "from org.hibernate.test.Baz baz left join baz.stringSet" );
		parse( "from org.hibernate.test.Baz baz join baz.stringSet str where str='foo'" );
		parse( "from org.hibernate.test.Baz baz left join fetch baz.stringSet" );
		parse( "from org.hibernate.test.Baz baz join baz.stringSet string where string='foo'" );
		parse( "from org.hibernate.test.Baz baz inner join baz.components comp where comp.name='foo'" );
		parse( "from org.hibernate.test.Glarch g inner join g.fooComponents comp where comp.fee is not null" );
		parse( "from org.hibernate.test.Glarch g inner join g.fooComponents comp join comp.fee fee where fee.count > 0" );
		parse( "from org.hibernate.test.Glarch g inner join g.fooComponents comp where comp.fee.count is not null" );
		parse( "from org.hibernate.test.Baz baz left join fetch baz.fooBag" );
		parse( "from org.hibernate.test.Glarch" );
		parse( "from org.hibernate.test.Fee" );
		parse( "from org.hibernate.test.Baz baz left join fetch baz.sortablez order by baz.name asc" );
		parse( "from org.hibernate.test.Baz baz order by baz.name asc" );
		parse( "from org.hibernate.test.Foo foo, org.hibernate.test.Baz baz left join fetch baz.fees" );
		parse( "from org.hibernate.test.Foo foo, org.hibernate.test.Bar bar" );
		parse( "from org.hibernate.test.Foo foo" );
		parse( "from org.hibernate.test.Foo foo, org.hibernate.test.Bar bar, org.hibernate.test.Bar bar2" );
		parse( "from org.hibernate.test.X x" );
		parse( "from org.hibernate.test.Foo foo" );
		parse( "select distinct foo from org.hibernate.test.Foo foo" );
		parse( "from org.hibernate.test.Glarch g where g.multiple.glarch=g and g.multiple.count=12" );
		parse( "from org.hibernate.test.Bar bar left join bar.baz baz left join baz.cascadingBars b where bar.name like 'Bar %'" );
		parse( "select bar, b from org.hibernate.test.Bar bar left join bar.baz baz left join baz.cascadingBars b where bar.name like 'Bar%'" );
		parse( "select bar, b from org.hibernate.test.Bar bar left join bar.baz baz left join baz.cascadingBars b where ( bar.name in (:nameList0_, :nameList1_, :nameList2_) or bar.name in (:nameList0_, :nameList1_, :nameList2_) ) and bar.string = :stringVal" );
		parse( "select bar, b from org.hibernate.test.Bar bar inner join bar.baz baz inner join baz.cascadingBars b where bar.name like 'Bar%'" );
		parse( "select bar, b from org.hibernate.test.Bar bar left join bar.baz baz left join baz.cascadingBars b where bar.name like :name and b.name like :name" );
		parse( "select bar from org.hibernate.test.Bar as bar where bar.x > ? or bar.short = 1 or bar.string = 'ff ? bb'" );
		parse( "select bar from org.hibernate.test.Bar as bar where bar.string = ' ? ' or bar.string = '?'" );
		parse( "from org.hibernate.test.Baz baz, baz.fooArray foo" );
		parse( "from s in class org.hibernate.test.Stuff where s.foo.id = ? and s.id.id = ? and s.moreStuff.id.intId = ? and s.moreStuff.id.stringId = ?" );
		parse( "from s in class org.hibernate.test.Stuff where s.foo.id = ? and s.id.id = ? and s.moreStuff.name = ?" );
		parse( "from s in class org.hibernate.test.Stuff where s.foo.string is not null" );
		parse( "from s in class org.hibernate.test.Stuff where s.foo > '0' order by s.foo" );
		parse( "from ms in class org.hibernate.test.MoreStuff" );
		parse( "from foo in class org.hibernate.test.Foo" );
		parse( "from fee in class org.hibernate.test.Fee" );
		parse( "select new Result(foo.string, foo.long, foo.integer) from foo in class org.hibernate.test.Foo" );
		parse( "select new Result( baz.name, foo.long, count(elements(baz.fooArray)) ) from org.hibernate.test.Baz baz join baz.fooArray foo group by baz.name, foo.long" );
		parse( "select new Result( baz.name, max(foo.long), count(foo) ) from org.hibernate.test.Baz baz join baz.fooArray foo group by baz.name" );
		parse( "select max( elements(bar.baz.fooArray) ) from org.hibernate.test.Bar as bar" );
		parse( "from org.hibernate.test.Baz baz left join baz.fooToGlarch join fetch baz.fooArray foo left join fetch foo.foo" );
		parse( "select baz.name from org.hibernate.test.Bar bar inner join bar.baz baz inner join baz.fooSet foo where baz.name = bar.string" );
		parse( "SELECT baz.name FROM org.hibernate.test.Bar AS bar INNER JOIN bar.baz AS baz INNER JOIN baz.fooSet AS foo WHERE baz.name = bar.string" );
		parse( "select baz.name from org.hibernate.test.Bar bar join bar.baz baz left outer join baz.fooSet foo where baz.name = bar.string" );
		parse( "select baz.name from org.hibernate.test.Bar bar, bar.baz baz, baz.fooSet foo where baz.name = bar.string" );
		parse( "SELECT baz.name FROM org.hibernate.test.Bar AS bar, bar.baz AS baz, baz.fooSet AS foo WHERE baz.name = bar.string" );
		parse( "select baz.name from org.hibernate.test.Bar bar left join bar.baz baz left join baz.fooSet foo where baz.name = bar.string" );
		parse( "select foo.string from org.hibernate.test.Bar bar left join bar.baz.fooSet foo where bar.string = foo.string" );
		parse( "select baz.name from org.hibernate.test.Bar bar left join bar.baz baz left join baz.fooArray foo where baz.name = bar.string" );
		parse( "select foo.string from org.hibernate.test.Bar bar left join bar.baz.fooArray foo where bar.string = foo.string" );
		parse( "select foo from bar in class org.hibernate.test.Bar inner join bar.baz as baz inner join baz.fooSet as foo" );
		parse( "select foo from bar in class org.hibernate.test.Bar inner join bar.baz.fooSet as foo" );
		parse( "select foo from bar in class org.hibernate.test.Bar, bar.baz as baz, baz.fooSet as foo" );
		parse( "select foo from bar in class org.hibernate.test.Bar, bar.baz.fooSet as foo" );
		parse( "from org.hibernate.test.Bar bar join bar.baz.fooArray foo" );
		parse( "from bar in class org.hibernate.test.Bar, foo in elements( bar.baz.fooArray )" );
		parse( "select one.id, elements(one.manies) from one in class org.hibernate.test.One" );
		parse( "select max( elements(one.manies) ) from one in class org.hibernate.test.One" );
		parse( "select one, elements(one.manies) from one in class org.hibernate.test.One" );
		parse( "select one, max(elements(one.manies)) from one in class org.hibernate.test.One group by one" );
		parse( "select elements(baz.fooArray) from baz in class org.hibernate.test.Baz where baz.id=?" );
		parse( "select elements(baz.fooArray) from baz in class org.hibernate.test.Baz where baz.id=?" );
		parse( "select indices(baz.fooArray) from baz in class org.hibernate.test.Baz where baz.id=?" );
		parse( "select baz, max(elements(baz.timeArray)) from baz in class org.hibernate.test.Baz group by baz" );
		parse( "select baz, baz.stringSet.size, count(distinct elements(baz.stringSet)), max(elements(baz.stringSet)) from baz in class org.hibernate.test.Baz group by baz" );
		parse( "select max( elements(baz.timeArray) ) from baz in class org.hibernate.test.Baz where baz.id=?" );
		parse( "select max(elements(baz.stringSet)) from baz in class org.hibernate.test.Baz where baz.id=?" );
		parse( "select size(baz.stringSet) from baz in class org.hibernate.test.Baz where baz.id=?" );
		parse( "from org.hibernate.test.Foo foo where foo.component.glarch.id is not null" );
		parse( "from baz in class org.hibernate.test.Baz" );
		parse( "select elements(baz.stringArray) from baz in class org.hibernate.test.Baz" );
		parse( "from foo in class org.hibernate.test.Foo" );
		parse( "select elements(baz.stringList) from baz in class org.hibernate.test.Baz" );
		parse( "select count(*) from org.hibernate.test.Bar" );
		parse( "select count(*) from b in class org.hibernate.test.Bar" );
		parse( "from g in class org.hibernate.test.Glarch" );
		parse( "select baz, baz from baz in class org.hibernate.test.Baz" );
		parse( "select baz from baz in class org.hibernate.test.Baz order by baz" );
		parse( "from bar in class org.hibernate.test.Bar" );
		parse( "from g in class org.hibernate.test.Glarch" );
		parse( "from f in class org.hibernate.test.Foo" );
		parse( "from o in class org.hibernate.test.One" );
		parse( "from q in class org.hibernate.test.Qux" );
		parse( "select foo from foo in class org.hibernate.test.Foo where foo.string='foo bar'" );
		parse( "from foo in class org.hibernate.test.Foo order by foo.string, foo.date" );
		parse( "from foo in class org.hibernate.test.Foo where foo.class='B'" );
		parse( "from foo in class org.hibernate.test.Foo where foo.class=Bar" );
		parse( "select bar from bar in class org.hibernate.test.Bar, foo in class org.hibernate.test.Foo where bar.string = foo.string and not bar=foo" );
		parse( "from foo in class org.hibernate.test.Foo where foo.string='foo bar'" );
		parse( "select foo from foo in class org.hibernate.test.Foo" );
		parse( "from bar in class org.hibernate.test.Bar where bar.barString='bar bar'" );
		parse( "from t in class org.hibernate.test.Trivial" );
		parse( "from foo in class org.hibernate.test.Foo where foo.date = ?" );
		parse( "from o in class org.hibernate.test.MoreStuff" );
		parse( "from o in class org.hibernate.test.Many" );
		parse( "from o in class org.hibernate.test.Fee" );
		parse( "from o in class org.hibernate.test.Qux" );
		parse( "from o in class org.hibernate.test.Y" );
		parse( "from o in class org.hibernate.test.Fumm" );
		parse( "from o in class org.hibernate.test.X" );
		parse( "from o in class org.hibernate.test.Simple" );
		parse( "from o in class org.hibernate.test.Location" );
		parse( "from o in class org.hibernate.test.Holder" );
		parse( "from o in class org.hibernate.test.Part" );
		parse( "from o in class org.hibernate.test.Baz" );
		parse( "from o in class org.hibernate.test.Vetoer" );
		parse( "from o in class org.hibernate.test.Sortable" );
		parse( "from o in class org.hibernate.test.Contained" );
		parse( "from o in class org.hibernate.test.Stuff" );
		parse( "from o in class org.hibernate.test.Immutable" );
		parse( "from o in class org.hibernate.test.Container" );
		parse( "from o in class org.hibernate.test.X$XX" );
		parse( "from o in class org.hibernate.test.One" );
		parse( "from o in class org.hibernate.test.Foo" );
		parse( "from o in class org.hibernate.test.Fo" );
		parse( "from o in class org.hibernate.test.Glarch" );
		parse( "from o in class org.hibernate.test.Fum" );
		parse( "from q in class org.hibernate.test.Qux where q.stuff is null" );
		parse( "from q in class org.hibernate.test.Qux where q.stuff=?" );
		parse( "from q in class org.hibernate.test.Qux" );
		parse( "from g in class org.hibernate.test.Glarch where g.version=2" );
		parse( "from g in class org.hibernate.test.Glarch where g.next is not null" );
		parse( "from g in class org.hibernate.test.Glarch order by g.order asc" );
		parse( "from foo in class org.hibernate.test.Foo order by foo.string asc" );
		parse( "select parent, child from parent in class org.hibernate.test.Foo, child in class org.hibernate.test.Foo where parent.foo = child" );
		parse( "select count(distinct child.id), count(distinct parent.id) from parent in class org.hibernate.test.Foo, child in class org.hibernate.test.Foo where parent.foo = child" );
		parse( "select child.id, parent.id, child.long from parent in class org.hibernate.test.Foo, child in class org.hibernate.test.Foo where parent.foo = child" );
		parse( "select child.id, parent.id, child.long, child, parent.foo from parent in class org.hibernate.test.Foo, child in class org.hibernate.test.Foo where parent.foo = child" );
		parse( "select parent, child from parent in class org.hibernate.test.Foo, child in class org.hibernate.test.Foo where parent.foo = child and parent.string='a string'" );
		parse( "from fee in class org.hibernate.test.Fee" );
		parse( "from org.hibernate.test.Foo foo where foo.custom.s1 = 'one'" );
		parse( "from im in class org.hibernate.test.Immutable where im = ?" );
		parse( "from foo in class org.hibernate.test.Foo" );
		parse( "from foo in class org.hibernate.test.Foo where foo.char='X'" );
		parse( "select elements(baz.stringArray) from baz in class org.hibernate.test.Baz" );
		parse( "select distinct elements(baz.stringArray) from baz in class org.hibernate.test.Baz" );
		parse( "select elements(baz.fooArray) from baz in class org.hibernate.test.Baz" );
		parse( "from foo in class org.hibernate.test.Fo" );
		parse( "from foo in class org.hibernate.test.Foo where foo.dependent.qux.foo.string = 'foo2'" );
		parse( "from org.hibernate.test.Bar bar where bar.object.id = ? and bar.object.class = ?" );
		parse( "select one from org.hibernate.test.One one, org.hibernate.test.Bar bar where bar.object.id = one.id and bar.object.class = 'O'" );
		parse( "from l in class org.hibernate.test.Location where l.countryCode = 'AU' and l.description='foo bar'" );
		parse( "from org.hibernate.test.Bar bar" );
		parse( "From org.hibernate.test.Bar bar" );
		parse( "From org.hibernate.test.Foo foo" );
		parse( "from o in class org.hibernate.test.Baz" );
		parse( "from o in class org.hibernate.test.Foo" );
		parse( "from f in class org.hibernate.test.Foo" );
		parse( "select fum.id from fum in class org.hibernate.test.Fum where not fum.fum='FRIEND'" );
		parse( "select fum.id from fum in class org.hibernate.test.Fum where not fum.fum='FRIEND'" );
		parse( "from fum in class org.hibernate.test.Fum where not fum.fum='FRIEND'" );
		parse( "from fo in class org.hibernate.test.Fo where fo.id.string like 'an instance of fo'" );
		parse( "from org.hibernate.test.Inner" );
		parse( "from org.hibernate.test.Outer o where o.id.detailId = ?" );
		parse( "from org.hibernate.test.Outer o where o.id.master.id.sup.dudu is not null" );
		parse( "from org.hibernate.test.Outer o where o.id.master.id.sup.id.akey is not null" );
		parse( "select o.id.master.id.sup.dudu from org.hibernate.test.Outer o where o.id.master.id.sup.dudu is not null" );
		parse( "select o.id.master.id.sup.id.akey from org.hibernate.test.Outer o where o.id.master.id.sup.id.akey is not null" );
		parse( "from org.hibernate.test.Outer o where o.id.master.bla = ''" );
		parse( "from org.hibernate.test.Outer o where o.id.master.id.one = ''" );
		parse( "from org.hibernate.test.Inner inn where inn.id.bkey is not null and inn.backOut.id.master.id.sup.id.akey > 'a'" );
		parse( "from org.hibernate.test.Outer as o left join o.id.master m left join m.id.sup where o.bubu is not null" );
		parse( "from org.hibernate.test.Outer as o left join o.id.master.id.sup s where o.bubu is not null" );
		parse( "from org.hibernate.test.Outer as o left join o.id.master m left join o.id.master.id.sup s where o.bubu is not null" );
		parse( "select fum1.fo from fum1 in class org.hibernate.test.Fum where fum1.fo.fum is not null" );
		parse( "from fum1 in class org.hibernate.test.Fum where fum1.fo.fum is not null order by fum1.fo.fum" );
		parse( "select elements(fum1.friends) from fum1 in class org.hibernate.test.Fum" );
		parse( "from fum1 in class org.hibernate.test.Fum, fr in elements( fum1.friends )" );
		parse( "select new Jay(eye) from org.hibernate.test.Eye eye" );
		parse( "from org.hibernate.test.Category cat where cat.name='new foo'" );
		parse( "from org.hibernate.test.Category cat where cat.name='new sub'" );
		parse( "from org.hibernate.test.Up up order by up.id2 asc" );
		parse( "from org.hibernate.test.Down down" );
		parse( "from org.hibernate.test.Up up" );
		parse( "from m in class org.hibernate.test.Master" );
		parse( "from s in class org.hibernate.test.Several" );
		parse( "from s in class org.hibernate.test.Single" );
		parse( "\n" +
				"		from d in class \n" +
				"			org.hibernate.test.Detail\n" +
				"	" );
		parse( "from c in class org.hibernate.test.Category where c.name = org.hibernate.test.Category.ROOT_CATEGORY" );
		parse( "select c from c in class org.hibernate.test.Container, s in class org.hibernate.test.Simple where c.oneToMany[2] = s" );
		parse( "select c from c in class org.hibernate.test.Container, s in class org.hibernate.test.Simple where c.manyToMany[2] = s" );
		parse( "select c from c in class org.hibernate.test.Container, s in class org.hibernate.test.Simple where s = c.oneToMany[2]" );
		parse( "select c from c in class org.hibernate.test.Container, s in class org.hibernate.test.Simple where s = c.manyToMany[2]" );
		parse( "select c from c in class org.hibernate.test.Container where c.oneToMany[0].name = 's'" );
		parse( "select c from c in class org.hibernate.test.Container where c.manyToMany[0].name = 's'" );
		parse( "select c from c in class org.hibernate.test.Container where 's' = c.oneToMany[2 - 2].name" );
		parse( "select c from c in class org.hibernate.test.Container where 's' = c.manyToMany[(3+1)/4-1].name" );
		parse( "select c from c in class org.hibernate.test.Container where c.manyToMany[ maxindex(c.manyToMany) ].count = 2" );
		parse( "select c from c in class org.hibernate.test.Container where c.oneToMany[ c.manyToMany[0].count ].name = 's'" );
		parse( "select c from org.hibernate.test.Container c where c.manyToMany[ c.oneToMany[0].count ].name = 's'" );
		parse( "select count(comp.name) from org.hibernate.test.Container c join c.components comp" );
		parse( "from org.hibernate.test.Parent p left join fetch p.child" );
		parse( "from org.hibernate.test.Parent p join p.child c where c.x > 0" );
		parse( "from org.hibernate.test.Child c join c.parent p where p.x > 0" );
		parse( "from org.hibernate.test.Child" );
		parse( "from org.hibernate.test.MoreStuff" );
		parse( "from org.hibernate.test.Many" );
		parse( "from org.hibernate.test.Fee" );
		parse( "from org.hibernate.test.Qux" );
		parse( "from org.hibernate.test.Fumm" );
		parse( "from org.hibernate.test.Parent" );
		parse( "from org.hibernate.test.Simple" );
		parse( "from org.hibernate.test.Holder" );
		parse( "from org.hibernate.test.Part" );
		parse( "from org.hibernate.test.Baz" );
		parse( "from org.hibernate.test.Vetoer" );
		parse( "from org.hibernate.test.Sortable" );
		parse( "from org.hibernate.test.Contained" );
		parse( "from org.hibernate.test.Circular" );
		parse( "from org.hibernate.test.Stuff" );
		parse( "from org.hibernate.test.Immutable" );
		parse( "from org.hibernate.test.Container" );
		parse( "from org.hibernate.test.One" );
		parse( "from org.hibernate.test.Foo" );
		parse( "from org.hibernate.test.Fo" );
		parse( "from org.hibernate.test.Glarch" );
		parse( "from org.hibernate.test.Fum" );
		parse( "from org.hibernate.test.Glarch g" );
		parse( "from org.hibernate.test.Part" );
		parse( "from org.hibernate.test.Baz baz join baz.parts" );
		parse( "from c in class org.hibernate.test.Child where c.parent.count=66" );
		parse( "from org.hibernate.test.Parent p join p.child c where p.count=66" );
		parse( "select c, c.parent from c in class org.hibernate.test.Child order by c.parent.count" );
		parse( "select c, c.parent from c in class org.hibernate.test.Child where c.parent.count=66 order by c.parent.count" );
		parse( "select c, c.parent, c.parent.count from c in class org.hibernate.test.Child order by c.parent.count" );
		parse( "FROM p IN CLASS org.hibernate.test.Parent WHERE p.count = ?" );
		parse( "select count(*) from org.hibernate.test.Container as c join c.components as ce join ce.simple as s where ce.name='foo'" );
		parse( "select c, s from org.hibernate.test.Container as c join c.components as ce join ce.simple as s where ce.name='foo'" );
		parse( "from s in class org.hibernate.test.Simple" );
		parse( "from m in class org.hibernate.test.Many" );
		parse( "from o in class org.hibernate.test.One" );
		parse( "from c in class org.hibernate.test.Container" );
		parse( "from o in class org.hibernate.test.Child" );
		parse( "from o in class org.hibernate.test.MoreStuff" );
		parse( "from o in class org.hibernate.test.Many" );
		parse( "from o in class org.hibernate.test.Fee" );
		parse( "from o in class org.hibernate.test.Qux" );
		parse( "from o in class org.hibernate.test.Fumm" );
		parse( "from o in class org.hibernate.test.Parent" );
		parse( "from o in class org.hibernate.test.Simple" );
		parse( "from o in class org.hibernate.test.Holder" );
		parse( "from o in class org.hibernate.test.Part" );
		parse( "from o in class org.hibernate.test.Baz" );
		parse( "from o in class org.hibernate.test.Vetoer" );
		parse( "from o in class org.hibernate.test.Sortable" );
		parse( "from o in class org.hibernate.test.Contained" );
		parse( "from o in class org.hibernate.test.Circular" );
		parse( "from o in class org.hibernate.test.Stuff" );
		parse( "from o in class org.hibernate.test.Immutable" );
		parse( "from o in class org.hibernate.test.Container" );
		parse( "from o in class org.hibernate.test.One" );
		parse( "from o in class org.hibernate.test.Foo" );
		parse( "from o in class org.hibernate.test.Fo" );
		parse( "from o in class org.hibernate.test.Glarch" );
		parse( "from o in class org.hibernate.test.Fum" );
		parse( "from c in class org.hibernate.test.C2 where 1=1 or 1=1" );
		parse( "from b in class org.hibernate.test.B" );
		parse( "from a in class org.hibernate.test.A" );
		parse( "from b in class org.hibernate.test.B" );
		parse( "from org.hibernate.test.E e join e.reverse as b where b.count=1" );
		parse( "from org.hibernate.test.E e join e.as as b where b.count=1" );
		parse( "from org.hibernate.test.B" );
		parse( "from org.hibernate.test.C1" );
		parse( "from org.hibernate.test.C2" );
		parse( "from org.hibernate.test.E e, org.hibernate.test.A a where e.reverse = a.forward and a = ?" );
		parse( "from org.hibernate.test.E e join fetch e.reverse" );
		parse( "from org.hibernate.test.E e" );
		parse( "select max(s.count) from s in class org.hibernate.test.Simple" );
		parse( "select new org.hibernate.test.S(s.count, s.address) from s in class org.hibernate.test.Simple" );
		parse( "select max(s.count) from s in class org.hibernate.test.Simple" );
		parse( "select count(*) from s in class org.hibernate.test.Simple" );
		parse( "from s in class org.hibernate.test.Simple where s.name=:name and s.count=:count" );
		parse( "from s in class org.hibernate.test.Simple where s.name in (:several0_, :several1_)" );
		parse( "from s in class org.hibernate.test.Simple where s.name in (:stuff0_, :stuff1_)" );
		parse( "from org.hibernate.test.Simple s where s.name=?" );
		parse( "from org.hibernate.test.Simple s where s.name=:name" );
		parse( "from s in class org.hibernate.test.Simple where upper( s.name ) ='SIMPLE 1'" );
		parse( "from s in class org.hibernate.test.Simple where not( upper( s.name ) ='yada' or 1=2 or 'foo'='bar' or not('foo'='foo') or 'foo' like 'bar' )" );
		parse( "from s in class org.hibernate.test.Simple where lower( s.name || ' foo' ) ='simple 1 foo'" );
		parse( "from s in class org.hibernate.test.Simple where upper( s.other.name ) ='SIMPLE 2'" );
		parse( "from s in class org.hibernate.test.Simple where not ( upper( s.other.name ) ='SIMPLE 2' )" );
		parse( "select distinct s from s in class org.hibernate.test.Simple where ( ( s.other.count + 3 ) = (15*2)/2 and s.count = 69) or ( ( s.other.count + 2 ) / 7 ) = 2" );
		parse( "select s from s in class org.hibernate.test.Simple where ( ( s.other.count + 3 ) = (15*2)/2 and s.count = 69) or ( ( s.other.count + 2 ) / 7 ) = 2 order by s.other.count" );
		parse( "select sum(s.count) from s in class org.hibernate.test.Simple group by s.count having sum(s.count) > 10" );
		parse( "select s.count from s in class org.hibernate.test.Simple group by s.count having s.count = 12" );
		parse( "select s.id, s.count, count(t), max(t.date) from s in class org.hibernate.test.Simple, t in class org.hibernate.test.Simple where s.count = t.count group by s.id, s.count order by s.count" );
		parse( "from s in class org.hibernate.test.Simple" );
		parse( "from s in class org.hibernate.test.Simple where s.name = ?" );
		parse( "from s in class org.hibernate.test.Simple where s.name = ? and upper(s.name) = ?" );
		parse( "from s in class org.hibernate.test.Simple where s.name = :foo and upper(s.name) = :bar or s.count=:count or s.count=:count + 1" );
		parse( "select s.id from s in class org.hibernate.test.Simple" );
		parse( "select all s, s.other from s in class org.hibernate.test.Simple where s = :s" );
		parse( "from s in class org.hibernate.test.Simple where s.name in (:name_list0_, :name_list1_) and s.count > :count" );
		parse( "from org.hibernate.test.Simple s" );
		parse( "from org.hibernate.test.Simple s" );
		parse( "from org.hibernate.test.Assignable" );
		parse( "from org.hibernate.test.Category" );
		parse( "from org.hibernate.test.Simple" );
		parse( "from org.hibernate.test.A" );
		parse( "from foo in class org.hibernate.test.Foo where foo.string=?" );
		parse( "from foo in class org.hibernate.test.Foo" );
		parse( "from org.hibernate.test.Po po, org.hibernate.test.Lower low where low.mypo = po" );
		parse( "from org.hibernate.test.Po po join po.set as sm where sm.amount > 0" );
		parse( "from org.hibernate.test.Po po join po.top as low where low.foo = 'po'" );
		parse( "from org.hibernate.test.SubMulti sm join sm.children smc where smc.name > 'a'" );
		parse( "select s, ya from org.hibernate.test.Lower s join s.yetanother ya" );
		parse( "from org.hibernate.test.Lower s1 join s1.bag s2" );
		parse( "from org.hibernate.test.Lower s1 left join s1.bag s2" );
		parse( "select s, a from org.hibernate.test.Lower s join s.another a" );
		parse( "select s, a from org.hibernate.test.Lower s left join s.another a" );
		parse( "from org.hibernate.test.Top s, org.hibernate.test.Lower ls" );
		parse( "from org.hibernate.test.Lower ls join ls.set s where s.name > 'a'" );
		parse( "from org.hibernate.test.Po po join po.list sm where sm.name > 'a'" );
		parse( "from org.hibernate.test.Lower ls inner join ls.another s where s.name is not null" );
		parse( "from org.hibernate.test.Lower ls where ls.other.another.name is not null" );
		parse( "from org.hibernate.test.Multi m where m.derived like 'F%'" );
		parse( "from org.hibernate.test.SubMulti m where m.derived like 'F%'" );
		parse( "select s from org.hibernate.test.SubMulti as sm join sm.children as s where s.amount>-1 and s.name is null" );
		parse( "select elements(sm.children) from org.hibernate.test.SubMulti as sm" );
		parse( "select distinct sm from org.hibernate.test.SubMulti as sm join sm.children as s where s.amount>-1 and s.name is null" );
		parse( "select distinct s from s in class org.hibernate.test.SubMulti where s.moreChildren[1].amount < 1.0" );
		parse( "from s in class org.hibernate.test.TrivialClass where s.id = 2" );
		parse( "select s.count from s in class org.hibernate.test.Top" );
		parse( "from s in class org.hibernate.test.Lower where s.another.name='name'" );
		parse( "from s in class org.hibernate.test.Lower where s.yetanother.name='name'" );
		parse( "from s in class org.hibernate.test.Lower where s.yetanother.name='name' and s.yetanother.foo is null" );
		parse( "from s in class org.hibernate.test.Top where s.count=1" );
		parse( "select s.count from s in class org.hibernate.test.Top, ls in class org.hibernate.test.Lower where ls.another=s" );
		parse( "select elements(ls.bag), elements(ls.set) from ls in class org.hibernate.test.Lower" );
		parse( "from s in class org.hibernate.test.Lower" );
		parse( "from s in class org.hibernate.test.Top" );
		parse( "from sm in class org.hibernate.test.SubMulti" );
		parse( "select\n" +
				"\n" +
				"s from s in class org.hibernate.test.Top where s.count>0" );
		parse( "from m in class org.hibernate.test.Multi where m.count>0 and m.extraProp is not null" );
		parse( "from m in class org.hibernate.test.Top where m.count>0 and m.name is not null" );
		parse( "from m in class org.hibernate.test.Lower where m.other is not null" );
		parse( "from m in class org.hibernate.test.Multi where m.other.id = 1" );
		parse( "from m in class org.hibernate.test.SubMulti where m.amount > 0.0" );
		parse( "from m in class org.hibernate.test.Multi" );
		parse( "from m in class org.hibernate.test.Multi where m.class = SubMulti" );
		parse( "from m in class org.hibernate.test.Top where m.class = Multi" );
		parse( "from s in class org.hibernate.test.Top" );
		parse( "from ls in class org.hibernate.test.Lower" );
		parse( "from ls in class org.hibernate.test.Lower, s in elements(ls.bag) where s.id is not null" );
		parse( "from ls in class org.hibernate.test.Lower, s in elements(ls.set) where s.id is not null" );
		parse( "from o in class org.hibernate.test.Top" );
		parse( "from o in class org.hibernate.test.Po" );
		parse( "from ChildMap cm where cm.parent is not null" );
		parse( "from ParentMap cm where cm.child is not null" );
		parse( "from org.hibernate.test.Componentizable" );
	}

	@Test public void testUnnamedParameter() throws Exception {
		parse( "select foo, bar from org.hibernate.test.Foo foo left outer join foo.foo bar where foo = ?" ); // Added '?' as a valid expression.
	}

	@Test public void testInElements() throws Exception {
		parse( "from bar in class org.hibernate.test.Bar, foo in elements(bar.baz.fooArray)" );   // Added collectionExpr as a valid 'in' clause.
	}

	@Test public void testDotElements() throws Exception {
		parse( "select distinct foo from baz in class org.hibernate.test.Baz, foo in elements(baz.fooArray)" );
		parse( "select foo from baz in class org.hibernate.test.Baz, foo in elements(baz.fooSet)" );
		parse( "select foo from baz in class org.hibernate.test.Baz, foo in elements(baz.fooArray)" );
		parse( "from org.hibernate.test.Baz baz where 'b' in elements(baz.collectionComponent.nested.foos) and 1.0 in elements(baz.collectionComponent.nested.floats)" );
	}

	@Test public void testSelectAll() throws Exception {
		parse( "select all s, s.other from s in class org.hibernate.test.Simple where s = :s" );
	}

	@Test public void testNot() throws Exception {
		// Cover NOT optimization in HqlParser
		parse( "from eg.Cat cat where not ( cat.kittens.size < 1 )" );
		parse( "from eg.Cat cat where not ( cat.kittens.size > 1 )" );
		parse( "from eg.Cat cat where not ( cat.kittens.size >= 1 )" );
		parse( "from eg.Cat cat where not ( cat.kittens.size <= 1 )" );
		parse( "from eg.DomesticCat cat where not ( cat.name between 'A' and 'B' ) " );
		parse( "from eg.DomesticCat cat where not ( cat.name not between 'A' and 'B' ) " );
		parse( "from eg.Cat cat where not ( not cat.kittens.size <= 1 )" );
		parse( "from eg.Cat cat where not  not ( not cat.kittens.size <= 1 )" );
	}

	@Test public void testOtherSyntax() throws Exception {
		parse( "select bar from org.hibernate.test.Bar bar order by ((bar.x - :valueX)*(bar.x - :valueX))" );
		parse( "from bar in class org.hibernate.test.Bar, foo in elements(bar.baz.fooSet)" );
		parse( "from one in class org.hibernate.test.One, many in elements(one.manies) where one.id = 1 and many.id = 1" );
		parse( "from org.hibernate.test.Inner _inner join _inner.middles middle" );
		parse( "FROM m IN CLASS org.hibernate.test.Master WHERE NOT EXISTS ( FROM d IN elements(m.details) WHERE NOT d.i=5 )" );
		parse( "FROM m IN CLASS org.hibernate.test.Master WHERE NOT 5 IN ( SELECT d.i FROM d IN elements(m.details) )" );
		parse( "SELECT m FROM m IN CLASS org.hibernate.test.Master, d IN elements(m.details) WHERE d.i=5" );
		parse( "SELECT m FROM m IN CLASS org.hibernate.test.Master, d IN elements(m.details) WHERE d.i=5" );
		parse( "SELECT m.id FROM m IN CLASS org.hibernate.test.Master, d IN elements(m.details) WHERE d.i=5" );
		// I'm not sure about these... [jsd]
//        parse("select bar.string, foo.string from bar in class org.hibernate.test.Bar inner join bar.baz as baz inner join elements(baz.fooSet) as foo where baz.name = 'name'");
//        parse("select bar.string, foo.string from bar in class org.hibernate.test.Bar, bar.baz as baz, elements(baz.fooSet) as foo where baz.name = 'name'");
//        parse("select count(*) where this.amount>-1 and this.name is null");
//        parse("from sm in class org.hibernate.test.SubMulti where exists sm.children.elements");
	}

	@Test public void testEjbqlExtensions() throws Exception {
		parse( "select object(a) from Animal a where a.mother member of a.offspring" );
		parse( "select object(a) from Animal a where a.mother member a.offspring" ); //no member of
		parse( "select object(a) from Animal a where a.offspring is empty" );
	}

	@Test public void testEmptyFilter() throws Exception {
		parseFilter( "" );  //  Blank is a legitimate filter.
	}

	@Test public void testOrderByFilter() throws Exception {
		parseFilter( "order by this.id" );
	}

	@Test public void testRestrictionFilter() throws Exception {
		parseFilter( "where this.name = ?" );
	}

	@Test public void testNoFrom() throws Exception {
		System.out.println( "***** This test ensures that an error is detected ERROR MESSAGES ARE OKAY!  *****" );
		HqlParser parser = HqlParser.getInstance( "" );
		parser.setFilter( false );
		parser.statement();
		assertEquals( "Parser allowed no FROM clause!", 1, parser.getParseErrorHandler().getErrorCount() );
		System.out.println( "***** END OF ERROR TEST  *****" );
	}

	@Test public void testHB1042() throws Exception {
		parse( "select x from fmc_web.pool.Pool x left join x.containers c0 where (upper(x.name) = upper(':') and c0.id = 1)" );
	}

	@Test public void testKeywordInPath() throws Exception {
		// The keyword 'order' used as a property name.
		parse( "from Customer c where c.order.status = 'argh'" );
		// The keyword 'order' and 'count' used as a property name.
		parse( "from Customer c where c.order.count > 3" );
		// The keywords 'where', 'order' and 'count' used as a property name.
		parse( "select c.where from Customer c where c.order.count > 3" );
		parse( "from Interval i where i.end <:end" );
		parse( "from Letter l where l.case = :case" );
	}

	@Test public void testPathologicalKeywordAsIdentifier() throws Exception {
		// Super evil badness... a legitimate keyword!
		parse( "from Order order" );
		//parse( "from Order order join order.group" );
		parse( "from X x order by x.group.by.from" );
		parse( "from Order x order by x.order.group.by.from" );
		parse( "select order.id from Order order" );
		parse( "select order from Order order" );
		parse( "from Order order where order.group.by.from is not null" );
		parse( "from Order order order by order.group.by.from" );
		// Okay, now this is getting silly.
		parse( "from Group as group group by group.by.from" );
	}

    @Test public void testHHH354() throws Exception {
        parse( "from Foo f where f.full = 'yep'");
    }

    @Test public void testWhereAsIdentifier() throws Exception {
        // 'where' as a package name
        parse( "from where.Order" );
    }

	@Test public void testEjbqlKeywordsAsIdentifier() throws Exception {
		parse( "from org.hibernate.test.Bar bar where bar.object.id = ? and bar.object.class = ?" );
	}

	@Test public void testConstructorIn() throws Exception {
		parse( "from org.hibernate.test.Bar bar where (b.x, b.y, b.z) in (select foo, bar, baz from org.hibernate.test.Foo)" );
	}

    @Test public void testMultiByteCharacters() throws Exception {
        parse ("from User user where user.name like '%nn\u4e2dnn%'");
        // Test for HHH-558
        parse ("from User user where user.\u432d like '%\u4e2d%'");
        parse ("from \u432d \u432d where \u432d.name like '%fred%'");        
    }

    @Test public void testHHH719() throws Exception {
        // Some SQLs have function names with package qualifiers.
        parse("from Foo f order by com.fooco.SpecialFunction(f.id)");
    }

	@Test public void testHHH1107() throws Exception {
		parse("from Animal where zoo.address.street = '123 Bogus St.'");
	}


	@Test public void testHHH1247() throws Exception {
		parse("select distinct user.party from com.itf.iceclaims.domain.party.user.UserImpl user inner join user.party.$RelatedWorkgroups relatedWorkgroups where relatedWorkgroups.workgroup.id = :workgroup and relatedWorkgroups.effectiveTime.start <= :datesnow and relatedWorkgroups.effectiveTime.end > :dateenow ");
	}

    @Test public void testHHH1780() throws Exception {
        // verifies the tree contains a NOT->EXISTS subtree
        class Verifier {
            public boolean verify(AST root) {
                Stack<AST> queue = new Stack<AST>();
                queue.push( root );
                while ( !queue.isEmpty() ) {
                    AST parent = queue.pop();
                    AST child = parent.getFirstChild();
                    while ( child != null ) {
                        if ( parent.getType() == HqlTokenTypes.NOT &&
                                child.getType() == HqlTokenTypes.EXISTS ) {
                            return true;
                        }
                        queue.push( child );
                        child = child.getNextSibling();
                    }
                }
                return false;
            }
        }

        // test inversion of AND
        AST ast = doParse(
                "from Person p where not ( p.name is null and exists(select a.id from Address a where a.id=p.id))",
                false
        );

        assertTrue( new Verifier().verify( ast ) );

        // test inversion of OR
        ast = doParse(
                "from Person p where not ( p.name is null or exists(select a.id from Address a where a.id=p.id))",
                false
        );

        assertTrue( new Verifier().verify( ast ) );
    }


    @Test public void testLineAndColumnNumber() throws Exception {
		AST ast = doParse("from Foo f\nwhere f.name = 'fred'",false);
		// Find some of the nodes and check line and column values.
		ASTIterator iter = new ASTIterator(ast);
		boolean foundFoo = false;
		boolean foundName = false;
		while (iter.hasNext())
		{
			AST n = iter.nextNode();
			if ("Foo".equals(n.getText()))
			{
				if (foundFoo)
					fail("Already found 'Foo'!");
				foundFoo = true;
				Node node = (Node)n;
				assertEquals(1,node.getLine());
				assertEquals(6,node.getColumn());
			}
			else if ("name".equals(n.getText()))
			{
				if (foundName)
					fail("Already found 'name'!");
				foundName = true;
				Node node = (Node)n;
				assertEquals(2,node.getLine());
				assertEquals(9,node.getColumn());
			}
		}
		assertTrue(foundFoo);
		assertTrue(foundName);
	}

	private void parseFilter(String input) throws TokenStreamException, RecognitionException {
		doParse( input, true );
	}

	private void parse(String input) throws RecognitionException, TokenStreamException {
		doParse( input, false );
	}

	private AST doParse(String input, boolean filter) throws RecognitionException, TokenStreamException {
		System.out.println( "input: ->" + ASTPrinter.escapeMultibyteChars(input) + "<-" );
		HqlParser parser = HqlParser.getInstance( input );
		parser.setFilter( filter );
		parser.statement();
		AST ast = parser.getAST();
		System.out.println( "AST  :  " + ast.toStringTree() + "" );
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		parser.showAst( ast, new PrintStream( baos ) );
		System.out.println( baos.toString() );
		assertEquals( "At least one error occurred during parsing!", 0, parser.getParseErrorHandler().getErrorCount() );
		return ast;
	}


}
