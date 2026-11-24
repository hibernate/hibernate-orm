package org.hibernate.processor.test.data.select;



import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.jupiter.api.Test;
import org.hibernate.processor.test.util.CompilationDiagnostics;
import org.hibernate.processor.test.util.IgnoreCompilationErrors;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.processor.test.util.TestUtil.getMetaModelSourceAsString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@CompilationTest
class SelectionTest {

	@Test
	@WithClasses({
			SelectionStatus.class,
			SelectionPublisher.class,
			SelectionBook.class,
			SelectionRepository.class
	})
	void generatedRepositorySupportsSelectAndFirst() {
		final String repository = getMetaModelSourceAsString( SelectionRepository.class, true );
		final String normalizedRepository = repository.replaceAll( "\\s+", " " );
		final String queryMetamodel = getMetaModelSourceAsString( SelectionRepository.class );
		System.out.println( repository );
		System.out.println( queryMetamodel );
		assertMetamodelClassGeneratedFor( SelectionBook.class );
		assertMetamodelClassGeneratedFor( SelectionRepository.class, true );

		assertTrue( repository.contains( "_query.select(_entity.get(SelectionBook_.title));" ) );
		assertTrue( repository.contains( "_query.select(_entity.get(SelectionBook_.pages));" ) );
		assertTrue( normalizedRepository.contains(
				"_builder.construct(TitleAndPages.class, _entity.get(SelectionBook_.title), _entity.get(SelectionBook_.pages))" )
				|| normalizedRepository.contains(
						"_builder.construct(SelectionRepository.TitleAndPages.class, _entity.get(SelectionBook_.title), _entity.get(SelectionBook_.pages))" ) );
		assertTrue( normalizedRepository.contains(
				"_builder.construct(Renamed.class, _entity.get(SelectionBook_.title), _entity.get(SelectionBook_.pages))" )
				|| normalizedRepository.contains(
						"_builder.construct(SelectionRepository.Renamed.class, _entity.get(SelectionBook_.title), _entity.get(SelectionBook_.pages))" ) );
		assertTrue( normalizedRepository.contains(
				"_builder.construct(Named.class, _entity.get(SelectionBook_.title), _entity.get(SelectionBook_.pages))" )
				|| normalizedRepository.contains(
						"_builder.construct(SelectionRepository.Named.class, _entity.get(SelectionBook_.title), _entity.get(SelectionBook_.pages))" ) );
		assertFalse( repository.contains( "SelectionSpecification.create(SelectionBook.class)" ) );
		assertFalse( repository.contains( "ProjectionSpecification.create" ) );
		assertFalse( repository.contains( "_projection.select" ) );
		assertTrue( repository.contains( ".setMaxResults(1)" ) );
		assertTrue( repository.contains( ".setMaxResults(3)" ) );
		assertTrue( repository.contains( ".setMaxResults(2)" ) );
		assertTrue( repository.contains(
				"SelectionBook firstByStatus(SelectionStatus status, @Nonnull Order<SelectionBook> order)" )
				|| repository.contains(
						"SelectionBook firstByStatus(SelectionStatus status, @Nonnull jakarta.data.Order<SelectionBook> order)" ) );
		assertTrue( repository.contains( "applyOrder(order, _query, _entity, _builder);" ) );
		assertTrue( repository.contains(
				"SelectionSpecification.create(SelectionRepository_.queryBooksWithOptions(titlePattern))" ) );
		assertTrue( repository.contains( ".setParameter(1, titlePattern)" ) );
		assertFalse( repository.contains( ".setCacheRetrieveMode(CacheRetrieveMode.BYPASS)" ) );
		assertFalse( repository.contains( ".setHint(\"hint\", \"1\")" ) );
		assertTrue( repository.contains(
				"var _reference = _builder.augment(SelectionRepository_.queryTitlesByStatus(status), String.class, _query -> {" ) );
		assertTrue( repository.contains(
				"var _reference = _builder.augment(SelectionRepository_.queryTitlesWithStringFallback(minPages, status), String.class, _query -> {" ) );
		assertFalse( repository.contains( ".setParameter(\"status\", status)" ) );
		assertFalse( repository.contains( ".setParameter(\"minPages\", minPages)" ) );
		assertTrue( repository.contains(
				"var _reference = _builder.augment(SelectionRepository_.queryRenamedByStatus(status), QueryRenamed.class, _query -> {" )
				|| repository.contains(
						"var _reference = _builder.augment(SelectionRepository_.queryRenamedByStatus(status), SelectionRepository.QueryRenamed.class, _query -> {" ) );
		assertTrue( normalizedRepository.contains(
				"_query.select(_builder.construct(QueryRenamed.class, _entity.get(SelectionBook_.title), _entity.get(SelectionBook_.pages)));" )
				|| normalizedRepository.contains(
						"_query.select(_builder.construct(SelectionRepository.QueryRenamed.class, _entity.get(SelectionBook_.title), _entity.get(SelectionBook_.pages)));" ) );
		assertTrue( queryMetamodel.contains(
				"TypedQueryReference<SelectionBook> queryTitlesByStatus(SelectionStatus status)" ) );
		assertTrue( queryMetamodel.contains(
				"TypedQueryReference<SelectionBook> queryTitlesWithStringFallback(int minPages, SelectionStatus status)" ) );
		assertTrue( queryMetamodel.contains(
				"TypedQueryReference<SelectionBook> queryRenamedByStatus(SelectionStatus status)" ) );
	}

	@Test
	@WithClasses({ SelectionStatus.class, SelectionPublisher.class, SelectionBook.class, InvalidSelectionRepository.class })
	@IgnoreCompilationErrors
	void invalidSelectAndFirstUsesMeaningfulDiagnostics(CompilationDiagnostics diagnostics) {
		diagnostics.assertError( "no matching field named 'missing' in entity class" );
		diagnostics.assertError( "selected attribute 'publisher' of entity" );
		diagnostics.assertError( "is not a single-valued basic attribute" );
		diagnostics.assertError( "has type 'java.lang.String', which does not match method return type 'java.lang.Integer'" );
		diagnostics.assertError( "multiple '@Select' annotations require a record return type" );
		diagnostics.assertError( "'@First' value must be greater than 0" );
		diagnostics.assertError( "'@First' may not be combined with a parameter of type 'jakarta.data.Limit'" );
		diagnostics.assertError(
				"Jakarta Data repository method annotations are mutually exclusive: @Find, @Query" );
	}
}
