/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.sequence;

import org.hibernate.MappingException;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.sequence.spi.ANSISequenceSupport;
import org.hibernate.dialect.sequence.spi.NextvalSequenceSupport;
import org.hibernate.dialect.sequence.spi.SequenceSupport;

/// Internal stock sequence strategies shared by legacy community Dialects.
///
/// These strategies preserve legacy vendor grammar without importing
/// Hibernate Core's vendor-specific internal implementations.
///
/// @author Steve Ebersole
public final class CommunitySequenceSupports {
	private static final SequenceSupport DB2_Z = new SequenceSupport() {
		@Override
		public String getFromDual() {
			return " from sysibm.sysdummy1";
		}

		@Override
		public String getSelectSequenceNextValString(String sequenceName) {
			return "nextval for " + sequenceName;
		}

		@Override
		public String getSelectSequencePreviousValString(String sequenceName) {
			return "prevval for " + sequenceName;
		}

		@Override
		public String getCreateSequenceString(String sequenceName) {
			return "create sequence " + sequenceName
					+ " as integer start with 1 increment by 1 minvalue 1 nomaxvalue nocycle nocache";
		}

		@Override
		public String getCreateSequenceString(String sequenceName, int initialValue, int incrementSize) {
			return "create sequence " + sequenceName
					+ " as integer start with " + initialValue
					+ " increment by " + incrementSize
					+ " minvalue 1 nomaxvalue nocycle nocache";
		}
	};

	private static final SequenceSupport H2_V2 = new ANSISequenceSupport() {
		@Override
		public String getDropSequenceString(String sequenceName) {
			return "drop sequence if exists " + sequenceName;
		}
	};

	private static final SequenceSupport HANA = new NextvalSequenceSupport() {
		@Override
		public String getFromDual() {
			return " from sys.dummy";
		}

		@Override
		public boolean sometimesNeedsStartingValue() {
			return true;
		}
	};

	private static final SequenceSupport HSQL = new ANSISequenceSupport() {
		@Override
		public String getCreateSequenceString(String sequenceName) {
			return "create sequence " + sequenceName + " start with 1";
		}

		@Override
		public String getCreateSequenceString(String sequenceName, int initialValue, int incrementSize) {
			return "create sequence " + sequenceName + " start with " + initialValue
					+ " increment by " + incrementSize;
		}

		@Override
		public String getDropSequenceString(String sequenceName) {
			return "drop sequence " + sequenceName + " if exists";
		}

		@Override
		public String getSequenceNextValString(String sequenceName) {
			return "call " + getSelectSequenceNextValString( sequenceName );
		}

		@Override
		public String getSequencePreviousValString(String sequenceName) {
			return "call " + getSelectSequencePreviousValString( sequenceName );
		}
	};

	private static final SequenceSupport MARIA_DB = new ANSISequenceSupport() {
		@Override
		public String getCreateSequenceString(String sequenceName) {
			return "create sequence " + sequenceName + " nocache";
		}

		@Override
		public String getCreateSequenceString(String sequenceName, int initialValue, int incrementSize) {
			return "create sequence " + sequenceName
					+ startingValue( initialValue, incrementSize )
					+ " start with " + initialValue
					+ " increment by " + incrementSize
					+ " nocache";
		}

		@Override
		public String getSelectSequencePreviousValString(String sequenceName) {
			return "previous value for " + sequenceName;
		}

		@Override
		public String getDropSequenceString(String sequenceName) {
			return "drop sequence if exists " + sequenceName;
		}

		@Override
		public boolean sometimesNeedsStartingValue() {
			return true;
		}
	};

	private static final SequenceSupport SQL_SERVER = new ANSISequenceSupport() {
		@Override
		public String getSequencePreviousValString(String sequenceName) {
			return "select convert(varchar(200),current_value) from sys.sequences where name='"
					+ sequenceName + "'";
		}
	};

	private static final SequenceSupport SQL_SERVER_16 = new ANSISequenceSupport() {
		@Override
		public String getSequencePreviousValString(String sequenceName) {
			return SQL_SERVER.getSequencePreviousValString( sequenceName );
		}

		@Override
		public String getDropSequenceString(String sequenceName) {
			return "drop sequence if exists " + sequenceName;
		}
	};

	private CommunitySequenceSupports() {
	}

	public static SequenceSupport db2z() {
		return DB2_Z;
	}

	public static SequenceSupport h2v2() {
		return H2_V2;
	}

	public static SequenceSupport hana() {
		return HANA;
	}

	public static SequenceSupport hsql() {
		return HSQL;
	}

	public static SequenceSupport mariaDB() {
		return MARIA_DB;
	}

	public static SequenceSupport oracle(DatabaseVersion version) {
		final boolean requiresFromDual = version.isBefore( 23 );
		final boolean supportsIfExists = version.isSameOrAfter( 23 );
		return new NextvalSequenceSupport() {
			@Override
			public String getFromDual() {
				return requiresFromDual ? " from dual" : "";
			}

			@Override
			public boolean sometimesNeedsStartingValue() {
				return true;
			}

			@Override
			public String getDropSequenceString(String sequenceName) throws MappingException {
				return "drop sequence " + (supportsIfExists ? "if exists " : "") + sequenceName;
			}

			@Override
			public String getRestartSequenceString(String sequenceName, long startWith) {
				return "alter sequence " + sequenceName + " restart start with " + startWith;
			}
		};
	}

	public static SequenceSupport sqlServer() {
		return SQL_SERVER;
	}

	public static SequenceSupport sqlServer16() {
		return SQL_SERVER_16;
	}
}
