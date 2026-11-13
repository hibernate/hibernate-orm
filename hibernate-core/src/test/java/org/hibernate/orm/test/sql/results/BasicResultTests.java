package org.hibernate.orm.test.sql.results;

import org.hibernate.cfg.MappingSettings;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.sql.ast.spi.query.select.SelectStatement;
import org.hibernate.sql.results.graph.DomainResultGraphPrinter;

import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

/**
 * @author Steve Ebersole
 */
@DomainModel( standardModels = StandardDomainModel.GAMBIT )
@SessionFactory( exportSchema = false )
// Retain the legacy temporal semantics of this regression fixture.
@ServiceRegistry(settings = @Setting(name = MappingSettings.TEMPORAL_PRESERVATION, value = "false"))
public class BasicResultTests extends AbstractResultTests {
	@Test
	public void simpleTest(SessionFactoryScope scope) {
		final SelectStatement sqlAst = interpret(
				"select s.id, s.someString, s.someLong from SimpleEntity s",
				scope.getSessionFactory()
		);
		DomainResultGraphPrinter.logDomainResultGraph( sqlAst.getDomainResultDescriptors() );

	}
}
