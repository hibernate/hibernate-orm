/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.procedure;

import java.sql.CallableStatement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.regex.Pattern;

import org.hibernate.Session;
import org.hibernate.dialect.SQLServerDialect;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;

import static org.hibernate.testing.transaction.TransactionUtil.doInAutoCommit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(value = SQLServerDialect.class, majorVersion = 11)
@Jpa(
		annotatedClasses = {
				Person.class,
				Phone.class,
		}
)
public class SQLServerStoredProcedureTest {

	@BeforeEach
	public void init(EntityManagerFactoryScope scope) {
		doInAutoCommit(
				"DROP PROCEDURE sp_count_phones",
				"DROP FUNCTION fn_count_phones",
				"DROP PROCEDURE sp_phones",
				"CREATE PROCEDURE sp_count_phones " +
						"   @personId INT, " +
						"   @phoneCount INT OUTPUT " +
						"AS " +
						"BEGIN " +
						"   SELECT @phoneCount = COUNT(*)  " +
						"   FROM Phone  " +
						"   WHERE person_id = @personId " +
						"END",
				"CREATE FUNCTION fn_count_phones (@personId INT)  " +
						"RETURNS INT  " +
						"AS  " +
						"BEGIN  " +
						"    DECLARE @phoneCount int;  " +
						"    SELECT @phoneCount = COUNT(*) " +
						"    FROM Phone   " +
						"    WHERE person_id = @personId;  " +
						"    RETURN(@phoneCount);  " +
						"END",
				"CREATE PROCEDURE sp_phones " +
						"    @personId INT, " +
						"    @phones CURSOR VARYING OUTPUT " +
						"AS " +
						"    SET NOCOUNT ON; " +
						"    SET @phones = CURSOR " +
						"    FORWARD_ONLY STATIC FOR " +
						"        SELECT *  " +
						"        FROM Phone   " +
						"        WHERE person_id = @personId;  " +
						"    OPEN @phones;"
		);

		scope.inTransaction( entityManager -> {
			Person person1 = new Person( 1L, "John Doe" );
			person1.setNickName( "JD" );
			person1.setAddress( "Earth" );
			person1.setCreatedOn( Timestamp.from( LocalDateTime.of( 2000, 1, 1, 0, 0, 0 )
														  .toInstant( ZoneOffset.UTC ) ) );

			entityManager.persist( person1 );

			Phone phone1 = new Phone( "123-456-7890" );
			phone1.setId( 1L );

			person1.addPhone( phone1 );

			Phone phone2 = new Phone( "098_765-4321" );
			phone2.setId( 2L );

			person1.addPhone( phone2 );
		} );
	}

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope){
		scope.releaseEntityManagerFactory();
	}

	@Test
	public void testStoredProcedureOutParameter(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			StoredProcedureQuery query = entityManager.createStoredProcedureQuery( "sp_count_phones" );
			query.registerStoredProcedureParameter( "personId", Long.class, ParameterMode.IN );
			query.registerStoredProcedureParameter( "phoneCount", Long.class, ParameterMode.OUT );

			query.setParameter( "personId", 1L );

			query.execute();
			Long phoneCount = (Long) query.getOutputParameterValue( "phoneCount" );
			assertEquals( Long.valueOf( 2 ), phoneCount );
		} );
	}

	@Test
	public void testStoredProcedureRefCursor(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			try {
				StoredProcedureQuery query = entityManager.createStoredProcedureQuery( "sp_phones" );
				query.registerStoredProcedureParameter( 1, Long.class, ParameterMode.IN );
				query.registerStoredProcedureParameter( 2, Class.class, ParameterMode.REF_CURSOR );
				query.setParameter( 1, 1L );

				query.execute();
				List<Object[]> postComments = query.getResultList();
				assertNotNull( postComments );
			}
			catch (Exception e) {
				assertTrue( Pattern.compile( "Dialect .*? not known to support REF_CURSOR parameters" )
									.matcher( e.getMessage() )
									.matches() );
			}
		} );
	}

	@Test
	public void testStoredProcedureReturnValue(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Session session = entityManager.unwrap( Session.class );
			session.doWork( connection -> {
				CallableStatement function = null;
				try {
					function = connection.prepareCall( "{ ? = call fn_count_phones(?) }" );
					function.registerOutParameter( 1, Types.INTEGER );
					function.setInt( 2, 1 );
					function.execute();
					int phoneCount = function.getInt( 1 );
					assertEquals( 2, phoneCount );
				}
				finally {
					if ( function != null ) {
						function.close();
					}
				}
			} );
		} );
	}
}
