/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2004-2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.stat.Statistics;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.tool.api.stat.StatisticsBrowser;
import org.hibernate.tool.test.utils.JdbcUtil;
import org.junit.jupiter.api.*;

public class TestCase {
	
	@BeforeEach
	public void setUp() {
		JdbcUtil.createDatabase(this);
	}

	@AfterEach
	public void tearDown() {
		JdbcUtil.dropDatabase(this);
	}

	// HBX-1554: Ignore the test for now
	// TODO: re-enable the test
	@Disabled
	@Test
	public void testBrowser() {
		MetadataSources mds = new MetadataSources();
		mds.addResource("/org/hibernate/tool/stat/Statistics/UserGroup.hbm.xml");
		Metadata md = mds.buildMetadata();
		SessionFactory sf = md.buildSessionFactory();
		sf.getStatistics().setStatisticsEnabled(true);

		new StatisticsBrowser().showStatistics( sf.getStatistics(), false );

		Session s = sf.openSession();
		Transaction tx = s.beginTransaction();
		
		for(int i=0; i<10; i++) {
			Group group = new Group( "Hibernate" + i );
			group.addUser(new User("gavin" + i, "figo123"));
			group.addUser(new User("cbauer" + i, "figo123"));
			group.addUser(new User("steve" + i, "figo123"));
			group.addUser(new User("max" + i, "figo123"));
			group.addUser(new User("anthony" + i, "figo123"));

			s.persist( group );
			if(i % 20==0) s.flush();
		}
		s.flush();
		s.clear();
		s.createQuery( "from java.lang.Object", (Class<?>)null).getResultList();
		tx.commit();
		s.close();
		sf.close();
	}

}
