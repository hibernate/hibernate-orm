/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.where.annotations;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.SqlFragmentAlias;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(annotatedClasses = { ToOneAssociationCompositionTest.BaseTarget.class,
		ToOneAssociationCompositionTest.Target.class, ToOneAssociationCompositionTest.Owner.class })
@SessionFactory
class ToOneAssociationCompositionTest {
	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@ParameterizedTest
	@ValueSource(strings = { "find", "fetch", "native" })
	void targetAndAssociationPredicatesComposeAcrossTables(String loading, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			for ( long id = 1; id <= 5; id++ ) {
				final Target target = new Target();
				target.id = id;
				target.deleted = id == 2;
				target.active = id != 3;
				target.category = id == 4 ? 2 : 1;
				target.approved = id != 5;
				session.persist( target );
				final Owner owner = new Owner();
				owner.id = id;
				owner.details = new Details();
				owner.details.note = "original";
				owner.details.sql = target;
				owner.details.filtered = target;
				session.persist( owner );
			}
		} );
		for ( int category : new int[] { 1, 2, 1 } ) {
			scope.inTransaction( session -> {
				session.enableFilter( "targetActive" );
				if ( category == 2 ) {
					session.enableFilter( "targetQueryOnly" );
				}
				session.enableFilter( "associationCategory" ).setParameter( "category", category );
				for ( long id = 1; id <= 5; id++ ) {
					final Owner owner = switch ( loading ) {
						case "find" -> session.find( Owner.class, id );
						case "native" -> session.createNativeQuery( "select * from composed_owner where id=:id", Owner.class )
								.setParameter( "id", id ).getSingleResult();
						default -> session.createQuery( "from ComposedOwner o left join fetch o.details.sql "
								+ "left join fetch o.details.filtered where o.id=:id", Owner.class )
								.setParameter( "id", id ).getSingleResult();
					};
					final boolean includedByEntityFilter = !loading.equals( "fetch" ) || category != 2;
					assertThat( owner.details.sql != null ).isEqualTo( includedByEntityFilter && id == 1 );
					assertThat( owner.details.filtered != null )
							.isEqualTo( includedByEntityFilter && id != 2 && id != 3 && (id == 4 ? 2 : 1) == category );
					owner.details.note = "changed";
				}
			} );
		}
		scope.inTransaction( session -> {
			for ( long id = 1; id <= 5; id++ ) {
				assertThat( session.createNativeQuery( "select sql_id, filtered_id from composed_owner where id=:id", Object[].class )
						.setParameter( "id", id ).getSingleResult() ).containsExactly( id, id );
			}
		} );
	}

	@Entity(name = "ComposedBaseTarget")
	@Table(name = "composed_base")
	@Inheritance(strategy = InheritanceType.JOINED)
	@SQLRestriction("deleted = false")
	@FilterDef(name = "targetActive", defaultCondition = "active = true", applyToLoadByKey = true)
	@Filter(name = "targetActive")
	@FilterDef(name = "targetQueryOnly", defaultCondition = "1=0")
	@Filter(name = "targetQueryOnly")
	@FilterDef(name = "associationCategory", defaultCondition = "{b}.category = :category",
			parameters = @ParamDef(name = "category", type = Integer.class))
	static class BaseTarget {
		@Id Long id;
		boolean deleted;
		boolean active;
		int category;
	}

	@Entity(name = "ComposedTarget")
	@Table(name = "composed_target")
	@SecondaryTable(name = "composed_details")
	static class Target extends BaseTarget {
		@Column(table = "composed_details")
		boolean approved;
	}

	@Entity(name = "ComposedOwner")
	@Table(name = "composed_owner")
	static class Owner {
		@Id Long id;
		@Embedded Details details;
	}

	@Embeddable
	@Access(AccessType.PROPERTY)
	static class Details {
		private String note;
		private Target sql;
		private Target filtered;

		public String getNote() {
			return note;
		}
		public void setNote(String note) {
			this.note = note;
		}

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "sql_id")
		@SQLRestriction("category = 1 and approved = true")
		public Target getSql() {
			return sql;
		}
		public void setSql(Target sql) {
			this.sql = sql;
		}

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "filtered_id")
		@Filter(name = "associationCategory", deduceAliasInjectionPoints = false,
				aliases = @SqlFragmentAlias(alias = "b", table = "composed_base"))
		public Target getFiltered() {
			return filtered;
		}
		public void setFiltered(Target filtered) {
			this.filtered = filtered;
		}
	}
}
