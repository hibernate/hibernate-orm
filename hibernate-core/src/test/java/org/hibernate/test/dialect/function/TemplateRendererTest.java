package org.hibernate.test.dialect.function;

import java.util.Collections;

import org.hibernate.dialect.function.TemplateRenderer;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Assert;
import org.junit.Test;

public class TemplateRendererTest extends BaseCoreFunctionalTestCase {
	
	@Test
	@TestForIssue( jiraKey = "HHH-13846" )
	public void testParamIndexWithoutDigit() {
		String template = "REGEXP_REPLACE( "
				+ "    REPLACE( "
				+ "        TRANSLATE( "
				+ "            LOWER(?1), "
				+ "            'áàãâäéèêëíìîïóòõôöúùûüç', "
				+ "            'aaaaaeeeeiiiiooooouuuuc' "
				+ "        ), "
				+ "        ' ', "
				+ "        '' "
				+ "    ), "
				+ "    '[\\u2000-\\u206F\\u2E00-\\u2E7F\\\\''!\"#$%&()*+,\\-.\\/:;<=>?@\\[\\]^_`{|}~]', " // -- '?'' causes the problem
				+ "    '' "
				+ ")";

		TemplateRenderer templateRenderer = new TemplateRenderer( template );
		Assert.assertEquals( 1, templateRenderer.getAnticipatedNumberOfArguments() );
		String renderResult = templateRenderer.render( Collections.singletonList( "argument" ), null );
		String expectedRenderResult = template.replace( "?1", "argument" );
		Assert.assertEquals( expectedRenderResult, renderResult );
	}
}
