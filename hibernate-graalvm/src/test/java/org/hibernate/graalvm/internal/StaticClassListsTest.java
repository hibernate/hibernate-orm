/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.graalvm.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.graalvm.internal.JandexTestUtils.findConcreteNamedImplementors;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hibernate.Session;
import org.hibernate.event.spi.EventType;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import org.jboss.jandex.Index;

public class StaticClassListsTest {
	private static Index hibernateIndex;

	@BeforeAll
	public static void index() throws IOException {
		hibernateIndex = JandexTestUtils.indexJar( Session.class );
	}

	@Nested
	class TypesNeedingAllConstructorsAccessible {
		@ParameterizedTest
		@EnumSource(TypesNeedingAllConstructorsAccessible_Category.class)
		void containsAllExpectedClasses(TypesNeedingAllConstructorsAccessible_Category category) {
			assertThat( StaticClassLists.typesNeedingAllConstructorsAccessible() )
					.containsAll( category.classes().collect( Collectors.toSet() ) );
		}

		@Test
		void meta_noMissingTestCategory() {
			assertThat( Arrays.stream( TypesNeedingAllConstructorsAccessible_Category.values() ).flatMap( TypesNeedingAllConstructorsAccessible_Category::classes ) )
					.as( "If this fails, a category is missing in " + TypesNeedingAllConstructorsAccessible_Category.class )
					.contains( StaticClassLists.typesNeedingAllConstructorsAccessible() );
		}
	}

	// TODO ORM 7: Move this inside TypesNeedingAllConstructorsAccessible (requires JDK 17) and rename to simple Category
	enum TypesNeedingAllConstructorsAccessible_Category {
		PERSISTERS {
			@Override
			Stream<Class<?>> classes() {
				return findConcreteNamedImplementors(
						hibernateIndex, EntityPersister.class, CollectionPersister.class )
						.stream();
			}
		},
		MISC {
			@Override
			Stream<Class<?>> classes() {
				// NOTE: Please avoid putting anything here, it's really a last resort.
				// Ideally you'd rather add new categories with their own way of listing classes,
				// like in PERSISTERS.
				// Putting anything here is running the risk of forgetting
				// why it was necessary in the first place...
				return Stream.of(
						// Logging - sometimes looked up without a static field
						org.hibernate.internal.CoreMessageLogger_$logger.class
				);
			}
		};

		abstract Stream<Class<?>> classes();
	}

	@Nested
	class TypesNeedingDefaultConstructorAccessible {
		@ParameterizedTest
		@EnumSource(TypesNeedingDefaultConstructorAccessible_Category.class)
		void containsAllExpectedClasses(TypesNeedingDefaultConstructorAccessible_Category category) {
			assertThat( StaticClassLists.typesNeedingDefaultConstructorAccessible() )
					.containsAll( category.classes().collect( Collectors.toSet() ) );
		}

		@Test
		void meta_noMissingTestCategory() {
			assertThat( Arrays.stream( TypesNeedingDefaultConstructorAccessible_Category.values() ).flatMap( TypesNeedingDefaultConstructorAccessible_Category::classes ) )
					.as( "If this fails, a category is missing in " + TypesNeedingDefaultConstructorAccessible_Category.class )
					.contains( StaticClassLists.typesNeedingDefaultConstructorAccessible() );
		}
	}

	// TODO ORM 7: Move this inside TypesNeedingDefaultConstructorAccessible (requires JDK 17) and rename to simple Category
	enum TypesNeedingDefaultConstructorAccessible_Category {
		MISC {
			@Override
			Stream<Class<?>> classes() {
				// NOTE: Please avoid putting anything here, it's really a last resort.
				// Ideally you'd rather add new categories with their own way of listing classes,
				// like in PERSISTERS.
				// Putting anything here is running the risk of forgetting
				// why it was necessary in the first place...
				return Stream.of(
						org.hibernate.resource.transaction.backend.jdbc.internal.JdbcResourceLocalTransactionCoordinatorBuilderImpl.class,
						org.hibernate.id.enhanced.SequenceStyleGenerator.class,
						org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl.class,
						org.hibernate.resource.transaction.backend.jta.internal.JtaTransactionCoordinatorBuilderImpl.class,
						org.hibernate.type.EnumType.class,
						org.hibernate.tool.schema.internal.script.MultiLineSqlScriptExtractor.class
				);
			}
		};

