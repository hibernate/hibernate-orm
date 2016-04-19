/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.procedure.internal;

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.Parameter;
import javax.persistence.ParameterMode;
import javax.persistence.TemporalType;
import javax.persistence.TransactionRequiredException;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.jpa.TypedParameterValue;
import org.hibernate.procedure.NoSuchParameterException;
import org.hibernate.procedure.ParameterStrategyException;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.procedure.ProcedureCallMemento;
import org.hibernate.procedure.ProcedureOutputs;
import org.hibernate.procedure.spi.ParameterRegistrationImplementor;
import org.hibernate.query.ParameterMetadata;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.internal.AbstractProducedQuery;
import org.hibernate.query.procedure.spi.ProcedureParameterImplementor;
import org.hibernate.query.procedure.spi.StoredProcedureQueryImplementor;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.result.NoMoreReturnsException;
import org.hibernate.result.Output;
import org.hibernate.result.ResultSetOutput;
import org.hibernate.result.UpdateCountOutput;
import org.hibernate.type.Type;

/**
 * Defines support for JPA-defined stored-procedure execution.  This is the one Query form
 * that still uses wrapping as there are very fundamental differences between Hibernate's
 * native procedure support and JPA's.
 *
 * @author Steve Ebersole
 */
public class StoredProcedureQueryImpl extends AbstractProducedQuery<Object> implements StoredProcedureQueryImplementor {
	private final ProcedureCall procedureCall;
	private ProcedureOutputs procedureResult;

	private ProcedureParameterMetadata parameterMetadata = new ProcedureParameterMetadata();
	private ProcedureParameterBindings parameterBindings = new ProcedureParameterBindings();

	public StoredProcedureQueryImpl(ProcedureCall procedureCall, SessionImplementor session) {
		super( session, null );
		this.procedureCall = procedureCall;
	}

	/**
	 * This form is used to build a StoredProcedureQueryImpl from a memento (usually from a NamedStoredProcedureQuery).
	 *
	 * @param memento The memento
	 * @param entityManager The EntityManager
	 */
	@SuppressWarnings("unchecked")
	public StoredProcedureQueryImpl(ProcedureCallMemento memento, SessionImplementor entityManager) {
		super( entityManager, null );
		this.procedureCall = memento.makeProcedureCall( entityManager.getSession() );
		for ( org.hibernate.procedure.ParameterRegistration nativeParamReg : procedureCall.getRegisteredParameters() ) {
			registerParameter( (ParameterRegistrationImplementor) nativeParamReg );
		}
	}

	private <T> void registerParameter(ParameterRegistrationImplementor<T> nativeParamRegistration) {
		registerParameter( new ProcedureParameterImpl<>( nativeParamRegistration ) );
	}

	private <T> void registerParameter(ProcedureParameterImplementor<T> parameter) {
		parameterMetadata.registerParameter( parameter );
		parameterBindings.registerParameter( parameter );
	}

	@Override
	@SuppressWarnings("unchecked")
	public StoredProcedureQueryImplementor registerStoredProcedureParameter(int position, Class type, ParameterMode mode) {
		getProducer().checkOpen( true );

		try {
			registerParameter( (ParameterRegistrationImplementor) procedureCall.registerParameter( position, type, mode ) );
		}
		catch (HibernateException he) {
			throw getProducer().convert( he );
		}
		catch (RuntimeException e) {
			getProducer().markForRollbackOnly();
			throw e;
		}

		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public StoredProcedureQueryImplementor registerStoredProcedureParameter(String parameterName, Class type, ParameterMode mode) {
		getProducer().checkOpen( true );

		try {
			registerParameter( (ParameterRegistrationImplementor) procedureCall.registerParameter( parameterName, type, mode ) );
		}
		catch (HibernateException he) {
			throw getProducer().convert( he );
		}
		catch (RuntimeException e) {
			getProducer().markForRollbackOnly();
			throw e;
		}

		return this;
	}

	@Override
	public SessionImplementor getProducer() {
		return (SessionImplementor) super.getProducer();
	}

	@Override
	public ProcedureCall getHibernateProcedureCall() {
		return procedureCall;
	}

	@Override
	public String getQueryString() {
		return procedureCall.toString();
	}

	@Override
	public Type[] getReturnTypes() {
		return procedureCall.getReturnTypes();
	}

	@Override
	protected boolean applyTimeoutHint(int timeout) {
		procedureCall.setTimeout( timeout );
		return true;
	}

	@Override
	protected boolean applyCacheableHint(boolean isCacheable) {
		procedureCall.setCacheable( isCacheable );
		return true;
	}

	@Override
	protected boolean applyCacheRegionHint(String regionName) {
		procedureCall.setCacheRegion( regionName );
		return true;
	}

	@Override
	protected boolean applyReadOnlyHint(boolean isReadOnly) {
		procedureCall.setReadOnly( isReadOnly );
		return true;
	}

	@Override
	protected boolean applyCacheModeHint(CacheMode cacheMode) {
		procedureCall.setCacheMode( cacheMode );
		return true;
	}

	@Override
	protected boolean applyFlushModeHint(FlushMode flushMode) {
		procedureCall.setHibernateFlushMode( flushMode );
		return true;
	}



	// outputs ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public boolean execute() {
		try {
			final Output rtn = outputs().getCurrent();
			return rtn != null && ResultSetOutput.class.isInstance( rtn );
		}
		catch (NoMoreReturnsException e) {
			return false;
		}
		catch (HibernateException he) {
			throw getProducer().convert( he );
		}
		catch (RuntimeException e) {
			getProducer().markForRollbackOnly();
			throw e;
		}
	}

