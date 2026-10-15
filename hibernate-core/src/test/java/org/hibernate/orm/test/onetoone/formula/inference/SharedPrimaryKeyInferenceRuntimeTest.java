package org.hibernate.orm.test.onetoone.formula.inference;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

import org.hibernate.FetchNotFoundException;
import org.hibernate.annotations.JoinColumnOrFormula;
import org.hibernate.annotations.JoinFormula;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.id.IdentifierGenerationException;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/// Exercises inferred owner-key joins without changing identifier ownership or NotFound policies.
///
/// @author Steve Ebersole
@DomainModel(annotatedClasses = {
		SharedPrimaryKeyInferenceRuntimeTest.UniqueTarget.class,
		SharedPrimaryKeyInferenceRuntimeTest.IgnoreOwner.class,
		SharedPrimaryKeyInferenceRuntimeTest.ExceptionOwner.class,
		SharedPrimaryKeyInferenceRuntimeTest.GeneratedOwner.class,
		SharedPrimaryKeyInferenceRuntimeTest.AssignedOwner.class,
		SharedPrimaryKeyInferenceRuntimeTest.PairedNode.class,
		SharedPrimaryKeyInferenceTest.AnnotatedTarget.class
})
@SessionFactory
public class SharedPrimaryKeyInferenceRuntimeTest {
	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			for ( String name : new String[] { "InferenceIgnoreOwner", "InferenceExceptionOwner", "InferenceGeneratedOwner",
					"InferenceAssignedOwner", "InferencePairedNode", "InferenceUniqueTarget" } ) {
				session.createMutationQuery( "delete from " + name ).executeUpdate();
			}
			session.createMutationQuery( "delete from " + SharedPrimaryKeyInferenceTest.AnnotatedTarget.class.getName() ).executeUpdate();
		} );
	}

	@Test
	void explicitIgnoreRetainsUniqueKeyResolution(SessionFactoryScope scope, DomainModelScope modelScope) {
		scope.inTransaction( session -> {
			UniqueTarget target = new UniqueTarget();
			target.id = 77L;
			target.code = 1L;
			target.kind = 1L;
			session.persist( target );
			IgnoreOwner present = new IgnoreOwner();
			present.id = 1L;
			session.persist( present );
			IgnoreOwner missing = new IgnoreOwner();
			missing.id = 2L;
			session.persist( missing );
		} );
		scope.inTransaction( session -> {
			assertThat( session.find( IgnoreOwner.class, 1L ).target.id ).isEqualTo( 77L );
			assertThat( session.find( IgnoreOwner.class, 2L ).target ).isNull();
		} );
		var value = (org.hibernate.mapping.ManyToOne) modelScope.getEntityBinding( IgnoreOwner.class ).getProperty( "target" ).getValue();
		assertThat( value.getNotFoundAction() ).isEqualTo( NotFoundAction.IGNORE );
		assertThat( value.getColumnInsertability() ).containsOnly( false );
		assertThat( value.getColumnUpdateability() ).containsOnly( false );
	}

	@Test
	void explicitExceptionStillRejectsMissingTarget(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			ExceptionOwner owner = new ExceptionOwner();
			owner.id = 3L;
			session.persist( owner );
		} );
		assertThrows( FetchNotFoundException.class, () -> scope.inTransaction(
				session -> session.find( ExceptionOwner.class, 3L ) ) );
	}

	@Test
	void generatorOwnsIdentifierValue(SessionFactoryScope scope) {
		Long[] generated = new Long[1];
		scope.inTransaction( session -> {
			var target = target( 99L );
			session.persist( target );
			GeneratedOwner owner = new GeneratedOwner();
			owner.target = target;
			session.persist( owner );
			session.flush();
			assertThat( owner.id ).isNotNull().isNotEqualTo( 99L );
			generated[0] = owner.id;
		} );
		scope.inTransaction( session -> {
			GeneratedOwner owner = session.find( GeneratedOwner.class, generated[0] );
			assertThat( owner.target ).isNull();
			owner.target = session.find( SharedPrimaryKeyInferenceTest.AnnotatedTarget.class,
					new SharedPrimaryKeyInferenceTest.TargetKey( 99L, "HOME" ) );
			session.flush();
			session.clear();
			assertThat( session.find( GeneratedOwner.class, generated[0] ).target ).isNull();
		} );
	}

	@Test
	void associationDoesNotSupplyAssignedIdentifier(SessionFactoryScope scope) {
		assertThrows( IdentifierGenerationException.class, () -> scope.inTransaction( session -> {
			var target = target( 100L );
			session.persist( target );
			AssignedOwner owner = new AssignedOwner();
			owner.target = target;
			session.persist( owner );
		} ) );
		scope.inTransaction( session -> {
			var target = target( 100L );
			session.persist( target );
			AssignedOwner owner = new AssignedOwner();
			owner.id = 100L;
			owner.target = target;
			session.persist( owner );
		} );
		scope.inTransaction( session -> {
			assertThat( session.find( AssignedOwner.class, 100L ).target.id ).isEqualTo( 100L );
		} );
	}

	@Test
	void selfReferenceUsesSharedSourceIdentifier(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			PairedNode first = new PairedNode();
			first.id = 7L;
			first.kind = 0L;
			PairedNode second = new PairedNode();
			second.id = 7L;
			second.kind = 1L;
			PairedNode missing = new PairedNode();
			missing.id = 8L;
			missing.kind = 0L;
			session.persist( first );
			session.persist( second );
			session.persist( missing );
		} );
		scope.inTransaction( session -> {
			PairedNode first = session.createQuery( "from InferencePairedNode n left join fetch n.pair left join fetch n.inverse where n.id = 7 and n.kind = 0", PairedNode.class ).getSingleResult();
			assertThat( first.pair.kind ).isEqualTo( 1L );
			assertThat( first.inverse ).isSameAs( first.pair );
			assertThat( first.pair.pair ).isSameAs( first );
			assertThat( first.pair.inverse ).isSameAs( first );
		} );
		scope.inTransaction( session -> {
			PairedNode missing = session.find( PairedNode.class, new PairedKey( 8L, 0L ) );
			assertThat( missing.pair ).isNull();
			assertThat( missing.inverse ).isNull();
		} );
	}

	private static SharedPrimaryKeyInferenceTest.AnnotatedTarget target(Long id) {
		var target = new SharedPrimaryKeyInferenceTest.AnnotatedTarget();
		target.id = id;
		target.kind = "HOME";
		return target;
	}

	@Entity(name="InferenceUniqueTarget")
	public static class UniqueTarget {
		@Id Long id;
		@Column(unique=true) Long code;
		Long kind;
	}
	@Entity(name="InferenceIgnoreOwner")
	public static class IgnoreOwner {
		@Id Long id;
		@OneToOne @NotFound(action=NotFoundAction.IGNORE)
		@JoinColumnOrFormula(column=@JoinColumn(name="id", referencedColumnName="code"))
		@JoinColumnOrFormula(formula=@JoinFormula(value="1", referencedColumnName="kind"))
		UniqueTarget target;
	}
	@Entity(name="InferenceExceptionOwner")
	public static class ExceptionOwner {
		@Id Long id;
		@OneToOne @NotFound(action=NotFoundAction.EXCEPTION)
		@JoinColumnOrFormula(column=@JoinColumn(name="id", referencedColumnName="code"))
		@JoinColumnOrFormula(formula=@JoinFormula(value="1", referencedColumnName="kind"))
		UniqueTarget target;
	}
	@Entity(name="InferenceGeneratedOwner")
	public static class GeneratedOwner {
		@Id @GeneratedValue Long id;
		@OneToOne @JoinColumnOrFormula(column=@JoinColumn(name="id", referencedColumnName="personName"))
		@JoinColumnOrFormula(formula=@JoinFormula(value="'HOME'", referencedColumnName="addressType"))
		SharedPrimaryKeyInferenceTest.AnnotatedTarget target;
	}
	@Entity(name="InferenceAssignedOwner")
	public static class AssignedOwner {
		@Id Long id;
		@OneToOne @JoinColumnOrFormula(column=@JoinColumn(name="id", referencedColumnName="personName"))
		@JoinColumnOrFormula(formula=@JoinFormula(value="'HOME'", referencedColumnName="addressType"))
		SharedPrimaryKeyInferenceTest.AnnotatedTarget target;
	}
	@Entity(name="InferencePairedNode") @IdClass(PairedKey.class)
	public static class PairedNode {
		@Id Long id;
		@Id Long kind;
		@OneToOne @JoinColumnOrFormula(column=@JoinColumn(name="id", referencedColumnName="id"))
		@JoinColumnOrFormula(formula=@JoinFormula(value="1 - kind", referencedColumnName="kind"))
		PairedNode pair;
		@OneToOne(mappedBy="pair") PairedNode inverse;
	}
	public record PairedKey(Long id, Long kind) implements Serializable {}
}
