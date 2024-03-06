/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.internal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.MappingException;
import org.hibernate.boot.model.internal.QueryHintDefinition;
import org.hibernate.boot.query.NamedProcedureCallDefinition;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.models.spi.AnnotationUsage;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.procedure.internal.NamedCallableQueryMementoImpl;
import org.hibernate.procedure.internal.Util;
import org.hibernate.procedure.spi.NamedCallableQueryMemento;
import org.hibernate.procedure.spi.ParameterStrategy;
import org.hibernate.query.results.ResultSetMapping;

import jakarta.persistence.NamedStoredProcedureQuery;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureParameter;

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

	public NamedProcedureCallDefinitionImpl(AnnotationUsage<NamedStoredProcedureQuery> annotation) {
		this.registeredName = annotation.getString( "name" );
		this.procedureName = annotation.getString( "procedureName" );
		this.hints = new QueryHintDefinition( registeredName, annotation.getList( "hints" ) ).getHintsMap();

		this.resultClasses = interpretResultClasses( annotation );
		this.resultSetMappings = interpretResultMappings( annotation );

		this.parameterDefinitions = new ParameterDefinitions( annotation.getList( "parameters" ) );

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

	private Class<?>[] interpretResultClasses(AnnotationUsage<NamedStoredProcedureQuery> annotation) {
		final List<ClassDetails> resultClassDetails = annotation.getList( "resultClasses" );
		if ( resultClassDetails == null ) {
			return null;
		}
		final Class<?>[] resultClasses = new Class<?>[resultClassDetails.size()];
		for ( int i = 0; i < resultClassDetails.size(); i++ ) {
			resultClasses[i] = resultClassDetails.get( i ).toJavaClass();
		}
		return resultClasses;
	}

	private String[] interpretResultMappings(AnnotationUsage<NamedStoredProcedureQuery> annotation) {
		final List<String> list = annotation.getList( "resultSetMappings" );
		if ( list == null ) {
			return null;
		}
		final String[] strings = new String[list.size()];
		for ( int i = 0; i < list.size(); i++ ) {
			strings[i] = list.get( i );
		}
		return strings;
	}

	@Override
	public String getRegistrationName() {
		return registeredName;
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
			Util.resolveResultSetMappingClasses(
					resultClasses,
					resultSetMapping,
					collectedQuerySpaces::add,
					() -> sessionFactory
			);
		}
		else if ( specifiesResultSetMappings ) {
			Util.resolveResultSetMappingNames(
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
		return sessionFactory
				.getFastSessionServices()
				.getJdbcValuesMappingProducerProvider()
				.buildResultSetMapping( registeredName, false, sessionFactory );
	}

	static class ParameterDefinitions {
		private final ParameterStrategy parameterStrategy;
		private final ParameterDefinition<?>[] parameterDefinitions;

		ParameterDefinitions(List<AnnotationUsage<StoredProcedureParameter>> parameters) {
			if ( CollectionHelper.isEmpty( parameters ) ) {
				parameterStrategy = ParameterStrategy.POSITIONAL;
				parameterDefinitions = new ParameterDefinition[0];
			}
			else {
				final AnnotationUsage<StoredProcedureParameter> parameterAnn = parameters.get( 0 );
				final boolean firstParameterHasName = StringHelper.isNotEmpty( parameterAnn.findAttributeValue( "name" ) );
				parameterStrategy = firstParameterHasName
						? ParameterStrategy.NAMED
						: ParameterStrategy.POSITIONAL;
				parameterDefinitions = new ParameterDefinition[ parameters.size() ];

				for ( int i = 0; i < parameters.size(); i++ ) {
					// i+1 for the position because the apis say the numbers are 1-based, not zero
					parameterDefinitions[i] = new ParameterDefinition<>(i + 1, parameters.get( i ));
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

		ParameterDefinition(int position, AnnotationUsage<StoredProcedureParameter> annotation) {
			this.position = position;
			this.name = normalize( annotation.getString( "name" ) );
			this.parameterMode = annotation.getEnum( "mode" );
			this.type = annotation.getClassDetails( "type" ).toJavaClass();
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
}
