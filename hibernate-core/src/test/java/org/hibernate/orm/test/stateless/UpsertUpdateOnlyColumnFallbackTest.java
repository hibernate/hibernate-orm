package org.hibernate.orm.test.stateless;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.RequiresDialects;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;

import static org.hibernate.cfg.JdbcSettings.DIALECT;

@RequiresDialects(@RequiresDialect(H2Dialect.class))
@ServiceRegistry(settings = @Setting(name = DIALECT,
		value = "org.hibernate.orm.test.secondarytable.OptionalSecondaryRowFallbackTest$FallbackDialect"))
class UpsertUpdateOnlyColumnFallbackTest extends UpsertUpdateOnlyColumnTest {
}
