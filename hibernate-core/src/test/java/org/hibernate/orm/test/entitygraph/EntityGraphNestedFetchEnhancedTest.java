package org.hibernate.orm.test.entitygraph;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.FetchMethod;
import org.hibernate.Hibernate;
import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks {@link FetchMethod#NESTED} with bytecode enhancement, comparing with {@link FetchMethod#BY_ID}.
 */
@DomainModel(
		annotatedClasses = {
				EntityGraphNestedFetchEnhancedTest.Catalog.class,
				EntityGraphNestedFetchEnhancedTest.Entry.class
		}
)
@SessionFactory(useCollectingStatementInspector = true)
@BytecodeEnhanced(testEnhancedClasses = EntityGraphNestedFetchEnhancedTest.Entry.class)
@EnhancementOptions(lazyLoading = true, inlineDirtyChecking = true, extendedEnhancement = true)
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsNestedCollectionFetch.class)
@JiraKey("HHH-9558")
class EntityGraphNestedFetchEnhancedTest {

	@BeforeEach
	void prepareData(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var catalog = new Catalog();
			catalog.id = 1L;
			session.persist( catalog );
			for ( long id = 1; id <= 2; id++ ) {
				final var entry = new Entry();
				entry.id = id;
				entry.name = "entry " + id;
				entry.description = "description " + id;
				entry.catalog = catalog;
				session.persist( entry );
			}
		} );
		scope.getCollectingStatementInspector().clear();
	}

	@AfterEach
	void cleanUp(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	void lazyAttributeOfElement_BY_ID(SessionFactoryScope scope) {
		lazyAttributeOfElement( FetchMethod.BY_ID, scope );
	}

	@Test
	void lazyAttributeOfElement_NESTED(SessionFactoryScope scope) {
		lazyAttributeOfElement( FetchMethod.NESTED, scope );
	}

	private void lazyAttributeOfElement(FetchMethod method, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var graph = session.createEntityGraph( Catalog.class );
			graph.addAttributeNode( "entries" ).addOption( method );
			final var catalog = session.find( graph, 1L );
			assertThat( scope.getCollectingStatementInspector().getSqlQueries().stream()
					.anyMatch( sql -> sql.contains( "json" ) ) )
					.isEqualTo( method == FetchMethod.NESTED );
			final var entry = catalog.entries.stream().filter( e -> e.id == 2L ).findFirst().orElseThrow();
			// the lazy attribute is not loaded by the fetch, but when it is accessed
			assertThat( Hibernate.isPropertyInitialized( entry, "description" ) ).isFalse();
			assertThat( entry.description ).isEqualTo( "description 2" );
			// inline dirty checking of the element
			entry.name = "renamed";
		} );
		scope.inTransaction( session -> {
			final var entry = session.find( Entry.class, 2L );
			assertThat( entry.name ).isEqualTo( "renamed" );
			assertThat( entry.description ).isEqualTo( "description 2" );
			assertThat( entry.version ).isEqualTo( 1 );
		} );
	}

	@Entity(name = "NfeCatalog")
	@Table(name = "nfe_catalog")
	static class Catalog {
		@Id
		Long id;
		@OneToMany(mappedBy = "catalog")
		List<Entry> entries = new ArrayList<>();
	}

	@Entity(name = "NfeEntry")
	@Table(name = "nfe_entry")
	static class Entry {
		@Id
		Long id;
		@Version
		int version;
		String name;
		@Basic(fetch = FetchType.LAZY)
		String description;
		@ManyToOne(fetch = FetchType.LAZY)
		Catalog catalog;
	}
}
