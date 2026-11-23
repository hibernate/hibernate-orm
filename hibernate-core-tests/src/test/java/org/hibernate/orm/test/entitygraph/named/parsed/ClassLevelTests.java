package org.hibernate.orm.test.entitygraph.named.parsed;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;

@ServiceRegistry(settings = @Setting(name = AvailableSettings.GRAPH_PARSER_MODE, value = "modern"))
public class ClassLevelTests extends AbstractClassLevelTests {

}
