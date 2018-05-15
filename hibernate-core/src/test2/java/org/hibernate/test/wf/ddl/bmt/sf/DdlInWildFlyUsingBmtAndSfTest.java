/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.wf.ddl.bmt.sf;

import org.hibernate.testing.TestForIssue;
import org.hibernate.test.wf.ddl.WildFlyDdlEntity;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;

/**
 * @author Andrea Boriero
 */
@RunWith(Arquillian.class)
@TestForIssue(jiraKey = "HHH-11024")
@Ignore( "WildFly has not released a version supporting JPA 2.2 and CDI 2.0" )
public class DdlInWildFlyUsingBmtAndSfTest {

	public static final String ARCHIVE_NAME = BmtSfStatefulBean.class.getSimpleName();

	public static final String hibernate_cfg = "<?xml version='1.0' encoding='utf-8'?>"
			+ "<!DOCTYPE hibernate-configuration PUBLIC " + "\"//Hibernate/Hibernate Configuration DTD 3.0//EN\" "
			+ "\"http://www.hibernate.org/dtd/hibernate-configuration-3.0.dtd\">"
			+ "<hibernate-configuration><session-factory>" + "<property name=\"show_sql\">true</property>"
			+ "<property name=\"hibernate.show_sql\">true</property>"
			+ "<property name=\"hibernate.hbm2ddl.auto\">create-drop</property>"
			+ "<property name=\"hibernate.connection.datasource\">java:jboss/datasources/ExampleDS</property>"
			+ "<property name=\"hibernate.transaction.jta.platform\">JBossAS</property>"
			+ "<property name=\"hibernate.transaction.coordinator_class\">jta</property>"
			+ "<property name=\"hibernate.id.new_generator_mappings\">true</property>"
			+ "</session-factory></hibernate-configuration>";

	@Deployment
	public static WebArchive deploy() throws Exception {
		final WebArchive war = ShrinkWrap.create( WebArchive.class, ARCHIVE_NAME + ".war" )
				.setManifest( "org/hibernate/test/wf/ddl/manifest.mf" )
				.addClasses( WildFlyDdlEntity.class )
				.addAsResource( new StringAsset( hibernate_cfg ), "hibernate.cfg.xml" )
				.addClasses( BmtSfStatefulBean.class )
				.addClasses( DdlInWildFlyUsingBmtAndSfTest.class );
		return war;
	}

	;

	@Test
	public void testCreateThenDrop(BmtSfStatefulBean ejb) throws Exception {
		assert ejb != null : "Method injected StatefulCMTBean reference was null";

		try {
			ejb.start();
		}
		finally {
			ejb.stop();
		}
	}
}
