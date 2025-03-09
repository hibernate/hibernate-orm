/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.integration.functions;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;

import org.hibernate.dialect.OracleDialect;
import org.hibernate.spatial.integration.Model;
import org.hibernate.spatial.testing.IsSupportedBySpatial;
import org.hibernate.spatial.testing.SpatialTestBase;
import org.hibernate.spatial.testing.datareader.TestSupport;

import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Dynamic tests for "common" spatial functions.
 *
 * <p>
 * The tests here are dynamic across several dimensions:
 *     <ul>
 *         <li>the spatial function (in so far as actually supported by the dialect)</li>
 *         <li>the Geometry model (JTS or Geolatte)</li>
 *         <li>the name for the function (pre-H6 style, or "st_*" style)</li>
 *     </ul>
 * </p>
 */

@SuppressWarnings("rawtypes")
@RequiresDialectFeature(feature = IsSupportedBySpatial.class)
@SessionFactory
@SkipForDialect(dialectClass = OracleDialect.class, reason = "See https://hibernate.atlassian.net/browse/HHH-15669")
public class CommonFunctionTests extends SpatialTestBase {

	public final static TestSupport.TestDataPurpose PURPOSE = TestSupport.TestDataPurpose.SpatialFunctionsData;

	List received = new ArrayList();
	List expected = new ArrayList();

	@Override
	public TestSupport.TestDataPurpose purpose() {
		return PURPOSE;
	}

	@TestFactory
	public Stream<DynamicTest> testFunction() {

		return
				TestTemplates.all( templates, hqlOverrides, geometryEquality, filterGeometry )
						.filter( f -> isSupported( f.function ) )
						.filter( f -> !exludeFromTest.contains( f.function ) )
						.flatMap( t -> Stream.of(
								t.build( Model.JTSMODEL, codec ),
								t.build( Model.GLMODEL, codec )
						) )
						.flatMap( this::buildTests );

	}

	protected Stream<DynamicTest> buildTests(FunctionTestTemplate template) {
		return Stream.of(
						template.getFunctionName(),
						template.getAltFunctionName()
				)
				.filter( Objects::nonNull )
				.map( fn -> DynamicTest.dynamicTest(
						displayName( template, fn ), executableTest( template, fn )
				) );
	}

	protected String displayName(FunctionTestTemplate template, String fnName) {
		return String.format(
				Locale.ROOT,
				"Test for function %s on entity %s",
				fnName,
				template.getModel().entityClass.getSimpleName()
		);
	}

	protected Executable executableTest(FunctionTestTemplate template, String fnName) {
		return () -> {
			expected.clear();
			received.clear();
			expected = template.executeNativeQuery( scope );
			received = template.executeHQL( scope, fnName );
			if ( !expected.equals( received ) ) {
				for ( int i = 0; i < expected.size(); i++ ) {
					assertEquals( expected.get( i ), received.get( i ) );
				}
			}
		};
	}
}
