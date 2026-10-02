package org.hibernate.orm.test.where.annotations;

import java.util.stream.Stream;

import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.OptimisticLocking;
import org.hibernate.annotations.OptimisticLockType;
import org.hibernate.annotations.SQLUpdate;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.testing.orm.junit.DomainModel;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@DomainModel(annotatedClasses = {
		AssociationRestrictedToOneTest.UnrestrictedTarget.class,
		AssociationRestrictedToOneTest.AssociationSqlCustomFk.class,
		AssociationRestrictedToOneTest.AssociationSqlAllFk.class,
		AssociationRestrictedToOneTest.AssociationSqlDirtyFk.class,
		AssociationRestrictedToOneTest.AssociationFilterCustomFk.class,
		AssociationRestrictedToOneTest.AssociationFilterAllFk.class,
		AssociationRestrictedToOneTest.AssociationFilterDirtyFk.class,
		AssociationRestrictedToOneTest.AssociationSqlFk.class,
		AssociationRestrictedToOneTest.AssociationSqlFkSelect.class,
		AssociationRestrictedToOneTest.AssociationSqlFkLazy.class,
		AssociationRestrictedToOneTest.AssociationSqlJoin.class,
		AssociationRestrictedToOneTest.AssociationSqlJoinSelect.class,
		AssociationRestrictedToOneTest.AssociationSqlJoinLazy.class,
		AssociationRestrictedToOneTest.AssociationSqlOneFk.class,
		AssociationRestrictedToOneTest.AssociationSqlOneFkSelect.class,
		AssociationRestrictedToOneTest.AssociationSqlOneFkLazy.class,
		AssociationRestrictedToOneTest.AssociationSqlOneJoin.class,
		AssociationRestrictedToOneTest.AssociationSqlOneJoinSelect.class,
		AssociationRestrictedToOneTest.AssociationSqlOneJoinLazy.class,
		AssociationRestrictedToOneTest.AssociationFilterFk.class,
		AssociationRestrictedToOneTest.AssociationFilterFkSelect.class,
		AssociationRestrictedToOneTest.AssociationFilterFkLazy.class,
		AssociationRestrictedToOneTest.AssociationFilterJoin.class,
		AssociationRestrictedToOneTest.AssociationFilterJoinSelect.class,
		AssociationRestrictedToOneTest.AssociationFilterJoinLazy.class,
		AssociationRestrictedToOneTest.AssociationFilterOneFk.class,
		AssociationRestrictedToOneTest.AssociationFilterOneFkSelect.class,
		AssociationRestrictedToOneTest.AssociationFilterOneFkLazy.class,
		AssociationRestrictedToOneTest.AssociationFilterOneJoin.class,
		AssociationRestrictedToOneTest.AssociationFilterOneJoinSelect.class,
		AssociationRestrictedToOneTest.AssociationFilterOneJoinLazy.class
})
class AssociationRestrictedToOneTest extends RestrictedToOneTest {
	static Stream<Mapping> mappings() {
		return Stream.of(
				new Mapping( AssociationSqlCustomFk.class, UnrestrictedTarget.class, false ),
				new Mapping( AssociationSqlAllFk.class, UnrestrictedTarget.class, false ),
				new Mapping( AssociationSqlDirtyFk.class, UnrestrictedTarget.class, false ),
				new Mapping( AssociationFilterCustomFk.class, UnrestrictedTarget.class, false ),
				new Mapping( AssociationFilterAllFk.class, UnrestrictedTarget.class, false ),
				new Mapping( AssociationFilterDirtyFk.class, UnrestrictedTarget.class, false ),
				new Mapping( AssociationSqlFk.class, UnrestrictedTarget.class, false ),
				new Mapping( AssociationSqlFkSelect.class, UnrestrictedTarget.class, false ),
				new Mapping( AssociationSqlFkLazy.class, UnrestrictedTarget.class, true ),
				new Mapping( AssociationSqlJoin.class, UnrestrictedTarget.class, false ),
				new Mapping( AssociationSqlJoinSelect.class, UnrestrictedTarget.class, false ),
				new Mapping( AssociationSqlJoinLazy.class, UnrestrictedTarget.class, true ),
				new Mapping( AssociationSqlOneFk.class, UnrestrictedTarget.class, false ),
				new Mapping( AssociationSqlOneFkSelect.class, UnrestrictedTarget.class, false ),
				new Mapping( AssociationSqlOneFkLazy.class, UnrestrictedTarget.class, true ),
				new Mapping( AssociationSqlOneJoin.class, UnrestrictedTarget.class, false ),
				new Mapping( AssociationSqlOneJoinSelect.class, UnrestrictedTarget.class, false ),
				new Mapping( AssociationSqlOneJoinLazy.class, UnrestrictedTarget.class, true ),
				new Mapping( AssociationFilterFk.class, UnrestrictedTarget.class, false ),
				new Mapping( AssociationFilterFkSelect.class, UnrestrictedTarget.class, false ),
				new Mapping( AssociationFilterFkLazy.class, UnrestrictedTarget.class, true ),
				new Mapping( AssociationFilterJoin.class, UnrestrictedTarget.class, false ),
				new Mapping( AssociationFilterJoinSelect.class, UnrestrictedTarget.class, false ),
				new Mapping( AssociationFilterJoinLazy.class, UnrestrictedTarget.class, true ),
				new Mapping( AssociationFilterOneFk.class, UnrestrictedTarget.class, false ),
				new Mapping( AssociationFilterOneFkSelect.class, UnrestrictedTarget.class, false ),
				new Mapping( AssociationFilterOneFkLazy.class, UnrestrictedTarget.class, true ),
				new Mapping( AssociationFilterOneJoin.class, UnrestrictedTarget.class, false ),
				new Mapping( AssociationFilterOneJoinSelect.class, UnrestrictedTarget.class, false ),
				new Mapping( AssociationFilterOneJoinLazy.class, UnrestrictedTarget.class, true )
		);
	}

