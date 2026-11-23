package org.hibernate.orm.test.query.hql;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Bulk deletes must support implicit navigation through two to-one associations.
///
/// @author Steve Ebersole
@DomainModel(annotatedClasses = {
		DeleteWithTwoHopAssociationTest.Limit.class,
		DeleteWithTwoHopAssociationTest.Bridge.class,
		DeleteWithTwoHopAssociationTest.Selector.class
})
@SessionFactory
@JiraKey("HHH-4145")
public class DeleteWithTwoHopAssociationTest {
	@Test
	void testDeleteWithTwoHopAssociationAndEntityParameter(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Selector selected = new Selector( 1L );
			Selector other = new Selector( 2L );
			session.persist( selected );
			session.persist( other );
			Bridge first = new Bridge( 10L, selected );
			Bridge second = new Bridge( 11L, selected );
			Bridge unrelated = new Bridge( 20L, other );
			session.persist( first );
			session.persist( second );
			session.persist( unrelated );
			session.persist( new Limit( 1L, first, 0 ) );
			session.persist( new Limit( 2L, second, null ) );
			session.persist( new Limit( 3L, first, 5 ) );
			session.persist( new Limit( 4L, unrelated, 0 ) );
			session.persist( new Limit( 5L, unrelated, null ) );
			session.persist( new Limit( 6L, unrelated, 5 ) );
		} );

		scope.inTransaction( session -> {
			int deleted = session.createMutationQuery(
					"delete from TwoHopDeleteLimit l where l.bridge.selector = :selector "
							+ "and (l.value = 0 or l.value is null)"
			).setParameter( "selector", session.getReference( Selector.class, 1L ) ).executeUpdate();
			assertEquals( 2, deleted );
		} );

		scope.inTransaction( session -> {
			assertEquals( List.of( 3L, 4L, 5L, 6L ), session.createQuery(
					"select l.id from TwoHopDeleteLimit l order by l.id", Long.class
			).getResultList() );
			assertEquals( 3L, session.createQuery( "select count(*) from TwoHopDeleteBridge", Long.class )
					.getSingleResult() );
			assertEquals( 2L, session.createQuery( "select count(*) from TwoHopDeleteSelector", Long.class )
					.getSingleResult() );
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Entity(name = "TwoHopDeleteLimit")
	@Table(name = "two_hop_delete_limit")
	static class Limit {
		@Id
		private Long id;

		@ManyToOne(optional = false)
		private Bridge bridge;

		@Column(name = "limit_value")
		private Integer value;

		public Limit() {
		}

		Limit(Long id, Bridge bridge, Integer value) {
			this.id = id;
			this.bridge = bridge;
			this.value = value;
		}
	}

	@Entity(name = "TwoHopDeleteBridge")
	@Table(name = "two_hop_delete_bridge")
	static class Bridge {
		@Id
		private Long id;

		@ManyToOne(optional = false)
		private Selector selector;

		public Bridge() {
		}

		Bridge(Long id, Selector selector) {
			this.id = id;
			this.selector = selector;
		}
	}

	@Entity(name = "TwoHopDeleteSelector")
	@Table(name = "two_hop_delete_selector")
	static class Selector {
		@Id
		private Long id;

		public Selector() {
		}

		Selector(Long id) {
			this.id = id;
		}
	}
}
