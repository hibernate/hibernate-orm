/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.envers.integration.reventity;

import org.hibernate.internal.util.collections.ArrayHelper;

import org.hibernate.testing.orm.junit.JiraKey;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@JiraKey(value = "HHH-6696")
public class OverrideDefaultRevListenerTest extends GloballyConfiguredRevListenerTest {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return ArrayHelper.join( super.getAnnotatedClasses(), LongRevNumberRevEntity.class );
	}
}