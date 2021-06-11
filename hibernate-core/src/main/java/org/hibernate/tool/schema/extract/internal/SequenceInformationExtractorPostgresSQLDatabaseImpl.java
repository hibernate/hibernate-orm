package org.hibernate.tool.schema.extract.internal;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * PostgreSQL stores the sequence metadata as strings. PostgreSQL's JDBC driver does the
 * conversion automatically, but, unfortunately Vert.x driver does not do this conversion.
 *
 * This class is intended to make this functionality work with both the JDBC and Vert.X
 * drivers.
 *
 * @author Gail Badner
 */
public class SequenceInformationExtractorPostgresSQLDatabaseImpl  extends SequenceInformationExtractorLegacyImpl {
	//Singleton access
	public static final SequenceInformationExtractorPostgresSQLDatabaseImpl INSTANCE = new SequenceInformationExtractorPostgresSQLDatabaseImpl();

	protected Long resultSetStartValueSize(ResultSet resultSet) throws SQLException {
		return convertStringValueToLong( resultSet, sequenceStartValueColumn() );
	}

	protected Long resultSetMinValue(ResultSet resultSet) throws SQLException {
		return convertStringValueToLong( resultSet, sequenceMinValueColumn() );
	}

	protected Long resultSetMaxValue(ResultSet resultSet) throws SQLException {
		return convertStringValueToLong( resultSet, sequenceMaxValueColumn() );
	}

	protected Long resultSetIncrementValue(ResultSet resultSet) throws SQLException {
		return convertStringValueToLong( resultSet, sequenceIncrementColumn() );
	}

	private Long convertStringValueToLong(ResultSet resultSet, String columnLabel) throws SQLException {
		// column value is of type character_data so get it as a String
		final String stringValue = resultSet.getString( columnLabel );
		return stringValue != null ? Long.valueOf( stringValue ) : null;
	}
}
