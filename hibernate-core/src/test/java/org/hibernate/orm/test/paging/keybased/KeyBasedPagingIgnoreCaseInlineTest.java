package org.hibernate.orm.test.paging.keybased;

import org.hibernate.cfg.QuerySettings;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;

/**
 * Runs {@link KeyBasedPagingIgnoreCaseTest} with criteria values inlined as literals.
 */
@ServiceRegistry(settings = @Setting(name = QuerySettings.CRITERIA_VALUE_HANDLING_MODE, value = "inline"))
@JiraKey("HHH-21161")
public class KeyBasedPagingIgnoreCaseInlineTest extends KeyBasedPagingIgnoreCaseTest {
}
