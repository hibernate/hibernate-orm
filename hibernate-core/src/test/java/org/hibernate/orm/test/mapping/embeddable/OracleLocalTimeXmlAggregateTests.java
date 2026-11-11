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
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.hibernate.testing.orm.junit.VersionMatchMode;

import static org.hibernate.type.SqlTypes.SQLXML;

/// Actual-provider XML regression; no synthetic database version is used.
///
/// @author Steve Ebersole
@RequiresDialect(OracleDialect.class)
@SkipForDialect(dialectClass = OracleDialect.class, majorVersion = 23, versionMatchMode = VersionMatchMode.SAME_OR_NEWER,
		reason = "Oracle Bug 37319693 affects XML check constraints")
@ServiceRegistry(settings = @Setting(name = MappingSettings.TEMPORAL_PRESERVATION, value = "true"))
@DomainModel(annotatedClasses = OracleLocalTimeXmlAggregateTests.XmlTimeHolder.class)
@SessionFactory
class OracleLocalTimeXmlAggregateTests extends OracleLocalTimeAggregateTestSupport {
	@Override
	Class<? extends Holder> holderType() {
		return XmlTimeHolder.class;
	}

	@Entity(name = "XmlTimeHolder")
	static class XmlTimeHolder extends Holder {
		@JdbcTypeCode(SQLXML) Data payload;
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
