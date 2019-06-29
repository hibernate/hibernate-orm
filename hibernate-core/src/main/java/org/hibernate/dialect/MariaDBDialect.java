/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import org.hibernate.LockOptions;
import org.hibernate.dialect.sequence.ANSISequenceSupport;
import org.hibernate.dialect.sequence.SequenceSupport;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorMariaDBDatabaseImpl;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;
import org.hibernate.type.spi.StandardSpiBasicTypes;

/**
 * SQL dialect for MariaDB
 *
 * @author Vlad Mihalcea
 * @author Gavin King
 */
public class MariaDBDialect extends MySQLDialect {

	private final int version;

	int getMariaVersion() {
		return version;
	}

	public MariaDBDialect() {
		this(500);
	}

	public MariaDBDialect(int version) {
		super(version < 530 ? 500 : 570);
		this.version = version;
	}

	public MariaDBDialect(DialectResolutionInfo info) {
		this( info.getDatabaseMajorVersion() * 100 + info.getDatabaseMinorVersion() * 10 );
	}

	@Override
	public void initializeFunctionRegistry(QueryEngine queryEngine) {
		super.initializeFunctionRegistry(queryEngine);

		if ( getMariaVersion() >= 1020 ) {
			queryEngine.getSqmFunctionRegistry().registerNamed("json_valid", StandardSpiBasicTypes.NUMERIC_BOOLEAN);
		}
	}

	public boolean supportsRowValueConstructorSyntaxInInList() {
		return true;
	}

	@Override
	public boolean supportsColumnCheck() {
		return getMariaVersion() >= 1020;
	}

	@Override
	protected MySQLStorageEngine getDefaultMySQLStorageEngine() {
		return InnoDBStorageEngine.INSTANCE;
	}

	@Override
	public boolean supportsIfExistsBeforeConstraintName() {
		return getMariaVersion() >= 1000;
	}

	@Override
	public SequenceSupport getSequenceSupport() {
		return getMariaVersion() < 1030
				? super.getSequenceSupport()
				: ANSISequenceSupport.INSTANCE;
	}

	@Override
	public String getQuerySequencesString() {
		return supportsSequences()
				? "select table_name from information_schema.TABLES where table_type='SEQUENCE'"
				: super.getQuerySequencesString(); //fancy way to write "null"
	}

	@Override
	public SequenceInformationExtractor getSequenceInformationExtractor() {
		return supportsSequences()
				? SequenceInformationExtractorMariaDBDatabaseImpl.INSTANCE
				: super.getSequenceInformationExtractor();
	}

	@Override
	public String getWriteLockString(int timeout) {
		if ( getMariaVersion() < 1030 ) {
			return super.getWriteLockString( timeout );
		}

		if ( timeout == LockOptions.NO_WAIT ) {
			return getForUpdateNowaitString();
		}

		if ( timeout > 0 ) {
			return getForUpdateString() + " wait " + timeout;
		}

		return getForUpdateString();
	}

	@Override
	public String getForUpdateNowaitString() {
		return getMariaVersion() < 1030
				? super.getForUpdateNowaitString()
				: getForUpdateString() + " nowait";
	}

	@Override
	public String getForUpdateNowaitString(String aliases) {
		return getMariaVersion() < 1030
				? super.getForUpdateNowaitString( aliases )
				: getForUpdateString( aliases ) + " nowait";
	}

}
