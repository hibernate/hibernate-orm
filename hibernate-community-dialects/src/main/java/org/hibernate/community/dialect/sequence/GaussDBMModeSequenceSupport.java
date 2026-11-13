/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.sequence;

import java.util.Locale;

import org.hibernate.MappingException;

/**
 * Sequence support for {@link org.hibernate.community.dialect.GaussDBDialect}
 * when the target database runs in MySQL-compatible mode (datcompatibility "B"/"M").
 * <p>
 * In M mode, {@code nextval()}/{@code currval()}/{@code setval()} treat their string
 * argument as case-sensitive (unlike PostgreSQL, which folds it to lowercase via regclass
 * resolution). How the sequence name must be rendered on the call side therefore depends on
 * whether the identifier was quoted at {@code CREATE SEQUENCE} time:
 * <ul>
 *   <li>Unquoted: {@code CREATE SEQUENCE ConcreteOne_SEQ} folds the identifier to lowercase
 *       ({@code concreteone_seq}), so the call must also use the lowercase name.</li>
 *   <li>Quoted (e.g. globally-quoted identifiers wrapped in backticks/double-quotes):
 *       {@code CREATE SEQUENCE `Person_SEQ`} preserves the original case, so the call must
 *       keep it — lower-casing would make {@code nextval('person_seq')} miss the stored
 *       {@code Person_SEQ}. The quote characters themselves are stripped, since the
 *       {@code nextval} argument is a name string, not a SQL identifier.</li>
 * </ul>
 * {@code CREATE}/{@code DROP SEQUENCE} are not overridden because their identifiers are
 * folded (or preserved) by the DDL parser itself.
 */
public class GaussDBMModeSequenceSupport extends GaussDBSequenceSupport {

	public static final GaussDBMModeSequenceSupport INSTANCE = new GaussDBMModeSequenceSupport();

	@Override
	public String getSelectSequenceNextValString(String sequenceName) {
		return "nextval('" + normalize( sequenceName ) + "')";
	}

	@Override
	public String getSelectSequencePreviousValString(String sequenceName) throws MappingException {
		return "currval('" + normalize( sequenceName ) + "')";
	}

	@Override
	public String getRestartSequenceString(String sequenceName, long startWith) {
		return "select setval('" + normalize( sequenceName ) + "', " + startWith + ", false)";
	}

	private static String normalize(String sequenceName) {
		if ( sequenceName.indexOf( '`' ) >= 0 || sequenceName.indexOf( '"' ) >= 0 ) {
			// Quoted identifier: CREATE SEQUENCE preserved the original case and nextval's
			// string argument is case-sensitive, so keep the case and strip the quote chars.
			return sequenceName.replace( "`", "" ).replace( "\"", "" );
		}
		// Unquoted: CREATE SEQUENCE folded the identifier to lowercase, so match that.
		return sequenceName.toLowerCase( Locale.ROOT );
	}
}
