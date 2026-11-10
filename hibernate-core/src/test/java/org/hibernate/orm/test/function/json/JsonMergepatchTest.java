package org.hibernate.orm.test.function.json;

import org.hibernate.cfg.MappingSettings;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

/**
 * @author Christian Beikov
 */
// Retain the shared fixture's legacy temporal interpretation while testing query behavior.
@ServiceRegistry(settings = @Setting(name = MappingSettings.TEMPORAL_PRESERVATION, value = "false"))
@DomainModel(standardModels = StandardDomainModel.GAMBIT)
@SessionFactory
@RequiresDialectFeature( feature = DialectFeatureChecks.SupportsJsonMergepatch.class)
public class JsonMergepatchTest {

	@Test
	public void testSimple(SessionFactoryScope scope) {
		scope.inSession( em -> {
			//tag::hql-json-mergepatch-example[]
			em.createQuery( "select json_mergepatch('{\"a\":1}', '{\"b\":2}')" ).getResultList();
			//end::hql-json-mergepatch-example[]
		} );
	}

}
