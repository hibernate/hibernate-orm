/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.model.resultset.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.Metamodel;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.boot.model.resultset.spi.ResultSetMappingDefinition;
import org.hibernate.query.sql.spi.QueryResultBuilder;

/**
 * @author Steve Ebersole
 */
public class InstantiationResultDefinitionImpl implements ResultSetMappingDefinition.InstantiationResult {
	private final String targetName;

	private List<Argument> arguments;

	@Override
	public String getTargetName() {
		return targetName;
	}


	@Override
	public List<Argument> getArguments() {
		return arguments == null ? Collections.emptyList() : arguments;
	}

	public InstantiationResultDefinitionImpl(String targetName) {
		this.targetName = targetName;
	}

	public void addArgument(Argument argument) {
		if ( arguments == null ) {
			arguments = new ArrayList<>();
		}
		arguments.add( argument );
	}

	@Override
	public QueryResultBuilder generateQueryResultBuilder(Metamodel metamodel) {
		throw new NotYetImplementedFor6Exception();
	}

	public static class ArgumentImpl implements Argument {
		private final ResultSetMappingDefinition.Result result;
		private final String alias;

		public ArgumentImpl(ResultSetMappingDefinition.Result result, String alias) {
			this.result = result;
			this.alias = alias;
		}

		@Override
		public ResultSetMappingDefinition.Result getResult() {
			return result;
		}

		@Override
		public String getAlias() {
			return alias;
		}
	}
}
