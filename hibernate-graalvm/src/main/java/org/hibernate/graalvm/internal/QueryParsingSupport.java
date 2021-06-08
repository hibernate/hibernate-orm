/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.graalvm.internal;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.hibernate.internal.build.AllowSysOut;
import org.hibernate.internal.util.ReflectHelper;

import com.oracle.svm.core.annotate.AutomaticFeature;
import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeReflection;

/**
 * This registers all ANTLR parser nodes for reflection, something that is necessary
 * as the HQL parser's inner workings are based on reflection.
 * This is different than the "static" registrations of {@link GraalVMStaticAutofeature}
 * as we only register these if the HQL parser is actually reachable: some particularly
 * simple applications might not need dynamic queries being expressed in string form,
 * and for such cases the reflective registrations can be skipped.
 *
 * At time of writing, this is particularly unlikely to be effective as Hibernate ORM
 * requires the parsers during bootstrap, but there is reasonable hope that this might
 * be improved on, and can already be used by framework integrations which are able
 * to bypass the traditional boot sequence.
 *
 * @author Sanne Grinovero
 */
@AutomaticFeature
public final class QueryParsingSupport implements Feature {

	private final AtomicBoolean triggered = new AtomicBoolean( false);

	/**
	 * To set this, add `-J-Dorg.hibernate.graalvm.diagnostics=true` to the native-image parameters
	 */
	private static final boolean log = Boolean.getBoolean( "org.hibernate.graalvm.diagnostics" );

	@Override
	public void beforeAnalysis(BeforeAnalysisAccess access) {
		Class<?> lexerClazz = access.findClassByName("org.hibernate.hql.internal.ast.HqlLexer");
		Class<?> parserClazz = access.findClassByName("org.hibernate.hql.internal.ast.HqlParser");
		access.registerReachabilityHandler(this::enableHQLSupport, lexerClazz);
		access.registerReachabilityHandler(this::enableHQLSupport, parserClazz);
	}

	@AllowSysOut
	private void enableHQLSupport(DuringAnalysisAccess duringAnalysisAccess) {
		final boolean needsEnablingYet = triggered.compareAndSet( false, true );
		if ( needsEnablingYet ) {
			if ( log ) {
				System.out.println( "Hibernate ORM 's automatic feature for GraalVM native images: enabling support for HQL query parsing" );
			}
			enableAntlrParsersSupport();
		}
	}

	private void enableAntlrParsersSupport() {
		final Class<?>[] needsHavingSimpleConstructors = typesNeedingDefaultConstructorAccessible();
		final Class[] neddingAllConstructorsAccessible = typesNeedingAllConstructorsAccessible();
		//Size formula is just a reasonable guess:
		ArrayList<Executable> executables = new ArrayList<>( needsHavingSimpleConstructors.length + neddingAllConstructorsAccessible.length * 3 );
		for ( Class c : needsHavingSimpleConstructors ) {
			executables.add( ReflectHelper.getDefaultConstructor( c ) );
		}
		for ( Class c : neddingAllConstructorsAccessible ) {
			for ( Constructor declaredConstructor : c.getDeclaredConstructors() ) {
				executables.add( declaredConstructor );
			}
		}
		RuntimeReflection.register( needsHavingSimpleConstructors );
		RuntimeReflection.register( neddingAllConstructorsAccessible );
		RuntimeReflection.register( executables.toArray(new Executable[0]) );
	}

	public static Class[] typesNeedingAllConstructorsAccessible() {
		return new Class[] {
				//ANTLR special ones:
				org.hibernate.hql.internal.ast.tree.EntityJoinFromElement.class,
				org.hibernate.hql.internal.ast.tree.MapKeyEntityFromElement.class,
				org.hibernate.hql.internal.ast.tree.ComponentJoin.class,
		};
	}

