package org.hibernate.orm.test.boot.cfgXml;

import java.io.InputStream;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.boot.cfgxml.internal.JaxbCfgProcessor;
import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.SourceType;
import org.hibernate.boot.jaxb.cfg.spi.JaxbCfgConfigPropertyType;
import org.hibernate.boot.jaxb.cfg.spi.JaxbCfgHibernateConfiguration;
import org.hibernate.boot.registry.classloading.internal.ClassLoaderServiceImpl;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.cfg.MappingSettings;

import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

/**
 * Tests that {@code hibernate.cfg.xml} DTD and external entity references are resolved locally
 * through {@link org.hibernate.boot.jaxb.internal.stax.LocalXmlResourceResolver} when
 * {@value MappingSettings#XML_ALLOW_DTD_AND_EXTERNAL_ENTITIES} is enabled, and rejected otherwise.
 */
@BaseUnitTest
@JiraKey("HHH-21047")
public class CfgXmlDtdResolutionTest {
	private static final String CFG_XML = "org/hibernate/orm/test/boot/cfgXml/dtd-entity.cfg.xml";

	private ClassLoaderService classLoaderService;

	@BeforeEach
	public void setUp() {
		classLoaderService = new ClassLoaderServiceImpl();
	}

	@AfterEach
	public void tearDown() {
		classLoaderService.stop();
	}

	@Test
	public void testExternalEntityResolvedFromClasspathWhenAllowed() throws Exception {
		final var processor = new JaxbCfgProcessor(
				classLoaderService,
				Map.of( MappingSettings.XML_ALLOW_DTD_AND_EXTERNAL_ENTITIES, "true" )
		);
		final JaxbCfgHibernateConfiguration configuration = unmarshal( processor );
		assertThat( configuration.getSessionFactory().getProperty() )
				.extracting( JaxbCfgConfigPropertyType::getName, JaxbCfgConfigPropertyType::getValue )
				.containsExactly( tuple( "hibernate.test.entity.property", "resolved" ) );
	}

	@Test
	public void testDtdRejectedByDefault() {
		final var processor = new JaxbCfgProcessor( classLoaderService );
		assertThatThrownBy( () -> unmarshal( processor ) )
				.isInstanceOf( HibernateException.class );
	}

	private JaxbCfgHibernateConfiguration unmarshal(JaxbCfgProcessor processor) throws Exception {
		try ( InputStream stream = classLoaderService.locateResourceStream( CFG_XML ) ) {
			return processor.unmarshal( stream, new Origin( SourceType.RESOURCE, CFG_XML ) );
		}
	}
}
