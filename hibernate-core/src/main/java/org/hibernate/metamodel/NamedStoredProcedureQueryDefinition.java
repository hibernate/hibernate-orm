/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.metamodel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.ParameterMode;

import org.hibernate.MappingException;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.procedure.spi.ParameterStrategy;

/**
 * Models the information about a {@link javax.persistence.NamedStoredProcedureQuery}
 *
 * @author Steve Ebersole
 */
public class NamedStoredProcedureQueryDefinition {
	private final String name;
	private final String procedureName;

	private final ParameterStrategy parameterStrategy;
	private final List<Parameter> parameters;

	private final Map<String,Object> queryHints;

	private final List<String> classNames;
	private final List<String> resultSetMappingNames;

	private NamedStoredProcedureQueryDefinition(
			String name,
			String procedureName,
			ParameterStrategy parameterStrategy,
			List<Parameter> parameters,
			Map<String, Object> queryHints,
			List<String> classNames,
			List<String> resultSetMappingNames) {
		if ( name == null ) {
			throw new  IllegalArgumentException( "Name cannot be null" );
		}
		if ( procedureName == null ) {
			throw new  IllegalArgumentException( "Procedure-name cannot be null" );
		}
		final boolean specifiesResultClasses = classNames.size() > 0;
		final boolean specifiesResultSetMappings = resultSetMappingNames.size() > 0;

		if ( specifiesResultClasses && specifiesResultSetMappings ) {
			throw new MappingException(
					String.format(
							"NamedStoredProcedureQuery [" + name + "] specified both resultClasses and resultSetMappings",
							name
					)
			);
		}

		this.name = name;
		this.procedureName = procedureName;
		this.parameterStrategy = parameterStrategy;
		this.parameters = Collections.unmodifiableList( parameters );
		this.queryHints = Collections.unmodifiableMap( queryHints );
		this.classNames = Collections.unmodifiableList( classNames );
		this.resultSetMappingNames = Collections.unmodifiableList( resultSetMappingNames );
	}

	public String getName() {
		return name;
	}

	public String getProcedureName() {
		return procedureName;
	}

	public ParameterStrategy getParameterStrategy() {
		return parameterStrategy;
	}

	public List<Parameter> getParameters() {
		return parameters;
	}

	public Map<String, Object> getQueryHints() {
		return queryHints;
	}

	public List<String> getClassNames() {
		return classNames;
	}

	public List<String> getResultSetMappingNames() {
		return resultSetMappingNames;
	}

	public static class Parameter {
		private final String name;
		private final ParameterMode mode;
		private final String javaType;

		private Parameter(String name, ParameterMode mode, String javaType) {
			this.name = name;
			this.mode = mode;
			this.javaType = javaType;
		}

		public String getName() {
			return name;
		}

		public ParameterMode getMode() {
			return mode;
		}

		public String getJavaType() {
			return javaType;
		}
	}

	public static class Builder {
		private final String name;
		private final String procedureName;
		private final List<Parameter> parameters = new ArrayList<Parameter>();
		private final Map<String, Object> queryHints = new HashMap<String, Object>();
		private final List<String> classNames = new ArrayList<String>();
		private final List<String> resultSetMappingNames = new ArrayList<String>();

		private ParameterStrategy parameterStrategy = ParameterStrategy.UNKNOWN;

		public Builder(String name, String procedureName) {
			this.name = name;
			this.procedureName = procedureName;
		}

		public void addParameter(String name, ParameterMode mode, String javaType) {
			final ParameterStrategy incomingParameterStrategy;
			if ( StringHelper.isNotEmpty( name ) ) {
				incomingParameterStrategy = ParameterStrategy.NAMED;
			}
			else {
				incomingParameterStrategy = ParameterStrategy.POSITIONAL;
			}

			if ( parameterStrategy == ParameterStrategy.UNKNOWN ) {
				parameterStrategy = incomingParameterStrategy;
			}
			else {
				if ( parameterStrategy != incomingParameterStrategy ) {
					throw new IllegalArgumentException(
							"Attempt to mix named and position parameters for " +
									"@NamedStoredProcedureQuery(name=" + name + ")"
					);
				}
			}
			parameters.add( new Parameter( name, mode, javaType ) );
		}

		public void addHint(String hintKey, String value) {
			queryHints.put( hintKey, value );
		}

		public void addResultClassName(String name) {
			classNames.add( name );
		}

		public void addResultSetMappingName(String name) {
			resultSetMappingNames.add( name );
		}

		public NamedStoredProcedureQueryDefinition buildDefinition() {
			return new NamedStoredProcedureQueryDefinition(
					name,
					procedureName,
					parameterStrategy,
					parameters,
					queryHints,
					classNames,
					resultSetMappingNames
			);
		}
	}

}