		abstract Stream<Class<?>> classes();
	}

	@Nested
	class TypesNeedingArrayCopy {
		@ParameterizedTest
		@EnumSource(TypesNeedingArrayCopy_Category.class)
		void containsAllExpectedClasses(TypesNeedingArrayCopy_Category category) {
			assertThat( StaticClassLists.typesNeedingArrayCopy() )
					.containsAll( category.classes().collect( Collectors.toSet() ) );
		}

		@Test
		void meta_noMissingTestCategory() {
			assertThat( Arrays.stream( TypesNeedingArrayCopy_Category.values() ).flatMap( TypesNeedingArrayCopy_Category::classes ) )
					.as( "If this fails, a category is missing in " + TypesNeedingArrayCopy_Category.class )
					.contains( StaticClassLists.typesNeedingArrayCopy() );
		}
	}

	// TODO ORM 7: Move this inside TypesNeedingArrayCopy (requires JDK 17) and rename to simple Category
	enum TypesNeedingArrayCopy_Category {
		EVENT_LISTENER_INTERFACES {
			@Override
			Stream<Class<?>> classes() {
				return EventType.values().stream().map( EventType::baseListenerInterface )
						.map( c -> Array.newInstance( c, 0 ).getClass() );
			}
		},
		MISC {
			@Override
			Stream<Class<?>> classes() {
				// NOTE: Please avoid putting anything here, it's really a last resort.
				// Ideally you'd rather add new categories with their own way of listing classes,
				// like in EVENT_LISTENER_INTERFACES.
				// Putting anything here is running the risk of forgetting
				// why it was necessary in the first place...
				return Stream.of(
						// Java classes -- the why is lost to history
						java.util.function.Function[].class,
						java.util.List[].class,
						java.util.Map.Entry[].class,
						java.util.function.Supplier[].class,
						// Graphs -- the why is lost to history
						org.hibernate.graph.spi.AttributeNodeImplementor[].class,
						org.hibernate.sql.results.graph.FetchParent[].class,
						org.hibernate.graph.spi.GraphImplementor[].class,
						org.hibernate.graph.internal.parse.SubGraphGenerator[].class,
						// AST/parsing -- no way to detect this automatically, you just have to know.
						org.hibernate.sql.ast.Clause[].class,
						org.hibernate.query.hql.spi.DotIdentifierConsumer[].class,
						org.hibernate.query.sqm.sql.FromClauseIndex[].class,
						org.hibernate.query.sqm.spi.ParameterDeclarationContext[].class,
						org.hibernate.sql.ast.tree.select.QueryPart[].class,
						org.hibernate.sql.ast.spi.SqlAstProcessingState[].class,
						org.hibernate.query.hql.spi.SqmCreationProcessingState[].class,
						org.hibernate.sql.ast.tree.Statement[].class,
						// Various internals -- the why is lost to history
						org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingState[].class
				);
			}
		};

		abstract Stream<Class<?>> classes();
	}

	@Nested
	class BasicConstructorsAvailable {

		@Test
		void checkNonDefaultConstructorsCanBeLoaded() {
			Class[] classes = StaticClassLists.typesNeedingAllConstructorsAccessible();
			for ( Class c : classes ) {
				Constructor[] declaredConstructors = c.getDeclaredConstructors();
				Assert.assertTrue( declaredConstructors.length > 0 );
				if ( declaredConstructors.length == 1 ) {
					//If there's only one, let's check that this class wasn't placed in the wrong cathegory:
					Assert.assertTrue( declaredConstructors[0].getParameterCount() > 0 );
				}
			}
		}

		@Test
		void checkDefaultConstructorsAreAvailable() {
			Class[] classes = StaticClassLists.typesNeedingDefaultConstructorAccessible();
			for ( Class c : classes ) {
				Constructor constructor = ReflectHelper.getDefaultConstructor( c );
				Assert.assertNotNull( "Failed for class: " + c.getName(), constructor );
			}
		}

		@Test
		public void checkArraysAreArrays() {
			Class[] classes = StaticClassLists.typesNeedingArrayCopy();
			for ( Class c : classes ) {
				Assert.assertTrue( "Wrong category for type: " + c.getName(), c.isArray() );
				Constructor[] constructors = c.getConstructors();
				Assert.assertEquals( 0, constructors.length );
			}
		}

	}
}
