/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.procedure.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.Parameter;
import javax.persistence.ParameterMode;

import org.hibernate.QueryException;
import org.hibernate.engine.jdbc.env.spi.ExtractedDatabaseMetaData;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.collections.streams.StingArrayCollector;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.internal.util.collections.streams.StreamUtils;
import org.hibernate.procedure.NoSuchParameterException;
import org.hibernate.procedure.ParameterRegistration;
import org.hibernate.procedure.ParameterStrategyException;
import org.hibernate.procedure.spi.ParameterRegistrationImplementor;
import org.hibernate.procedure.spi.ParameterStrategy;
import org.hibernate.query.ParameterMetadata;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.spi.QueryParameterBinding;
import org.hibernate.query.spi.QueryParameterBindings;

/**
 * @author Steve Ebersole
 */
public class ParameterManager implements ParameterMetadata, QueryParameterBindings {
	private static final CoreMessageLogger log = CoreLogging.messageLogger( ParameterManager.class );

	private final ProcedureCallImpl procedureCall;
	private final boolean globalParameterPassNullsSetting;

	private ParameterStrategy parameterStrategy = ParameterStrategy.UNKNOWN;
	private Collection<ParameterRegistrationImplementor<?>> parameterRegistrations;


	public ParameterManager(ProcedureCallImpl procedureCall) {
		this.procedureCall = procedureCall;
		this.globalParameterPassNullsSetting = procedureCall.getProducer().getFactory().getSessionFactoryOptions().isProcedureParameterNullPassingEnabled();
	}

	public ProcedureCallImpl getProcedureCall() {
		return procedureCall;
	}

	public ParameterStrategy getParameterStrategy() {
		return parameterStrategy;
	}

	public void registerParameters(ProcedureCallMementoImpl callMemento) {
		this.parameterStrategy = callMemento.getParameterStrategy();

		if ( parameterStrategy == ParameterStrategy.UNKNOWN ) {
			parameterRegistrations = Collections.emptyList();
		}
		else {
			final List<ProcedureCallMementoImpl.ParameterMemento> parameterMementos = callMemento.getParameterDeclarations();
			if ( parameterMementos == null ) {
				// most likely a problem if ParameterStrategy != UNKNOWN...
				log.debugf(
						"ParameterStrategy was [%s] on named copy [%s], but no parameters stored",
						parameterStrategy,
						procedureCall.getProcedureName()
				);
				return;
			}

			parameterRegistrations = CollectionHelper.arrayList( parameterMementos.size() );

			for ( ProcedureCallMementoImpl.ParameterMemento parameterMemento : parameterMementos ) {
				register( toProcedureParameter( parameterMemento ) );
			}
		}
	}

	@SuppressWarnings("unchecked")
	private ProcedureParameterImpl toProcedureParameter(ProcedureCallMementoImpl.ParameterMemento parameterMemento) {
		ProcedureParameterImpl queryParameter;
		if ( StringHelper.isNotEmpty( parameterMemento.getName() ) ) {
			if ( parameterStrategy != ParameterStrategy.NAMED ) {
				throw new IllegalStateException(
						"Found named stored procedure parameter associated with positional parameters"
				);
			}
			queryParameter = new ProcedureParameterImpl(
					parameterMemento.getName(),
					parameterMemento.getMode(),
					parameterMemento.getType(),
					null,//parameterMemento.getHibernateType(),
					parameterMemento.isPassNullsEnabled()
			);
		}
		else {
			if ( parameterStrategy != ParameterStrategy.POSITIONAL ) {
				throw new IllegalStateException(
						"Found named stored procedure parameter associated with positional parameters"
				);
			}
			queryParameter = new ProcedureParameterImpl(
					parameterMemento.getPosition(),
					parameterMemento.getMode(),
					parameterMemento.getType(),
					null,//parameterMemento.getHibernateType(),
					parameterMemento.isPassNullsEnabled()
			);
		}
		return queryParameter;
	}

	private <T> ParameterRegistrationImplementor<T> register(ProcedureParameterImpl<T> queryParameter) {
		if ( StringHelper.isNotEmpty( queryParameter.getName() ) ) {
			prepareForNamedParameters();
		}
		else if ( queryParameter.getPosition() != null ) {
			prepareForPositionalParameters();
		}
		else {
			throw new IllegalArgumentException( "Given parameter did not define name or position [" + queryParameter + "]" );
		}

		if ( parameterRegistrations == null ) {
			parameterRegistrations = new ArrayList<>();
		}

		final ParameterRegistrationImplementor<T> registration = new ParameterRegistrationStandardImpl<>( this, queryParameter );
		parameterRegistrations.add( registration );

		return registration;
	}

