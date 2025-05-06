/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.lazyCache;

import java.util.Date;
import java.util.Locale;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Formula;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.entry.StandardCacheEntryImpl;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.graph.RootGraph;
import org.hibernate.persister.entity.EntityPersister;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Basic;
import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;

import static org.hibernate.Hibernate.isPropertyInitialized;
import static org.hibernate.jpa.SpecHints.HINT_SPEC_FETCH_GRAPH;
import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Steve EbersolenPropertyRefTest
 */
@DomainModel(
		annotatedClasses = {
				InitFromCacheTest.Document.class
		}
)
@ServiceRegistry(
		settings = {
				@Setting( name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true" ),
				@Setting( name = AvailableSettings.GENERATE_STATISTICS, value = "true" ),
		}
)
@SessionFactory
@BytecodeEnhanced
public class InitFromCacheTest {

	private EntityPersister persister;

	private Long documentID;

	@BeforeEach
	public void prepare(SessionFactoryScope scope) {
		persister = scope.getSessionFactory().getRuntimeMetamodels()
				.getMappingMetamodel()
				.getEntityDescriptor( Document.class );
		assertTrue( persister.hasCache() );

		scope.inTransaction( s -> {
			Document document = new Document( "HiA", "Hibernate book", "Hibernate is...." );
			s.persist( document );
			documentID = document.id;
		} );
	}

	@Test
	public void execute(SessionFactoryScope scope) {
	scope.inTransaction( s -> {
					final RootGraph<Document> entityGraph = s.createEntityGraph( Document.class );
					entityGraph.addAttributeNodes( "text", "summary" );
					final Document document = s.createQuery( "from Document", Document.class )
							.setHint( HINT_SPEC_FETCH_GRAPH, entityGraph )
							.uniqueResult();
					assertTrue( isPropertyInitialized( document, "text" ) );
					assertTrue( isPropertyInitialized( document, "summary" ) );

					final EntityDataAccess entityDataAccess = persister.getCacheAccessStrategy();
					final Object cacheKey = entityDataAccess.generateCacheKey(
							document.id,
							persister,
							scope.getSessionFactory(),
							null
					);
					final Object cachedItem = entityDataAccess.get( (SharedSessionContractImplementor) s, cacheKey );
					assertNotNull( cachedItem );
					assertTyping( StandardCacheEntryImpl.class, cachedItem );
				}
		);

		scope.getSessionFactory().getStatistics().clear();

		scope.inTransaction( s -> {
			CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
			CriteriaQuery<Document> criteria = criteriaBuilder.createQuery( Document.class );
			criteria.from( Document.class );
			Document d = s.createQuery( criteria ).uniqueResult();
//            Document d = (Document) s.createCriteria( Document.class ).uniqueResult();
			assertFalse( isPropertyInitialized( d, "text" ) );
			assertFalse( isPropertyInitialized( d, "summary" ) );
			assertEquals( "Hibernate is....", d.text );
			assertTrue( isPropertyInitialized( d, "text" ) );
			assertTrue( isPropertyInitialized( d, "summary" ) );
		} );

		assertEquals( 2, scope.getSessionFactory().getStatistics().getPrepareStatementCount() );

		scope.inTransaction( s -> {
			Document d = s.get( Document.class, documentID );
			assertFalse( isPropertyInitialized( d, "text" ) );
			assertFalse( isPropertyInitialized( d, "summary" ) );
		} );
	}

	// --- //

	@Entity( name = "Document" )
	@Table( name = "DOCUMENT" )
	@Cacheable
	@Cache( usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE, includeLazy = false, region = "foo" )
	static class Document {

		@Id
		@GeneratedValue
		Long id;

		String name;

		@Basic( fetch = FetchType.LAZY )
		@Formula( "upper(name)" )
		String upperCaseName;

		@Basic( fetch = FetchType.LAZY )
		String summary;

		@Basic( fetch = FetchType.LAZY )
		String text;

		@Basic( fetch = FetchType.LAZY )
		Date lastTextModification;

		Document() {
		}

		Document(String name, String summary, String text) {
			this.lastTextModification = new Date();
			this.name = name;
			this.upperCaseName = name.toUpperCase( Locale.ROOT );
			this.summary = summary;
			this.text = text;
		}
	}
}
