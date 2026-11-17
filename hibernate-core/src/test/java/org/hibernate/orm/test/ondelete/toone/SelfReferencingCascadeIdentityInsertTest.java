package org.hibernate.orm.test.ondelete.toone;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import org.hibernate.PropertyValueException;
import org.hibernate.TransientPropertyValueException;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@JiraKey("HHH-21133")
@DomainModel(annotatedClasses = {
		SelfReferencingCascadeIdentityInsertTest.RequiredParent.class,
		SelfReferencingCascadeIdentityInsertTest.OptionalParent.class
})
@SessionFactory(useCollectingStatementObserver = true)
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsIdentityColumns.class)
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsCascadeDeleteCheck.class)
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsCircularCascadeDeleteCheck.class)
@BytecodeEnhanced(runNotEnhancedAsWell = true)
@EnhancementOptions(lazyLoading = true, inlineDirtyChecking = true)
public class SelfReferencingCascadeIdentityInsertTest {

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	void nonNullableIdentitySelfReferenceStillFailsOnInsert(SessionFactoryScope scope) {
		scope.getCollectingStatementObserver().clear();
		final var exception = assertThrows( RuntimeException.class, () -> scope.inTransaction( session -> {
			var node = new RequiredParent();
			node.parent = node;
			session.persist( node );
			session.flush();
		} ) );
		if ( exception instanceof PropertyValueException propertyValueException ) {
			assertEquals( "parent", propertyValueException.getPropertyName() );
		}
		else {
			final var cause = assertInstanceOf( TransientPropertyValueException.class, exception.getCause() );
			assertEquals( "parent", cause.getPropertyName() );
		}
		scope.getCollectingStatementObserver().assertQueries().isEmpty();
	}

	@Test
	void nullableIdentitySelfReferenceCanStillBeInserted(SessionFactoryScope scope) {
		var node = new OptionalParent();
		node.parent = node;
		scope.inTransaction( session -> session.persist( node ) );
		assertNotNull( node.id );
		scope.inTransaction( session -> {
			var loaded = session.find( OptionalParent.class, node.id );
			assertNotNull( loaded.parent );
			assertEquals( node.id, loaded.parent.id );
		} );
	}

	@Entity(name = "RequiredIdentityParent")
	public static class RequiredParent {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		Integer id;
		@ManyToOne(optional = false)
		@OnDelete(action = OnDeleteAction.CASCADE)
		RequiredParent parent;
	}

	@Entity(name = "OptionalIdentityParent")
	public static class OptionalParent {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		Integer id;
		@ManyToOne
		@OnDelete(action = OnDeleteAction.CASCADE)
		OptionalParent parent;
	}
}
