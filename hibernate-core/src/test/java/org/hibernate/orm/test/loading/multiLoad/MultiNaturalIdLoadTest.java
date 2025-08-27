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
import org.hibernate.dialect.Dialect;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.loader.ast.internal.MultiKeyLoadHelper;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
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
		annotatedClasses = {
				MultiNaturalIdLoadTest.SimpleNaturalIdEntity.class,
				MultiNaturalIdLoadTest.SimpleMutableNaturalIdEntity.class,
				MultiNaturalIdLoadTest.CompositeNaturalIdEntity.class
		}
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
						session.persist( new SimpleMutableNaturalIdEntity( i, "MIdEntity" + i ) );
					}
					for ( int i = 1; i <= 10; i++ ) {
						session.persist( new CompositeNaturalIdEntity( i, "Entity" + i, i + "Entity" ) );
					}
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
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
	public void testCompoundNaturalIdUnorderedMultiLoad(SessionFactoryScope scope) {
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
		verify( scope.getSessionFactory().getJdbcServices().getDialect(), statementInspector );
	}

	@Test
	public void testCompoundNaturalIdOrderedMultiLoad(SessionFactoryScope scope) {
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
		verify( scope.getSessionFactory().getJdbcServices().getDialect(), statementInspector );
	}

	private void verify( Dialect dialect, SQLStatementInspector statementInspector ) {
		if ( dialect.supportsRowValueConstructorSyntaxInInList() ) {
			Matcher m = p.matcher( statementInspector.getSqlQueries().get( 0 ) );
			int paramCount = 0;
			while ( m.find() ) {
				paramCount++;
			}
			assertEquals( 3, paramCount );
		}
		else {
			// DB2Dialect.class, HSQLDialect.class, SQLServerDialect.class, SybaseDialect.class, SpannerDialect.class
			final int paramCount = StringHelper.countUnquoted(
					statementInspector.getSqlQueries().get( 0 ),
					'?'
			);
			assertEquals( 6, paramCount );
		}
	}

	@Test
	public void testNonExistentIdRequest(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					// ordered multiLoad
					List<SimpleNaturalIdEntity> list = session.byMultipleNaturalId( SimpleNaturalIdEntity.class ).multiLoad( "Entity4","Entity99","Entity5" );
					assertEquals( 3, list.size() );
					assertNull( list.get( 1 ) );

					// unordered multiLoad
					list = session.byMultipleNaturalId( SimpleNaturalIdEntity.class ).enableOrderedReturn( false ).multiLoad( "Entity4","Entity99","Entity5" );
					assertEquals( 2, list.size() );
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
					assertEquals( 3, results.size() );
				}
		);
	}

	@Test
	public void testUnflushedDeleteAndThenOrderedMultiLoadSimpleNonmutableIdsWithReturnDeletedEnabled(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					// delete one of them (but do not flush)...
					SimpleNaturalIdEntity se = session.find( SimpleNaturalIdEntity.class, 2 );
					Hibernate.initialize( se );
					session.remove( se );

					// with enableReturnOfDeletedEntities set to true, multiLoad should return 3 entities (no nulls)
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
	public void testUnflushedDeleteAndThenOrderedMultiLoadSimpleNonmutableIdsWithReturnDeletedDisabled(SessionFactoryScope scope) {
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
							.enableReturnOfDeletedEntities( false )
							.multiLoad( ids );
					assertEquals( 3, results.size() );
					assertEquals(ids.get(0), results.get(0).getSsn() );
					assertNull(results.get(1));
					assertEquals(ids.get(2), results.get(2).getSsn() );
				}
		);
	}

	@Test
	public void testOrderedMultiLoadSimpleIds1(SessionFactoryScope scope) {
		// mutate one of the natural ids (but do not flush)
		scope.inTransaction(
				session -> {
					SimpleMutableNaturalIdEntity sme = session.find( SimpleMutableNaturalIdEntity.class, 2 );
					sme.setSsn( "MIdEntity22" );

					List<String> ids = List.of( "MIdEntity4", "MIdEntity22", "MIdEntity5" );
					List<SimpleMutableNaturalIdEntity> results = session
							.byMultipleNaturalId( SimpleMutableNaturalIdEntity.class )
							.multiLoad( ids );
					assertEquals( 3, results.size() );
					assertEquals( ids.get( 0 ), results.get( 0 ).getSsn() );
					assertEquals( ids.get( 1 ), results.get( 1 ).getSsn() );
					assertEquals( ids.get( 2 ), results.get( 2 ).getSsn() );
				}
		);
	}

	@Test
	public void testOrderedMultiLoadSimpleIds2(SessionFactoryScope scope) {
		// mutate and delete one of the natural ids (but do not flush)
		// don't return deleted instances
		scope.inTransaction(
				session -> {
					SimpleMutableNaturalIdEntity sme = session.find( SimpleMutableNaturalIdEntity.class, 2 );
					sme.setSsn( "MIdEntity22" );
					session.remove( sme );

					List<String> ids = List.of( "MIdEntity4", "MIdEntity22", "MIdEntity5" );
					List<SimpleMutableNaturalIdEntity> results = session
							.byMultipleNaturalId( SimpleMutableNaturalIdEntity.class )
							.multiLoad( ids );
					assertEquals( 3, results.size() );
					assertEquals( ids.get( 0 ), results.get( 0 ).getSsn() );
					assertNull( results.get( 1 ) );
					assertEquals( ids.get( 2 ), results.get( 2 ).getSsn() );
				}
		);
	}

	@Test
	public void testOrderedMultiLoadSimpleIds3(SessionFactoryScope scope) {
		// mutate and delete one of the natural ids (but do not flush)
		// return deleted instances
		scope.inTransaction(
				session -> {
					SimpleMutableNaturalIdEntity sme = session.find( SimpleMutableNaturalIdEntity.class, 2 );
					sme.setSsn( "MIdEntity22" );
					session.remove( sme );

					List<String> ids = List.of( "MIdEntity4", "MIdEntity22", "MIdEntity5" );

					List<SimpleMutableNaturalIdEntity> results = session
							.byMultipleNaturalId( SimpleMutableNaturalIdEntity.class )
							.enableReturnOfDeletedEntities( true )
							.multiLoad( ids );
					// the deleted instance is still not returned, since the natural id synchronisation only happens
					// on managed entities, so in this case the PC simply isn't aware of the mutated id instance
					assertEquals( 3, results.size() );
					assertEquals( ids.get( 0 ), results.get( 0 ).getSsn() );
					assertNull( results.get( 1 ) );
					assertEquals( ids.get( 2 ), results.get( 2 ).getSsn() );
				}
		);
	}

	@Test
	public void testOrderedMultiLoadSimpleIds4(SessionFactoryScope scope) {
		// mutate and delete another one of the natural ids (but do not flush)...
		// don't return deleted instances
		scope.inTransaction(
				session -> {
					// mutate one of the natural ids and delete another instance (but do not flush)...
					SimpleMutableNaturalIdEntity sme = session.find( SimpleMutableNaturalIdEntity.class, 2 );
					sme.setSsn( "MIdEntity22" );
					session.remove( session.find( SimpleMutableNaturalIdEntity.class, 5 ) );

					List<String> ids = List.of( "MIdEntity4", "MIdEntity22", "MIdEntity5" );

					List<SimpleMutableNaturalIdEntity> results = session
							.byMultipleNaturalId( SimpleMutableNaturalIdEntity.class )
							.multiLoad( ids );
					assertEquals( 3, results.size() );
					assertEquals( ids.get( 0 ), results.get( 0 ).getSsn() );
					assertEquals( ids.get( 1 ), results.get( 1 ).getSsn() );
					assertNull( results.get( 2 ) );
				}
		);
	}

	@Test
	public void testOrderedMultiLoadSimpleIds5(SessionFactoryScope scope) {
		// mutate and delete another one of the natural ids (but do not flush)...
		// return deleted instances
		scope.inTransaction(
				session -> {
					// mutate one of the natural ids and delete another instance (but do not flush)...
					SimpleMutableNaturalIdEntity sme = session.find( SimpleMutableNaturalIdEntity.class, 2 );
					sme.setSsn( "MIdEntity22" );
					session.remove( session.find( SimpleMutableNaturalIdEntity.class, 5 ) );

					List<String> ids = List.of( "MIdEntity4", "MIdEntity22", "MIdEntity5" );

					List<SimpleMutableNaturalIdEntity> results = session
							.byMultipleNaturalId( SimpleMutableNaturalIdEntity.class )
							.enableReturnOfDeletedEntities( true )
							.multiLoad( ids );
					assertEquals( 3, results.size() );
					assertEquals( ids.get( 0 ), results.get( 0 ).getSsn() );
					assertEquals( ids.get( 1 ), results.get( 1 ).getSsn() );
					assertEquals( ids.get( 2 ), results.get( 2 ).getSsn() );
				}
		);
	}

	@Test
	public void testUnorderedMultiLoadSimpleIds1(SessionFactoryScope scope) {
		// mutate one of the natural ids (but do not flush)
		scope.inTransaction(
				session -> {
					SimpleMutableNaturalIdEntity sme = session.find( SimpleMutableNaturalIdEntity.class, 2 );
					sme.setSsn( "MIdEntity22" );

					List<String> ids = List.of( "MIdEntity4", "MIdEntity22", "MIdEntity5" );
					List<SimpleMutableNaturalIdEntity> results = session
							.byMultipleNaturalId( SimpleMutableNaturalIdEntity.class )
							.enableOrderedReturn( false )
							.multiLoad( ids );
					assertEquals( 3, results.size() );
					verifyUnorderedResult( ids, results );
				}
		);
	}

	@Test
	public void testUnorderedMultiLoadSimpleIds2(SessionFactoryScope scope) {
		// mutate and delete one of the natural ids (but do not flush)
		// don't return deleted instances
		scope.inTransaction(
				session -> {
					SimpleMutableNaturalIdEntity sme = session.find( SimpleMutableNaturalIdEntity.class, 2 );
					sme.setSsn( "MIdEntity22" );
					session.remove( sme );

					List<String> ids = List.of( "MIdEntity4", "MIdEntity22", "MIdEntity5" );
					List<SimpleMutableNaturalIdEntity> results = session
							.byMultipleNaturalId( SimpleMutableNaturalIdEntity.class )
							.enableOrderedReturn( false )
							.multiLoad( ids );
					assertEquals( 2, results.size() );
					verifyUnorderedResult( List.of( "MIdEntity4", "MIdEntity5" ), results );
				}
		);
	}

	@Test
	public void testUnorderedMultiLoadSimpleIds3(SessionFactoryScope scope) {
		// mutate and delete one of the natural ids (but do not flush)
		// return deleted instances
		scope.inTransaction(
				session -> {
					SimpleMutableNaturalIdEntity sme = session.find( SimpleMutableNaturalIdEntity.class, 2 );
					sme.setSsn( "MIdEntity22" );
					session.remove( sme );

					List<String> ids = List.of( "MIdEntity4", "MIdEntity22", "MIdEntity5" );

					List<SimpleMutableNaturalIdEntity> results = session
							.byMultipleNaturalId( SimpleMutableNaturalIdEntity.class )
							.enableOrderedReturn( false )
							.enableReturnOfDeletedEntities( true )
							.multiLoad( ids );
					// the deleted instance is still not returned, since the natural id synchronisation only happens
					// on managed entities, so in this case the PC simply isn't aware of the mutated id instance
					assertEquals( 2, results.size() );
					verifyUnorderedResult( List.of( "MIdEntity4", "MIdEntity5" ), results );
				}
		);
	}

	@Test
	public void testUnorderedMultiLoadSimpleIds4(SessionFactoryScope scope) {
		// mutate and delete another one of the natural ids (but do not flush)...
		// don't return deleted instances
		scope.inTransaction(
				session -> {
					// mutate one of the natural ids and delete another instance (but do not flush)...
					SimpleMutableNaturalIdEntity sme = session.find( SimpleMutableNaturalIdEntity.class, 2 );
					sme.setSsn( "MIdEntity22" );
					session.remove( session.find( SimpleMutableNaturalIdEntity.class, 5 ) );

					List<String> ids = List.of( "MIdEntity4", "MIdEntity22", "MIdEntity5" );

					List<SimpleMutableNaturalIdEntity> results = session
							.byMultipleNaturalId( SimpleMutableNaturalIdEntity.class )
							.enableOrderedReturn( false )
							.multiLoad( ids );
					assertEquals( 2, results.size() );
					verifyUnorderedResult( List.of( "MIdEntity4", "MIdEntity22" ), results );
				}
		);
	}

	@Test
	public void testUnorderedMultiLoadSimpleIds5(SessionFactoryScope scope) {
		// mutate and delete another one of the natural ids (but do not flush)...
		// return deleted instances
		scope.inTransaction(
				session -> {
					// mutate one of the natural ids and delete another instance (but do not flush)...
					SimpleMutableNaturalIdEntity sme = session.find( SimpleMutableNaturalIdEntity.class, 2 );
					sme.setSsn( "MIdEntity22" );
					session.remove( session.find( SimpleMutableNaturalIdEntity.class, 5 ) );

					List<String> ids = List.of( "MIdEntity4", "MIdEntity22", "MIdEntity5" );

					List<SimpleMutableNaturalIdEntity> results = session
							.byMultipleNaturalId( SimpleMutableNaturalIdEntity.class )
							.enableOrderedReturn( false )
							.enableReturnOfDeletedEntities( true )
							.multiLoad( ids );
					assertEquals( 3, results.size() );
					verifyUnorderedResult( ids, results );
				}
		);
	}

	private void verifyUnorderedResult(List<String> ids, List<SimpleMutableNaturalIdEntity> results) {
		int count = 0;
		Iterator<SimpleMutableNaturalIdEntity> it = results.iterator();
		do {
			SimpleMutableNaturalIdEntity sme = it.next();
			for ( String id : ids ) {
				if ( id.equalsIgnoreCase( sme.getSsn() ) ) {
					it.remove();
					count++;
				}
			}
		} while ( it.hasNext() );
		assertEquals( 0, results.size() );
		assertEquals( ids.size(), count );
	}

//	private List<SimpleMutableNaturalIdEntity> removeNulls(List<SimpleMutableNaturalIdEntity> l) {
//		return l.stream().filter( simpleMutableNaturalIdEntity -> simpleMutableNaturalIdEntity != null ).toList();
//	}

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

	@Cacheable
	@Entity(name = "SimpleMutableNaturalIdEntity")
	public static class SimpleMutableNaturalIdEntity {
		@Id
		Integer id;
		@NaturalId(mutable = true)
		String ssn;

		public SimpleMutableNaturalIdEntity() {
		}

		public SimpleMutableNaturalIdEntity(Integer id, String ssn) {
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
