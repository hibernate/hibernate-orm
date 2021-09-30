/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.integration.functions;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;

import org.hibernate.spatial.integration.Model;
import org.hibernate.spatial.testing.IsSupportedBySpatial;
import org.hibernate.spatial.testing.SpatialTestBase;
import org.hibernate.spatial.testing.datareader.TestSupport;

import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
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
public class CommonFunctionTests extends SpatialTestBase {

	public final static TestSupport.TestDataPurpose PURPOSE = TestSupport.TestDataPurpose.SpatialFunctionsData;

	List received;
	List expected;

	@Override
	public TestSupport.TestDataPurpose purpose() {
		return PURPOSE;
	}

	@TestFactory
	public Stream<DynamicTest> testFunction() {

		return
				TestTemplates.all( templates, hqlOverrides )
						.filter( f -> isSupported( f.function ) )
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

	protected <T> String displayName(FunctionTestTemplate template, String fnName) {
		return String.format(
				Locale.ROOT,
				"Test for function %s on entity %s",
				fnName,
				template.getModel().entityClass.getSimpleName()
		);
	}

	protected Executable executableTest(FunctionTestTemplate template, String fnName) {
		return () -> {
			expected = template.executeNativeQuery( scope );
			received = template.executeHQL( scope, fnName );
			assertEquals( expected, received );
		};
	}
}
