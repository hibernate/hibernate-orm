package org.hibernate.processor.test.data.restriction;



import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.jupiter.api.Test;
import org.hibernate.processor.test.util.CompilationDiagnostics;
import org.hibernate.processor.test.util.IgnoreCompilationErrors;

import static org.hibernate.processor.test.util.TestUtil.getMetaModelSourceAsString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@CompilationTest
class DataRestrictionTest {
	@Test
	@WithClasses({
			DataRestrictionBook.class,
			DataRestrictionPublisher.class,
			DataRestrictionRepository.class
	})
	void generatedRepositoryAcceptsJakartaDataRestrictions() {
		final String repository = getMetaModelSourceAsString( DataRestrictionRepository.class, true );
		final String queryMetamodel = getMetaModelSourceAsString( DataRestrictionRepository.class );
		final String metamodel = getMetaModelSourceAsString( DataRestrictionBook.class, true );
		System.out.println( repository );
		System.out.println( queryMetamodel );
		System.out.println( metamodel );

		assertTrue( repository.contains( "Restriction<? super DataRestrictionBook> restriction" ) );
		assertTrue( repository.contains( "List<Restriction<? super DataRestrictionBook>> restrictions" ) );
		assertTrue( repository.contains( "Restriction<? super DataRestrictionBook>[] restrictions" ) );
		assertTrue( repository.contains(
				"List<DataRestrictionBook> search(@Nonnull Restriction<? super DataRestrictionBook> restriction,"
						+ " @Nonnull Order<? super DataRestrictionBook> order, @Nonnull Limit limit)" ) );
		assertTrue( repository.contains( "applyRestriction(restriction, _query, _entity, _builder);" ) );
		assertTrue( repository.contains( "applyRestriction(Restrict.all(restrictions), _query, _entity, _builder);" ) );
		assertTrue( repository.contains( "applyOrder(order, _query, _entity, _builder);" ) );
		assertTrue( repository.contains( ".setFirstResult((int) limit.startAt() - 1)" ) );
		assertTrue( repository.contains( ".setMaxResults(limit.maxResults())" ) );
		assertTrue( repository.contains( "_spec.restrict(adaptRestriction(queryRestriction));" ) );
		assertTrue( repository.contains( "_spec.restrict(adaptRestriction(deleteRestriction));" ) );
		assertFalse( repository.contains( "TypedQueryReference<" ) );
		assertTrue( repository.contains( "SelectionSpecification.create(DataRestrictionRepository_.query())" ) );
		assertTrue( repository.contains( "SelectionSpecification.create(DataRestrictionRepository_.query(title))" ) );
		assertFalse( repository.contains( "SelectionSpecification.create(new StaticTypedQueryReference<>(" ) );
		assertTrue( queryMetamodel.contains( "\"org.hibernate.processor.test.data.restriction"
				+ ".DataRestrictionRepository#query(jakarta.data.restrict.Restriction)\"" ) );
		assertFalse( repository.contains( "DataRestrictionBook_.restriction" ) );
		assertFalse( repository.contains( "DataRestrictionBook_.queryRestriction" ) );
		assertFalse( repository.contains( "DataRestrictionBook_.deleteRestriction" ) );

		assertTrue( metamodel.contains( "TextAttribute.of(DataRestrictionBook.class, TITLE)" ) );
		assertTrue( metamodel.contains( "NumericAttribute.of(DataRestrictionBook.class, PAGES, int.class)" ) );
		assertTrue( metamodel.contains(
				"NavigableAttribute.of(DataRestrictionBook.class, PUBLISHER, DataRestrictionPublisher.class)" ) );
	}

	@Test
	@WithClasses({
			DataRestrictionBook.class,
			DataRestrictionPublisher.class,
			ExplicitQueryRestrictionRepository.class
	})
	void queryRootEntitySupportsProjectionRestrictionsWithoutPrimaryEntity() {
		final String repository = getMetaModelSourceAsString( ExplicitQueryRestrictionRepository.class, true );
		final String queryMetamodel = getMetaModelSourceAsString( ExplicitQueryRestrictionRepository.class );
		System.out.println( repository );
		System.out.println( queryMetamodel );

		assertTrue( repository.contains(
				"List<Summary> books(@Nonnull Restriction<DataRestrictionBook> restriction)" ) );
		assertTrue( repository.contains(
				"var _reference = _builder.augment(ExplicitQueryRestrictionRepository_.books(), Summary.class, _query -> {" ) );
		assertTrue( repository.contains(
				"var _entity = (Root<DataRestrictionBook>) _query.getRootList().get(0);" ) );
		assertTrue( repository.contains( "restriction.apply(_query, _entity);" ) );
		assertTrue( repository.contains( "_builder.construct(Summary.class" ) );
		assertTrue( queryMetamodel.contains( "TypedQueryReference<DataRestrictionBook> books()" ) );
		assertTrue( queryMetamodel.contains( "DataRestrictionBook.class" ) );
	}

	@Test
	@WithClasses({ DataRestrictionPublisher.class, DataRestrictionBook.class, InvalidDataRestrictionRepository.class })
	@IgnoreCompilationErrors
	void invalidAutomaticDeleteUsesMeaningfulDiagnostics(CompilationDiagnostics diagnostics) {
		diagnostics.assertError(
				"parameter of type 'jakarta.data.Limit' is not allowed on an automatic '@Delete' method" );
		diagnostics.assertError(
				"parameter of type 'jakarta.data.Order<? super org.hibernate.processor.test.data.restriction.DataRestrictionBook>' is not allowed on an automatic '@Delete' method" );
		diagnostics.assertError(
				"parameter of type 'jakarta.data.Sort<? super org.hibernate.processor.test.data.restriction.DataRestrictionBook>' is not allowed on an automatic '@Delete' method" );
		diagnostics.assertError(
				"parameter of type 'jakarta.data.page.PageRequest' is not allowed on an automatic '@Delete' method" );
		diagnostics.assertError( "automatic '@Delete' methods may have at most one Restriction parameter" );
		diagnostics.assertError(
				"parameters matching entity attributes must appear before the Restriction parameter" );
		diagnostics.assertError(
				"automatic '@Delete' methods accept a single Restriction parameter, not a collection or array" );
		diagnostics.assertError( "mismatched type of restriction (should be 'Restriction<? super DataRestrictionBook>')" );
		diagnostics.assertSingleError(
				"'@NativeQuery' methods may not declare Restriction or Range parameters; "
						+ "native SQL cannot be augmented with restrictions" );
		diagnostics.assertSingleError(
				"'@NativeQuery' methods may not declare Order or Sort parameters or '@OrderBy'; "
						+ "native SQL cannot be augmented with ordering" );
	}

	@Test
	@WithClasses({ DataRestrictionPublisher.class, DataRestrictionBook.class, InvalidNativeStaticQuery.class })
	@IgnoreCompilationErrors
	void nonRepositoryNativeQueryWithRestrictionUsesSingleDiagnostic(CompilationDiagnostics diagnostics) {
		diagnostics.assertSingleError( "'@NativeQuery' methods may not declare Restriction or Range parameters" );
	}
}
