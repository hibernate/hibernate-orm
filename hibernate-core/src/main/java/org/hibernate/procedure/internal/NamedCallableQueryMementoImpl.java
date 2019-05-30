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
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.procedure.spi.NamedCallableQueryMemento;
import org.hibernate.procedure.spi.ParameterRegistrationImplementor;
import org.hibernate.procedure.spi.ParameterStrategy;
import org.hibernate.query.spi.AbstractNamedQueryMemento;
import org.hibernate.query.spi.NamedQueryMemento;
import org.hibernate.type.Type;

/**
 * Implementation of NamedCallableQueryMemento
 *
 * @author Steve Ebersole
 */
public class NamedCallableQueryMementoImpl extends AbstractNamedQueryMemento implements NamedCallableQueryMemento {
	private final String callableName;

	private final ParameterStrategy parameterStrategy;
	private final List<ParameterMemento> parameterMementos;

	private final String[] resultSetMappingNames;
	private final Class[] resultSetMappingClasses;

	private final Set<String> querySpaces;


	/**
	 * Constructs a ProcedureCallImpl
	 */
	public NamedCallableQueryMementoImpl(
			String name,
			String callableName,
			ParameterStrategy parameterStrategy,
			List<ParameterMemento> parameterMementos,
			String[] resultSetMappingNames,
			Class[] resultSetMappingClasses,
			Set<String> querySpaces,
			Boolean cacheable,
			String cacheRegion,
			CacheMode cacheMode,
			FlushMode flushMode,
			Boolean readOnly,
			Integer timeout,
			Integer fetchSize,
			String comment,
			Map<String, Object> hints) {
		super(
				name,
				cacheable,
				cacheRegion,
				cacheMode,
				flushMode,
				readOnly,
				timeout,
				fetchSize,
				comment,
				hints
		);
		this.callableName = callableName;
		this.parameterStrategy = parameterStrategy;
		this.parameterMementos = parameterMementos;
		this.resultSetMappingNames = resultSetMappingNames;
		this.resultSetMappingClasses = resultSetMappingClasses;
		this.querySpaces = querySpaces;
	}

	@Override
	public String getCallableName() {
		return callableName;
	}

	@Override
	public ProcedureCall makeProcedureCall(SharedSessionContractImplementor session) {
		return new ProcedureCallImpl( session, this );
	}

	@Override
	public NamedQueryMemento makeCopy(String name) {
		return new NamedCallableQueryMementoImpl(
				name,
				callableName,
				parameterStrategy,
				parameterMementos,
				resultSetMappingNames,
				resultSetMappingClasses,
				querySpaces,
				getCacheable(),
				getCacheRegion(),
				getCacheMode(),
				getFlushMode(),
				getReadOnly(),
				getTimeout(),
				getFetchSize(),
				getComment(),
				getHints()
		);
	}

	/**
	 * A "disconnected" copy of the metadata for a parameter, that can be used in ProcedureCallMementoImpl.
	 */
	public static class ParameterMementoImpl implements ParameterMemento {
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
		public ParameterMementoImpl(
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
		public ParameterRegistrationImplementor resolve(SharedSessionContractImplementor session) {
			throw new NotYetImplementedFor6Exception();
		}

		/**
		 * Build a ParameterMemento from the given parameter registration
		 *
		 * @param registration The parameter registration from a ProcedureCall
		 *
		 * @return The memento
		 */
		public static ParameterMementoImpl fromRegistration(ParameterRegistrationImplementor registration) {
			return new ParameterMementoImpl(
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