	static Stream<Mapping> filterMappings() {
		return mappings().filter( mapping -> mapping.ownerType().getSimpleName().contains( "Filter" ) );
	}

	static Stream<Mapping> orphanMappings() {
		return mappings().filter( mapping -> mapping.ownerType().getSimpleName().contains( "One" ) );
	}

	@Entity(name = "UnrestrictedTarget")
	@Table(name = "association_target")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	@FilterDef(name = "visibleTarget", parameters = @ParamDef(name = "active", type = Boolean.class))
	public static class UnrestrictedTarget extends Target {
	}

	@Entity(name = "AssociationSqlFk")
	@Table(name = "at_sql_fk")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	public static class AssociationSqlFk extends Owner {
		@ManyToOne(fetch = FetchType.EAGER)
		@JoinColumn(name = "target_id")
		@SQLRestriction("active = true")
		UnrestrictedTarget target;

		@Override
		public Target getTarget() {
			return target;
		}

		@Override
		public void setTarget(Target target) {
			this.target = (UnrestrictedTarget) target;
		}
	}

	@Entity(name = "AssociationSqlFkSelect")
	@Table(name = "at_sql_fk_select")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	public static class AssociationSqlFkSelect extends Owner {
		@ManyToOne(fetch = FetchType.EAGER)
		@Fetch(FetchMode.SELECT)
		@JoinColumn(name = "target_id")
		@SQLRestriction("active = true")
		UnrestrictedTarget target;

		@Override
		public Target getTarget() {
			return target;
		}

		@Override
		public void setTarget(Target target) {
			this.target = (UnrestrictedTarget) target;
		}
	}

	@Entity(name = "AssociationSqlFkLazy")
	@Table(name = "at_sql_fk_lazy")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	public static class AssociationSqlFkLazy extends Owner {
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "target_id")
		@SQLRestriction("active = true")
		UnrestrictedTarget target;

		@Override
		public Target getTarget() {
			return target;
		}

