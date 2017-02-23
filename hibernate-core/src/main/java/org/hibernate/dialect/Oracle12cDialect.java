/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import org.hibernate.cfg.Environment;

/**
 * An SQL dialect for Oracle 12c.
 *
 * @author zhouyanming (zhouyanming@gmail.com)
 */
public class Oracle12cDialect extends Oracle10gDialect {
	public static final String PREFER_LONG_RAW = "hibernate.dialect.oracle.prefer_long_raw";

	public Oracle12cDialect() {
		super();
		getDefaultProperties().setProperty( Environment.BATCH_VERSIONED_DATA, "true" );
	}

	@Override
	protected void registerDefaultProperties() {
		super.registerDefaultProperties();
		getDefaultProperties().setProperty( Environment.USE_GET_GENERATED_KEYS, "true" );
	}

// Overridden on master, but would also need changes from SequenceStyleGenerator
//	@Override
//    public Class getNativeIdentifierGeneratorClass()
//    {
//        return SequenceStyleGenerator.class;
//    }
}
