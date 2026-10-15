package org.hibernate.tool.reveng.internal.core.strategy;

import java.io.File;
import java.sql.Types;

import org.hibernate.tool.reveng.api.core.RevengSettings;
import org.hibernate.tool.reveng.api.core.RevengStrategy;
import org.hibernate.tool.reveng.api.core.RevengStrategyFactory;
import org.hibernate.tool.reveng.api.core.TableIdentifier;
import org.hibernate.tool.reveng.guide.GuideStrategy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Exercises the reverse-engineering XML and strategy included in the User Guide.
///
/// @author Steve Ebersole
class BuildToolGuideConfigurationTest {
	@Test
	void xmlPatternsAndTypeMappings() throws Exception {
		var strategy = strategy();
		assertTrue( strategy.excludeTable( TableIdentifier.create( null, "PUBLIC", "BOOK_AUD" ) ) );
		assertTrue( strategy.excludeTable( TableIdentifier.create( null, "PUBLIC", "PROD_BOOK_AUD" ) ) );
		assertFalse( strategy.excludeTable( TableIdentifier.create( null, "PUBLIC", "BOOK" ) ) );
		assertEquals( "example.product.ProdBook", strategy.tableToClassName( TableIdentifier.create( null, "PUBLIC", "PROD_BOOK" ) ) );
		var table = TableIdentifier.create( null, "PUBLIC", "BOOK" );
		assertEquals( "character", strategy.columnToHibernateTypeName( table, "CODE", Types.VARCHAR, 1, 0, 0, true, false ) );
		assertEquals( "big_decimal", strategy.columnToHibernateTypeName( table, "AMOUNT", Types.NUMERIC, 0, 10, 2, true, false ) );
		assertEquals( "big_integer", strategy.columnToHibernateTypeName( table, "AMOUNT", Types.NUMERIC, 0, 12, 0, true, false ) );
	}

	@Test
	void delegatingConstructorPreservesXmlOverrides() throws Exception {
		var strategy = RevengStrategyFactory.createReverseEngineeringStrategy( GuideStrategy.class.getName(), strategy() );
		strategy.setSettings( new RevengSettings( strategy ) );
		assertTrue( strategy.excludeTable( TableIdentifier.create( null, "PUBLIC", "TMP_BOOK" ) ) );
		assertTrue( strategy.excludeTable( TableIdentifier.create( null, "PUBLIC", "BOOK_AUD" ) ) );
		assertEquals( "example.product.ProdBookEntity", strategy.tableToClassName( TableIdentifier.create( null, "PUBLIC", "PROD_BOOK" ) ) );
	}

	@Test
	void xmlValidatesAgainstBundledDtd() throws Exception {
		var factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
		factory.setValidating( true );
		var builder = factory.newDocumentBuilder();
		builder.setEntityResolver( OverrideRepository.DTD_RESOLVER );
		builder.setErrorHandler( new org.xml.sax.helpers.DefaultHandler() {
			@Override
			public void error(org.xml.sax.SAXParseException error) throws org.xml.sax.SAXException {
				throw error;
			}
		} );
		builder.parse( configuration() );
	}

	private RevengStrategy strategy() throws Exception {
		var strategy = RevengStrategyFactory.createReverseEngineeringStrategy( null, new File[] { configuration() } );
		strategy.setSettings( new RevengSettings( strategy ) );
		return strategy;
	}

	private File configuration() throws Exception {
		return new File( getClass().getResource( "/guide/hibernate.reveng.xml" ).toURI() );
	}
}
