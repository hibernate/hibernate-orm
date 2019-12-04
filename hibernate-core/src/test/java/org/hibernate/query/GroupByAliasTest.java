/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.jdbc.SQLStatementInterceptor;
import org.junit.Test;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.ManyToOne;
import javax.persistence.Tuple;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertNotNull;

/**
 * @author Jan-Willem Gmelig Meyling
 * @author Sayra Ranjha
 */
@RequiresDialectFeature(value = DialectChecks.SupportsSelectAliasInGroupByClause.class, jiraKey = "HHH-9301")
public class GroupByAliasTest extends BaseEntityManagerFunctionalTestCase {

	public static final int MAX_COUNT = 15;

	private SQLStatementInterceptor sqlStatementInterceptor;

	@Override
	protected void addConfigOptions(Map options) {
		sqlStatementInterceptor = new SQLStatementInterceptor( options );
	}

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] {
			Person.class,
			Association.class
		};
	}

	@Override
	protected void afterEntityManagerFactoryBuilt() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			for ( int i = 0; i < MAX_COUNT; i++ ) {
				Association association = new Association();
				association.setId( i );
				association.setName(String.format( "Association nr %d", i ) );

				Person person = new Person();
				person.setId( i );
				person.setName( String.format( "Person nr %d", i ) );
				person.setAssociation(association);
				person.setAge(5);
				entityManager.persist( person );
			}
		} );
	}

	@Test
	public void testSingleIdAlias() {
		sqlStatementInterceptor.clear();

		List<Tuple> list = doInJPA(this::entityManagerFactory, entityManager -> {
			return entityManager.createQuery(
					"select p.id as id_alias, sum(p.age) " +
							"from Person p group by id_alias order by id_alias", Tuple.class)
					.getResultList();
		});

		String s = sqlStatementInterceptor.getSqlQueries().get(0);
		assertNotNull(s);
	}

	@Test
	public void testCompoundIdAlias() {
		sqlStatementInterceptor.clear();

		List<Tuple> list = doInJPA(this::entityManagerFactory, entityManager -> {
			return entityManager.createQuery(
					"select p.association as id_alias, sum(p.age) " +
							"from Person p group by id_alias, p.association.id, p.association.name order by id_alias", Tuple.class)
					.getResultList();
		});

		String s = sqlStatementInterceptor.getSqlQueries().get(0);
		assertNotNull(s);
	}


	@Test
	public void testMultiIdAlias() {
		sqlStatementInterceptor.clear();

		List<Tuple> list = doInJPA(this::entityManagerFactory, entityManager -> {
			return entityManager.createQuery(
					"select p.id as id_alias_1, p.association.id as id_alias_2, sum(p.age) " +
							"from Person p group by id_alias_1, id_alias_2 order by id_alias_1, id_alias_2 ", Tuple.class)
					.getResultList();
		});

		String s = sqlStatementInterceptor.getSqlQueries().get(0);
		assertNotNull(s);
	}

	@Entity(name = "Person")
	public static class Person {

		@Id
		private Integer id;

		private String name;

		private Integer age;

		@ManyToOne(cascade = CascadeType.PERSIST)
		private Association association;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Integer getAge() {
			return age;
		}

		public void setAge(Integer age) {
			this.age = age;
		}

		public Association getAssociation() {
			return association;
		}

		public void setAssociation(Association association) {
			this.association = association;
		}
	}


	@IdClass(Association.IdClass.class)
	@Entity(name = "Association")
	public static class Association {

		public static class IdClass implements Serializable {
			private Integer id;
			private String name;

			public Integer getId() {
				return id;
			}

			public void setId(Integer id) {
				this.id = id;
			}

			public String getName() {
				return name;
			}

			public void setName(String name) {
				this.name = name;
			}

			@Override
			public boolean equals(Object o) {
				if (this == o) return true;
				if (o == null || getClass() != o.getClass()) return false;
				IdClass id1 = (IdClass) o;
				return Objects.equals(id, id1.id) &&
						Objects.equals(name, id1.name);
			}

			@Override
			public int hashCode() {
				return Objects.hash(id, name);
			}
		}

		@Id
		private Integer id;

		@Id
		private String name;


		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

	}

}
