package org.hibernate.orm.test.where.annotations;

import java.util.stream.Stream;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.ParamDef;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.testing.orm.junit.DomainModel;
import org.junit.jupiter.params.provider.Arguments;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@DomainModel(annotatedClasses = { RestrictedToOneNotFoundTest.PlainTarget.class,
		AssociationRestrictedToOneNotFoundTest.AssocSqlIgnore.class,
		AssociationRestrictedToOneNotFoundTest.AssocSqlException.class,
		AssociationRestrictedToOneNotFoundTest.AssocFilterIgnore.class,
		AssociationRestrictedToOneNotFoundTest.AssocFilterException.class })
class AssociationRestrictedToOneNotFoundTest extends RestrictedToOneNotFoundTest {
	static Stream<Arguments> cases() {
		return Stream.of(
				new Mapping( AssocSqlIgnore.class, "Sql", true ),
				new Mapping( AssocSqlException.class, "Sql", false ),
				new Mapping( AssocFilterIgnore.class, "Filter", true ),
				new Mapping( AssocFilterException.class, "Filter", false ) )
				.flatMap( mapping -> Stream.of( false, true ).flatMap( enabled ->
						Stream.of( Load.values() ).map( load -> Arguments.of( mapping, enabled, load ) ) ) );
	}

	@Override
	String targetTable(Mapping mapping) {
		return "nf_plain_target";
	}

	@Entity(name = "AssocSqlIgnore")
	@Table(name = "nf_assocsqlignore")
	static class AssocSqlIgnore extends Owner {
		@ManyToOne(fetch = FetchType.LAZY)
		@NotFound(action = NotFoundAction.IGNORE)
		@Fetch(FetchMode.JOIN)
		@SQLRestriction("visible = 1")
		@JoinColumn(name = "fk_id")
		PlainTarget fk;
		@ManyToOne(fetch = FetchType.LAZY)
		@NotFound(action = NotFoundAction.IGNORE)
		@Fetch(FetchMode.JOIN)
		@SQLRestriction("visible = 1")
		@JoinTable(name = "nf_assocsqlignore_join", joinColumns = @JoinColumn(name = "owner_id"), inverseJoinColumns = @JoinColumn(name = "joined_id"))
		PlainTarget joined;
		@ManyToOne(fetch = FetchType.LAZY)
		@NotFound(action = NotFoundAction.IGNORE)
		@Fetch(FetchMode.SELECT)
		@SQLRestriction("visible = 1")
		@JoinColumn(name = "selected_id")
		PlainTarget selected;
		@ManyToOne(fetch = FetchType.LAZY)
		@NotFound(action = NotFoundAction.IGNORE)
		@Fetch(FetchMode.SELECT)
		@SQLRestriction("visible = 1")
		@JoinTable(name = "nf_assocsqlignore_select_join", joinColumns = @JoinColumn(name = "owner_id"), inverseJoinColumns = @JoinColumn(name = "selected_join_id"))
		PlainTarget selectedJoin;

		@Override
		Object[] targets() {
			return new Object[] { fk, joined, selected, selectedJoin };
		}
	}

	@Entity(name = "AssocSqlException")
	@Table(name = "nf_assocsqlexception")
	static class AssocSqlException extends Owner {
		@ManyToOne(fetch = FetchType.LAZY)
		@NotFound(action = NotFoundAction.EXCEPTION)
		@Fetch(FetchMode.JOIN)
		@SQLRestriction("visible = 1")
		@JoinColumn(name = "fk_id")
		PlainTarget fk;
		@ManyToOne(fetch = FetchType.LAZY)
		@NotFound(action = NotFoundAction.EXCEPTION)
		@Fetch(FetchMode.JOIN)
		@SQLRestriction("visible = 1")
		@JoinTable(name = "nf_assocsqlexception_join", joinColumns = @JoinColumn(name = "owner_id"), inverseJoinColumns = @JoinColumn(name = "joined_id"))
		PlainTarget joined;
		@ManyToOne(fetch = FetchType.LAZY)
		@NotFound(action = NotFoundAction.EXCEPTION)
		@Fetch(FetchMode.SELECT)
		@SQLRestriction("visible = 1")
		@JoinColumn(name = "selected_id")
		PlainTarget selected;
		@ManyToOne(fetch = FetchType.LAZY)
		@NotFound(action = NotFoundAction.EXCEPTION)
		@Fetch(FetchMode.SELECT)
		@SQLRestriction("visible = 1")
		@JoinTable(name = "nf_assocsqlexception_select_join", joinColumns = @JoinColumn(name = "owner_id"), inverseJoinColumns = @JoinColumn(name = "selected_join_id"))
		PlainTarget selectedJoin;