		@Override
		public void setTarget(Target target) {
			this.target = (UnrestrictedTarget) target;
		}
	}

	@Entity(name = "AssociationSqlJoin")
	@Table(name = "at_sql_jt")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	public static class AssociationSqlJoin extends Owner {
		@ManyToOne(fetch = FetchType.EAGER)
		@JoinTable(name = "at_sql_jt_link", joinColumns = @JoinColumn(name = "owner_id"),
				inverseJoinColumns = @JoinColumn(name = "target_id"))
		@SQLRestriction("active = true")
		UnrestrictedTarget target;

		@Override
		public Target getTarget() {
			return target;
		}

		@Override
		public void setTarget(Target target) {
			this.target = (UnrestrictedTarget) target;
		}
	}

	@Entity(name = "AssociationSqlJoinSelect")
	@Table(name = "at_sql_jt_select")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	public static class AssociationSqlJoinSelect extends Owner {
		@ManyToOne(fetch = FetchType.EAGER)
		@Fetch(FetchMode.SELECT)
		@JoinTable(name = "at_sql_jt_select_link", joinColumns = @JoinColumn(name = "owner_id"),
				inverseJoinColumns = @JoinColumn(name = "target_id"))
		@SQLRestriction("active = true")
		UnrestrictedTarget target;

		@Override
		public Target getTarget() {
			return target;
		}

		@Override
		public void setTarget(Target target) {
			this.target = (UnrestrictedTarget) target;
		}
	}

	@Entity(name = "AssociationSqlJoinLazy")
	@Table(name = "at_sql_jt_lazy")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	public static class AssociationSqlJoinLazy extends Owner {
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinTable(name = "at_sql_jt_lazy_link", joinColumns = @JoinColumn(name = "owner_id"),
				inverseJoinColumns = @JoinColumn(name = "target_id"))
		@SQLRestriction("active = true")
		UnrestrictedTarget target;

		@Override
		public Target getTarget() {
			return target;
		}

		@Override
		public void setTarget(Target target) {
			this.target = (UnrestrictedTarget) target;
		}
	}

	@Entity(name = "AssociationSqlOneFk")
	@Table(name = "at_sql_one_fk")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	public static class AssociationSqlOneFk extends Owner {
		@OneToOne(fetch = FetchType.EAGER, orphanRemoval = true)
		@JoinColumn(name = "target_id")
		@SQLRestriction("active = true")
		UnrestrictedTarget target;

		@Override
		public Target getTarget() {
			return target;
		}

		@Override
		public void setTarget(Target target) {
			this.target = (UnrestrictedTarget) target;
		}
	}

	@Entity(name = "AssociationSqlOneFkSelect")
	@Table(name = "at_sql_one_fk_select")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	public static class AssociationSqlOneFkSelect extends Owner {
		@OneToOne(fetch = FetchType.EAGER, orphanRemoval = true)
		@Fetch(FetchMode.SELECT)
		@JoinColumn(name = "target_id")
		@SQLRestriction("active = true")
		UnrestrictedTarget target;

		@Override
		public Target getTarget() {
			return target;
		}

		@Override
		public void setTarget(Target target) {
			this.target = (UnrestrictedTarget) target;
		}
	}

	@Entity(name = "AssociationSqlOneFkLazy")
	@Table(name = "at_sql_one_fk_lazy")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	public static class AssociationSqlOneFkLazy extends Owner {
		@OneToOne(fetch = FetchType.LAZY, orphanRemoval = true)
		@JoinColumn(name = "target_id")
		@SQLRestriction("active = true")
		UnrestrictedTarget target;

		@Override
		public Target getTarget() {
			return target;
		}

		@Override
		public void setTarget(Target target) {
			this.target = (UnrestrictedTarget) target;
		}
	}

	@Entity(name = "AssociationSqlOneJoin")
	@Table(name = "at_sql_one_jt")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	public static class AssociationSqlOneJoin extends Owner {
		@OneToOne(fetch = FetchType.EAGER, orphanRemoval = true)
		@JoinTable(name = "at_sql_one_jt_link", joinColumns = @JoinColumn(name = "owner_id"),
				inverseJoinColumns = @JoinColumn(name = "target_id"))
		@SQLRestriction("active = true")
		UnrestrictedTarget target;

		@Override
		public Target getTarget() {
			return target;
		}

		@Override
		public void setTarget(Target target) {
			this.target = (UnrestrictedTarget) target;
		}
	}

	@Entity(name = "AssociationSqlOneJoinSelect")
	@Table(name = "at_sql_one_jt_select")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	public static class AssociationSqlOneJoinSelect extends Owner {
		@OneToOne(fetch = FetchType.EAGER, orphanRemoval = true)
		@Fetch(FetchMode.SELECT)
		@JoinTable(name = "at_sql_one_jt_select_link", joinColumns = @JoinColumn(name = "owner_id"),
				inverseJoinColumns = @JoinColumn(name = "target_id"))
		@SQLRestriction("active = true")
		UnrestrictedTarget target;

		@Override
		public Target getTarget() {
			return target;
		}

		@Override
		public void setTarget(Target target) {
			this.target = (UnrestrictedTarget) target;
		}
	}

	@Entity(name = "AssociationSqlOneJoinLazy")
	@Table(name = "at_sql_one_jt_lazy")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	public static class AssociationSqlOneJoinLazy extends Owner {
		@OneToOne(fetch = FetchType.LAZY, orphanRemoval = true)
		@JoinTable(name = "at_sql_one_jt_lazy_link", joinColumns = @JoinColumn(name = "owner_id"),
				inverseJoinColumns = @JoinColumn(name = "target_id"))
		@SQLRestriction("active = true")
		UnrestrictedTarget target;

		@Override
		public Target getTarget() {
			return target;
		}

		@Override
		public void setTarget(Target target) {
			this.target = (UnrestrictedTarget) target;
		}
	}

	@Entity(name = "AssociationFilterFk")
	@Table(name = "at_filter_fk")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	public static class AssociationFilterFk extends Owner {
		@ManyToOne(fetch = FetchType.EAGER)
		@JoinColumn(name = "target_id")
		@Filter(name = "visibleTarget", condition = "active = :active")
		UnrestrictedTarget target;

		@Override
		public Target getTarget() {
			return target;
		}

		@Override
		public void setTarget(Target target) {
			this.target = (UnrestrictedTarget) target;
		}
	}

	@Entity(name = "AssociationFilterFkSelect")
	@Table(name = "at_filter_fk_select")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	public static class AssociationFilterFkSelect extends Owner {
		@ManyToOne(fetch = FetchType.EAGER)
		@Fetch(FetchMode.SELECT)
		@JoinColumn(name = "target_id")
		@Filter(name = "visibleTarget", condition = "active = :active")
		UnrestrictedTarget target;

		@Override
		public Target getTarget() {
			return target;
		}

		@Override
		public void setTarget(Target target) {
			this.target = (UnrestrictedTarget) target;
		}
	}

	@Entity(name = "AssociationFilterFkLazy")
	@Table(name = "at_filter_fk_lazy")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	public static class AssociationFilterFkLazy extends Owner {
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "target_id")
		@Filter(name = "visibleTarget", condition = "active = :active")
		UnrestrictedTarget target;

		@Override
		public Target getTarget() {
			return target;
		}

		@Override
		public void setTarget(Target target) {
			this.target = (UnrestrictedTarget) target;
		}
	}

	@Entity(name = "AssociationFilterJoin")
	@Table(name = "at_filter_jt")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	public static class AssociationFilterJoin extends Owner {
		@ManyToOne(fetch = FetchType.EAGER)
		@JoinTable(name = "at_filter_jt_link", joinColumns = @JoinColumn(name = "owner_id"),
				inverseJoinColumns = @JoinColumn(name = "target_id"))
		@Filter(name = "visibleTarget", condition = "active = :active")
		UnrestrictedTarget target;

		@Override
		public Target getTarget() {
			return target;
		}

		@Override
		public void setTarget(Target target) {
			this.target = (UnrestrictedTarget) target;
		}
	}

	@Entity(name = "AssociationFilterJoinSelect")
	@Table(name = "at_filter_jt_select")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	public static class AssociationFilterJoinSelect extends Owner {
		@ManyToOne(fetch = FetchType.EAGER)
		@Fetch(FetchMode.SELECT)
		@JoinTable(name = "at_filter_jt_select_link", joinColumns = @JoinColumn(name = "owner_id"),
				inverseJoinColumns = @JoinColumn(name = "target_id"))
		@Filter(name = "visibleTarget", condition = "active = :active")
		UnrestrictedTarget target;

		@Override
		public Target getTarget() {
			return target;
		}

		@Override
		public void setTarget(Target target) {
			this.target = (UnrestrictedTarget) target;
		}
	}

	@Entity(name = "AssociationFilterJoinLazy")
	@Table(name = "at_filter_jt_lazy")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	public static class AssociationFilterJoinLazy extends Owner {
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinTable(name = "at_filter_jt_lazy_link", joinColumns = @JoinColumn(name = "owner_id"),
				inverseJoinColumns = @JoinColumn(name = "target_id"))
		@Filter(name = "visibleTarget", condition = "active = :active")
		UnrestrictedTarget target;

		@Override
		public Target getTarget() {
			return target;
		}

		@Override
		public void setTarget(Target target) {
			this.target = (UnrestrictedTarget) target;
		}
	}

	@Entity(name = "AssociationFilterOneFk")
	@Table(name = "at_filter_one_fk")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	public static class AssociationFilterOneFk extends Owner {
		@OneToOne(fetch = FetchType.EAGER, orphanRemoval = true)
		@JoinColumn(name = "target_id")
		@Filter(name = "visibleTarget", condition = "active = :active")
		UnrestrictedTarget target;

		@Override
		public Target getTarget() {
			return target;
		}

		@Override
		public void setTarget(Target target) {
			this.target = (UnrestrictedTarget) target;
		}
	}

	@Entity(name = "AssociationFilterOneFkSelect")
	@Table(name = "at_filter_one_fk_select")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	public static class AssociationFilterOneFkSelect extends Owner {
		@OneToOne(fetch = FetchType.EAGER, orphanRemoval = true)
		@Fetch(FetchMode.SELECT)
		@JoinColumn(name = "target_id")
		@Filter(name = "visibleTarget", condition = "active = :active")
		UnrestrictedTarget target;

		@Override
		public Target getTarget() {
			return target;
		}

		@Override
		public void setTarget(Target target) {
			this.target = (UnrestrictedTarget) target;
		}
	}

	@Entity(name = "AssociationFilterOneFkLazy")
	@Table(name = "at_filter_one_fk_lazy")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	public static class AssociationFilterOneFkLazy extends Owner {
		@OneToOne(fetch = FetchType.LAZY, orphanRemoval = true)
		@JoinColumn(name = "target_id")
		@Filter(name = "visibleTarget", condition = "active = :active")
		UnrestrictedTarget target;

		@Override
		public Target getTarget() {
			return target;
		}

		@Override
		public void setTarget(Target target) {
			this.target = (UnrestrictedTarget) target;
		}
	}

	@Entity(name = "AssociationFilterOneJoin")
	@Table(name = "at_filter_one_jt")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	public static class AssociationFilterOneJoin extends Owner {
		@OneToOne(fetch = FetchType.EAGER, orphanRemoval = true)
		@JoinTable(name = "at_filter_one_jt_link", joinColumns = @JoinColumn(name = "owner_id"),
				inverseJoinColumns = @JoinColumn(name = "target_id"))
		@Filter(name = "visibleTarget", condition = "active = :active")
		UnrestrictedTarget target;

		@Override
		public Target getTarget() {
			return target;
		}

		@Override
		public void setTarget(Target target) {
			this.target = (UnrestrictedTarget) target;
		}
	}

	@Entity(name = "AssociationFilterOneJoinSelect")
	@Table(name = "at_filter_one_jt_select")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	public static class AssociationFilterOneJoinSelect extends Owner {
		@OneToOne(fetch = FetchType.EAGER, orphanRemoval = true)
		@Fetch(FetchMode.SELECT)
		@JoinTable(name = "at_filter_one_jt_select_link", joinColumns = @JoinColumn(name = "owner_id"),
				inverseJoinColumns = @JoinColumn(name = "target_id"))
		@Filter(name = "visibleTarget", condition = "active = :active")
		UnrestrictedTarget target;

		@Override
		public Target getTarget() {
			return target;
		}

		@Override
		public void setTarget(Target target) {
			this.target = (UnrestrictedTarget) target;
		}
	}

	@Entity(name = "AssociationFilterOneJoinLazy")
	@Table(name = "at_filter_one_jt_lazy")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	public static class AssociationFilterOneJoinLazy extends Owner {
		@OneToOne(fetch = FetchType.LAZY, orphanRemoval = true)
		@JoinTable(name = "at_filter_one_jt_lazy_link", joinColumns = @JoinColumn(name = "owner_id"),
				inverseJoinColumns = @JoinColumn(name = "target_id"))
		@Filter(name = "visibleTarget", condition = "active = :active")
		UnrestrictedTarget target;

		@Override
		public Target getTarget() {
			return target;
		}

		@Override
		public void setTarget(Target target) {
			this.target = (UnrestrictedTarget) target;
		}
	}

	@Entity(name = "AssociationSqlCustomFk")
	@Table(name = "at_sql_custom")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	@SQLUpdate(sql = "update at_sql_custom set name=?, target_id=? where id=?")
	public static class AssociationSqlCustomFk extends Owner {
		@ManyToOne
		@JoinColumn(name = "target_id")
		@SQLRestriction("active = true")
		UnrestrictedTarget target;

		@Override
		public Target getTarget() {
			return target;
		}

		@Override
		public void setTarget(Target target) {
			this.target = (UnrestrictedTarget) target;
		}
	}

	@Entity(name = "AssociationSqlAllFk")
	@Table(name = "at_sql_all")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	@DynamicUpdate
	@OptimisticLocking(type = OptimisticLockType.ALL)
	public static class AssociationSqlAllFk extends Owner {
		@ManyToOne
		@JoinColumn(name = "target_id")
		@SQLRestriction("active = true")
		UnrestrictedTarget target;

		@Override
		public Target getTarget() {
			return target;
		}

		@Override
		public void setTarget(Target target) {
			this.target = (UnrestrictedTarget) target;
		}
	}

	@Entity(name = "AssociationSqlDirtyFk")
	@Table(name = "at_sql_dirty")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	@DynamicUpdate
	@OptimisticLocking(type = OptimisticLockType.DIRTY)
	public static class AssociationSqlDirtyFk extends Owner {
		@ManyToOne
		@JoinColumn(name = "target_id")
		@SQLRestriction("active = true")
		UnrestrictedTarget target;

		@Override
		public Target getTarget() {
			return target;
		}

		@Override
		public void setTarget(Target target) {
			this.target = (UnrestrictedTarget) target;
		}
	}

	@Entity(name = "AssociationFilterCustomFk")
	@Table(name = "at_filter_custom")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	@SQLUpdate(sql = "update at_filter_custom set name=?, target_id=? where id=?")
	public static class AssociationFilterCustomFk extends Owner {
		@ManyToOne
		@JoinColumn(name = "target_id")
		@Filter(name = "visibleTarget", condition = "active = :active")
		UnrestrictedTarget target;

		@Override
		public Target getTarget() {
			return target;
		}

		@Override
		public void setTarget(Target target) {
			this.target = (UnrestrictedTarget) target;
		}
	}

	@Entity(name = "AssociationFilterAllFk")
	@Table(name = "at_filter_all")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	@DynamicUpdate
	@OptimisticLocking(type = OptimisticLockType.ALL)
	public static class AssociationFilterAllFk extends Owner {
		@ManyToOne
		@JoinColumn(name = "target_id")
		@Filter(name = "visibleTarget", condition = "active = :active")
		UnrestrictedTarget target;

		@Override
		public Target getTarget() {
			return target;
		}

		@Override
		public void setTarget(Target target) {
			this.target = (UnrestrictedTarget) target;
		}
	}

	@Entity(name = "AssociationFilterDirtyFk")
	@Table(name = "at_filter_dirty")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	@DynamicUpdate
	@OptimisticLocking(type = OptimisticLockType.DIRTY)
	public static class AssociationFilterDirtyFk extends Owner {
		@ManyToOne
		@JoinColumn(name = "target_id")
		@Filter(name = "visibleTarget", condition = "active = :active")
		UnrestrictedTarget target;

		@Override
		public Target getTarget() {
			return target;
		}

		@Override
		public void setTarget(Target target) {
			this.target = (UnrestrictedTarget) target;
		}
	}
}
