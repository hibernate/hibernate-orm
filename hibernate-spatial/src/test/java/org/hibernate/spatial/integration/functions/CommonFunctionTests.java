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
import java.util.stream.Stream;

import org.hibernate.spatial.integration.SpatialTestDataProvider;
import org.hibernate.spatial.testing.datareader.TestSupport;
import org.hibernate.spatial.testing.domain.GeomEntity;
import org.hibernate.spatial.testing.domain.JtsGeomEntity;
import org.hibernate.spatial.testing.domain.SpatialDomainModel;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SessionFactoryScopeAware;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Dynamic tests for "common" spatial functions.
 *
 * <p>
 *     The tests here are dynamic across several dimensions:
 *     <ul>
 *         <li>the spatial function (in so far as actually supported by the dialect)</li>
 *         <li>the Geometry model (JTS or Geolatte)</li>
 *         <li>the name for the function (pre-H6 style, or "st_*" style)</li>
 *     </ul>
 * </p>
 */
@SuppressWarnings("ALL")
@DomainModel(modelDescriptorClasses = SpatialDomainModel.class)
@SessionFactory
public class CommonFunctionTests extends SpatialTestDataProvider
		implements SessionFactoryScopeAware {

	private SessionFactoryScope scope;
	List received;
	List expected;

	@Override
	public void injectSessionFactoryScope(SessionFactoryScope scope) {
		this.scope = scope;
	}

	@BeforeEach
	public void beforeEach() {
		scope.inTransaction( session -> super.entities(
						JtsGeomEntity.class,
						TestSupport.TestDataPurpose.SpatialFunctionsData
				)
				.forEach( session::save ) );
		scope.inTransaction( session -> super.entities(
				GeomEntity.class,
				TestSupport.TestDataPurpose.SpatialFunctionsData
		).forEach( session::save ) );
	}

	@AfterEach
	public void cleanup() {
		scope.inTransaction( session -> session.createQuery( "delete from GeomEntity" ).executeUpdate() );
		scope.inTransaction( session -> session.createQuery( "delete from JtsGeomEntity" ).executeUpdate() );
	}

	@TestFactory
	public Stream<DynamicTest> testFunction() {
		return
				TestTemplates.all( templates, hqlOverrides )
						// TODO -- filter for supported functions
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
				.filter( s -> s != null )
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

	protected <T> Executable executableTest(FunctionTestTemplate template, String fnName) {
		Executable testF = () -> {
			expected = template.executeNativeQuery( scope );
			received = template.executeHQL( scope, fnName );
			assertEquals( expected, received );
		};
		return testF;
	}
}