	private void prepareForPositionalParameters() {
		if ( parameterStrategy == ParameterStrategy.NAMED ) {
			throw new QueryException( "Cannot mix named and positional parameters" );
		}
		parameterStrategy = ParameterStrategy.POSITIONAL;
	}

	private void prepareForNamedParameters() {
		if ( parameterStrategy == ParameterStrategy.POSITIONAL ) {
			throw new QueryException( "Cannot mix named and positional parameters" );
		}
		if ( parameterStrategy == ParameterStrategy.UNKNOWN ) {
			// protect to only do this check once
			// todo : this also depends on our assumption that ProcedureCall / StoredProcedureQuery named parameters correlate directly to JDBC named parameters.
			final ExtractedDatabaseMetaData databaseMetaData = procedureCall.getProducer()
					.getJdbcServices()
					.getJdbcEnvironment()
					.getExtractedDatabaseMetaData();
			if ( ! databaseMetaData.supportsNamedParameters() ) {
				log.unsupportedNamedParameters();
			}
			parameterStrategy = ParameterStrategy.NAMED;
		}
	}

	@SuppressWarnings("unchecked")
	public ParameterRegistrationImplementor registerParameter(String name, ParameterMode mode, Class javaType) {
		return register(
				new ProcedureParameterImpl(
						name,
						mode,
						javaType,
						null,
						globalParameterPassNullsSetting
				)
		);
	}

	@SuppressWarnings("unchecked")
	public ParameterRegistrationImplementor registerParameter(Integer position, ParameterMode mode, Class javaType) {
		return register(
				new ProcedureParameterImpl(
						position,
						mode,
						javaType,
						null,
						globalParameterPassNullsSetting
				)
		);
	}

	@Override
	public boolean hasNamedParameters() {
		return parameterStrategy == ParameterStrategy.NAMED;
	}

	@Override
	public boolean hasPositionalParameters() {
		return parameterStrategy == ParameterStrategy.POSITIONAL;
	}

	@Override
	public int getNamedParameterCount() {
		return hasNamedParameters() ? parameterRegistrations.size() : 0;
	}

	@Override
	public int getPositionalParameterCount() {
		return hasPositionalParameters() ? parameterRegistrations.size() : 0;
	}

	@Override
	public Set<String> getNamedParameterNames() {
		return hasNamedParameters()
				? collectAllParametersJpa().stream().map( Parameter::getName ).collect( Collectors.toSet() )
				: Collections.emptySet();
	}

	@Override
	public Set<QueryParameter<?>> collectAllParameters() {
		if ( parameterRegistrations.size() == 0 ) {
			return Collections.emptySet();
		}
		return parameterRegistrations.stream().collect( Collectors.toSet() );
	}

