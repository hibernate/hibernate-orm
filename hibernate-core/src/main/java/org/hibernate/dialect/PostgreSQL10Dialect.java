/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.identity.PostgreSQL10IdentityColumnSupport;

/**
 * An SQL dialect for Postgres 10 and later.
 */
public class PostgreSQL10Dialect extends PostgreSQL95Dialect {

	private static final String SEQUENCES_QUERY_STRING = "SELECT current_database() sequence_catalog, " +
			"    n.nspname AS sequence_schema, " +
			"    c.relname AS sequence_name, " +
			"    s.seqtypid::REGTYPE AS data_type, " +
			"    (information_schema._pg_numeric_precision(s.seqtypid, '-1'::INTEGER)) " +
			" ::information_schema.CARDINAL_NUMBER AS numeric_precision, " +
			"    (2)::information_schema.CARDINAL_NUMBER AS numeric_precision_radix, " +
			"    (0)::information_schema.CARDINAL_NUMBER AS numeric_scale, " +
			"    (s.seqstart)::information_schema.CHARACTER_DATA AS start_value, " +
			"    (s.seqmin)::information_schema.CHARACTER_DATA AS minimum_value, " +
			"    (s.seqmax)::information_schema.CHARACTER_DATA AS maximum_value, " +
			"    (s.seqincrement)::information_schema.CHARACTER_DATA AS increment, " +
			"    (CASE WHEN s.seqcycle THEN 'YES'::TEXT ELSE 'NO'::TEXT END) " +
			" ::information_schema.YES_OR_NO AS cycle_option " +
			"FROM pg_catalog.pg_sequence s " +
			"  JOIN pg_catalog.pg_class c ON s.seqrelid = c.oid " +
			"  LEFT JOIN pg_catalog.pg_namespace n ON n.oid = c.relnamespace " +
			"WHERE (NOT pg_is_other_temp_schema(n.oid)) AND c.relkind = 'S';";

	@Override
	public String getQuerySequencesString() {
		return SEQUENCES_QUERY_STRING;
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return new PostgreSQL10IdentityColumnSupport();
	}
}
