package org.hibernate.orm.test.collection.basic;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OrderColumn;

import org.hibernate.Hibernate;
import org.hibernate.annotations.ListIndexBase;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@DomainModel(annotatedClasses = OwnerWithValues.class)
@SessionFactory
@JiraKey("HHH-21223")
class ScalarCollectionCardinalityTest {
	@Test
	void uninitializedCollectionSizes(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var owner = new OwnerWithValues();
			var pair = new Pair();
			pair.secondValue = "present";
			owner.pairs.add( pair );
			session.persist( owner );
		} );
		scope.inSession( session -> {
			var owner = session.find( OwnerWithValues.class, 1 );
			assertEquals( 0, Hibernate.size( owner.words ) );
			assertEquals( 1, Hibernate.size( owner.pairs ) );
			assertFalse( Hibernate.isInitialized( owner.words ) );
			assertFalse( Hibernate.isInitialized( owner.pairs ) );
		} );
	}
}

@Entity
class OwnerWithValues {
	@Id int id = 1;
	@ElementCollection Set<Pair> pairs = new HashSet<>();
	@ElementCollection @OrderColumn @ListIndexBase(5) List<String> words = new ArrayList<>();
}

@Embeddable
class Pair {
	String firstValue;
	String secondValue;
}
