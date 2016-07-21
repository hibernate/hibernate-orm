package org.hibernate.boot.model.naming;

import org.hibernate.boot.model.source.spi.AttributePath;
import org.hibernate.boot.spi.MetadataBuildingContext;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Test for HHH-10810
 *
 * @author Dmytro Bondar
 */
public class ImplicitIndexColumnNameSourceTest {

	@Test
	@TestForIssue(jiraKey = "HHH-10810")
	public void testExtensionImplicitNameSource() {
		ImplicitIndexColumnNameSource implicitIndexColumnNameSource = new ImplicitIndexColumnNameSource() {
			@Override
			public AttributePath getPluralAttributePath() {
				return null;
			}

			@Override
			public MetadataBuildingContext getBuildingContext() {
				return null;
			}
		};
		assertTrue( implicitIndexColumnNameSource instanceof ImplicitNameSource );
	}

}
