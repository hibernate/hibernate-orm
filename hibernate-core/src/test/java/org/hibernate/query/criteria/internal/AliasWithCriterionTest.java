/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.criteria.internal;

import java.util.Date;
import java.util.List;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.Criteria;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.sql.JoinType;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 */
public class AliasWithCriterionTest extends BaseCoreFunctionalTestCase {

	@Test
	@RequiresDialect( H2Dialect.class )
	public void testCaseClause() {
		doInHibernate( this::sessionFactory, session -> {
			Criteria criteria = session.createCriteria( TableA.class );

			final String TABLE_B_ALIAS = "tableBAlias";
			final String TABLE_C_ALIAS = "tableCAlias";

			Criterion tableCRestriction = Restrictions.eq( TABLE_C_ALIAS + ".tableCBoolean", false );
			criteria.createAlias(
					TABLE_B_ALIAS + ".tableCs",
					TABLE_C_ALIAS,
					JoinType.LEFT_OUTER_JOIN,
					tableCRestriction
			);

			Criterion tableBRestriction = Restrictions.eq( TABLE_B_ALIAS + ".tableBDate", new Date() );
			criteria.createAlias( "tableBs", TABLE_B_ALIAS, JoinType.LEFT_OUTER_JOIN, tableBRestriction );

			criteria.add( Restrictions.eq( "tableACharacter", "c" ) );

			ProjectionList projectionList = Projections.projectionList();
			projectionList.add( Projections.property( "tableACharacter" ) );
			criteria.setProjection( projectionList );

			criteria.list();
		} );
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				TableA.class,
				TableB.class,
				TableC.class,
		};
	}

	@Entity(name = "TableA")
	public static class TableA {

		@Id
		@Column(name = "table_a_id")
		private Long tableAId;

		@Column(name = "table_a_character")
		private String tableACharacter;

		@OneToMany(mappedBy = "tableA")
		private Set<TableB> tableBs;

		public TableA() {
		}

		public Long getTableAId() {
			return this.tableAId;
		}

		public void setTableAId_(Long _tableAId_) {
			this.tableAId = _tableAId_;
		}

		public String getTableACharacter() {
			return this.tableACharacter;
		}

		public void setTableACharacter(String _tableACharacter_) {
			this.tableACharacter = _tableACharacter_;
		}

		public Set<TableB> getTableBs() {
			return this.tableBs;
		}

		public void setTableBs(Set<TableB> tableBs) {
			this.tableBs = tableBs;
		}

	}

	@Entity(name = "TableB")
	public static class TableB {

		@Id
		@Column(name = "table_b_id")
		private Long tableBId;

		@Column(name = "table_a_id", insertable = false, updatable = false)
		private Long tableAId;

		@Temporal(TemporalType.DATE)
		@Column(name = "table_b_date")
		private Date tableBDate;

		@ManyToOne
		@JoinColumn(name = "table_a_id")
		private TableA tableA;

		@OneToMany(mappedBy = "tableB")
		private Set<TableC> tableCs;


		public TableB() {
		}

		public Long getTableBId() {
			return this.tableBId;
		}

		public void setTableBId(Long _tableBId_) {
			this.tableBId = _tableBId_;
		}

		public Long getTableAId() {
			return this.tableAId;
		}

		public void setTableAId(Long _tableAId_) {
			this.tableAId = _tableAId_;
		}

		public Date getTableBDate() {
			return this.tableBDate;
		}

		public void setTableBDate(Date _tableBDate_) {
			this.tableBDate = _tableBDate_;
		}

		public TableA getTableA() {
			return tableA;
		}

		public void setTableA(TableA tableA) {
			this.tableA = tableA;
		}

		public Set<TableC> getTableCs() {
			return tableCs;
		}

		public void setTableCs(Set<TableC> tableCs) {
			this.tableCs = tableCs;
		}

	}

	@Entity(name = "TableC")
	public static class TableC {

		@Id
		@Column(name = "table_c_id")
		private Long tableCId;

		@Column(name = "table_b_id", insertable = false, updatable = false)
		private Long tableBId;

		@Column(name = "table_c_boolean")
		private Boolean tableCBoolean;

		@ManyToOne
		@JoinColumn(name = "table_b_id")
		private TableB tableB;

		public TableC() {
		}

		public Long getTableCId() {
			return this.tableCId;
		}

		public void setTableCId(Long tableCId) {
			this.tableCId = tableCId;
		}

		public Long getTableBId() {
			return this.tableBId;
		}

		public void setTableBId(Long tableBId) {
			this.tableBId = tableBId;
		}

		public Boolean getTableCBoolean() {
			return this.tableCBoolean;
		}

		public void setTableCBoolean(Boolean tableCBoolean) {
			this.tableCBoolean = tableCBoolean;
		}

		public TableB getTableB() {
			return tableB;
		}

		public void setTableB(TableB tableB) {
			this.tableB = tableB;
		}
	}
}