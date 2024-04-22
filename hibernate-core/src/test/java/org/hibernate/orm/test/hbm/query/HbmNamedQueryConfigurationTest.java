/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.hbm.query;

import java.util.Map;

import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;
import org.hibernate.query.named.NamedObjectRepository;
import org.hibernate.query.sqm.spi.NamedSqmQueryMemento;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.jupiter.api.Assertions.assertTrue;

@RunWith(BytecodeEnhancerRunner.class)
@EnhancementOptions(inlineDirtyChecking = true, lazyLoading = true, extendedEnhancement = true)
public class HbmNamedQueryConfigurationTest extends BaseEntityManagerFunctionalTestCase {
	@Override
	protected String[] getMappings() {
		return new String[]{
				"org/hibernate/orm/test/hbm/query/HbmOverridesAnnotation.orm.xml",
				"org/hibernate/orm/test/hbm/query/HbmOverridesAnnotation.hbm.xml"
		};
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	protected void addConfigOptions(Map options) {
		options.put( "hibernate.enable_specj_proprietary_syntax", "true" );
		options.put( "hibernate.transform_hbm_xml.enabled", "true" );
	}

	@Test
	@TestForIssue( jiraKey = { "HHH-15619", "HHH-15620"} )
	public void testHbmOverride() {
		NamedObjectRepository namedObjectRepository = entityManagerFactory()
				.getQueryEngine()
				.getNamedObjectRepository();
		NamedSqmQueryMemento<?> sqmQueryMemento = namedObjectRepository.getSqmQueryMemento( Bar.FIND_ALL );
		assertTrue( sqmQueryMemento.getCacheable() );
	}

}
