/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.loading.multiLoad;


import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.CacheMode;
import org.hibernate.Hibernate;
import org.hibernate.annotations.NaturalId;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SpannerDialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.loader.ast.internal.MultiKeyLoadHelper;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.RequiresDialects;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.hibernate.testing.orm.junit.SkipForDialectGroup;
import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Jan Schatteman
 */
@DomainModel(
		annotatedClasses = { MultiNaturalIdLoadTest.SimpleNaturalIdEntity.class, MultiNaturalIdLoadTest.CompositeNaturalIdEntity.class }
)
@SessionFactory( useCollectingStatementInspector = true )
class MultiNaturalIdLoadTest {

	private final Pattern p = Pattern.compile( "\\(\\?,\\?\\)" );

	@BeforeEach
	public void setup(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.setCacheMode( CacheMode.IGNORE );
					for ( int i = 1; i <= 10; i++ ) {
						session.persist( new SimpleNaturalIdEntity( i, "Entity" + i ) );
					}
					for ( int i = 1; i <= 10; i++ ) {
						session.persist( new CompositeNaturalIdEntity( i, "Entity" + i, i + "Entity" ) );
					}
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createMutationQuery( "delete SimpleNaturalIdEntity" ).executeUpdate();
					session.createMutationQuery( "delete CompositeNaturalIdEntity" ).executeUpdate();
				}
		);
	}

	@Test
	public void testBasicUnorderedMultiLoad(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		scope.inTransaction(
				session -> {
					statementInspector.getSqlQueries().clear();

					List<SimpleNaturalIdEntity> results = session
							.byMultipleNaturalId( SimpleNaturalIdEntity.class )
							.enableOrderedReturn( false )
							.multiLoad( "Entity1","Entity2","Entity3","Entity4","Entity5" );
					assertEquals( 5, results.size() );

					Iterator<SimpleNaturalIdEntity> it = results.iterator();
					for ( int i = 1; i <= 5; i++ ) {
						SimpleNaturalIdEntity se= it.next();
						if ( i == se.getId() ) {
							it.remove();
						}
					}
					assertEquals( 0, results.size() );

					final int paramCount = StringHelper.countUnquoted(
							statementInspector.getSqlQueries().get( 0 ),
							'?'
					);

					final Dialect dialect = session.getSessionFactory()
							.getJdbcServices()
							.getDialect();
					if ( MultiKeyLoadHelper.supportsSqlArrayType( dialect ) ) {
						assertEquals(1, paramCount );
					}
					else {
						assertEquals(5, paramCount );
					}
				}
		);
	}

	@Test
	public void testBasicOrderedMultiLoad(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		scope.inTransaction(
				session -> {
					statementInspector.getSqlQueries().clear();

					List<String> ids = List.of("Entity4","Entity2","Entity5","Entity1","Entity3");
					List<SimpleNaturalIdEntity> results = session
							.byMultipleNaturalId( SimpleNaturalIdEntity.class )
							.enableOrderedReturn( true )
							.multiLoad( ids );

					assertEquals( 5, results.size() );
					for ( int i = 0; i < 5; i++ ) {
						assertEquals(ids.get(i), results.get(i).getSsn() );
					}

					final int paramCount = StringHelper.countUnquoted(
							statementInspector.getSqlQueries().get( 0 ),
							'?'
					);

					final Dialect dialect = session.getSessionFactory()
							.getJdbcServices()
							.getDialect();
					if ( MultiKeyLoadHelper.supportsSqlArrayType( dialect ) ) {
						assertEquals(1, paramCount );
					}
					else {
						assertEquals(5, paramCount );
					}
				}
		);
	}

	@Test
	/*
	 * The skipped dialects below produce queries that don't support row value syntax in 'IN' lists (the '(?,?)' pattern which is what the regex pattern 'p' above
	 * detects and checks).
	 */
	@SkipForDialectGroup(
			value = {
					@SkipForDialect( dialectClass = DB2Dialect.class, matchSubTypes = true ),
					@SkipForDialect( dialectClass = HSQLDialect.class, matchSubTypes = true ),
					@SkipForDialect( dialectClass = SQLServerDialect.class, matchSubTypes = true ),
					@SkipForDialect( dialectClass = SybaseDialect.class, matchSubTypes = true ),
					@SkipForDialect( dialectClass = SpannerDialect.class, matchSubTypes = true )
			}
	)
	public void testAllExceptDb2HsqldbMssqlSybase(SessionFactoryScope scope) {
		doVerify1( testCompoundNaturalIdUnorderedMultiLoad(scope) );
		doVerify1( testCompoundNaturalIdOrderedMultiLoad(scope) );
	}

	@Test
	/*
	 * The dialects below produce queries that don't support row value syntax in 'IN' lists (same test as above, but with a different result expectation).
	 */
	@RequiresDialects(
			value = {
					@RequiresDialect( value = DB2Dialect.class ),
					@RequiresDialect( value = HSQLDialect.class ),
					@RequiresDialect( value = SQLServerDialect.class ),
					@RequiresDialect( value = SybaseDialect.class ),
					@RequiresDialect( value = SpannerDialect.class )
			}
	)
	public void testDb2HsqldbMssqlSybase(SessionFactoryScope scope) {
		doVerify2( testCompoundNaturalIdUnorderedMultiLoad(scope) );
		doVerify2( testCompoundNaturalIdOrderedMultiLoad(scope) );
	}

	private void doVerify1(SQLStatementInspector statementInspector) {
		Matcher m = p.matcher( statementInspector.getSqlQueries().get( 0 ) );
		int paramCount = 0;
		while ( m.find() ) {
			paramCount++;
		}
		assertEquals( 3, paramCount );
	}

	private void doVerify2(SQLStatementInspector statementInspector) {
		final int paramCount = StringHelper.countUnquoted(
				statementInspector.getSqlQueries().get( 0 ),
				'?'
		);
		assertEquals( 6, paramCount );
	}


	private SQLStatementInspector testCompoundNaturalIdUnorderedMultiLoad(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		scope.inTransaction(
				session -> {
					statementInspector.getSqlQueries().clear();

					List<String[]> ids = List.of( new String[]{"Entity1", "1Entity"}, new String[]{"Entity2", "2Entity"}, new String[]{"Entity3", "3Entity"} );
					List<CompositeNaturalIdEntity> results = session
							.byMultipleNaturalId( CompositeNaturalIdEntity.class )
							.enableOrderedReturn( true )
							.multiLoad( ids );

					Iterator<CompositeNaturalIdEntity> it = results.iterator();
					for ( int i = 1; i <= 3; i++ ) {
						CompositeNaturalIdEntity se= it.next();
						if ( i == se.getId() ) {
							it.remove();
						}
					}
					assertEquals( 0, results.size() );
				}
		);
		return statementInspector;
	}

	private SQLStatementInspector testCompoundNaturalIdOrderedMultiLoad(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		scope.inTransaction(
				session -> {
					statementInspector.getSqlQueries().clear();

					List<String[]> ids = List.of( new String[]{"Entity4", "4Entity"}, new String[]{"Entity2", "2Entity"}, new String[]{"Entity5", "5Entity"} );
					List<CompositeNaturalIdEntity> results = session
							.byMultipleNaturalId( CompositeNaturalIdEntity.class )
							.enableOrderedReturn( true )
							.multiLoad( ids );

					assertEquals( 3, results.size() );
					for ( int i = 0; i < 3; i++ ) {
						assertEquals(ids.get(i)[0], results.get(i).getSsn() );
						assertEquals(ids.get(i)[1], results.get(i).getSsn2() );
					}
				}
		);
		return statementInspector;
	}

	@Test
	public void testNonExistentIdRequest(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					// ordered multiLoad
					List<SimpleNaturalIdEntity> list = session.byMultipleNaturalId( SimpleNaturalIdEntity.class ).multiLoad( "Entity4","Entity99","Entity5" );
					assertEquals( 3, list.size() );
					assertNull( list.get( 1 ) );

					// un-ordered multiLoad
					list = session.byMultipleNaturalId( SimpleNaturalIdEntity.class ).enableOrderedReturn( false ).multiLoad( "Entity4","Entity99","Entity5" );
					assertEquals( 2, list.size() );
				}
		);
	}

	@Test
	public void testUnflushedDeleteAndThenOrderedMultiLoadSimpleIds(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					// delete one of them (but do not flush)...
					SimpleNaturalIdEntity se = session.getReference( SimpleNaturalIdEntity.class, 2 );
					Hibernate.initialize( se );
					session.remove( se );

					// finally assert how multiLoad handles it
					List<String> ids = List.of("Entity4","Entity2","Entity5");
					List<SimpleNaturalIdEntity> results = session
							.byMultipleNaturalId( SimpleNaturalIdEntity.class )
							.multiLoad( ids );
					assertEquals( 3, results.size() );
					assertEquals(ids.get(0), results.get(0).getSsn() );
					Assertions.assertNull( results.get(1) );
					assertEquals(ids.get(2), results.get(2).getSsn() );
				}
		);
	}

	@Test
	@FailureExpected(jiraKey = "HHH-19248", reason = "should return 3 entities similarly to the multiloadtest with return delete enabled, but returns only 2")
	public void testUnflushedDeleteAndThenOrderedMultiLoadSimpleIdsWithReturnDeletedEnabled(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					// delete one of them (but do not flush)...
					SimpleNaturalIdEntity se = session.getReference( SimpleNaturalIdEntity.class, 2 );
					Hibernate.initialize( se );
					session.remove( se );

					// finally assert how multiLoad handles it
					List<String> ids = List.of("Entity4","Entity2","Entity5");
					List<SimpleNaturalIdEntity> results = session
							.byMultipleNaturalId( SimpleNaturalIdEntity.class )
							.enableReturnOfDeletedEntities( true )
							.multiLoad( ids );
					assertEquals( 3, results.size() );
					assertEquals(ids.get(0), results.get(0).getSsn() );
					assertEquals(ids.get(1), results.get(1).getSsn() );
					assertEquals(ids.get(2), results.get(2).getSsn() );
				}
		);
	}

	@Test
	@FailureExpected(jiraKey = "HHH-19248", reason = "should return 3 entities similarly to the multiloadtest with return delete enabled, but returns only 2")
	public void testUnflushedDeleteAndThenUnorderedMultiLoadSimpleIds(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					// delete one of them (but do not flush)...
					SimpleNaturalIdEntity se = session.getReference( SimpleNaturalIdEntity.class, 2 );
					Hibernate.initialize( se );
					session.remove( se );

					// finally assert how multiLoad handles it
					List<String> ids = List.of("Entity4","Entity2","Entity5");
					List<SimpleNaturalIdEntity> results = session
							.byMultipleNaturalId( SimpleNaturalIdEntity.class )
							.enableOrderedReturn( false )
							.multiLoad( ids );
					assertEquals( 3, results.size() );
				}
		);
	}

	@Test
	public void testOrderedDuplicatedRequestedSimpleIds(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					// ordered multiLoad
					List<String> ids = List.of("Entity4","Entity2","Entity4","Entity5","Entity4");
					List<SimpleNaturalIdEntity> results = session.byMultipleNaturalId( SimpleNaturalIdEntity.class ).multiLoad( ids );
					assertEquals( 5, results.size() );
					assertEquals( results.get( 0 ), results.get( 2 ) );
					assertEquals( results.get( 0 ), results.get( 4 ) );
				}
		);
	}

	@Test
	public void testUnorderedDuplicatedRequestedSimpleIds(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					// un-ordered multiLoad
					List<String> ids = List.of("Entity4","Entity2","Entity4","Entity5","Entity4");
					List<SimpleNaturalIdEntity> results = session
							.byMultipleNaturalId( SimpleNaturalIdEntity.class )
							.enableOrderedReturn( false )
							.multiLoad( ids );
					Assert.assertEquals( 3, results.size() );
				}
		);
	}

	@Cacheable
	@Entity(name = "SimpleNaturalIdEntity")
	public static class SimpleNaturalIdEntity {
		@Id
		Integer id;
		@NaturalId
		String ssn;

		public SimpleNaturalIdEntity() {
		}

		public SimpleNaturalIdEntity(Integer id, String ssn) {
			this.id = id;
			this.ssn = ssn;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getSsn() {
			return ssn;
		}

		public void setSsn(String ssn) {
			this.ssn = ssn;
		}
	}

	@Entity(name = "CompositeNaturalIdEntity")
	public static class CompositeNaturalIdEntity {
		@Id
		Integer id;
		@NaturalId
		String ssn;
		@NaturalId
		String ssn2;

		public CompositeNaturalIdEntity() {
		}

		public CompositeNaturalIdEntity(Integer id, String ssn, String ssn2) {
			this.id = id;
			this.ssn = ssn;
			this.ssn2 = ssn2;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getSsn() {
			return ssn;
		}

		public void setSsn(String ssn) {
			this.ssn = ssn;
		}

		public String getSsn2() { return ssn2; }

		public void setSsn2(String ssn2) { this.ssn2 = ssn2; }
	}

}
