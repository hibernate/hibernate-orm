package org.hibernate.orm.test.mapping.where;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.testing.orm.domain.gambit.EmbeddedIdEntity;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.testing.util.ast.HqlHelper.translateHql;

@JiraKey("HHH-21078")
@ServiceRegistry(settings = @Setting(name = AvailableSettings.DIALECT, value = "org.hibernate.dialect.SQLServerDialect"))
@DomainModel(annotatedClasses = {
		EmbeddedIdEntity.class, ToOneSQLRestrictionGroupByTest.Child.class, ToOneSQLRestrictionGroupByTest.Parent.class
})
@SessionFactory(exportSchema = false)
class SQLServerCorrelatedGroupingTest {
	@Test
	void restrictedAssociation(SessionFactoryScope scope) {
		final var sql = translateHql( """
				select c.id from Child c where exists (
					select count(*) from Child x group by c.parent having count(*) > 0
				)
				""", Long.class, scope.getSessionFactory() ).sql();
		assertThat( sql ).containsPattern( "values *\\(\\w+\\.parent_id\\)" )
				.contains( "group by grp_keys_.c0" )
				.doesNotContain( "join parents" );
	}

	@Test
	void groupingExpressionsWithOuterAndLocalColumns(SessionFactoryScope scope) {
		final var sql = translateHql( """
				select c.id from Child c where exists (
					select count(*) from Child x group by upper(c.name), c.id + x.id
				)
				""", Long.class, scope.getSessionFactory() ).sql();
		assertThat( sql ).containsPattern( "values *\\(upper\\(\\w+\\.name\\)\\)" )
				.containsPattern( "group by grp_keys_\\.c0,\\(\\w+\\.id\\+\\w+\\.id\\)" );
	}

	@Test
	void compositeOuterKeyAndLocalKey(SessionFactoryScope scope) {
		final var sql = translateHql( """
				select e.data from EmbeddedIdEntity e where exists (
					select count(*) from EmbeddedIdEntity x group by e.id, x.data
				)
				""", String.class, scope.getSessionFactory() ).sql();
		assertThat( sql ).containsPattern( "values *\\(\\w+\\.value1,\\w+\\.value2\\)" )
				.containsPattern( "group by grp_keys_\\.c0,grp_keys_\\.c1,\\w+\\.data" );
	}
}