	protected ProcedureOutputs outputs() {
		if ( procedureResult == null ) {
			procedureResult = procedureCall.getOutputs();
		}
		return procedureResult;
	}

	@Override
	public int executeUpdate() {
		if ( ! getProducer().isTransactionInProgress() ) {
			throw new TransactionRequiredException( "javax.persistence.Query.executeUpdate requires active transaction" );
		}

		// the expectation is that there is just one Output, of type UpdateCountOutput
		try {
			execute();
			return getUpdateCount();
		}
		finally {
			outputs().release();
		}
	}

	@Override
	public Object getOutputParameterValue(int position) {
		// NOTE : according to spec (specifically), an exception thrown from this method should not mark for rollback.
		try {
			return outputs().getOutputParameterValue( position );
		}
		catch (ParameterStrategyException e) {
			throw new IllegalArgumentException( "Invalid mix of named and positional parameters", e );
		}
		catch (NoSuchParameterException e) {
			throw new IllegalArgumentException( e.getMessage(), e );
		}
	}

	@Override
	public Object getOutputParameterValue(String parameterName) {
		// NOTE : according to spec (specifically), an exception thrown from this method should not mark for rollback.
		try {
			return outputs().getOutputParameterValue( parameterName );
		}
		catch (ParameterStrategyException e) {
			throw new IllegalArgumentException( "Invalid mix of named and positional parameters", e );
		}
		catch (NoSuchParameterException e) {
			throw new IllegalArgumentException( e.getMessage(), e );
		}
	}

	@Override
	public boolean hasMoreResults() {
		return outputs().goToNext() && ResultSetOutput.class.isInstance( outputs().getCurrent() );
	}

	@Override
	public int getUpdateCount() {
		try {
			final Output rtn = outputs().getCurrent();
			if ( rtn == null ) {
				return -1;
			}
			else if ( UpdateCountOutput.class.isInstance( rtn ) ) {
				return ( (UpdateCountOutput) rtn ).getUpdateCount();
			}
			else {
				return -1;
			}
		}
		catch (NoMoreReturnsException e) {
			return -1;
		}
		catch (HibernateException he) {
			throw getProducer().convert( he );
		}
		catch (RuntimeException e) {
			getProducer().markForRollbackOnly();
			throw e;
		}
	}


	@Override
	public List getResultList() {
		try {
			final Output rtn = outputs().getCurrent();
			if ( ! ResultSetOutput.class.isInstance( rtn ) ) {
				throw new IllegalStateException( "Current CallableStatement ou was not a ResultSet, but getResultList was called" );
			}

			return ( (ResultSetOutput) rtn ).getResultList();
		}
		catch (NoMoreReturnsException e) {
			// todo : the spec is completely silent on these type of edge-case scenarios.
			// Essentially here we'd have a case where there are no more results (ResultSets nor updateCount) but
			// getResultList was called.
			return null;
		}
		catch (HibernateException he) {
			throw getProducer().convert( he );
		}
		catch (RuntimeException e) {
			getProducer().markForRollbackOnly();
			throw e;
		}
	}

