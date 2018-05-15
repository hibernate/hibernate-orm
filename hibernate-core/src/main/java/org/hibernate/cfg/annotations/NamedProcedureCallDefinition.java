/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg.annotations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.persistence.FlushModeType;
import javax.persistence.NamedStoredProcedureQuery;
import javax.persistence.ParameterMode;
import javax.persistence.StoredProcedureParameter;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.LockOptions;
import org.hibernate.MappingException;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.jpa.QueryHints;
import org.hibernate.jpa.internal.util.FlushModeTypeHelper;
import org.hibernate.procedure.internal.ProcedureParameterImpl;
import org.hibernate.procedure.internal.Util;
import org.hibernate.procedure.spi.ParameterStrategy;
import org.hibernate.query.named.internal.NamedCallableQueryMementoImpl;
import org.hibernate.query.named.spi.NamedCallableQueryMemento;
import org.hibernate.query.named.spi.ParameterMemento;

/**
 * Holds all the information needed from a named procedure call declaration in order to create a
 * {@link org.hibernate.procedure.internal.ProcedureCallImpl}
 *
 * @author Steve Ebersole
 *
 * @see javax.persistence.NamedStoredProcedureQuery
 */
public class NamedProcedureCallDefinition {
	private final String registeredName;
	private final String procedureName;
	private final Class[] resultClasses;
	private final String[] resultSetMappings;
	private final ParameterDefinitions parameterDefinitions;
	private final Map<String, Object> hints;

