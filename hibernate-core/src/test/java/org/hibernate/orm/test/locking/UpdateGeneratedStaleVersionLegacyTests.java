package org.hibernate.orm.test.locking;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.Setting;

import static org.hibernate.cfg.FlushSettings.FLUSH_QUEUE_TYPE;

@DomainModel(annotatedClasses = UpdateGeneratedStaleVersionTests.Generation.class)
@SessionFactory
@ServiceRegistry(settings = @Setting(name = FLUSH_QUEUE_TYPE, value = "legacy"))
class UpdateGeneratedStaleVersionLegacyTests extends UpdateGeneratedStaleVersionTests {
}
