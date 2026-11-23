package org.hibernate.orm.test.stateless;

import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

import org.hibernate.FetchNotFoundException;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

@DomainModel(annotatedClasses = {
		StatelessSessionOptionalOneToOneTest.Owner.class,
		StatelessSessionOptionalOneToOneTest.Detail.class,
		StatelessSessionOptionalOneToOneTest.MandatoryOwner.class,
		StatelessSessionOptionalOneToOneTest.IgnoreOwner.class,
		StatelessSessionOptionalOneToOneTest.ExceptionOwner.class
})
@SessionFactory
@JiraKey("HHH-21015")
public class StatelessSessionOptionalOneToOneTest {
	@BeforeEach
	void prepareData(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Detail detail = new Detail();
			detail.id = 1L;
			detail.info = "has detail";
			session.persist( detail );

			Owner withDetail = new Owner();
			withDetail.id = 1L;
			withDetail.detail = detail;
			session.persist( withDetail );

			Owner withoutDetail = new Owner();
			withoutDetail.id = 2L;
			session.persist( withoutDetail );

			session.flush();

			for ( String table : new String[] { "mandatory_owner", "ignore_owner", "exception_owner" } ) {
				for ( long id = 1; id <= 2; id++ ) {
					session.createNativeMutationQuery( "insert into " + table + " (id, detail_id) values (:id, :id)" )
							.setParameter( "id", id )
							.executeUpdate();
				}
			}
		} );
	}

	@AfterEach
	void cleanUp(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	void regularSessionMissingDetail(SessionFactoryScope scope) {
		scope.inSession( session -> {
			Owner owner = session.createSelectionQuery( "from OptionalOwner where id = 2", Owner.class )
					.getSingleResult();
			assertNull( owner.detail );
		} );
	}

	@Test
	void statelessSessionExistingDetail(SessionFactoryScope scope) {
		scope.inStatelessSession( session -> {
			Owner owner = session.createSelectionQuery( "from OptionalOwner where id = 1", Owner.class )
					.getSingleResult();
			assertNotNull( owner.detail );
			assertEquals( 1L, owner.detail.id );
			assertEquals( "has detail", owner.detail.info );
		} );
	}

	@Test
	void statelessSessionMissingDetail(SessionFactoryScope scope) {
		scope.inStatelessSession( session -> {
			Owner owner = session.createSelectionQuery( "from OptionalOwner where id = 2", Owner.class )
					.getSingleResult();
			assertNull( owner.detail );
		} );
	}


	@Test
	void mandatoryMissingDetail(SessionFactoryScope scope) {
		scope.inStatelessSession( session -> assertThrowsExactly( EntityNotFoundException.class,
				() -> session.createSelectionQuery( "from MandatoryOwner where id = 2", MandatoryOwner.class )
						.getSingleResult() ) );
	}

	@Test
	void ignoreMissingDetail(SessionFactoryScope scope) {
		scope.inStatelessSession( session -> {
			IgnoreOwner owner = session.createSelectionQuery( "from IgnoreOwner where id = 2", IgnoreOwner.class )
					.getSingleResult();
			assertNull( owner.detail );
		} );
	}

	@Test
	void exceptionForMissingDetail(SessionFactoryScope scope) {
		scope.inStatelessSession( session -> {
			FetchNotFoundException exception = assertThrowsExactly( FetchNotFoundException.class,
					() -> session.createSelectionQuery( "from ExceptionOwner where id = 2", ExceptionOwner.class )
							.getSingleResult() );
			assertEquals( Detail.class.getName(), exception.getEntityName() );
			assertEquals( 2L, exception.getIdentifier() );
		} );
	}

	@Test
	void existingDetailsWithMissingRowPolicies(SessionFactoryScope scope) {
		scope.inStatelessSession( session -> {
			MandatoryOwner mandatory = session.createSelectionQuery( "from MandatoryOwner where id = 1", MandatoryOwner.class )
					.getSingleResult();
			assertEquals( "has detail", mandatory.detail.info );
			IgnoreOwner ignore = session.createSelectionQuery( "from IgnoreOwner where id = 1", IgnoreOwner.class )
					.getSingleResult();
			assertEquals( "has detail", ignore.detail.info );
			ExceptionOwner exception = session.createSelectionQuery( "from ExceptionOwner where id = 1", ExceptionOwner.class )
					.getSingleResult();
			assertEquals( "has detail", exception.detail.info );
		} );
	}

	@Test
	void findMissingRoot(SessionFactoryScope scope) {
		scope.inStatelessSession( session -> {
			assertNull( session.find( Detail.class, 2L ) );
			assertNull( session.find( Detail.class.getName(), 2L ) );
		} );
	}

	@Test
	void getMissingRoot(SessionFactoryScope scope) {
		scope.inStatelessSession( session -> {
			assertThrowsExactly( EntityNotFoundException.class, () -> session.get( Detail.class, 2L ) );
			assertThrowsExactly( EntityNotFoundException.class, () -> session.get( Detail.class.getName(), 2L ) );
		} );
	}

	@Entity(name = "OptionalOwner")
	@Table(name = "optional_owner")
	static class Owner {
		@Id
		Long id;

		@OneToOne
		@PrimaryKeyJoinColumn(foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
		Detail detail;
	}

	@Entity(name = "OptionalDetail")
	@Table(name = "optional_detail")
	static class Detail {
		@Id
		Long id;

		String info;
	}

	@Entity(name = "MandatoryOwner")
	@Table(name = "mandatory_owner")
	static class MandatoryOwner {
		@Id
		Long id;

		@ManyToOne(optional = false)
		@JoinColumn(name = "detail_id", nullable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
		Detail detail;
	}

	@Entity(name = "IgnoreOwner")
	@Table(name = "ignore_owner")
	static class IgnoreOwner {
		@Id
		Long id;

		@ManyToOne
		@JoinColumn(name = "detail_id")
		@NotFound(action = NotFoundAction.IGNORE)
		Detail detail;
	}

	@Entity(name = "ExceptionOwner")
	@Table(name = "exception_owner")
	static class ExceptionOwner {
		@Id
		Long id;

		@ManyToOne
		@JoinColumn(name = "detail_id")
		@NotFound(action = NotFoundAction.EXCEPTION)
		Detail detail;
	}
}
