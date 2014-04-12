/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.envers.test.integration.basic;

import java.util.Map;

import org.junit.Test;

import org.hibernate.cfg.Environment;
import org.hibernate.hql.internal.classic.ClassicQueryTranslatorFactory;

import org.hibernate.testing.FailureExpectedWithNewMetamodel;
import org.hibernate.testing.TestForIssue;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@TestForIssue(jiraKey = "HHH-8497")
public class ClassicQueryTranslatorFactoryTest extends Simple {
	@Override
	protected void addConfigOptions(Map options) {
		super.addConfigOptions( options );
		options.put( Environment.QUERY_TRANSLATOR, ClassicQueryTranslatorFactory.class.getName() );
	}

	@Test
	@FailureExpectedWithNewMetamodel( message = "ClassicQueryTranslatorFactoryTest confuses package name with an alias" )
	public void testHistoryOfId1() {
		super.testHistoryOfId1();
	}

	@Test
	@FailureExpectedWithNewMetamodel( message = "ClassicQueryTranslatorFactoryTest confuses package name with an alias" )
	public void testRevisionsCounts() {
		super.testRevisionsCounts();
	}
}
