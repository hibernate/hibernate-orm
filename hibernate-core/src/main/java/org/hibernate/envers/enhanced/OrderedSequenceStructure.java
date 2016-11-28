/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.enhanced;

import org.hibernate.HibernateException;
import org.hibernate.boot.model.relational.AuxiliaryDatabaseObject;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.QualifiedName;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.Oracle8iDialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.id.enhanced.SequenceStructure;

/**
 * Describes a sequence that supports ordered sequences.
 *
 * @author Chris Cranford
 */
public class OrderedSequenceStructure extends SequenceStructure {

	private static final String ORDER = " ORDER";

	private AuxiliaryDatabaseObject sequenceObject;

	public OrderedSequenceStructure(
			JdbcEnvironment jdbcEnvironment,
			QualifiedName qualifiedSequenceName,
			int initialValue,
			int incrementSize,
			Class numberType) {
		super( jdbcEnvironment, qualifiedSequenceName, initialValue, incrementSize, numberType );
		this.sequenceObject = new OrderedSequence();
	}

	@Override
	public String[] sqlCreateStrings(Dialect dialect) throws HibernateException {
		// delegate to auxiliary object
		return sequenceObject.sqlCreateStrings( dialect );
	}

	@Override
	public String[] sqlDropStrings(Dialect dialect) throws HibernateException {
		// delegate to auxiliary object
		return sequenceObject.sqlDropStrings( dialect );
	}

	@Override
	protected void buildSequence(Database database) {
		database.addAuxiliaryDatabaseObject( sequenceObject );
		this.sequenceName = database.getJdbcEnvironment().getQualifiedObjectNameFormatter().format(
				getQualifiedName(),
				database.getJdbcEnvironment().getDialect()
		);
	}

	private class OrderedSequence implements AuxiliaryDatabaseObject {
		@Override
		public String getExportIdentifier() {
			return getName();
		}

		@Override
		public boolean appliesToDialect(Dialect dialect) {
			// applies to all dialects
			// sqlCreateStrings applies dialect specific changes
			return true;
		}

		@Override
		public boolean beforeTablesOnCreation() {
			return true;
		}

		@Override
		public String[] sqlCreateStrings(Dialect dialect) {
			final String[] createStrings = dialect.getCreateSequenceStrings(
					getName(),
					getInitialValue(),
					getSourceIncrementSize()
			);

			if ( dialect instanceof Oracle8iDialect ) {
				for ( int i = 0; i < createStrings.length; ++i ) {
					createStrings[ i ] = createStrings[ i ] + ORDER;
				}
			}

			return createStrings;
		}

		@Override
		public String[] sqlDropStrings(Dialect dialect) {
			return dialect.getDropSequenceStrings( getName() );
		}
	}
}
