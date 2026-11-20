package org.hibernate.temporal.audit.auditoverrides;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;

import org.hibernate.SharedSessionContract;
import org.hibernate.annotations.Audited;
import org.hibernate.audit.AuditLogFactory;
import org.hibernate.cfg.StateManagementSettings;
import org.hibernate.temporal.spi.ChangesetIdentifierSupplier;
import org.hibernate.testing.orm.junit.AfterClassTemplate;
import org.hibernate.testing.orm.junit.AuditedTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.temporal.audit.auditoverrides.AuditOverrideAssertions.assertNoColumn;
import static org.hibernate.temporal.audit.auditoverrides.AuditOverrideAssertions.assertTable;

@AuditedTest
@DomainModel(annotatedClasses = {
		AuditInheritanceOverrideTest.SingleBase.class,
		AuditInheritanceOverrideTest.SingleSub.class,
		AuditInheritanceOverrideTest.SingleSibling.class,
		AuditInheritanceOverrideTest.JoinedBase.class,
		AuditInheritanceOverrideTest.JoinedSub.class,
		AuditInheritanceOverrideTest.UnionBase.class,
		AuditInheritanceOverrideTest.UnionSub.class,
		AuditInheritanceOverrideTest.SecondarySingleBase.class,
		AuditInheritanceOverrideTest.SecondarySingleSub.class,
		AuditInheritanceOverrideTest.SecondaryJoinedBase.class,
		AuditInheritanceOverrideTest.SecondaryJoinedSub.class,
		AuditInheritanceOverrideTest.IntermediateEntity.class,
		AuditInheritanceOverrideTest.NearestEntity.class,
		AuditInheritanceOverrideTest.InheritedAuditEntity.class
})
@SessionFactory
@ServiceRegistry(settings = @Setting(
		name = StateManagementSettings.CHANGESET_ID_SUPPLIER,
		value = "org.hibernate.temporal.audit.auditoverrides.AuditInheritanceOverrideTest$TxIdSupplier"
))
@Jira("https://hibernate.atlassian.net/browse/HHH-20641")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuditInheritanceOverrideTest {
	private static int currentTxId;

	public static class TxIdSupplier implements ChangesetIdentifierSupplier<Integer> {
		@Override
		public Integer generateIdentifier(SharedSessionContract session) {
			return ++currentTxId;
		}
	}

	@BeforeClassTemplate
	void createData(SessionFactoryScope scope) {
		currentTxId = 0;
		scope.inTransaction( session -> {
			session.persist( withSubFields( new SingleSub(), 1L ) );
			session.persist( withSubFields( new SingleSibling(), 2L ) );

			final var secondarySingleSub = new SecondarySingleSub();
			secondarySingleSub.id = 1L;
			secondarySingleSub.restored = "restored";
			secondarySingleSub.hidden = "hidden";
			secondarySingleSub.ownRestored = "own restored";
			secondarySingleSub.ownHidden = "own hidden";
			session.persist( secondarySingleSub );
			final var secondaryJoinedSub = new SecondaryJoinedSub();
			secondaryJoinedSub.id = 1L;
			secondaryJoinedSub.restored = "restored";
			secondaryJoinedSub.hidden = "hidden";
			secondaryJoinedSub.ownRestored = "own restored";
			secondaryJoinedSub.ownHidden = "own hidden";
			session.persist( secondaryJoinedSub );

			final var intermediate = new IntermediateEntity();
			intermediate.id = 2L;
			intermediate.secret = "intermediate secret";
			intermediate.visible = "intermediate visible";
			session.persist( intermediate );
			final var nearest = new NearestEntity();
			nearest.id = 3L;
			nearest.secret = "nearest secret";
			nearest.visible = "nearest visible";
			session.persist( nearest );
			final var inherited = new InheritedAuditEntity();
			inherited.id = 5L;
			inherited.tags.add( "first" );
			session.persist( inherited );
		} );
		scope.inTransaction( session -> {
			session.find( IntermediateEntity.class, 2L ).secret = "updated secret";
			session.find( NearestEntity.class, 3L ).visible = "updated visible";
			session.find( InheritedAuditEntity.class, 5L ).tags.add( "second" );
		} );
	}

	private static SubFields withSubFields(SubFields entity, long id) {
		entity.id = id;
		entity.a = "a";
		entity.b = "b";
		return entity;
	}

	@AfterClassTemplate
	void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Test
	void auditTablesUseEffectiveExclusions(DomainModelScope scope) {
		for ( var name : new String[] { "override_single_AUD", "override_joined_AUD", "override_union_AUD",
				"override_union_sub_AUD" } ) {
			final var table = assertTable( scope, name, "hidden" );
			assertNoColumn( table, "restored" );
			assertNoColumn( table, "declaredExcluded" );
		}
		assertTable( scope, "override_joined_sub_AUD", "detail" );

		for ( var name : new String[] { "secondary_single_details_AUD", "secondary_joined_details_AUD" } ) {
			assertNoColumn( assertTable( scope, name, "hidden" ), "restored" );
		}
		assertNoColumn( assertTable( scope, "secondary_single_own_AUD", "ownRestored" ), "ownHidden" );
		assertNoColumn( assertTable( scope, "secondary_joined_own_AUD", "ownRestored" ), "ownHidden" );

		assertNoColumn( assertTable( scope, "msc_intermediate_AUD", "secret" ), "visible" );
		assertNoColumn( assertTable( scope, "msc_nearest_AUD", "visible" ), "secret" );
		assertThat( scope.getDomainModel().getEntityBinding( IntermediateEntity.class.getName() )
				.getProperty( "secret" ).isOptimisticLocked() ).isFalse();
	}

	@Test
	void siblingsKeepTheirOwnOverridesOfSharedColumns(DomainModelScope scope, SessionFactoryScope sfScope) {
		// Each sibling owns a copy of the mapped superclass properties, audited by one of them
		assertTable( scope, "override_single_AUD", "a", "b" );
		try (var auditLog = AuditLogFactory.create( sfScope.getSessionFactory() )) {
			final var sub = auditLog.find( SingleSub.class, 1L, 1 );
			assertThat( sub.a ).isEqualTo( "a" );
			assertThat( sub.b ).isNull();
			final var sibling = auditLog.find( SingleSibling.class, 2L, 1 );
			assertThat( sibling.a ).isNull();
			assertThat( sibling.b ).isEqualTo( "b" );
		}
	}

	@Test
	void secondaryTableReadsUseSubclassOverrides(SessionFactoryScope scope) {
		assertSecondaryReads( scope, SecondarySingleSub.class,
				sub -> new String[] { sub.restored, sub.hidden, sub.ownRestored, sub.ownHidden } );
		assertSecondaryReads( scope, SecondaryJoinedSub.class,
				sub -> new String[] { sub.restored, sub.hidden, sub.ownRestored, sub.ownHidden } );
	}

	@Test
	void collectionOfEntityAuditedThroughMappedSuperclass(DomainModelScope scope, SessionFactoryScope sfScope) {
		assertThat( scope.getDomainModel()
				.getCollectionBinding( InheritedAuditEntity.class.getName() + ".tags" )
				.getAuxiliaryTable() ).isNotNull();
		try (var auditLog = AuditLogFactory.create( sfScope.getSessionFactory() )) {
			assertThat( auditLog.find( InheritedAuditEntity.class, 5L, 1 ).tags ).containsExactly( "first" );
			assertThat( auditLog.find( InheritedAuditEntity.class, 5L, 2 ).tags )
					.containsExactlyInAnyOrder( "first", "second" );
		}
	}

	@Test
	void nearestOverridesControlAuditReads(SessionFactoryScope scope) {
		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			final var intermediate = auditLog.find( IntermediateEntity.class, 2L, 1 );
			assertThat( intermediate.secret ).isEqualTo( "intermediate secret" );
			assertThat( intermediate.visible ).isNull();
			assertThat( auditLog.find( IntermediateEntity.class, 2L, 2 ).secret )
					.isEqualTo( "updated secret" );
			final var nearest = auditLog.find( NearestEntity.class, 3L, 1 );
			assertThat( nearest.secret ).isNull();
			assertThat( nearest.visible ).isEqualTo( "nearest visible" );
		}
		try (var session = scope.getSessionFactory().withOptions().atChangeset( 2 ).openSession()) {
			final var entity = session.createSelectionQuery( "from NearestEntity", NearestEntity.class )
					.getSingleResult();
			assertThat( entity.secret ).isNull();
			assertThat( entity.visible ).isEqualTo( "updated visible" );
		}
	}

	/**
	 * {@code values} extracts {@code (restored, hidden, ownRestored, ownHidden)}.
	 */
	private static <S> void assertSecondaryReads(
			SessionFactoryScope scope,
			Class<S> subType,
			Function<S, String[]> values
	) {
		final var expected = new String[] { null, "hidden", "own restored", null };
		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			assertThat( values.apply( auditLog.find( subType, 1L, 1 ) ) ).containsExactly( expected );
		}
		try (var session = scope.getSessionFactory().withOptions().atChangeset( 1 ).openSession()) {
			final var entityName = subType.getAnnotation( Entity.class ).name();
			assertThat( values.apply( session.createSelectionQuery( "from " + entityName, subType ).getSingleResult() ) )
					.containsExactly( expected );
		}
	}

	@MappedSuperclass
	static class Fields {
		@Id
		long id;
		String restored;
		String hidden;
		@Audited.Excluded
		String declaredExcluded;
	}

	@Entity(name = "OverrideSingleBase")
	@Table(name = "override_single")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	@Audited
	@Audited.Override(name = "restored", isAudited = false)
	static class SingleBase extends Fields {
	}

	@MappedSuperclass
	static class SubFields extends SingleBase {
		@Audited.Excluded
		String a;
		String b;
	}

	@Entity(name = "OverrideSingleSub")
	@Audited.Override(name = "a")
	@Audited.Override(name = "b", isAudited = false)
	static class SingleSub extends SubFields {
	}

	@Entity(name = "OverrideSingleSibling")
	static class SingleSibling extends SubFields {
	}

	@Entity(name = "OverrideJoinedBase")
	@Table(name = "override_joined")
	@Inheritance(strategy = InheritanceType.JOINED)
	@Audited
	@Audited.Override(name = "restored", isAudited = false)
	static class JoinedBase extends Fields {
	}

	@Entity(name = "OverrideJoinedSub")
	@Table(name = "override_joined_sub")
	static class JoinedSub extends JoinedBase {
		String detail = "detail";
	}

	@Entity(name = "OverrideUnionBase")
	@Table(name = "override_union")
	@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
	@Audited
	@Audited.Override(name = "restored", isAudited = false)
	static class UnionBase extends Fields {
	}

	@Entity(name = "OverrideUnionSub")
	@Table(name = "override_union_sub")
	static class UnionSub extends UnionBase {
	}

	@Entity(name = "SecondarySingleBase")
	@Table(name = "secondary_single")
	@SecondaryTable(name = "secondary_single_details")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	@Audited
	@Audited.Override(name = "restored", isAudited = false)
	static class SecondarySingleBase {
		@Id
		long id;
		@Column(table = "secondary_single_details")
		String restored;
		@Column(table = "secondary_single_details")
		String hidden;
	}

	@Entity(name = "SecondarySingleSub")
	@SecondaryTable(name = "secondary_single_own")
	@Audited.Override(name = "ownRestored")
	@Audited.Override(name = "ownHidden", isAudited = false)
	static class SecondarySingleSub extends SecondarySingleBase {
		@Column(table = "secondary_single_own")
		@Audited.Excluded
		String ownRestored;
		@Column(table = "secondary_single_own")
		String ownHidden;
	}

	@Entity(name = "SecondaryJoinedBase")
	@Table(name = "secondary_joined")
	@SecondaryTable(name = "secondary_joined_details")
	@Inheritance(strategy = InheritanceType.JOINED)
	@Audited
	@Audited.Override(name = "restored", isAudited = false)
	static class SecondaryJoinedBase {
		@Id
		long id;
		@Column(table = "secondary_joined_details")
		String restored;
		@Column(table = "secondary_joined_details")
		String hidden;
	}

	@Entity(name = "SecondaryJoinedSub")
	@Table(name = "secondary_joined_sub")
	@SecondaryTable(name = "secondary_joined_own")
	@Audited.Override(name = "ownRestored")
	@Audited.Override(name = "ownHidden", isAudited = false)
	static class SecondaryJoinedSub extends SecondaryJoinedBase {
		@Column(table = "secondary_joined_own")
		@Audited.Excluded
		String ownRestored;
		@Column(table = "secondary_joined_own")
		String ownHidden;
	}

	@MappedSuperclass
	static class Base {
		@Id
		long id;
		@Audited.Excluded
		String secret;
		String visible;
	}

	@MappedSuperclass
	@Audited.Override(name = "secret")
	@Audited.Override(name = "visible", isAudited = false)
	static class Intermediate extends Base {
	}

	@Entity(name = "IntermediateEntity")
	@Table(name = "msc_intermediate")
	@Audited
	static class IntermediateEntity extends Intermediate {
	}

	@Entity(name = "NearestEntity")
	@Table(name = "msc_nearest")
	@Audited
	@Audited.Override(name = "secret", isAudited = false)
	@Audited.Override(name = "visible")
	static class NearestEntity extends Intermediate {
	}

	@MappedSuperclass
	@Audited
	static class AuditedBase {
		@Id
		long id;
	}

	@Entity(name = "MscInheritedAudit")
	@Table(name = "msc_inherited")
	static class InheritedAuditEntity extends AuditedBase {
		@ElementCollection
		@CollectionTable(name = "msc_inherited_tags")
		Set<String> tags = new HashSet<>();
	}
}
