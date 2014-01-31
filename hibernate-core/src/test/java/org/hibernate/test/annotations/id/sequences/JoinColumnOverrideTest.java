//$Id$
package org.hibernate.test.annotations.id.sequences;

import org.hibernate.metamodel.Metadata;
import org.hibernate.metamodel.MetadataSources;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.relational.Table;

import org.hibernate.testing.FailureExpectedWithNewMetamodel;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.test.annotations.id.sequences.entities.Bunny;
import org.hibernate.test.annotations.id.sequences.entities.PointyTooth;
import org.hibernate.test.annotations.id.sequences.entities.TwinkleToes;
import org.junit.Test;

import org.jboss.logging.Logger;

import static org.junit.Assert.assertEquals;

/**
 * Tests for JIRA issue ANN-748.
 *
 * @author Hardy Ferentschik
 */
@SuppressWarnings("unchecked")
public class JoinColumnOverrideTest extends BaseUnitTestCase {
	private static final Logger log = Logger.getLogger( JoinColumnOverrideTest.class );

	@Test
	@TestForIssue( jiraKey = "ANN-748" )
	public void testBlownPrecision() throws Exception {
		MetadataSources sources = new MetadataSources()
				.addAnnotatedClass( Bunny.class )
				.addAnnotatedClass( PointyTooth.class )
				.addAnnotatedClass( TwinkleToes.class );
		Metadata metadata = sources.buildMetadata();

		EntityBinding eb = metadata.getEntityBinding( PointyTooth.class.getName() );
		Table table = (Table) eb.getPrimaryTable();

		final org.hibernate.metamodel.spi.relational.Column idColumn = table.locateColumn( "id" );
		assertEquals( 128, idColumn.getSize().getPrecision() );
	}
}
