/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.wf.ddl;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.hibernate.testing.TestForIssue;

/**
 * @author Andrea Boriero
 */
@RunWith(Arquillian.class)
@TestForIssue(jiraKey = "HHH-11024")
public class WildFlyHibernateDdlTest {

	public static final String ARCHIVE_NAME = "WildFlyHibernateDdlTest";

	public static final String hibernate_cfg = "<?xml version='1.0' encoding='utf-8'?>"
			+ "<!DOCTYPE hibernate-configuration PUBLIC " + "\"//Hibernate/Hibernate Configuration DTD 3.0//EN\" "
			+ "\"http://www.hibernate.org/dtd/hibernate-configuration-3.0.dtd\">"
			+ "<hibernate-configuration><session-factory>" + "<property name=\"show_sql\">true</property>"
			+ "<property name=\"hibernate.show_sql\">true</property>"
			+ "<property name=\"hibernate.hbm2ddl.auto\">create-drop</property>"
			+ "<property name=\"hibernate.connection.datasource\">java:jboss/datasources/ExampleDS</property>"
			+ "<property name=\"hibernate.transaction.jta.platform\">JBossAppServerJtaPlatform.class</property>"
			+ "<property name=\"hibernate.id.new_generator_mappings\">true</property>"
			+ "</session-factory></hibernate-configuration>";

	@ArquillianResource
	private static InitialContext iniCtx;

	@BeforeClass
	public static void beforeClass() throws NamingException {
		iniCtx = new InitialContext();
	}

	@Deployment
	public static WebArchive deploy() throws Exception {
		final WebArchive war = ShrinkWrap.create( WebArchive.class, ARCHIVE_NAME + ".war" )
				.setManifest( "org/hibernate/test/wf/ddl/manifest.mf" )
				.addClasses( WildFlyDdlEntity.class )
				.addAsResource( new StringAsset( hibernate_cfg ), "hibernate.cfg.xml" )
				.addClasses( SFSBHibernateSessionFactory.class )
				.addClasses( WildFlyHibernateDdlTest.class );
		return war;
	}

	@Test
	public void testCreateThenDrop() throws Exception {
		final SFSBHibernateSessionFactory sfsb = lookup(
				"SFSBHibernateSessionFactory",
				SFSBHibernateSessionFactory.class
		);
		try {
			sfsb.setupConfig();
		}
		finally {
			sfsb.cleanup();
		}
	}

	private static <T> T lookup(String beanName, Class<T> interfaceType) throws NamingException {
		try {
			return interfaceType.cast( iniCtx.lookup( "java:global/" + ARCHIVE_NAME + "/" + beanName + "!"
															  + interfaceType.getName() ) );
		}
		catch (NamingException e) {
			throw e;
		}
	}
}