	@Override
	public Object getSingleResult() {
		final List resultList = getResultList();
		if ( resultList == null || resultList.isEmpty() ) {
			throw new NoResultException(
					String.format(
							"Call to stored procedure [%s] returned no results",
							procedureCall.getProcedureName()
					)
			);
		}
		else if ( resultList.size() > 1 ) {
			throw new NonUniqueResultException(
					String.format(
							"Call to stored procedure [%s] returned multiple results",
							procedureCall.getProcedureName()
					)
			);
		}

		return resultList.get( 0 );
	}

	@Override
	public Object unwrap(Class cls) {
		if ( ProcedureCall.class.isAssignableFrom( cls ) ) {
			return procedureCall;
		}
		else if ( ProcedureOutputs.class.isAssignableFrom( cls ) ) {
			return outputs();
		}
		else if ( ParameterMetadata.class.isAssignableFrom( cls ) ) {
			return parameterMetadata;
		}
		else if ( QueryParameterBindings.class.isAssignableFrom( cls ) ) {
			return parameterBindings;
		}

		return super.unwrap( cls );
	}

	@Override
	public StoredProcedureQueryImpl setLockMode(LockModeType lockMode) {
		throw new IllegalStateException( "javax.persistence.Query.setLockMode not valid on javax.persistence.StoredProcedureQuery" );
	}

	@Override
	public LockModeType getLockMode() {
		throw new IllegalStateException( "javax.persistence.Query.getHibernateFlushMode not valid on javax.persistence.StoredProcedureQuery" );
	}


	// unsupported hints/calls ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~


	@Override
	protected boolean canApplyAliasSpecificLockModeHints() {
		return false;
	}

	@Override
	protected void applyAliasSpecificLockModeHint(String alias, LockMode lockMode) {
		throw new UnsupportedOperationException( "Applying alias-specific LockModes is not available for callable queries" );
	}

	@Override
	protected boolean isNativeQuery() {
		return false;
	}

	@Override
	protected boolean applyLockTimeoutHint(int timeout) {
		return false;
	}

	@Override
	protected boolean applyCommentHint(String comment) {
		return false;
	}

