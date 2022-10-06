/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.envers.test.integration.manytoone.lazy;

import java.util.Map;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.envers.AuditTable;
import org.hibernate.envers.Audited;
import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.jdbc.SQLStatementInterceptor;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Jan Schatteman
 */
@TestForIssue( jiraKey = "HHH-14176" )
public class ManyToOneLazyProxyTest extends BaseEnversJPAFunctionalTestCase {

	private SQLStatementInterceptor sqlStatementInspector;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			OtherEntity.class, MyEntity.class
		};
	}

	@Override
	protected void addConfigOptions(Map options) {
		super.addConfigOptions( options );
 		sqlStatementInspector = new SQLStatementInterceptor(options);
	}

	@After
	public void clearData() {
		doInJPA(
				this::entityManagerFactory,
				em -> {
					em.createQuery( "delete from MyEntity" );
					em.createQuery( "delete from OtherEntity" );
				}
		);
	}

	@Test
	public void testLazyProxy() {
		doInJPA(
				this::entityManagerFactory,
				em -> {
					OtherEntity other = new OtherEntity();
					other.setId( 1L );
					other.setDesc( "abc" );
					em.persist( other );
				}
		);

		sqlStatementInspector.clear();
		final Long meId = doInJPA(
				this::entityManagerFactory,
				em -> {
					MyEntity me = new MyEntity();
					me.setOther( em.getReference( OtherEntity.class, 1L ) );
					em.persist(me);
					return me.getId();
				}
		);
		Assert.assertEquals( 0, sqlStatementInspector.getSqlQueries().stream().filter( s -> s.startsWith( "select" ) && s.contains( "other_entity" ) ).count() );
	}

	@Entity(name = "OtherEntity")
	@Table(name = "other_entity")
	@Audited
	@AuditTable(value = "other_entity_aud")
	public static class OtherEntity {
		@Id
		private Long id;

		private String desc;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getDesc() {
			return desc;
		}

		public void setDesc(String desc) {
			this.desc = desc;
		}
	}

	@Entity(name = "MyEntity")
	@Table(name = "my_entity")
	@Audited
	@AuditTable(value = "my_entity_aud")
	public static class MyEntity {
		@Id
		@GeneratedValue
		private Long id;

		@ManyToOne(optional = false, fetch = FetchType.LAZY)
		private OtherEntity other;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public OtherEntity getOther() {
			return other;
		}

		public void setOther(OtherEntity other) {
			this.other = other;
		}
	}

}
