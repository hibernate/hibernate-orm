package org.hibernate.orm.test.refresh;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DomainModel(annotatedClasses = {
		RefreshAcrossSelectTest.Owner.class, RefreshAcrossSelectTest.Middle.class, RefreshAcrossSelectTest.Leaf.class
})
@SessionFactory
@JiraKey("HHH-21044")
class RefreshAcrossSelectTest {
	@Test
	void refreshAcrossNonCascadingSelectPreservesPendingChange(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var leaf = new Leaf();
			leaf.id = 1;
			leaf.label = "stored";
			session.persist( leaf );
			var middle = new Middle();
			middle.id = 1;
			middle.leaf = leaf;
			session.persist( middle );
			var owner = new Owner();
			owner.id = 1;
			owner.middle = middle;
			session.persist( owner );
		} );
		scope.inTransaction( session -> {
			var leaf = session.find( Leaf.class, 1 );
			var owner = session.getReference( Owner.class, 1 );
			leaf.label = "pending";
			session.refresh( owner );
		} );
		scope.inTransaction( session ->
				assertEquals( "pending", session.find( Leaf.class, 1 ).label ) );
	}

	@Entity(name = "ReviewOwner")
	static class Owner {
		@Id int id;
		@ManyToOne @Fetch(FetchMode.SELECT) Middle middle;
	}

	@Entity(name = "ReviewMiddle")
	static class Middle {
		@Id int id;
		@ManyToOne(cascade = CascadeType.REFRESH) Leaf leaf;
	}

	@Entity(name = "ReviewLeaf")
	static class Leaf {
		@Id int id;
		String label;
	}
}
