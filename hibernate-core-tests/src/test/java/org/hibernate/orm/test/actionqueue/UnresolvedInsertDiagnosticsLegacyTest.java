package org.hibernate.orm.test.actionqueue;

import org.hibernate.cfg.FlushSettings;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;

@ServiceRegistry(settings = @Setting(name = FlushSettings.FLUSH_QUEUE_TYPE, value = "legacy"))
public class UnresolvedInsertDiagnosticsLegacyTest extends AbstractUnresolvedInsertDiagnosticsTest {
}
