package org.hibernate.orm.test.locking;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;

import static org.hibernate.cfg.JdbcSettings.USE_GET_GENERATED_KEYS;

@ServiceRegistry(settings = @Setting(name = USE_GET_GENERATED_KEYS, value = "false"))
class UpdateGeneratedStaleVersionNoGetGeneratedKeysTests extends UpdateGeneratedStaleVersionTests {
}
