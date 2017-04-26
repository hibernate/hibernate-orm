/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.select;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.domain.SqmExpressableType;
import org.hibernate.query.sqm.domain.type.SqmDomainType;
import org.hibernate.query.sqm.tree.expression.SqmExpression;

import org.jboss.logging.Logger;

import static org.hibernate.query.sqm.tree.select.SqmDynamicInstantiationTarget.Nature.CLASS;
import static org.hibernate.query.sqm.tree.select.SqmDynamicInstantiationTarget.Nature.LIST;
import static org.hibernate.query.sqm.tree.select.SqmDynamicInstantiationTarget.Nature.MAP;

/**
 * Represents a dynamic instantiation ({@code select new XYZ(...) ...}) as part of the SQM.
 *
 * @author Steve Ebersole
 */
public class SqmDynamicInstantiation
		implements SqmExpression, SqmAliasedExpressionContainer<SqmDynamicInstantiationArgument> {
	private static final Logger log = Logger.getLogger( SqmDynamicInstantiation.class );

	public static SqmDynamicInstantiation forClassInstantiation(Class targetJavaType) {
		return new SqmDynamicInstantiation(
				new DynamicInstantiationTargetImpl( CLASS, targetJavaType )
		);
	}

	public static SqmDynamicInstantiation forMapInstantiation() {
		return new SqmDynamicInstantiation( new DynamicInstantiationTargetImpl( MAP, Map.class ) );
	}

	public static SqmDynamicInstantiation forListInstantiation() {
		return new SqmDynamicInstantiation( new DynamicInstantiationTargetImpl( LIST, List.class ) );
	}

	private final SqmDynamicInstantiationTarget instantiationTarget;
	private List<SqmDynamicInstantiationArgument> arguments;

	private SqmDynamicInstantiation(SqmDynamicInstantiationTarget instantiationTarget) {
		this.instantiationTarget = instantiationTarget;
	}

	@Override
	public String asLoggableText() {
		return "<new " + instantiationTarget.getJavaType().getName() + ">";
	}

	@Override
	public SqmExpressableType getExpressionType() {
		return null;
	}

	@Override
	public SqmExpressableType getInferableType() {
		return null;
	}

	public SqmDynamicInstantiationTarget getInstantiationTarget() {
		return instantiationTarget;
	}

	public List<SqmDynamicInstantiationArgument> getArguments() {
		return arguments;
	}

	public void addArgument(SqmDynamicInstantiationArgument argument) {
		if ( instantiationTarget.getNature() == LIST ) {
			// really should not have an alias...
			if ( argument.getAlias() != null ) {
				log.debugf(
						"Argument [%s] for dynamic List instantiation declared an 'injection alias' [%s] " +
								"but such aliases are ignored for dynamic List instantiations",
						argument.getExpression().toString(),
						argument.getAlias()
				);
			}
		}
		else if ( instantiationTarget.getNature() == MAP ) {
			// must(?) have an alias...
			log.warnf(
					"Argument [%s] for dynamic Map instantiation did not declare an 'injection alias' [%s] " +
							"but such aliases are needed for dynamic Map instantiations; " +
							"will likely cause problems later translating sqm",
					argument.getExpression().toString(),
					argument.getAlias()
			);
		}

		if ( arguments == null ) {
			arguments = new ArrayList<>();
		}
		arguments.add( argument );
	}

	@Override
	public SqmDynamicInstantiationArgument add(SqmExpression expression, String alias) {
		SqmDynamicInstantiationArgument argument = new SqmDynamicInstantiationArgument( expression, alias );
		addArgument( argument );
		return argument;
	}

	@Override
	public void add(SqmDynamicInstantiationArgument aliasExpression) {
		addArgument( aliasExpression );
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitDynamicInstantiation( this );
	}

	public SqmDynamicInstantiation makeShallowCopy() {
		return new SqmDynamicInstantiation( getInstantiationTarget() );
	}

	@Override
	public SqmDomainType getExportedDomainType() {
		return null;
	}

	private static class DynamicInstantiationTargetImpl implements SqmDynamicInstantiationTarget {
		private final Nature nature;
		private final Class javaType;;


		public DynamicInstantiationTargetImpl(Nature nature, Class javaType) {
			this.nature = nature;
			this.javaType = javaType;
		}

		@Override
		public Nature getNature() {
			return nature;
		}

		@Override
		public Class getJavaType() {
			return javaType;
		}
	}
}