	@Override
	protected boolean applyFetchSizeHint(int fetchSize) {
		return false;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// covariant returns


	@Override
	@SuppressWarnings("unchecked")
	public StoredProcedureQueryImplementor setParameter(QueryParameter parameter, Object value) {
		super.setParameter( parameter, value );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public StoredProcedureQueryImplementor setParameter(Parameter parameter, Object value) {
		super.setParameter( parameter, value );
		return this;
	}

	@Override
	public StoredProcedureQueryImplementor setParameter(Parameter param, Calendar value, TemporalType temporalType) {
		super.setParameter( param, value, temporalType );
		return this;
	}

	@Override
	public StoredProcedureQueryImplementor setParameter(Parameter param, Date value, TemporalType temporalType) {
		super.setParameter( param, value, temporalType );
		return this;
	}

	@Override
	public StoredProcedureQueryImplementor setParameter(String name, Object value) {
		super.setParameter( name, value );
		return this;
	}

	@Override
	public StoredProcedureQueryImplementor setParameter(String name, Calendar value, TemporalType temporalType) {
		super.setParameter( name, value, temporalType );
		return this;
	}

	@Override
	public StoredProcedureQueryImplementor setParameter(String name, Date value, TemporalType temporalType) {
		super.setParameter( name, value, temporalType );
		return this;
	}

	@Override
	public StoredProcedureQueryImplementor setParameter(int position, Object value) {
		super.setParameter( position, value );
		return this;
	}

	@Override
	public StoredProcedureQueryImplementor setEntity(int position, Object val) {
		setParameter( position, val, getProducer().getFactory().getTypeHelper().entity( resolveEntityName( val ) ) );
		return this;
	}

	@Override
	public StoredProcedureQueryImplementor setEntity(String name, Object val) {
		setParameter( name, val, getProducer().getFactory().getTypeHelper().entity( resolveEntityName( val ) ) );
		return this;
	}

	@Override
	public StoredProcedureQueryImplementor setParameter(int position, Calendar value, TemporalType temporalType) {
		super.setParameter( position, value, temporalType );
		return this;
	}

	@Override
	public StoredProcedureQueryImplementor setParameter(int position, Date value, TemporalType temporalType) {
		super.setParameter( position, value, temporalType );
		return this;
	}

	@Override
	public StoredProcedureQueryImplementor setFlushMode(FlushModeType jpaFlushMode) {
		super.setFlushMode( jpaFlushMode );
		return this;
	}

	@Override
	public StoredProcedureQueryImplementor setHint(String hintName, Object value) {
		super.setHint( hintName, value );
		return this;
	}

	@Override
	public StoredProcedureQueryImplementor setFirstResult(int startPosition) {
		super.setFirstResult( startPosition );
		return this;
	}

	@Override
	public StoredProcedureQueryImplementor setMaxResults(int maxResult) {
		super.setMaxResults( maxResult );
		return this;
	}

	// parameters ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~


	protected static void validateBinding(Class parameterType, Object bind, TemporalType temporalType) {
		if ( bind == null || parameterType == null ) {
			// nothing we can check
			return;
		}

		if ( bind instanceof TypedParameterValue ) {
			bind = ( (TypedParameterValue ) bind ).getValue();
		}

		if ( Collection.class.isInstance( bind ) && ! Collection.class.isAssignableFrom( parameterType ) ) {
			// we have a collection passed in where we are expecting a non-collection.
			// 		NOTE : this can happen in Hibernate's notion of "parameter list" binding
			// 		NOTE2 : the case of a collection value and an expected collection (if that can even happen)
			//			will fall through to the main check.
			validateCollectionValuedParameterBinding( parameterType, (Collection) bind, temporalType );
		}
		else if ( bind.getClass().isArray() ) {
			validateArrayValuedParameterBinding( parameterType, bind, temporalType );
		}
		else {
			if ( ! isValidBindValue( parameterType, bind, temporalType ) ) {
				throw new IllegalArgumentException(
						String.format(
								"Parameter value [%s] did not match expected type [%s (%s)]",
								bind,
								parameterType.getName(),
								extractName( temporalType )
						)
				);
			}
		}
	}

	private static String extractName(TemporalType temporalType) {
		return temporalType == null ? "n/a" : temporalType.name();
	}

	private static void validateCollectionValuedParameterBinding(
			Class parameterType,
			Collection value,
			TemporalType temporalType) {
		// validate the elements...
		for ( Object element : value ) {
			if ( ! isValidBindValue( parameterType, element, temporalType ) ) {
				throw new IllegalArgumentException(
						String.format(
								"Parameter value element [%s] did not match expected type [%s (%s)]",
								element,
								parameterType.getName(),
								extractName( temporalType )
						)
				);
			}
		}
	}

	private static void validateArrayValuedParameterBinding(
			Class parameterType,
			Object value,
			TemporalType temporalType) {
		if ( ! parameterType.isArray() ) {
			throw new IllegalArgumentException(
					String.format(
							"Encountered array-valued parameter binding, but was expecting [%s (%s)]",
							parameterType.getName(),
							extractName( temporalType )
					)
			);
		}

		if ( value.getClass().getComponentType().isPrimitive() ) {
			// we have a primitive array.  we validate that the actual array has the component type (type of elements)
			// we expect based on the component type of the parameter specification
			if ( ! parameterType.getComponentType().isAssignableFrom( value.getClass().getComponentType() ) ) {
				throw new IllegalArgumentException(
						String.format(
								"Primitive array-valued parameter bind value type [%s] did not match expected type [%s (%s)]",
								value.getClass().getComponentType().getName(),
								parameterType.getName(),
								extractName( temporalType )
						)
				);
			}
		}
		else {
			// we have an object array.  Here we loop over the array and physically check each element against
			// the type we expect based on the component type of the parameter specification
			final Object[] array = (Object[]) value;
			for ( Object element : array ) {
				if ( ! isValidBindValue( parameterType.getComponentType(), element, temporalType ) ) {
					throw new IllegalArgumentException(
							String.format(
									"Array-valued parameter value element [%s] did not match expected type [%s (%s)]",
									element,
									parameterType.getName(),
									extractName( temporalType )
							)
					);
				}
			}
		}
	}


	private static boolean isValidBindValue(Class expectedType, Object value, TemporalType temporalType) {
		if ( expectedType.isInstance( value ) ) {
			return true;
		}

		if ( temporalType != null ) {
			final boolean parameterDeclarationIsTemporal = Date.class.isAssignableFrom( expectedType )
					|| Calendar.class.isAssignableFrom( expectedType );
			final boolean bindIsTemporal = Date.class.isInstance( value )
					|| Calendar.class.isInstance( value );

			if ( parameterDeclarationIsTemporal && bindIsTemporal ) {
				return true;
			}
		}

		return false;
	}
}