	NamedProcedureCallDefinition(NamedStoredProcedureQuery annotation) {
		this.registeredName = annotation.name();
		this.procedureName = annotation.procedureName();
		this.hints = new QueryHintDefinition( annotation.hints() ).getHintsMap();
		this.resultClasses = annotation.resultClasses();
		this.resultSetMappings = annotation.resultSetMappings();

		this.parameterDefinitions = new ParameterDefinitions( annotation.parameters(), hints );

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

	public String getRegisteredName() {
		return registeredName;
	}

	public String getProcedureName() {
		return procedureName;
	}

	public NamedCallableQueryMemento toMemento(SessionFactoryImplementor sessionFactory) {

		final boolean isCacheable = isCacheable( hints, sessionFactory );

		return new NamedCallableQueryMementoImpl(
				registeredName,
				procedureName,
				parameterDefinitions.getParameterStrategy(),
				parameterDefinitions.toMementos( sessionFactory ),
				resultClasses,
				resultSetMappings,
				Collections.emptySet(),
				isCacheable,
				isCacheable ? determineCacheRegion( hints, sessionFactory ) : null,
				isCacheable ? determineCacheMode( hints, sessionFactory ) : null,
				determineFlushMode( hints, sessionFactory ),
				ConfigurationHelper.getBoolean( QueryHints.HINT_READONLY, hints, false ),
				LockOptions.NONE,
				ConfigurationHelper.getInt(
						QueryHints.SPEC_HINT_TIMEOUT,
						hints,
						ConfigurationHelper.getInt( QueryHints.HINT_TIMEOUT, hints, 0 ) * 1000
				) / 1000,
				0,
				null,
				Util.copy( hints )
		);
	}

	private boolean isCacheable(Map<String, Object> hints, SessionFactoryImplementor sessionFactory) {
		return sessionFactory.getSessionFactoryOptions().isQueryCacheEnabled()
				&& ConfigurationHelper.getBoolean( QueryHints.HINT_CACHEABLE, hints, false );
	}

	private String determineCacheRegion(Map<String, Object> hints, SessionFactoryImplementor sessionFactory) {
		assert sessionFactory.getSessionFactoryOptions().isQueryCacheEnabled();
		return ConfigurationHelper.getString( QueryHints.HINT_CACHE_REGION, hints, null );
	}

	private CacheMode determineCacheMode(Map<String, Object> hints, SessionFactoryImplementor sessionFactory) {
		assert sessionFactory.getSessionFactoryOptions().isQueryCacheEnabled();
		final Object setting = hints.get( QueryHints.HINT_CACHE_MODE );

		if ( setting != null ) {
			if ( CacheMode.class.isInstance( setting ) ) {
				return (CacheMode) setting;
			}

			final CacheMode cacheMode = CacheMode.interpretExternalSetting( setting.toString() );
			if ( cacheMode != null ) {
				return cacheMode;
			}
		}

		return CacheMode.NORMAL;
	}

	private FlushMode determineFlushMode(Map<String, Object> hints, SessionFactoryImplementor sessionFactory) {
		final Object setting = hints.get( QueryHints.HINT_FLUSH_MODE );

		if ( setting != null ) {
			if ( FlushMode.class.isInstance( setting ) ) {
				return (FlushMode) setting;
			}

			if ( FlushModeType.class.isInstance( setting ) ) {
				return FlushModeTypeHelper.getFlushMode( FlushModeType.class.cast( setting ) );
			}

			final FlushMode mode = FlushMode.interpretExternalSetting( setting.toString() );
			if ( mode != null ) {
				return mode;
			}
		}

		return FlushMode.AUTO;
	}


	static class ParameterDefinitions {
		private final ParameterStrategy parameterStrategy;
		private final ParameterDefinition[] parameterDefinitions;

		ParameterDefinitions(StoredProcedureParameter[] parameters, Map<String, Object> queryHintMap) {
			if ( parameters == null || parameters.length == 0 ) {
				parameterStrategy = ParameterStrategy.POSITIONAL;
				parameterDefinitions = new ParameterDefinition[0];
			}
			else {
				parameterStrategy = StringHelper.isNotEmpty( parameters[0].name() )
						? ParameterStrategy.NAMED
						: ParameterStrategy.POSITIONAL;
				parameterDefinitions = new ParameterDefinition[ parameters.length ];

				for ( int i = 0; i < parameters.length; i++ ) {
					parameterDefinitions[i] = ParameterDefinition.from(
							parameterStrategy,
							parameters[i],
							// i+1 for the position because the apis say the numbers are 1-based, not zero
							i+1,
							queryHintMap
					);
				}
			}
		}

		public ParameterStrategy getParameterStrategy() {
			return parameterStrategy;
		}

		public List<ParameterMemento> toMementos(SessionFactoryImplementor sessionFactory) {
			final List<ParameterMemento> mementos = new ArrayList<>();
			for ( ParameterDefinition definition : parameterDefinitions ) {
				mementos.add( definition.toMemento( sessionFactory ) );
			}
			return mementos;
		}
	}

	static class ParameterDefinition {
		private final Integer position;
		private final String name;
		private final ParameterMode parameterMode;
		private final Class type;
		private final Boolean explicitPassNullSetting;

		static ParameterDefinition from(
				ParameterStrategy parameterStrategy,
				StoredProcedureParameter parameterAnnotation,
				int adjustedPosition,
				Map<String, Object> queryHintMap) {
			// see if there was an explicit hint for this parameter in regards to NULL passing
			final Object explicitNullPassingHint;
			if ( parameterStrategy == ParameterStrategy.NAMED ) {
				explicitNullPassingHint = queryHintMap.get( AvailableSettings.PROCEDURE_NULL_PARAM_PASSING + '.' + parameterAnnotation.name() );
			}
			else {
				explicitNullPassingHint = queryHintMap.get( AvailableSettings.PROCEDURE_NULL_PARAM_PASSING + '.' + adjustedPosition );
			}

			return new ParameterDefinition(
					adjustedPosition,
					parameterAnnotation,
					interpretBoolean( explicitNullPassingHint )
			);
		}

		private static Boolean interpretBoolean(Object value) {
			if ( value == null ) {
				return null;
			}

			if ( value instanceof Boolean ) {
				return (Boolean) value;
			}

			return Boolean.valueOf( value.toString() );
		}

		ParameterDefinition(int position, StoredProcedureParameter annotation, Boolean explicitPassNullSetting) {
			this.position = position;
			this.name = normalize( annotation.name() );
			this.parameterMode = annotation.mode();
			this.type = annotation.type();
			this.explicitPassNullSetting = explicitPassNullSetting;
		}

		@SuppressWarnings({"UnnecessaryUnboxing", "unchecked"})
		public ParameterMemento toMemento(SessionFactoryImplementor sessionFactory) {
			final boolean initialPassNullSetting = explicitPassNullSetting != null
					? explicitPassNullSetting.booleanValue()
					: sessionFactory.getSessionFactoryOptions().isProcedureParameterNullPassingEnabled();

			return session -> {
				if ( name != null ) {
					return new ProcedureParameterImpl( name, parameterMode, type, null, initialPassNullSetting );
				}
				else {
					return new ProcedureParameterImpl( position, parameterMode, type, null, initialPassNullSetting );
				}
			};
		}
	}

	private static String normalize(String name) {
		return StringHelper.isNotEmpty( name ) ? name : null;
	}
}
