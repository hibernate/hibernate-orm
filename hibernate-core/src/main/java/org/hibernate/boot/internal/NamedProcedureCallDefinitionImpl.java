/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.internal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.MappingException;
import org.hibernate.boot.model.internal.QueryHintDefinition;
import org.hibernate.boot.query.NamedProcedureCallDefinition;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.procedure.internal.NamedCallableQueryMementoImpl;
import org.hibernate.procedure.spi.NamedCallableQueryMemento;
import org.hibernate.procedure.spi.ParameterStrategy;
import org.hibernate.query.results.ResultSetMapping;

import jakarta.persistence.NamedStoredProcedureQuery;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureParameter;

import static org.hibernate.procedure.internal.Util.resolveResultSetMappingClasses;
import static org.hibernate.procedure.internal.Util.resolveResultSetMappingNames;
import static org.hibernate.procedure.spi.NamedCallableQueryMemento.ParameterMemento;

/**
 * Holds all the information needed from a named procedure call declaration in order to create a
 * {@link org.hibernate.procedure.internal.ProcedureCallImpl}
 *
 * @author Steve Ebersole
 *
 * @see jakarta.persistence.NamedStoredProcedureQuery
 */
public class NamedProcedureCallDefinitionImpl implements NamedProcedureCallDefinition {
	private final String registeredName;
	private final String procedureName;
	private final Class<?>[] resultClasses;
	private final String[] resultSetMappings;
	private final ParameterDefinitions parameterDefinitions;
	private final Map<String, Object> hints;

	public NamedProcedureCallDefinitionImpl(NamedStoredProcedureQuery annotation) {
		this.registeredName = annotation.name();
		this.procedureName = annotation.procedureName();
		this.hints = new QueryHintDefinition( registeredName, annotation.hints() ).getHintsMap();

		this.resultClasses = annotation.resultClasses();
		this.resultSetMappings = annotation.resultSetMappings();

		this.parameterDefinitions = new ParameterDefinitions( annotation.parameters() );

		final boolean specifiesResultClasses = resultClasses != null && resultClasses.length > 0;
		final boolean specifiesResultSetMappings = resultSetMappings != null && resultSetMappings.length > 0;

		if ( specifiesResultClasses && specifiesResultSetMappings ) {
			throw new MappingException(
					String.format(
							"NamedStoredProcedureQuery [%s] specified both resultClasses and resultSetMappings",
							registeredName
					)
			);
		}
	}

	@Override
	public String getRegistrationName() {
		return registeredName;
	}

	@Override
	public @Nullable String getLocation() {
		// not kept for now
		return null;
	}

	@Override
	public String getProcedureName() {
		return procedureName;
	}

	@Override
	public NamedCallableQueryMemento resolve(SessionFactoryImplementor sessionFactory) {
		final Set<String> collectedQuerySpaces = new HashSet<>();

		final boolean specifiesResultClasses = resultClasses != null && resultClasses.length > 0;
		final boolean specifiesResultSetMappings = resultSetMappings != null && resultSetMappings.length > 0;

		final ResultSetMapping resultSetMapping = buildResultSetMapping( registeredName, sessionFactory );

		if ( specifiesResultClasses ) {
			resolveResultSetMappingClasses(
					resultClasses,
					resultSetMapping,
					collectedQuerySpaces::add,
					() -> sessionFactory
			);
		}
		else if ( specifiesResultSetMappings ) {
			resolveResultSetMappingNames(
					resultSetMappings,
					resultSetMapping,
					collectedQuerySpaces::add,
					() -> sessionFactory
			);
		}

		return new NamedCallableQueryMementoImpl(
				getRegistrationName(),
				procedureName,
				parameterDefinitions.getParameterStrategy(),
				parameterDefinitions.toMementos( sessionFactory ),
				resultSetMappings,
				resultClasses,
				collectedQuerySpaces,
				false,
				null,
				CacheMode.IGNORE,
				FlushMode.AUTO,
				false,
				null,
				null,
				null,
				hints
		);
	}

	private ResultSetMapping buildResultSetMapping(String registeredName, SessionFactoryImplementor sessionFactory) {
		return sessionFactory.getJdbcValuesMappingProducerProvider()
				.buildResultSetMapping( registeredName, false, sessionFactory );
	}

	static class ParameterDefinitions {
		private final ParameterStrategy parameterStrategy;
		private final ParameterDefinition<?>[] parameterDefinitions;

		ParameterDefinitions(StoredProcedureParameter[] parameters) {
			if ( CollectionHelper.isEmpty( parameters ) ) {
				parameterStrategy = ParameterStrategy.POSITIONAL;
				parameterDefinitions = new ParameterDefinition[0];
			}
			else {
				final StoredProcedureParameter parameterAnn = parameters[0];
				final boolean firstParameterHasName = StringHelper.isNotEmpty( parameterAnn.name() );
				parameterStrategy = firstParameterHasName
						? ParameterStrategy.NAMED
						: ParameterStrategy.POSITIONAL;
				parameterDefinitions = new ParameterDefinition[ parameters.length ];

				for ( int i = 0; i < parameters.length; i++ ) {
					// i+1 for the position because the apis say the numbers are 1-based, not zero
					parameterDefinitions[i] = new ParameterDefinition<>(i + 1, parameters[i]);
				}
			}
		}

		public ParameterStrategy getParameterStrategy() {
			return parameterStrategy;
		}

		public List<ParameterMemento> toMementos(SessionFactoryImplementor sessionFactory) {
			final List<ParameterMemento> mementos = new ArrayList<>();
			for ( ParameterDefinition<?> definition : parameterDefinitions ) {
				mementos.add( definition.toMemento( sessionFactory ) );
			}
			return mementos;
		}
	}

	static class ParameterDefinition<T> {
		private final Integer position;
		private final String name;
		private final ParameterMode parameterMode;
		private final Class<T> type;

		ParameterDefinition(int position, StoredProcedureParameter annotation) {
			this.position = position;
			this.name = normalize( annotation.name() );
			this.parameterMode = annotation.mode();
			//noinspection unchecked
			this.type = (Class<T>) annotation.type();
		}

		public ParameterMemento toMemento(SessionFactoryImplementor sessionFactory) {
			// todo (6.0): figure out how to handle this
//			final boolean initialPassNullSetting = explicitPassNullSetting != null
//					? explicitPassNullSetting.booleanValue()
//					: sessionFactory.getSessionFactoryOptions().isProcedureParameterNullPassingEnabled();

			return new NamedCallableQueryMementoImpl.ParameterMementoImpl<>(
					position,
					name,
					parameterMode,
					type,
					sessionFactory.getTypeConfiguration().getBasicTypeForJavaType( type )
//					,initialPassNullSetting
			);
		}
	}

	private static String normalize(String name) {
		return StringHelper.isNotEmpty( name ) ? name : null;
	}

	@Override
	public Map<String, Object> getHints() {
		return hints;
	}
}
