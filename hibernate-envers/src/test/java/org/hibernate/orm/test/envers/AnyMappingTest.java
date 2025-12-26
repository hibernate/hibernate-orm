/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.envers;

import java.util.List;

import org.hibernate.annotations.Any;
import org.hibernate.annotations.AnyDiscriminatorValue;
import org.hibernate.annotations.AnyKeyJavaClass;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.Audited;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertSame;

@DomainModel(
		annotatedClasses = AnyMappingTest.SimpleEntity.class
)
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-3855" )
public class AnyMappingTest {

	@Test
	public void basicTest(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new SimpleEntity( 1, "abc" ) ) );
		scope.inTransaction(
				session -> {
					SimpleEntity e = session.find( SimpleEntity.class, 1 );
					e.ref = e;
				}
		);
		scope.inTransaction(
				session -> {
					SimpleEntity e = session.find( SimpleEntity.class, 1 );
					assertSame( e, e.ref );
					List<Number> revisions = AuditReaderFactory.get( session ).getRevisions( SimpleEntity.class, 1 );
					assertThat( revisions.size(), equalTo( 2 ) );
				}
		);
	}

	@Entity( name = "SimpleEntity" )
	@Table( name = "simple" )
	@Audited
	public static class SimpleEntity {
		@Id
		private Integer id;
		String name;
		@Any
		@AnyKeyJavaClass( String.class )
		@Column(name = "ref_type")
		@JoinColumn(name = "ref_id")
		@AnyDiscriminatorValue(discriminator = "S", entity = SimpleEntity.class)
		SimpleEntity ref;


		public SimpleEntity() {
		}

		public SimpleEntity(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		private void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public SimpleEntity getRef() {
			return ref;
		}

		public void setRef(SimpleEntity ref) {
			this.ref = ref;
		}
	}
}
