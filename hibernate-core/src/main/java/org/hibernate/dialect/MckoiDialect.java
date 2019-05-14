/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.sql.Types;

import org.hibernate.LockMode;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.metamodel.model.domain.spi.Lockable;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.dialect.lock.OptimisticForceIncrementLockingStrategy;
import org.hibernate.dialect.lock.OptimisticLockingStrategy;
import org.hibernate.dialect.lock.PessimisticForceIncrementLockingStrategy;
import org.hibernate.dialect.lock.PessimisticReadUpdateLockingStrategy;
import org.hibernate.dialect.lock.PessimisticWriteUpdateLockingStrategy;
import org.hibernate.dialect.lock.SelectLockingStrategy;
import org.hibernate.dialect.lock.UpdateLockingStrategy;
import org.hibernate.sql.CaseFragment;
import org.hibernate.sql.MckoiCaseFragment;
import org.hibernate.type.spi.StandardSpiBasicTypes;

/**
 * An SQL dialect compatible with McKoi SQL
 *
 * @author Doug Currie
 * @author Gabe Hicks
 */
public class MckoiDialect extends Dialect {
	/**
	 * Constructs a MckoiDialect
	 */
	public MckoiDialect() {
		super();

		//Note: there is no single-precision type
		//'float' and 'double' are the exact same
		//double precision type.
		registerColumnType( Types.FLOAT, "float" ); //precision argument not supported
		registerColumnType( Types.DOUBLE, "double" ); //'double precision' not supported

		getDefaultProperties().setProperty( Environment.STATEMENT_BATCH_SIZE, NO_BATCH );
	}

	@Override
	public int getPreferredSqlTypeCodeForBoolean() {
		return Types.BOOLEAN;
	}

	@Override
	public void initializeFunctionRegistry(QueryEngine queryEngine) {
		super.initializeFunctionRegistry(queryEngine);

		CommonFunctionFactory.characterLength_length( queryEngine );

		queryEngine.getSqmFunctionRegistry().patternTemplateBuilder( "nullif", "if(?1=?2, null, ?1)" )
				.setExactArgumentCount(2)
				.register();
	}

	@Override
	public String getAddColumnString() {
		return "add column";
	}

	@Override
	public String getSequenceNextValString(String sequenceName) {
		return "select " + getSelectSequenceNextValString( sequenceName );
	}

	@Override
	public String getSelectSequenceNextValString(String sequenceName) {
		return "nextval('" + sequenceName + "')";
	}

	@Override
	public String getCreateSequenceString(String sequenceName) {
		return "create sequence " + sequenceName;
	}

	@Override
	public String getDropSequenceString(String sequenceName) {
		return "drop sequence " + sequenceName;
	}

	@Override
	public String getForUpdateString() {
		return "";
	}

	@Override
	public boolean supportsSequences() {
		return true;
	}

	@Override
	public CaseFragment createCaseFragment() {
		return new MckoiCaseFragment();
	}

	@Override
	public LockingStrategy getLockingStrategy(Lockable lockable, LockMode lockMode) {
		// Mckoi has no known variation of a "SELECT ... FOR UPDATE" syntax...
		if ( lockMode==LockMode.PESSIMISTIC_FORCE_INCREMENT) {
			return new PessimisticForceIncrementLockingStrategy( lockable, lockMode);
		}
		else if ( lockMode==LockMode.PESSIMISTIC_WRITE) {
			return new PessimisticWriteUpdateLockingStrategy( lockable, lockMode);
		}
		else if ( lockMode==LockMode.PESSIMISTIC_READ) {
			return new PessimisticReadUpdateLockingStrategy( lockable, lockMode);
		}
		else if ( lockMode==LockMode.OPTIMISTIC) {
			return new OptimisticLockingStrategy( lockable, lockMode);
		}
		else if ( lockMode==LockMode.OPTIMISTIC_FORCE_INCREMENT) {
			return new OptimisticForceIncrementLockingStrategy( lockable, lockMode);
		}
		else if ( lockMode.greaterThan( LockMode.READ ) ) {
			return new UpdateLockingStrategy( lockable, lockMode );
		}
		else {
			return new SelectLockingStrategy( lockable, lockMode );
		}
	}
}
