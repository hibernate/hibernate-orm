/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.function;

import java.util.List;
import java.util.function.Supplier;

import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.query.ReturnableType;
import org.hibernate.query.sqm.function.FunctionKind;
import org.hibernate.query.sqm.function.PatternBasedSqmFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.FunctionReturnTypeResolver;
import org.hibernate.query.sqm.produce.function.internal.PatternRenderer;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.type.BasicTypeReference;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Simplified API allowing users to easily contribute pattern-based
 * {@link org.hibernate.query.sqm.function.SqmFunctionDescriptor}s
 * to HQL.
 * <p>
 * In HQL we might define a function {@code concat(?1, ?2)} to
 * concatenate two strings p1 and p2. Dialects register different
 * instances of this class using the same name (concat) but with
 * different templates or patterns: {@code (?1 || ?2)} for Oracle,
 * {@code concat(?1, ?2)} for MySQL, {@code (?1 + ?2)} for SQL
 * Server. Each dialect defines a template as a string exactly as
 * shown above, marking each function parameter with '?' followed
 * by the parameter index. Parameters are indexed from 1. The
 * last parameter may be a vararg, indicated with the syntax
 * {@code (?1 || ?2...)}.
 *
 * @author Alexey Loubyansky
 *
 * @apiNote For internally-registered functions, we use the builders from
 *          {@link org.hibernate.query.sqm.function.SqmFunctionRegistry}.
 */
public class SQLFunctionTemplate extends PatternBasedSqmFunctionDescriptor {

	/**
	 * @apiNote The signature of this constructor only approximates the
	 *          same API in Hibernate 5 and below. Sorry.
	 */
	public SQLFunctionTemplate(String name, BasicTypeReference<?> type, String template) {
		super(
				new PatternRenderer( template ),
				null,
				new FunctionReturnTypeResolver() {
					@Override
					public ReturnableType<?> resolveFunctionReturnType(
							ReturnableType<?> impliedType,
							List<? extends SqmTypedNode<?>> arguments,
							TypeConfiguration typeConfiguration) {
						return type == null ? null : typeConfiguration.getBasicTypeRegistry().resolve( type );
					}

					@Override
					public BasicValuedMapping resolveFunctionReturnType(
							Supplier<BasicValuedMapping> impliedTypeAccess,
							List<? extends SqlAstNode> arguments) {
						return type == null || impliedTypeAccess == null ? null : impliedTypeAccess.get();
					}
				},
				name,
				FunctionKind.NORMAL
		);
	}
}
