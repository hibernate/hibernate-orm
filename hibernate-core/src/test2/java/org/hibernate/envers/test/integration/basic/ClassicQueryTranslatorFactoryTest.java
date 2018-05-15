/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.basic;

import java.util.Map;

import org.hibernate.cfg.Environment;
import org.hibernate.hql.internal.classic.ClassicQueryTranslatorFactory;

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
}
