package org.hibernate.orm.test.where.annotations;

import java.io.Serializable;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.JoinFormula;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

@JiraKey("HHH-21131")
@DomainModel(annotatedClasses = { RestrictedToOneInverseFormulaTest.Owner.class,
		RestrictedToOneInverseFormulaTest.Target.class })
@SessionFactory
@BytecodeEnhanced(runNotEnhancedAsWell = true, testEnhancedClasses = {
		RestrictedToOneInverseFormulaTest.Owner.class, RestrictedToOneInverseFormulaTest.Target.class })
class RestrictedToOneInverseFormulaTest {
	@BeforeEach
	void prepare(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			for ( long id = 1; id <= 3; id++ ) {
				final var owner = new Owner();
				owner.id = id;
				session.persist( owner );
				if ( id != 3 ) {
					final var target = new Target();
					target.id = id * 100;
					target.part = 1;
					target.ownerId = id;
					target.active = id == 1 ? 1 : 0;
					target.name = "target";
					session.persist( target );
				}
			}
		} );
	}

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Test
	void findResolvesRestrictedInverseFormula(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			assertTarget( session.find( Owner.class, 1L ) );
			assertThat( session.find( Owner.class, 2L ).getTarget() ).isNull();
			assertThat( session.find( Owner.class, 3L ).getTarget() ).isNull();
		} );
	}

	@Test
	void queryResolvesRestrictedInverseFormula(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var owners = session.createSelectionQuery(
					"from FormulaOwner o order by o.id", Owner.class ).getResultList();
			assertTarget( owners.get( 0 ) );
			assertThat( owners.get( 1 ).getTarget() ).isNull();
			assertThat( owners.get( 2 ).getTarget() ).isNull();
		} );
	}

	private static void assertTarget(Owner owner) {
		assertThat( owner.getTarget().getId() ).isEqualTo( 100L );
		assertThat( owner.getTarget().getName() ).isEqualTo( "target" );
		assertThat( owner.getTarget().getOwner() ).isSameAs( owner );
	}

	@Entity(name = "FormulaOwner")
	@Table(name = "restricted_formula_owner")
	public static class Owner {
		@Id Long id;
		@OneToOne(mappedBy = "owner", fetch = FetchType.LAZY)
		@Fetch(FetchMode.SELECT)
		@SQLRestriction("active = 1")
		Target target;
		public Target getTarget() { return target; }
	}

	public record TargetId(Long id, int part) implements Serializable {}

	@Entity(name = "FormulaTarget")
	@Table(name = "restricted_formula_target")
	@IdClass(TargetId.class)
	public static class Target {
		@Id Long id;
		@Id int part;
		@Column(name = "owner_id") Long ownerId;
		int active;
		String name;
		@OneToOne(fetch = FetchType.LAZY)
		@JoinFormula(value = "owner_id", referencedColumnName = "id")
		Owner owner;
		public Long getId() { return id; }
		public String getName() { return name; }
		public Owner getOwner() { return owner; }
	}
}