	@Override
	public Set<Parameter<?>> collectAllParametersJpa() {
		if ( parameterRegistrations.size() == 0 ) {
			return Collections.emptySet();
		}
		return parameterRegistrations.stream().collect( Collectors.toSet() );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> ParameterRegistrationImplementor<T> getQueryParameter(String name) {
		if ( parameterStrategy != ParameterStrategy.NAMED ) {
			throw new ParameterStrategyException( "Names were not used to register parameters with this stored procedure call" );
		}

		for ( ParameterRegistrationImplementor parameter : parameterRegistrations ) {
			if ( name.equals( parameter.getName() ) ) {
				return parameter;
			}
		}
		throw new NoSuchParameterException( "Could not locate parameter registered under that name [" + name + "]" );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> ParameterRegistrationImplementor<T> getQueryParameter(int position) {
		if ( parameterStrategy != ParameterStrategy.POSITIONAL ) {
			throw new ParameterStrategyException( "Positions were not used to register parameters with this stored procedure call" );
		}

		for ( ParameterRegistrationImplementor parameter : parameterRegistrations ) {
			if ( parameter.getPosition() != null && parameter.getPosition() == position ) {
				return parameter;
			}
		}

		throw new NoSuchParameterException( "Could not locate parameter registered under that position [" + position + "]" );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> ParameterRegistrationImplementor<T> resolve(Parameter<T> param) {
		if ( param instanceof ParameterRegistrationImplementor ) {
			final ParameterRegistrationImplementor registration = (ParameterRegistrationImplementor) param;
			if ( procedureCall.equals( registration.getProcedureCall() ) ) {
				return registration;
			}
		}

		if ( param.getName() != null ) {
			if ( param.getPosition() != null ) {
				log.debugf( "Parameter passed to org.hibernate.query.ParameterMetadata.resolve defined both a name and a position; assuming named resolution" );
			}
			return getQueryParameter( param.getName() );
		}

		if ( param.getPosition() != null ) {
			return getQueryParameter( param.getPosition() );
		}

		throw new NoSuchParameterException( "Could not resolve Parameter reference [" + param + "] to registered QueryParameter" );
	}


	@Override
	public boolean isBound(QueryParameter parameter) {
		if ( parameter.getMode() == ParameterMode.OUT || parameter.getMode() == ParameterMode.REF_CURSOR ) {
			return false;
		}

		final QueryParameterBinding binding = getBinding( parameter );
		return binding != null && binding.isBound();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> QueryParameterBinding<T> getBinding(QueryParameter<T> parameter) {
		if ( parameter == null ) {
			throw new IllegalArgumentException( "Passed parameter cannot be null" );
		}

		if ( parameter.getMode() == ParameterMode.IN || parameter.getMode() == ParameterMode.INOUT ) {
			throw new IllegalArgumentException( "Parameter is not an input param, it has no binding" );
		}

		return (QueryParameterBinding<T>) resolve( parameter ).getBind();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> QueryParameterBinding<T> getBinding(String name) {
		if ( parameterStrategy != ParameterStrategy.NAMED ) {
			throw new ParameterStrategyException( "Names were not used to register parameters with this stored procedure call" );
		}

		final ParameterRegistration<Object> registration = getQueryParameter( name );

		if ( registration == null ) {
			throw new NoSuchParameterException( "Could not locate parameter registered under that name [" + name + "]" );
		}

		if ( registration.getMode() == ParameterMode.IN || registration.getMode() == ParameterMode.INOUT ) {
			throw new IllegalArgumentException( "Parameter is not an input param, it has no binding" );
		}

		return (QueryParameterBinding<T>) registration.getBind();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> QueryParameterBinding<T> getBinding(int position) {
		if ( parameterStrategy != ParameterStrategy.POSITIONAL ) {
			throw new ParameterStrategyException( "Positions were not used to register parameters with this stored procedure call" );
		}

		final ParameterRegistration<Object> registration = getQueryParameter( position );

		if ( registration == null ) {
			throw new NoSuchParameterException( "Could not locate parameter registered under that position [" + position + "]" );
		}

		if ( registration.getMode() == ParameterMode.IN || registration.getMode() == ParameterMode.INOUT ) {
			throw new IllegalArgumentException( "Parameter is not an input param, it has no binding" );
		}

		return (QueryParameterBinding<T>) registration.getBind();
	}

	@Override
	public void validate() {

	}

	public <T> ParameterBindImpl<T> makeBinding(QueryParameter<T> parameter) {
		assert parameter.getMode() == ParameterMode.IN || parameter.getMode() == ParameterMode.INOUT;

		return new ParameterBindImpl<>(
				parameter.getHibernateType(),
				parameter,
				procedureCall.getProducer().getFactory()
		);
	}

	public List<ParameterRegistration> collectParameterRegistrations() {
		return parameterRegistrations.stream().collect( Collectors.toList() );
	}

	public List<ProcedureCallMementoImpl.ParameterMemento> toParameterMementos() {
		if ( parameterRegistrations == null || parameterRegistrations.isEmpty() ) {
			return null;
		}

		return parameterRegistrations.stream()
				.map( ProcedureCallMementoImpl.ParameterMemento::fromRegistration )
				.collect( Collectors.toList() );
	}

	public ParameterRegistrationImplementor[] collectRefCursorParameters() {
		// todo : there is probably a better way to collect these as we walk each registration to prepare them...

		return parameterRegistrations.stream()
				.filter( param -> param.getMode() == ParameterMode.REF_CURSOR )
				.collect( StreamUtils.toArray( ParameterRegistrationImplementor.class ) );
	}
}
