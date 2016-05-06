package org.hibernate.dialect;

import java.util.List;

/**
 * An SQL Dialect for PostgreSQL 9.3 and later. Adds support for Materialized view.
 *
 * @author Dionis Argiri
 */
public class PostgreSQL93Dialect extends PostgreSQL9Dialect {
	@Override
	public void augmentRecognizedTableTypes(List<String> tableTypesList) {
		super.augmentRecognizedTableTypes( tableTypesList );
		tableTypesList.add( "MATERIALIZED VIEW" );
	}
}
