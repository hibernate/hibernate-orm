package org.hibernate.orm.test.bytecode.enhancement.collectionelement.flush;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;
import org.junit.runner.RunWith;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@RunWith(BytecodeEnhancerRunner.class)
@TestForIssue(jiraKey = "HHH-16337")
public class ElementCollectionFlushAfterQueryTest extends BaseCoreFunctionalTestCase {
	private static final Long MY_ENTITY_ID = 1l;

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				MyEntity.class,
				MyOtherEntity.class
		};
	}

	@Test
	public void testAutoFlush() {
		inTransaction(
				session -> {
					MyEntity myEntity = new MyEntity( MY_ENTITY_ID, "my entity" );
					myEntity.addRedirectUris( "1" );
					session.persist( myEntity );
				}
		);

		inTransaction(
				session -> {
					MyEntity myEntity = session.find( MyEntity.class, MY_ENTITY_ID );

					Set<String> set = new HashSet<>();
					set.add( "3" );
					myEntity.setRedirectUris( set );

					session.createQuery( "from MyOtherEntity ", MyOtherEntity.class ).getResultList();
				}
		);

		inTransaction(
				session -> {
					MyEntity myEntity = session.find( MyEntity.class, MY_ENTITY_ID );
					Set<String> redirectUris = myEntity.getRedirectUris();
					assertThat( redirectUris.size() ).isEqualTo( 1 );
					assertThat( redirectUris.contains( "3" ) );
				}
		);

	}

	@Entity(name = "MyEntity")
	public static class MyEntity {
		@Id
		private Long id;

		private String name;

		@ElementCollection
		@Column(name = "val")
		@CollectionTable(name = "REDIRECT_URIS", joinColumns = { @JoinColumn(name = "ID") })
		protected Set<String> redirectUris = new HashSet<>();

		public MyEntity() {
		}

		public MyEntity(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return this.id;
		}

		public Set<String> getRedirectUris() {
			return redirectUris;
		}

		public void setRedirectUris(Set<String> redirectUris) {
			this.redirectUris = redirectUris;
		}

		public void addRedirectUris(String redirectUri) {
			this.redirectUris.add( redirectUri );
		}
	}

	@Entity(name = "MyOtherEntity")
	public class MyOtherEntity {

		@Id
		private Long id;

		private String name;

		public MyOtherEntity() {
		}

		public MyOtherEntity(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return this.id;
		}

		public void setId(Long id) {
			this.id = id;
		}
	}
}
