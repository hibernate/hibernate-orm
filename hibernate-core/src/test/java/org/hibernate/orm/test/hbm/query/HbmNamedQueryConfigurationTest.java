/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.hbm.query;

import java.util.Map;

import org.hibernate.query.named.NamedObjectRepository;
import org.hibernate.query.sqm.spi.NamedSqmQueryMemento;

import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

@DomainModel(
		xmlMappings = {
				"org/hibernate/orm/test/hbm/query/HbmOverridesAnnotation.orm.xml",
				"org/hibernate/orm/test/hbm/query/HbmOverridesAnnotation.hbm.xml"
		}
)
@ServiceRegistry(
		settings = {
				@Setting( name ="hibernate.enable_specj_proprietary_syntax", value = "true"),
				@Setting( name ="hibernate.transform_hbm_xml.enabled", value = "true"),
		}
)
@SessionFactory
@BytecodeEnhanced
@EnhancementOptions(inlineDirtyChecking = true, lazyLoading = true, extendedEnhancement = true)
public class HbmNamedQueryConfigurationTest {

	@Test
	@JiraKey("HHH-15619")
	@JiraKey("HHH-15620")
	public void testHbmOverride(SessionFactoryScope scope) {
		NamedObjectRepository namedObjectRepository = scope.getSessionFactory()
				.getQueryEngine()
				.getNamedObjectRepository();
		NamedSqmQueryMemento sqmQueryMemento = namedObjectRepository.getSqmQueryMemento( Bar.FIND_ALL );
		assertTrue( sqmQueryMemento.getCacheable() );
	}

}
