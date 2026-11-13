package org.hibernate.orm.test.triage.hhh2824;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
/**
 * Label has many Translations, unique per (label, language); with a language filter
 * enabled, Label.myTranslation is the single translation in that language.
 */
@DomainModel(annotatedClasses = {FilteredOneToOneExplicitAliasTest.Label.class, FilteredOneToOneExplicitAliasTest.Translation.class})
@SessionFactory(useCollectingStatementInspector = true)
public class FilteredOneToOneExplicitAliasTest {
	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			long tid = 0;
			// English texts sort a < b < c for labels 3, 2, 1; Dutch texts sort the other way round
			String[][] texts = {{"c-en", "x-nl"}, {"b-en", "y-nl"}, {"a-en", "z-nl"}};
			for ( int i = 0; i < 3; i++ ) {
				Label label = new Label();
				label.id = (long) i + 1;
				session.persist( label );
				session.persist( new Translation( ++tid, label, "en", texts[i][0] ) );
				session.persist( new Translation( ++tid, label, "nl", texts[i][1] ) );
			}
		} );
	}
	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}
	@Test
	void findWithLanguageFilter(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.enableFilter( "language" ).setParameter( "lang", "en" );
			assertEquals( "c-en", session.find( Label.class, 1L ).myTranslation.text );
		} );
	}
	@Test
	void multipleFiltersOnSameAssociation(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.enableFilter( "language" ).setParameter( "lang", "en" );
			session.enableFilter( "textContains" ).setParameter( "pattern", "c%" );
			Label label = session.find( Label.class, 1L );
			assertEquals( "c-en", label.myTranslation.text );
		} );
	}
	@Test
	void orderByTranslationWithPaging(SessionFactoryScope scope) {
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		scope.inTransaction( session -> {
			session.enableFilter( "language" ).setParameter( "lang", "en" );
			inspector.clear();
			List<Label> labels = session.createSelectionQuery(
							"select l from LabelX_HHH2824 l left join fetch l.myTranslation t order by t.text", Label.class )
					.setMaxResults( 2 )
					.getResultList();
			String sql = inspector.getSqlQueries().get( 0 );
			assertTrue( sql.toLowerCase().contains( ".language" ), "filter should reference language column" );
			assertTrue( !sql.toLowerCase().contains( " alias." ), "{alias} placeholder must be replaced, not literal" );
			List<String> texts = new ArrayList<>();
			labels.forEach( l -> texts.add( l.myTranslation.text ) );
			assertEquals( List.of( "a-en", "b-en" ), texts );
		} );
	}
	@Entity(name = "LabelX_HHH2824")
	@Table(name = "LabelX_HHH2824")
	@FilterDef(name = "language", parameters = @ParamDef(name = "lang", type = String.class))
	@FilterDef(name = "textContains", parameters = @ParamDef(name = "pattern", type = String.class))
	public static class Label {
		@Id
		Long id;
		@OneToMany(mappedBy = "label")
		Collection<Translation> translations;
		@OneToOne(mappedBy = "label")
		@Filter(name = "language", deduceAliasInjectionPoints = false, condition = "{alias}.language = :lang")
		@Filter(name = "textContains", deduceAliasInjectionPoints = false, condition = "{alias}.text like :pattern")
		Translation myTranslation;
	}
	@Entity(name = "TranslationX_HHH2824")
	@Table(name = "TranslationX_HHH2824",
			uniqueConstraints = @UniqueConstraint(columnNames = {"label_id", "language"}))
	public static class Translation {
		@Id
		Long id;
		@ManyToOne
		@JoinColumn(name = "label_id")
		Label label;
		String language;
		String text;
		Translation() {
		}
		Translation(Long id, Label label, String language, String text) {
			this.id = id;
			this.label = label;
			this.language = language;
			this.text = text;
		}
	}
}
