package org.hibernate.orm.test.metamodel.mapping.collections;

import org.hibernate.query.spi.QueryImplementor;

import org.hibernate.testing.hamcrest.InitializationCheckMatcher;
import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.domain.gambit.EntityOfArrays;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Nathan Xu
 */
@SuppressWarnings("WeakerAccess")
@DomainModel( standardModels = StandardDomainModel.GAMBIT )
@ServiceRegistry
@SessionFactory
public class ArrayOperationTests {

	@BeforeEach
	public void createTestData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final EntityOfArrays entityContainingArrays = new EntityOfArrays( 1, "first" );

					entityContainingArrays.setArrayOfBasics( new String[] {
							"abc",
							"def",
							"ghi"
					} );
					session.save( entityContainingArrays );
				}
		);
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> session.createQuery( "delete from EntityOfArrays" ).executeUpdate()
		);
	}

	@Test
	public void arrayBaselineTest(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final QueryImplementor<EntityOfArrays> query = session.createQuery(
							"select e from EntityOfArrays e",
							EntityOfArrays.class
					);
					final EntityOfArrays result = query.uniqueResult();

					assertThat( result, notNullValue() );
					assertThat( result.getArrayOfBasics(), notNullValue() );
					assertThat( result.getArrayOfBasics(), InitializationCheckMatcher.isInitialized() );
					assertThat( result.getArrayOfBasics(), is( new String[] {
						"abc",
						"def",
						"ghi"
					} ));
				}
		);
	}

}
