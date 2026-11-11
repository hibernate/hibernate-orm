package org.hibernate.orm.test.mapping.embeddable;

import jakarta.persistence.Entity;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.Setting;

import static org.hibernate.type.SqlTypes.JSON;

/// Actual-provider JSON regression; no synthetic database version is used.
///
/// @author Steve Ebersole
@RequiresDialect(OracleDialect.class)
@ServiceRegistry(settings = @Setting(name = MappingSettings.TEMPORAL_PRESERVATION, value = "true"))
@DomainModel(annotatedClasses = OracleLocalTimeJsonAggregateTests.JsonTimeHolder.class)
@SessionFactory
class OracleLocalTimeJsonAggregateTests extends OracleLocalTimeAggregateTestSupport {
	@Override
	Class<? extends Holder> holderType() {
		return JsonTimeHolder.class;
	}

	@Entity(name = "JsonTimeHolder")
	static class JsonTimeHolder extends Holder {
		@JdbcTypeCode(JSON) Data payload;
		@Override
		Data data() {
			return payload;
		}
		@Override
		void data(Data data) {
			payload = data;
		}
	}
}
