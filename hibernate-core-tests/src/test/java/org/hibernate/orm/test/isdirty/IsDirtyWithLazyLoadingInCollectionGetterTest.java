package org.hibernate.orm.test.isdirty;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.PostLoad;
import jakarta.persistence.Transient;

import org.hibernate.Hibernate;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DomainModel(annotatedClasses = {
		IsDirtyWithLazyLoadingInCollectionGetterTest.Owner.class,
		IsDirtyWithLazyLoadingInCollectionGetterTest.Line.class,
		IsDirtyWithLazyLoadingInCollectionGetterTest.Reference.class
})
@SessionFactory
@Jira("https://hibernate.atlassian.net/browse/HHH-21061")
class IsDirtyWithLazyLoadingInCollectionGetterTest {
	@Test
	void detectsCollectionChangedByPostLoadDuringCollectionCheck(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Reference reference = new Reference();
			reference.id = 1L;
			session.persist( reference );
			Owner owner = new Owner();
			owner.id = 1L;
			Line line = new Line();
			line.setText( "line" );
			owner.lines.add( line );
			session.persist( owner );
		} );
		scope.inTransaction( session -> {
			Owner owner = session.find( Owner.class, 1L );
			Hibernate.initialize( owner.lines );
			Reference reference = session.getReference( Reference.class, 1L );
			assertFalse( Hibernate.isInitialized( reference ) );
			// Trigger lazy loading from the element getter during equalsSnapshot(), after loading the owner.
			owner.lines.get( 0 ).onRead = () -> Hibernate.initialize( reference );
			boolean dirty = session.isDirty();
			assertTrue( Hibernate.isInitialized( reference ) );
			assertTrue( dirty, "The first check must see the collection changed by @PostLoad" );
		} );
	}

	@Entity(name = "CollectionOwner")
	static class Owner {
		@Id
		Long id;
		@ElementCollection
		@OrderColumn
		List<Line> lines = new ArrayList<>();
	}

	@Embeddable
	@Access(AccessType.PROPERTY)
	public static class Line {
		private String text;
		@Transient
		Runnable onRead;

		public String getText() {
			if ( onRead != null ) {
				onRead.run();
			}
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}
	}

	@Entity(name = "ReferenceData")
	public static class Reference {
		@Id
		Long id;
		@ElementCollection(fetch = FetchType.EAGER)
		List<String> labels = new ArrayList<>();

		@PostLoad
		void populateDefaultLabel() {
			if ( labels.isEmpty() ) {
				labels.add( "default" );
			}
		}
	}
}