		@Override
		Object[] targets() {
			return new Object[] { fk, joined, selected, selectedJoin };
		}
	}

	@Entity(name = "AssocFilterIgnore")
	@Table(name = "nf_assocfilterignore")
	@FilterDef(name = "nf_visible", parameters = @ParamDef(name = "visible", type = Integer.class))
	static class AssocFilterIgnore extends Owner {
		@ManyToOne(fetch = FetchType.LAZY)
		@NotFound(action = NotFoundAction.IGNORE)
		@Fetch(FetchMode.JOIN)
		@Filter(name = "nf_visible", condition = "visible = :visible")
		@JoinColumn(name = "fk_id")
		PlainTarget fk;
		@ManyToOne(fetch = FetchType.LAZY)
		@NotFound(action = NotFoundAction.IGNORE)
		@Fetch(FetchMode.JOIN)
		@Filter(name = "nf_visible", condition = "visible = :visible")
		@JoinTable(name = "nf_assocfilterignore_join", joinColumns = @JoinColumn(name = "owner_id"), inverseJoinColumns = @JoinColumn(name = "joined_id"))
		PlainTarget joined;
		@ManyToOne(fetch = FetchType.LAZY)
		@NotFound(action = NotFoundAction.IGNORE)
		@Fetch(FetchMode.SELECT)
		@Filter(name = "nf_visible", condition = "visible = :visible")
		@JoinColumn(name = "selected_id")
		PlainTarget selected;
		@ManyToOne(fetch = FetchType.LAZY)
		@NotFound(action = NotFoundAction.IGNORE)
		@Fetch(FetchMode.SELECT)
		@Filter(name = "nf_visible", condition = "visible = :visible")
		@JoinTable(name = "nf_assocfilterignore_select_join", joinColumns = @JoinColumn(name = "owner_id"), inverseJoinColumns = @JoinColumn(name = "selected_join_id"))
		PlainTarget selectedJoin;

		@Override
		Object[] targets() {
			return new Object[] { fk, joined, selected, selectedJoin };
		}
	}

	@Entity(name = "AssocFilterException")
	@Table(name = "nf_assocfilterexception")
	static class AssocFilterException extends Owner {
		@ManyToOne(fetch = FetchType.LAZY)
		@NotFound(action = NotFoundAction.EXCEPTION)
		@Fetch(FetchMode.JOIN)
		@Filter(name = "nf_visible", condition = "visible = :visible")
		@JoinColumn(name = "fk_id")
		PlainTarget fk;
		@ManyToOne(fetch = FetchType.LAZY)
		@NotFound(action = NotFoundAction.EXCEPTION)
		@Fetch(FetchMode.JOIN)
		@Filter(name = "nf_visible", condition = "visible = :visible")
		@JoinTable(name = "nf_assocfilterexception_join", joinColumns = @JoinColumn(name = "owner_id"), inverseJoinColumns = @JoinColumn(name = "joined_id"))
		PlainTarget joined;
		@ManyToOne(fetch = FetchType.LAZY)
		@NotFound(action = NotFoundAction.EXCEPTION)
		@Fetch(FetchMode.SELECT)
		@Filter(name = "nf_visible", condition = "visible = :visible")
		@JoinColumn(name = "selected_id")
		PlainTarget selected;
		@ManyToOne(fetch = FetchType.LAZY)
		@NotFound(action = NotFoundAction.EXCEPTION)
		@Fetch(FetchMode.SELECT)
		@Filter(name = "nf_visible", condition = "visible = :visible")
		@JoinTable(name = "nf_assocfilterexception_select_join", joinColumns = @JoinColumn(name = "owner_id"), inverseJoinColumns = @JoinColumn(name = "selected_join_id"))
		PlainTarget selectedJoin;

		@Override
		Object[] targets() {
			return new Object[] { fk, joined, selected, selectedJoin };
		}
	}
}
