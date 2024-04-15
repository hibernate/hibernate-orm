/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.lazy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

import org.hibernate.Hibernate;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

@RunWith(BytecodeEnhancerRunner.class)
@TestForIssue(jiraKey = "HHH-15090")
public class LazyLoadingAndInheritanceTest extends BaseCoreFunctionalTestCase {

	private Long containingID;

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Containing.class, Contained.class, ContainedExtended.class };
	}

	@Override
	protected void configure(Configuration configuration) {
		configuration.setProperty( AvailableSettings.USE_SECOND_LEVEL_CACHE, "false" );
		configuration.setProperty( AvailableSettings.ENABLE_LAZY_LOAD_NO_TRANS, "true" );
	}

	@Before
	public void prepare() {
		doInHibernate( this::sessionFactory, s -> {
			Containing containing = new Containing();
			ContainedExtended contained = new ContainedExtended( "George" );
			containing.contained = contained;
			s.persist( contained );
			s.persist( containing );
			containingID = containing.id;
		} );
	}

	@Test
	public void test() {
		doInHibernate( this::sessionFactory, s -> {
			Containing containing = s.load( Containing.class, containingID );
			Contained contained = containing.contained;
			assertThat( contained ).isNotNull();
			assertThat( Hibernate.isPropertyInitialized( contained, "name" ) ).isFalse();
			assertThat( contained.name ).isNotNull();
		} );
	}

	@Entity(name = "Containing")
	private static class Containing {

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		public Long id;

		@ManyToOne(fetch = FetchType.LAZY, optional = false)
		public Contained contained;
	}

	@Entity(name = "Contained")
	private static class Contained {

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		public Long id;

		public String name;

		Contained() {
		}

		Contained(String name) {
			this.name = name;
		}
	}

	@Entity(name = "ContainedExtended")
	private static class ContainedExtended extends Contained {

		ContainedExtended() {
		}

		ContainedExtended(String name) {
			this.name = name;
		}

	}
}
