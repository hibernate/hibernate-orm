//$Id: Main.java 7369 2005-07-04 03:18:34Z oneovthafew $
package org.hibernate.auction;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.hibernate.FetchMode;
import org.hibernate.FlushMode;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.criterion.Example;
import org.hibernate.criterion.Expression;
import org.hibernate.criterion.MatchMode;


/**
 * Demonstrate some useful features of Hibernate.
 *
 * @author Gavin King
 */
public class Main {

	private SessionFactory factory;

	/**
	 * Demonstrates HQL projection/aggregation
	 */
	public void viewAllAuctionsFast() throws Exception {
		System.out.println("Viewing all auction item info");

		Session s = factory.openSession();
		Transaction tx=null;
		try {
			tx = s.beginTransaction();

			List auctions = s.createQuery(
				"select new AuctionInfo( item.id, item.description, item.ends, max(bid.amount) ) "
				+ "from AuctionItem item "
				+ "left join item.bids bid "
				+ "group by item.id, item.description, item.ends "
				+ "order by item.ends desc"
				)
				.setMaxResults(100)
				.list();

			Iterator iter = auctions.iterator();
			while ( iter.hasNext() ) {
				AuctionInfo ai = (AuctionInfo) iter.next();
				System.out.println(
					"Auction: " + ai.getId() + " - " + ai.getDescription() +
					", ends: " + ai.getEnds() +
					", highest bid: " + ai.getMaxAmount()
				);
			}
			System.out.println();

			tx.commit();
		}
		catch (Exception e) {
			if (tx!=null) tx.rollback();
			throw e;
		}
		finally {
			s.close();
		}
	}

	/**
	 * Demonstrates HQL with runtime fetch strategy
	 */
	public void viewAllAuctionsSlow() throws Exception {
		System.out.println("Viewing all auction item objects");

		Session s = factory.openSession();
		Transaction tx=null;
		try {
			s.setFlushMode(FlushMode.NEVER); //entirely optional!!
			tx = s.beginTransaction();

			List auctions = s.createQuery(
				"from AuctionItem item "
				+ "left join fetch item.bids bid left join fetch bid.bidder "
				+ "order by item.ends desc"
				)
				.setMaxResults(100)
				.list();

			Iterator iter = new HashSet(auctions).iterator();
			while ( iter.hasNext() ) {
				AuctionItem auction = (AuctionItem) iter.next();
				System.out.println(
					"Auction: " + auction.getId() + " - " + auction.getDescription() +
					", ends: " + auction.getEnds() +
					", bids: " + auction.getBids()
				);
			}
			System.out.println();

			tx.commit();
		}
		catch (Exception e) {
			if (tx!=null) tx.rollback();
			throw e;
		}
		finally {
			s.close();
		}
	}

	/**
	 * Demonstrates transitive persistence with detached object support
	 */
	public void bidOnAuction(User bidder, AuctionItem item, float amount) throws Exception {
		System.out.println("Creating a new bid for auction item: " + item.getId() + " by user: " + bidder.getId() );

		Session s = factory.openSession();
		Transaction tx=null;
		try {
			tx = s.beginTransaction();

			s.lock(item, LockMode.NONE);
			s.lock(bidder, LockMode.NONE);

			Bid bid = new Bid();
			bid.setBidder(bidder);
			bid.setDatetime( new Date() );
			bid.setAmount(amount);
			bid.setItem(item);
			bidder.getBids().add(bid);
			item.getBids().add(bid);

			tx.commit();
		}
		catch (Exception e) {
			if (tx!=null) tx.rollback();
			throw e;
		}
		finally {
			s.close();
		}
	}

	/**
	 * Demonstrates detached object support
	 */
	public void changeUserDetails(User user) throws Exception {
		System.out.println("Changing user details for: " + user.getId() );

		Session s = factory.openSession();
		Transaction tx=null;
		try {
			tx = s.beginTransaction();

			s.merge(user);

			tx.commit();
		}
		catch (Exception e) {
			if (tx!=null) tx.rollback();
			throw e;
		}
		finally {
			s.close();
		}
	}

	/**
	 * Demonstrates automatic dirty checking
	 */
	public void changeItemDescription(Long itemId, String description) throws Exception {
		System.out.println("Changing auction item description for: " + itemId );

		Session s = factory.openSession();
		Transaction tx=null;
		try {
			tx = s.beginTransaction();

			AuctionItem item = (AuctionItem) s.get(AuctionItem.class, itemId);
			if (item==null) throw new IllegalArgumentException("No item for the given id: " + itemId);
			item.setDescription(description);

			tx.commit();
		}
		catch (Exception e) {
			if (tx!=null) tx.rollback();
			throw e;
		}
		finally {
			s.close();
		}
	}

	/**
	 * Demonstrates query by criteria with runtime fetch strategy
	 */
	public void viewUserAuctions(Long sellerId) throws Exception {
		System.out.println("Viewing user and auctions: " + sellerId);

		Session s = factory.openSession();
		Transaction tx=null;
		try {
			tx = s.beginTransaction();

			List list = s.createCriteria(User.class)
				.add( Expression.eq("id", sellerId) )
				.setFetchMode("auctions", FetchMode.JOIN)
				.list();

			if (list.size()==0) throw new IllegalArgumentException("No user for the given id: " + sellerId);
			User user = (User) list.get(0);
			System.out.println(
				"User: " + user.getId() + " - " + user.getName() +
				", email: " + user.getEmail() +
				", auctions: " + user.getAuctions()
			);

			tx.commit();
		}
		catch (Exception e) {
			if (tx!=null) tx.rollback();
			throw e;
		}
		finally {
			s.close();
		}
	}

