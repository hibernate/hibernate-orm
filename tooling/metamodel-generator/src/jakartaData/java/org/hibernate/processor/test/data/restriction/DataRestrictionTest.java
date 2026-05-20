/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.restriction;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;

import org.hibernate.processor.HibernateProcessor;
import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.TestUtil;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.jupiter.api.Test;

import static java.nio.charset.Charset.defaultCharset;
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
		assertTrue( repository.contains( "_spec.restrict(adaptRestriction(restriction));" ) );
		assertTrue( repository.contains( "_spec.restrict(adaptRestriction(Restrict.all(restrictions)));" ) );
		assertTrue( repository.contains( "_spec.restrict(adaptRestriction(queryRestriction));" ) );
		assertTrue( repository.contains( "_spec.restrict(adaptRestriction(deleteRestriction));" ) );
		assertFalse( repository.contains( "TypedQueryReference<" ) );
		assertTrue( repository.contains( "SelectionSpecification.create(DataRestrictionRepository_.query())" ) );
		assertTrue( repository.contains( "SelectionSpecification.create(DataRestrictionRepository_.query(title))" ) );
		assertFalse( repository.contains( "SelectionSpecification.create(new StaticTypedQueryReference<>(" ) );
		assertTrue( queryMetamodel.contains( "\"DataRestrictionRepository.query\"" ) );
		assertTrue( repository.contains( "for (var _sort : order.sorts())" ) );
		assertFalse( repository.contains( "DataRestrictionBook_.restriction" ) );
		assertFalse( repository.contains( "DataRestrictionBook_.queryRestriction" ) );
		assertFalse( repository.contains( "DataRestrictionBook_.deleteRestriction" ) );

		assertTrue( metamodel.contains( "TextAttribute.of(DataRestrictionBook.class, TITLE)" ) );
		assertTrue( metamodel.contains( "NumericAttribute.of(DataRestrictionBook.class, PAGES, int.class)" ) );
		assertTrue( metamodel.contains(
				"NavigableAttribute.of(DataRestrictionBook.class, PUBLISHER, DataRestrictionPublisher.class)" ) );
	}

	@Test
	void invalidAutomaticDeleteUsesMeaningfulDiagnostics() throws Exception {
		final DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
		final var compiler = ToolProvider.getSystemJavaCompiler();
		try ( var fileManager = compiler.getStandardFileManager( diagnostics, Locale.ROOT, defaultCharset() ) ) {
			final var sourceFiles = List.of(
					sourceFile( DataRestrictionPublisher.class ),
					sourceFile( DataRestrictionBook.class ),
					sourceFile( InvalidDataRestrictionRepository.class )
			);
			final var task = compiler.getTask(
					null,
					fileManager,
					diagnostics,
					List.of(
							"-d",
							TestUtil.getOutBaseDir( DataRestrictionTest.class ).getAbsolutePath(),
							"-processor",
							HibernateProcessor.class.getName()
					),
					null,
					fileManager.getJavaFileObjectsFromFiles( sourceFiles )
			);
			assertFalse( task.call() );
		}
		final String messages = diagnostics.getDiagnostics()
				.stream()
				.filter( diagnostic -> diagnostic.getKind() == Diagnostic.Kind.ERROR )
				.map( diagnostic -> diagnostic.getMessage( Locale.ROOT ) )
				.collect( Collectors.joining( "\n" ) );

		assertTrue( messages.contains(
				"parameter of type 'jakarta.data.Limit' is not allowed on an automatic '@Delete' method" ) );
		assertTrue( messages.contains(
				"parameter of type 'jakarta.data.Order<? super org.hibernate.processor.test.data.restriction.DataRestrictionBook>' is not allowed on an automatic '@Delete' method" ) );
		assertTrue( messages.contains(
				"parameter of type 'jakarta.data.Sort<? super org.hibernate.processor.test.data.restriction.DataRestrictionBook>' is not allowed on an automatic '@Delete' method" ) );
		assertTrue( messages.contains(
				"parameter of type 'jakarta.data.page.PageRequest' is not allowed on an automatic '@Delete' method" ) );
		assertTrue( messages.contains( "automatic '@Delete' methods may have at most one Restriction parameter" ) );
		assertTrue( messages.contains(
				"parameters matching entity attributes must appear before the Restriction parameter" ) );
		assertTrue( messages.contains(
				"automatic '@Delete' methods accept a single Restriction parameter, not a collection or array" ) );
		assertTrue( messages.contains( "mismatched type of restriction (should be 'Restriction<? super DataRestrictionBook>')" ) );
		assertTrue( messages.contains(
				"'@NativeQuery' methods may not declare Restriction or Range parameters; "
						+ "native SQL cannot be augmented with restrictions" ) );
		assertTrue( messages.contains(
				"'@NativeQuery' methods may not declare Order or Sort parameters or '@OrderBy'; "
						+ "native SQL cannot be augmented with ordering" ) );
	}

	private static File sourceFile(Class<?> type) {
		return new File(
				TestUtil.getSourceBaseDir( type ),
				type.getName().replace( '.', File.separatorChar ) + ".java"
		);
	}
}
