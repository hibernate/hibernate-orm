/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.cascade.circle;

import org.junit.Test;

import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Oracle10gDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.testing.SkipForDialect;

/**
 * @author Gail Badner
 */
public class MultiPathCircleCascadeCheckNullibilityFalseTest extends MultiPathCircleCascadeTest {
	@Override
	 public void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.CHECK_NULLABILITY, "false" );
	}

	@Override
	@SkipForDialect(value = { Oracle10gDialect.class, PostgreSQLDialect.class }, comment = "This test is known to fail for dialects using a sequence for the native generator. See HHH-6744")
	@Test
	public void testMergeEntityWithNonNullableTransientEntity() {
		super.testMergeEntityWithNonNullableTransientEntity();
	}
}