/*
 * Copyright 2014 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.internal.hhh12076;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.query.Query;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertNotNull;

@TestForIssue(jiraKey = "HHH-12076")
public class HQLJoinClassTest extends BaseCoreFunctionalTestCase {

	// Add your entities here.
	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				ClassA.class,
				ClassB.class,
				ClassC.class,
				ClassD.class,
				ClassE.class
		};
	}

	// If you use *.hbm.xml mappings, instead of annotations, add the mappings here.
	@Override
	protected String[] getMappings() {
		return new String[] {
//				"Foo.hbm.xml",
//				"Bar.hbm.xml"
		};
	}
	// If those mappings reside somewhere other than resources/org/hibernate/test, change this.
	@Override
	protected String getBaseForMappings() {
		return "org/hibernate/test/";
	}

	// Add in any settings that are specific to your test.  See resources/hibernate.properties for the defaults.
	@Override
	protected void configure(Configuration configuration) {
		super.configure( configuration );

		configuration.setProperty( AvailableSettings.DIALECT, "org.hibernate.dialect.H2Dialect" );
		configuration.setProperty( AvailableSettings.SHOW_SQL, Boolean.TRUE.toString() );
		configuration.setProperty( AvailableSettings.FORMAT_SQL, Boolean.TRUE.toString() );
		//configuration.setProperty( AvailableSettings.GENERATE_STATISTICS, "true" );
	}

	// Add your tests, using standard JUnit.
	@Test
	public void hhh12076Test() throws Exception {
		// BaseCoreFunctionalTestCase automatically creates the SessionFactory and provides the Session.
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		// Do stuff...

		for (int i = 0; i < 10; i++) {
			ClassA classA = new ClassA();
			ClassE classE = new ClassE();
			s.save(classE);
			classA.setAdditionalClass(classE);
			for (int j = 0; j < 2; j++) {
				ClassC classC = new ClassC();
				ClassD classD = new ClassD();

				classA.getSubClasses().add(classC);
				classA.getSubClasses().add(classD);
			}

			s.save(classA);
		}

		final String hql = "select c.id, c.name from ClassA as c join ClassE as e left join ClassB as sub with sub.class = ClassD";

		Query<ClassA> query = session.createQuery(hql);
		List<ClassA> results = query.getResultList();
		assertNotNull(results);

		tx.commit();
		s.close();
	}
}
