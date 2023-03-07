/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.annotations.idclass;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * @author Marco Belladelli
 */
@SessionFactory
@DomainModel(annotatedClasses = {
		IdClassBackrefTest.Bucket.class,
		IdClassBackrefTest.Line.class,
		IdClassBackrefTest.Item.class
})
@Jira("https://hibernate.atlassian.net/browse/HHH-16215")
public class IdClassBackrefTest {
	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createNativeMutationQuery( "insert into ITEM (id) values (100)" ).executeUpdate();
			session.createNativeMutationQuery( "insert into ITEM (id) values (101)" ).executeUpdate();
			session.createNativeMutationQuery( "insert into BUCKET (id) values (200)" ).executeUpdate();
			session.createNativeMutationQuery( "insert into LINE (id, item_id, bucket_id) values (300, 100, 200)" )
					.executeUpdate();
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createNativeMutationQuery( "delete from LINE" ).executeUpdate();
					session.createNativeMutationQuery( "delete from ITEM " ).executeUpdate();
					session.createNativeMutationQuery( "delete from BUCKET" ).executeUpdate();
				}
		);
	}

	@Test
	public void testInitializedLines(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Item item100 = session.find( Item.class, 100L );
			assertThat( item100.getLines().size() ).isEqualTo( 1 );

			Item item = item100.getLines().get( 0 ).getItem();
			assertThat( item ).isEqualTo( item100 );
		} );
	}

	@Test
	public void testFindABucket(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Bucket bucket = session.find( Bucket.class, new PkBucket().withId( 200l ) );
			assertThat( bucket.getLines().size() ).isEqualTo( 1 );
		} );
	}

	@Entity(name = "Line")
	@Table(name = "LINE")
	public static class Line {
		@Id
		private Long id;

		@ManyToOne(targetEntity = Item.class)
		private Item item;

		public Line() {
		}

		public Line(Long id, Item item) {
			this.id = id;
			this.item = item;
			item.addLine( this );
		}

		public Long getId() {
			return id;
		}

		public Item getItem() {
			return item;
		}
	}

	@Entity(name = "Item")
	@Table(name = "ITEM")
	public static class Item {
		@Id
		private Long id;

		@OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true)
		private List<Line> lines = new ArrayList<>();

		public Item() {
		}

		public Item(Long id) {
			this.id = id;
		}

		public Long getId() {
			return id;
		}

		public List<Line> getLines() {
			return lines;
		}

		public void addLine(Line line) {
			this.lines.add( line );
		}
	}

	public static class PkBucket implements Serializable {
		private Long id;

		public PkBucket withId(Long id) {
			this.id = id;
			return this;
		}

		public Long getId() {
			return id;
		}
	}

	@Entity(name = "Bucket")
	@Table(name = "BUCKET")
	@IdClass(PkBucket.class)
	public static class Bucket {
		@Id
		private Long id;

		@OneToMany(cascade = CascadeType.ALL)
		@JoinColumn( name = "bucket_id", referencedColumnName = "id", nullable = false )
		private List<Line> lines = new ArrayList<>();

		public Bucket() {
		}

		public Bucket(Long id) {
			this.id = id;
		}

		public Long getId() {
			return id;
		}

		public List<Line> getLines() {
			return lines;
		}

		public void addLine(Line line) {
			this.lines.add( line );
		}
	}
}
