/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.customtype;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.envers.test.BaseEnversFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.customtype.UnspecifiedEnumTypeEntity;

import org.hibernate.jdbc.Work;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@TestForIssue(jiraKey = "HHH-7780")
@RequiresDialect(value = H2Dialect.class)
public class UnspecifiedEnumTypeTest extends BaseEnversFunctionalTestCase {
	private Long id = null;

	@Override
	protected String[] getMappings() {
		return new String[] {"mappings/customType/mappings.hbm.xml"};
	}

	@Override
	protected void addSettings(Map settings) {
		super.addSettings( settings );

		settings.put( Environment.HBM2DDL_AUTO, "" );
	}

	@Test
	@Priority(10)
	public void prepareSchema() {
		Session session = openSession();
		dropSchema( session );
		createSchema( session );
		session.close();
	}

	@Test
	@Priority(1)
	public void dropSchema() {
		dropSchema( getSession() );
	}

	public void dropSchema(Session session) {
		executeUpdateSafety( session, "drop table ENUM_ENTITY if exists" );
		executeUpdateSafety( session, "drop table ENUM_ENTITY_AUD if exists" );
		executeUpdateSafety( session, "drop table REVINFO if exists" );
		executeUpdateSafety( session, "drop sequence REVISION_GENERATOR if exists" );
	}

	private void createSchema(Session session) {
		executeUpdateSafety(
				session,
				"create table ENUM_ENTITY (ID bigint not null, enum1 integer, enum2 integer, primary key (ID))"
		);
		executeUpdateSafety(
				session,
				"create table ENUM_ENTITY_AUD (ID bigint not null, REV integer not null, REVTYPE tinyint, enum1 integer, enum2 integer, primary key (ID, REV))"
		);
		executeUpdateSafety(
				session,
				"create table REVINFO (REV integer not null, REVTSTMP bigint, primary key (REV))"
		);
		executeUpdateSafety(
				session,
				"alter table ENUM_ENTITY_AUD add constraint FK_AUD_REV foreign key (REV) references REVINFO"
		);
		executeUpdateSafety( session, "create sequence REVISION_GENERATOR start with 1 increment by 1" );
	}

	private void executeUpdateSafety(Session session, String query) {
		session.doWork(
				new Work() {
					@Override
					public void execute(Connection connection) throws SQLException {
						connection.createStatement().execute( query );
					}
				}
		);
	}

	@Test
	@Priority(9)
	public void initData() {
		Session session = getSession();

		// Revision 1
		session.getTransaction().begin();
		UnspecifiedEnumTypeEntity entity = new UnspecifiedEnumTypeEntity(
				UnspecifiedEnumTypeEntity.E1.X,
				UnspecifiedEnumTypeEntity.E2.A
		);
		session.persist( entity );
		session.getTransaction().commit();

		id = entity.getId();

		// Revision 2
		session.getTransaction().begin();
		entity = (UnspecifiedEnumTypeEntity) session.get( UnspecifiedEnumTypeEntity.class, entity.getId() );
		entity.setEnum1( UnspecifiedEnumTypeEntity.E1.Y );
		entity.setEnum2( UnspecifiedEnumTypeEntity.E2.B );
		session.update( entity );
		session.getTransaction().commit();

		session.close();
	}

	@Test
	@Priority(8)
	public void testRevisionCount() {
		Assert.assertEquals(
				Arrays.asList( 1, 2 ), getAuditReader().getRevisions(
				UnspecifiedEnumTypeEntity.class,
				id
		)
		);
	}

	@Test
	@Priority(7)
	public void testHistoryOfEnums() {
		UnspecifiedEnumTypeEntity ver1 = new UnspecifiedEnumTypeEntity(
				UnspecifiedEnumTypeEntity.E1.X,
				UnspecifiedEnumTypeEntity.E2.A,
				id
		);
		UnspecifiedEnumTypeEntity ver2 = new UnspecifiedEnumTypeEntity(
				UnspecifiedEnumTypeEntity.E1.Y,
				UnspecifiedEnumTypeEntity.E2.B,
				id
		);

		Assert.assertEquals( ver1, getAuditReader().find( UnspecifiedEnumTypeEntity.class, id, 1 ) );
		Assert.assertEquals( ver2, getAuditReader().find( UnspecifiedEnumTypeEntity.class, id, 2 ) );
	}

	@Test
	@Priority(6)
	public void testEnumRepresentation() {
		Session session = getSession();
		List<Object[]> values = session.createSQLQuery( "SELECT enum1, enum2 FROM enum_entity_aud ORDER BY rev ASC" )
				.list();
		session.close();

		Assert.assertNotNull( values );
		Assert.assertEquals( 2, values.size() );
		Assert.assertArrayEquals( new Object[] {0, 0}, values.get( 0 ) );
		Assert.assertArrayEquals( new Object[] {1, 1}, values.get( 1 ) );
	}
}