	/**
	 * Demonstrates query by example
	 */
	public void viewAuctionsByDescription(String description, int condition) throws Exception {
		String msg = "Viewing auctions containing: " + description;
		if (condition>0) msg += " with condition: " + condition + "/10";

		AuctionItem item = new AuctionItem();
		item.setDescription(description);
		item.setCondition(condition);

		Session s = factory.openSession();
		Transaction tx=null;
		try {
			tx = s.beginTransaction();

			Iterator iter = s.createCriteria(AuctionItem.class)
				.add( Example.create(item)
					.enableLike(MatchMode.ANYWHERE)
					.ignoreCase()
					.excludeZeroes()
				)
				.list()
				.iterator();

			System.out.println(msg);
			while ( iter.hasNext() ) {
				item = (AuctionItem) iter.next();
				System.out.println("Item: " + item.getId() + " - " + item.getDescription() );
			}
			System.out.println();

			tx.commit();
		}
		catch (Exception e) {
			if (tx!=null) tx.rollback();
			throw e;
		}
		finally {
			s.close();
		}
	}

	/**
	 * Demonstrates transitive persistence
	 */
	public void createTestAuctions() throws Exception {
		System.out.println("Setting up some test data");

		Session s = factory.openSession();
		Transaction tx = s.beginTransaction();

		User seller = new User();
		seller.setUserName("xam");
		seller.setName( new Name("Max", new Character('R'), "Andersen") );
		seller.setEmail("max@hibernate.org");
		seller.setPassword("******");
		seller.setAuctions( new ArrayList() );
		s.save(seller);
		User bidder1 = new User();
		bidder1.setUserName("1E1");
		bidder1.setName( new Name( "Gavin", new Character('A'), "King") );
		bidder1.setEmail("gavin@hibernate.org");
		bidder1.setPassword("******");
		bidder1.setBids( new ArrayList() );
		s.save(bidder1);
		User bidder2 = new User();
		bidder2.setUserName("steve");
		bidder2.setName( new Name("Steve", null, "Ebersole") );
		bidder2.setEmail("steve@hibernate.org");
		bidder2.setPassword("******");
		bidder2.setBids( new ArrayList() );
		s.save(bidder2);

		for ( int i=0; i<3; i++ ) {
			AuctionItem item = new AuctionItem();
			item.setShortDescription("Auction " + i);
			item.setDescription("the auction item number " + i);
			item.setEnds( new Date() );
			item.setBids( new ArrayList() );
			item.setSeller(seller);
			item.setCondition(i*3 + 2);
			for ( int j=0; j<i; j++ ) {

				Bid bid = new Bid();
				bid.setBidder(bidder1);
				bid.setAmount(j);
				bid.setDatetime( new Date() );
				bid.setItem(item);
				item.getBids().add(bid);
				bidder1.getBids().add(bid);

				Bid bid2 = new Bid();
				bid2.setBidder(bidder2);
				bid2.setAmount( j + 0.5f);
				bid2.setDatetime( new Date() );
				bid2.setItem(item);
				item.getBids().add(bid2);
				bidder2.getBids().add(bid2);
			}
			seller.getAuctions().add(item);
			mainItem = item;
		}
		mainBidder = bidder2;
		mainSeller = seller;

		BuyNow buyNow = new BuyNow();
		buyNow.setAmount(1.2f);
		buyNow.setDatetime( new Date() );
		buyNow.setBidder(mainBidder);
		buyNow.setItem(mainItem);
		mainBidder.getBids().add(buyNow);
		mainItem.getBids().add(buyNow);

		tx.commit();
		s.close();
	}

	static AuctionItem mainItem;
	static User mainBidder;
	static User mainSeller;

	public static void main(String[] args) throws Exception {

		final Main test = new Main();

		Configuration cfg = new Configuration()
			.addClass(AuctionItem.class)
			.addClass(Bid.class)
			.addClass(User.class)
			.setProperty(Environment.HBM2DDL_AUTO, "create");
		//cfg.setProperty("hibernate.show_sql", "true");

		test.factory = cfg.buildSessionFactory();

		test.createTestAuctions();
		test.viewAllAuctionsSlow();

		test.viewAllAuctionsFast();
		test.bidOnAuction(mainBidder, mainItem, 5.5f);
		test.viewAllAuctionsFast();

		test.viewUserAuctions( mainSeller.getId() );
		mainSeller.setEmail("max@jboss.org");
		test.changeUserDetails(mainSeller);
		test.changeItemDescription(mainItem.getId(), "new description");
		test.viewUserAuctions( mainSeller.getId() );

		test.viewAuctionsByDescription("It", 0);
		test.viewAuctionsByDescription("DESC", 3);
		test.viewAuctionsByDescription("DESC", 8);

		test.factory.close();

	}
}
