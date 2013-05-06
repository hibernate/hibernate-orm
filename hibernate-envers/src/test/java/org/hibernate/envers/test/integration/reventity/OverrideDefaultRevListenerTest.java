package org.hibernate.envers.test.integration.reventity;

import org.hibernate.internal.util.collections.ArrayHelper;

import org.hibernate.testing.TestForIssue;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@TestForIssue(jiraKey = "HHH-6696")
public class OverrideDefaultRevListenerTest extends GloballyConfiguredRevListenerTest {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return ArrayHelper.join( super.getAnnotatedClasses(), LongRevNumberRevEntity.class );
	}
}