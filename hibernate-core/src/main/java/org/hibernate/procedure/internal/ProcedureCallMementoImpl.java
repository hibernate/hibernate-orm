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

import org.hibernate.engine.query.spi.sql.NativeSQLQueryReturn;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.procedure.ProcedureCallMemento;
import org.hibernate.procedure.spi.ParameterRegistrationImplementor;
import org.hibernate.procedure.spi.ParameterStrategy;
import org.hibernate.type.Type;

/**
 * Implementation of ProcedureCallMemento
 *
 * @author Steve Ebersole
 */
public class ProcedureCallMementoImpl implements ProcedureCallMemento {
	private final String procedureName;
	private final NativeSQLQueryReturn[] queryReturns;

	private final ParameterStrategy parameterStrategy;
	private final List<ParameterMemento> parameterDeclarations;

	private final Set<String> synchronizedQuerySpaces;

	private final Map<String, Object> hintsMap;

	/**
	 * Constructs a ProcedureCallImpl
	 *
	 * @param procedureName The name of the procedure to be called
	 * @param queryReturns The result mappings
	 * @param parameterStrategy Are parameters named or positional?
	 * @param parameterDeclarations The parameters registrations
	 * @param synchronizedQuerySpaces Any query spaces to synchronize on execution
	 * @param hintsMap Map of JPA query hints
	 */
	public ProcedureCallMementoImpl(
			String procedureName,
			NativeSQLQueryReturn[] queryReturns,
			ParameterStrategy parameterStrategy,
			List<ParameterMemento> parameterDeclarations,
			Set<String> synchronizedQuerySpaces,
			Map<String, Object> hintsMap) {
		this.procedureName = procedureName;
		this.queryReturns = queryReturns;
		this.parameterStrategy = parameterStrategy;
		this.parameterDeclarations = parameterDeclarations;
		this.synchronizedQuerySpaces = synchronizedQuerySpaces;
		this.hintsMap = hintsMap;
	}

	@Override
	public ProcedureCall makeProcedureCall(SharedSessionContractImplementor session) {
		return new ProcedureCallImpl( session, this );
	}

	public String getProcedureName() {
		return procedureName;
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
	public static class ParameterMemento {
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
		public ParameterMemento(
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
					registration.getType(),
					registration.getHibernateType(),
					registration.isPassNullsEnabled()
			);
		}

	}
}
