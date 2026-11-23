package org.hibernate.orm.test.mapping.joinformula;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(
		annotatedClasses = {
				ParentEntity.class,
				ChildEntity.class
		}
)
@SessionFactory
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.SHOW_SQL, value = "true"),
				@Setting(name = AvailableSettings.FORMAT_SQL, value = "true")
		}
)
public class JoinFormulaTest {


	@Test
	public void hhh13722Test(SessionFactoryScope scope) {
		scope.inSession(
				s -> {
					//Nothing to do: the test just needs to verify that
					//this can boot.
				}
		);
	}

	/**
	 * Runtime coverage for a {@code @OneToOne} whose join mixes a read-only physical
	 * {@code @JoinColumn} (the parent's own id, shared with the child's {@code PARENT_ID})
	 * with a {@code @JoinFormula} literal discriminating which child row to load.
	 * This tuple references the child's parent-id/name unique key instead of its
	 * primary key, so it keeps its existing logical many-to-one binding and behaves like
	 * an ordinary required one-to-one.
	 */
	@Test
	public void testLoadThroughMixedColumnAndFormula(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final ChildEntity tom = new ChildEntity();
			tom.setId( 1L );
			tom.setParentId( 10L );
			tom.setName( "Tom" );
			session.persist( tom );

			final ChildEntity ben = new ChildEntity();
			ben.setId( 2L );
			ben.setParentId( 10L );
			ben.setName( "Ben" );
			session.persist( ben );

			final ParentEntity parent = new ParentEntity();
			parent.setId( 10L );
			session.persist( parent );
		} );

		scope.inTransaction( session -> {
			final ParentEntity parent = session.find( ParentEntity.class, 10L );
			assertThat( parent.getTom().getId() ).isEqualTo( 1L );
			assertThat( parent.getTom().getName() ).isEqualTo( "Tom" );
			assertThat( parent.getBen().getId() ).isEqualTo( 2L );
			assertThat( parent.getBen().getName() ).isEqualTo( "Ben" );
		} );

		scope.inTransaction( session -> {
			final ParentEntity parent = session.createQuery(
					"from ParentEntity p join fetch p.tom join fetch p.ben where p.id = :id",
					ParentEntity.class
			).setParameter( "id", 10L ).getSingleResult();
			assertThat( parent.getTom().getName() ).isEqualTo( "Tom" );
			assertThat( parent.getBen().getName() ).isEqualTo( "Ben" );
		} );
	}

}
