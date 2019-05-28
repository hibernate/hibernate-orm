/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.procedure.internal;

import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.ParameterMode;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.LockOptions;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryReturn;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.procedure.spi.NamedCallableQueryMemento;
import org.hibernate.procedure.spi.ParameterRegistrationImplementor;
import org.hibernate.procedure.spi.ParameterStrategy;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.spi.AbstractNamedQueryMemento;
import org.hibernate.query.spi.ParameterMemento;
import org.hibernate.type.Type;

/**
 * Implementation of NamedCallableQueryMemento
 *
 * @author Steve Ebersole
 */
public class NamedCallableQueryMementoImpl extends AbstractNamedQueryMemento implements NamedCallableQueryMemento {
	private final String callableName;
	private final ParameterStrategy parameterStrategy;
	private final Class[] resultClasses;
	private final String[] resultSetMappingNames;
	private final Set<String> querySpaces;


	/**
	 * Constructs a ProcedureCallImpl
	 */
	public NamedCallableQueryMementoImpl(
			String name,
			String callableName,
			ParameterStrategy parameterStrategy,
			List<CallableParameterMemento> parameterMementos,
			Class[] resultClasses,
			String[] resultSetMappingNames,
			Set<String> querySpaces,
			Boolean cacheable,
			String cacheRegion,
			CacheMode cacheMode,
			FlushMode flushMode,
			Boolean readOnly,
			LockOptions lockOptions,
			Integer timeout,
			Integer fetchSize,
			String comment,
			Map<String, Object> hints) {
		super(
				name,
				parameterMementos,
				cacheable,
				cacheRegion,
				cacheMode,
				flushMode,
				readOnly,
				lockOptions,
				timeout,
				fetchSize,
				comment,
				hints
		);
		this.callableName = callableName;
		this.parameterStrategy = parameterStrategy;
		this.resultClasses = resultClasses;
		this.resultSetMappingNames = resultSetMappingNames;
		this.querySpaces = querySpaces;
	}

	@Override
	public ProcedureCall makeProcedureCall(SharedSessionContractImplementor session) {
		return new ProcedureCallImpl( session, this );
	}

	public String getCallableName() {
		return callableName;
	}

	public NativeSQLQueryReturn[] getQueryReturns() {
		return queryReturns;
	}

	public ParameterStrategy getParameterStrategy() {
		return parameterStrategy;
	}

	public List<ParameterMemento> getParameterDeclarations() {
		return parameterDeclarations;
	}

	public Set<String> getSynchronizedQuerySpaces() {
		return synchronizedQuerySpaces;
	}

	@Override
	public Map<String, Object> getHintsMap() {
		return hintsMap;
	}

	/**
	 * A "disconnected" copy of the metadata for a parameter, that can be used in ProcedureCallMementoImpl.
	 */
	public static class CallableParameterMemento implements ParameterMemento {
		private final Integer position;
		private final String name;
		private final ParameterMode mode;
		private final Class type;
		private final Type hibernateType;
		private final boolean passNulls;

		/**
		 * Create the memento
		 *
		 * @param position The parameter position
		 * @param name The parameter name
		 * @param mode The parameter mode
		 * @param type The Java type of the parameter
		 * @param hibernateType The Hibernate Type.
		 * @param passNulls Should NULL values to passed to the database?
		 */
		public CallableParameterMemento(
				int position,
				String name,
				ParameterMode mode,
				Class type,
				Type hibernateType,
				boolean passNulls) {
			this.position = position;
			this.name = name;
			this.mode = mode;
			this.type = type;
			this.hibernateType = hibernateType;
			this.passNulls = passNulls;
		}

		public Integer getPosition() {
			return position;
		}

		public String getName() {
			return name;
		}

		public ParameterMode getMode() {
			return mode;
		}

		public Class getType() {
			return type;
		}

		public Type getHibernateType() {
			return hibernateType;
		}

		public boolean isPassNullsEnabled() {
			return passNulls;
		}

		@Override
		public QueryParameter toQueryParameter(SharedSessionContractImplementor session) {
			return null;
		}

		/**
		 * Build a ParameterMemento from the given parameter registration
		 *
		 * @param registration The parameter registration from a ProcedureCall
		 *
		 * @return The memento
		 */
		public static ParameterMemento fromRegistration(ParameterRegistrationImplementor registration) {
			return new ParameterMemento(
					registration.getPosition(),
					registration.getName(),
					registration.getMode(),
					registration.getParameterType(),
					registration.getHibernateType(),
					registration.isPassNullsEnabled()
			);
		}

	}
}