	public static Class[] typesNeedingDefaultConstructorAccessible() {
		return new Class[] {
				//Support for @OrderBy
				org.hibernate.sql.ordering.antlr.NodeSupport.class,
				org.hibernate.sql.ordering.antlr.OrderByFragment.class,
				org.hibernate.sql.ordering.antlr.SortSpecification.class,
				org.hibernate.sql.ordering.antlr.OrderingSpecification.class,
				org.hibernate.sql.ordering.antlr.CollationSpecification.class,
				org.hibernate.sql.ordering.antlr.SortKey.class,

				//ANTLR tokens:
				antlr.CommonToken.class,
				org.hibernate.hql.internal.ast.tree.SelectClause.class,
				org.hibernate.hql.internal.ast.tree.HqlSqlWalkerNode.class,
				org.hibernate.hql.internal.ast.tree.MethodNode.class,
				org.hibernate.hql.internal.ast.tree.UnaryLogicOperatorNode.class,
				org.hibernate.hql.internal.ast.tree.NullNode.class,
				org.hibernate.hql.internal.ast.tree.IntoClause.class,
				org.hibernate.hql.internal.ast.tree.UpdateStatement.class,
				org.hibernate.hql.internal.ast.tree.SelectExpressionImpl.class,
				org.hibernate.hql.internal.ast.tree.CastFunctionNode.class,
				org.hibernate.hql.internal.ast.tree.DeleteStatement.class,
				org.hibernate.hql.internal.ast.tree.SqlNode.class,
				org.hibernate.hql.internal.ast.tree.SearchedCaseNode.class,
				org.hibernate.hql.internal.ast.tree.FromElement.class,
				org.hibernate.hql.internal.ast.tree.JavaConstantNode.class,
				org.hibernate.hql.internal.ast.tree.SqlFragment.class,
				org.hibernate.hql.internal.ast.tree.MapKeyNode.class,
				org.hibernate.hql.internal.ast.tree.ImpliedFromElement.class,
				org.hibernate.hql.internal.ast.tree.IsNotNullLogicOperatorNode.class,
				org.hibernate.hql.internal.ast.tree.InsertStatement.class,
				org.hibernate.hql.internal.ast.tree.UnaryArithmeticNode.class,
				org.hibernate.hql.internal.ast.tree.CollectionFunction.class,
				org.hibernate.hql.internal.ast.tree.BinaryLogicOperatorNode.class,
				org.hibernate.hql.internal.ast.tree.CountNode.class,
				org.hibernate.hql.internal.ast.tree.IsNullLogicOperatorNode.class,
				org.hibernate.hql.internal.ast.tree.IdentNode.class,
				org.hibernate.hql.internal.ast.tree.ParameterNode.class,
				org.hibernate.hql.internal.ast.tree.MapEntryNode.class,
				org.hibernate.hql.internal.ast.tree.MapValueNode.class,
				org.hibernate.hql.internal.ast.tree.InLogicOperatorNode.class,
				org.hibernate.hql.internal.ast.tree.IndexNode.class,
				org.hibernate.hql.internal.ast.tree.DotNode.class,
				org.hibernate.hql.internal.ast.tree.ResultVariableRefNode.class,
				org.hibernate.hql.internal.ast.tree.BetweenOperatorNode.class,
				org.hibernate.hql.internal.ast.tree.AggregateNode.class,
				org.hibernate.hql.internal.ast.tree.QueryNode.class,
				org.hibernate.hql.internal.ast.tree.BooleanLiteralNode.class,
				org.hibernate.hql.internal.ast.tree.SimpleCaseNode.class,
				org.hibernate.hql.internal.ast.tree.OrderByClause.class,
				org.hibernate.hql.internal.ast.tree.FromClause.class,
				org.hibernate.hql.internal.ast.tree.ConstructorNode.class,
				org.hibernate.hql.internal.ast.tree.LiteralNode.class,
				org.hibernate.hql.internal.ast.tree.BinaryArithmeticOperatorNode.class,

				//Special tokens:
				org.hibernate.hql.internal.ast.HqlToken.class,
				org.hibernate.hql.internal.ast.tree.Node.class,

		};
	}

}
