package org.hibernate.persister.collection;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.MapKeyJoinColumn;
import org.hibernate.Hibernate;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.Statement;
import org.hibernate.sql.ast.spi.translation.SqlAstTranslator;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.hibernate.cfg.JdbcSettings.DIALECT;
import static org.junit.jupiter.api.Assertions.*;

@DomainModel(annotatedClasses = { CollectionScalarReadTest.Owner.class, CollectionScalarReadTest.Tag.class })
@SessionFactory(useCollectingStatementObserver = true)
@JiraKey("HHH-21216")
class CollectionScalarReadTest {
	@Test
	void transientMapKey(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new Owner( 1 ) ) );
		scope.inSession( session -> {
			final var tags = session.find( Owner.class, 1 ).byTag;
			assertNull( Hibernate.get( tags, new Tag() ) );
			assertFalse( Hibernate.isInitialized( tags ) );
		} );
	}

	@Test
	@RequiresDialect(H2Dialect.class)
	@SessionFactory(useCollectingStatementObserver = true)
	@ServiceRegistry(settings = @Setting(name = DIALECT,
			value = "org.hibernate.persister.collection.CollectionScalarReadTest$CountingDialect"))
	void batchSizeDoesNotRebuildSingleKeyQueries(SessionFactoryScope scope) {
		final var tagId = scope.fromTransaction( session -> {
			var tag = new Tag();
			session.persist( tag );
			var owner = new Owner( 2 );
			owner.tags.add( tag );
			owner.byTag.put( tag, tag );
			session.persist( owner );
			return tag.id;
		} );
		final var dialect = (CountingDialect) scope.getSessionFactory().getJdbcServices().getDialect();
		final var observer = scope.getCollectingStatementObserver();
		for ( int round = 0; round < 2; round++ ) {
			scope.inSession( session -> {
				session.setFetchBatchSize( 24 );
				var owner = session.find( Owner.class, 2 );
				var tag = session.find( Tag.class, tagId );
				int translations = dialect.translations;
				assertEquals( 1, Hibernate.size( owner.tags ) );
				observer.clear();
				assertTrue( Hibernate.contains( owner.tags, tag ) );
				observer.assertQuery( 0 ).doesNotContain( " join " );
				assertSame( tag, Hibernate.get( owner.byTag, tag ) );
				assertEquals( translations, dialect.translations );
			} );
		}
	}

	public static class CountingDialect extends H2Dialect {
		private int translations;

		@Override
		public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
			final var delegate = super.getSqlAstTranslatorFactory();
			return new SqlAstTranslatorFactory() {
				@Override
				public <S extends Statement, O extends JdbcOperation> SqlAstTranslator<O> buildTranslator(
						SqlAstTranslationRequest<S, O> request) {
					if ( request instanceof SqlAstTranslationRequest.Select ) {
						translations++;
					}
					return delegate.buildTranslator( request );
				}
			};
		}
	}

	@Entity(name = "TagOwner")
	static class Owner {
		@Id int id;
		@ManyToMany @JoinTable(name = "owner_tags") Set<Tag> tags = new HashSet<>();
		@ManyToMany @JoinTable(name = "owner_tag_map") @MapKeyJoinColumn(name = "key_id")
		Map<Tag, Tag> byTag = new HashMap<>();

		Owner() {
		}

		Owner(int id) {
			this.id = id;
		}
	}

	@Entity(name = "ScalarReadTag")
	static class Tag {
		@Id @GeneratedValue Long id;
	}
}
