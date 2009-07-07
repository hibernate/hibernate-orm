// $Id$
/*
* JBoss, Home of Professional Open Source
* Copyright 2008, Red Hat Middleware LLC, and individual contributors
* by the @authors tag. See the copyright.txt in the distribution for a
* full listing of individual contributors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package test;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Set;
import javax.persistence.Tuple;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import static javax.persistence.criteria.JoinType.INNER;
import javax.persistence.criteria.ListJoin;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;

import model.Item;
import model.Item_;
import model.Order;
import model.Order_;
import model.Product;
import model.Product_;
import model.Shop_;

/**
 * Writing queries involves passing typesafe, statically cached, metamodel
 * objects to the query builder in order to create the various parts of
 * the query. The typesafe metamodel objects were validated at init time,
 * so it is impossible to build invalid queries in the application code.
 *
 * @author Max Andersen
 * @author Hardy Ferentschik
 * @author Emmanuel Bernard
 */
public class QueryTest {

	CriteriaBuilder qb;

	public void test() {
		CriteriaQuery<Tuple> q = qb.createTupleQuery();

		Root<Order> order = q.from( Order.class );
		Join<Item, Product> product = order.join( Order_.items )
				.join( Item_.product );

		Path<BigDecimal> price = product.get( Product_.price );
		Path<Boolean> filled = order.get( Order_.filled );
		Path<Date> date = order.get( Order_.date );

		q.select( qb.tuple( order, product ) )
				.where( qb.and( qb.gt( price, 100.00 ), qb.not( filled ) ) )
				.orderBy( qb.asc( price ), qb.desc( date ) );
	}

	public void testUntypesafe() {
		CriteriaQuery<Tuple> q = qb.createTupleQuery();

		Root<Order> order = q.from( Order.class );
		Join<Item, Product> product = order.join( "items" )
				.join( "product" );

		Path<BigDecimal> price = product.get( "price" );
		Path<Boolean> filled = order.get( "filled" );
		Path<Date> date = order.get( "date" );

		q.select( qb.tuple( order, product ) )
				.where( qb.and( qb.gt( price, 100.00 ), qb.not( filled ) ) )
				.orderBy( qb.asc( price ), qb.desc( date ) );
	}

	/**
	 * Navigation by joining
	 */
	public void test2() {
		CriteriaQuery<Product> q = qb.createQuery( Product.class );

		Root<Product> product = q.from( Product.class );
		Join<Item, Order> order = product.join( Product_.items )
				.join( Item_.order );

		q.select( product )
				.where( qb.equal( order.get( Order_.id ), 12345l ) );
	}

	public void testMap() {
		CriteriaQuery<Item> q = qb.createQuery( Item.class );

		Root<Item> item = q.from( Item.class );
		item.join( Item_.namedOrders );
	}

	/**
	 * Navigation by compound Path
	 */
	public void test3() {
		CriteriaQuery<Item> q = qb.createQuery( Item.class );

		Root<Item> item = q.from( Item.class );
		Path<String> shopName = item.get( Item_.order )
				.get( Order_.shop )
				.get( Shop_.name );
		q.select( item )
				.where( qb.equal( shopName, "amazon.com" ) );
	}

//	public void test4() {
//		CriteriaQuery q = qb.create();
//
//		Root<Order> order = q.from(Order.class);
//		ListJoin<Order, String> note = order.join(Order_.notes);
//		Expression<Set<Item>> items = order.get(Order_.items);
//		order.fetch(Order_.items, JoinType.INNER);
//
//		q.select(note)
//		 .where( qb.and( qb.lt(note.index(), 10), qb.isNotEmpty(items) ) );
//	}

	public void test4Untypesafe() {
		CriteriaQuery<String> q = qb.createQuery( String.class );

		Root<Order> order = q.from( Order.class );
		ListJoin<Order, String> note = order.joinList( "notes" );
		Expression<Set<Item>> items = order.get( "items" );
		order.fetch( "items", INNER );

		q.select( note )
				.where( qb.and( qb.lt( note.index(), 10 ), qb.isNotEmpty( items ) ) );
	}

	/*public void test5() {
		Expression<Long> l= null;
		Expression<Integer> i= null;
		Expression<Float> x= null;
		Expression<Float> y= null;
		
		Expression<Number> n;
		Expression<Float> f;
		Expression<String> s = null;
		
		n = qb.quot(l, i);
		
		f = qb.sum(x, y);
		
		n = qb.quot(x, y);
		
		javax.jpamodelgen.criteria.Order o = qb.asc(n);
		javax.jpamodelgen.criteria.Order p = qb.ascending(s);
	}*/

}
