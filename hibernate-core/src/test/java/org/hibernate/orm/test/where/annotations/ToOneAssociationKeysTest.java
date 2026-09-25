package org.hibernate.orm.test.where.annotations;

import java.io.Serializable;
import java.util.Objects;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(annotatedClasses = { ToOneAssociationKeysTest.Target.class, ToOneAssociationKeysTest.Owner.class })
@SessionFactory
class ToOneAssociationKeysTest {
	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@ParameterizedTest
	@ValueSource(strings = { "find", "fetch", "native" })
	void hiddenCompositeAndUniqueKeysArePreserved(String loading, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			for ( int id = 1; id <= 2; id++ ) {
				final Target target = new Target();
				target.id = new Key( "target", id );
				target.code = "code" + id;
				target.registration = new Registration();
				target.registration.code = "registered" + id;
				target.active = id == 1 ? 1 : 0;
				session.persist( target );
				final Owner owner = new Owner();
				owner.id = id;
				owner.composite = target;
				owner.unique = target;
				owner.embeddedUnique = target;
				session.persist( owner );
			}
		} );
		for ( boolean preload : new boolean[] { false, true } ) {
			scope.inTransaction( session -> {
				session.enableFilter( "keyActive" );
				if ( preload ) {
					assertThat( session.find( Target.class, new Key( "target", 2 ) ) ).isNotNull();
				}
				for ( int id = 1; id <= 2; id++ ) {
					final Owner owner = switch ( loading ) {
						case "find" -> session.find( Owner.class, id );
						case "native" -> session.createNativeQuery( "select * from association_keys where id=:id", Owner.class )
								.setParameter( "id", id ).getSingleResult();
						default -> session.createQuery( "from KeyOwner o left join fetch o.composite left join fetch o.unique "
								+ "left join fetch o.embeddedUnique where o.id=:id",
								Owner.class ).setParameter( "id", id ).getSingleResult();
					};
					assertThat( owner.composite == null ).isEqualTo( id == 2 );
					assertThat( owner.unique == null ).isEqualTo( id == 2 );
					assertThat( owner.embeddedUnique == null ).isEqualTo( id == 2 );
					owner.name = "updated";
				}
			} );
		}
		scope.inTransaction( session -> {
			final Object[] keys = session.createNativeQuery(
					"select key_part, key_number, target_code, target_registration from association_keys where id=2", Object[].class )
					.getSingleResult();
			assertThat( keys[0] ).isEqualTo( "target" );
			assertThat( ( (Number) keys[1] ).intValue() ).isEqualTo( 2 );
			assertThat( keys[2] ).isEqualTo( "code2" );
			assertThat( keys[3] ).isEqualTo( "registered2" );
		} );
	}

	@Embeddable
	static class Key implements Serializable {
		String part;
		@Column(name = "key_number")
		int number;
		Key() {}
		Key(String part, int number) { this.part = part; this.number = number; }
		@Override
		public boolean equals(Object other) {
			return other instanceof Key key && number == key.number && Objects.equals( part, key.part );
		}
		@Override
		public int hashCode() { return Objects.hash( part, number ); }
	}

	@Entity(name = "KeyTarget")
	@Table(name = "association_key_target")
	@FilterDef(name = "keyActive", defaultCondition = "active = 1")
	static class Target {
		@EmbeddedId Key id;
		@Column(unique = true) String code;
		@Embedded Registration registration;
		int active;
	}

	@Embeddable
	static class Registration {
		@Column(name = "registration_code", unique = true)
		String code;
	}

	@Entity(name = "KeyOwner")
	@Table(name = "association_keys")
	static class Owner {
		@Id int id;
		String name;
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumns({ @JoinColumn(name = "key_part", referencedColumnName = "part"),
				@JoinColumn(name = "key_number", referencedColumnName = "key_number") })
		@SQLRestriction("active = 1")
		Target composite;
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "target_code", referencedColumnName = "code")
		@Filter(name = "keyActive")
		Target unique;
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "target_registration", referencedColumnName = "registration_code")
		@SQLRestriction("active = 1")
		Target embeddedUnique;
	}
}
