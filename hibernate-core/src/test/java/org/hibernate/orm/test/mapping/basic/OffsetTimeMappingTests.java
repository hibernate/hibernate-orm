package org.hibernate.orm.test.mapping.basic;

import java.time.OffsetTime;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.orm.test.mapping.javatime.redesign.TemporalEligibilityChecks;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.internal.BasicAttributeMapping;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.SqlTypes;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import org.hamcrest.Matcher;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.isOneOf;

/**
 * @author Steve Ebersole
 */
@RequiresDialectFeature(feature = TemporalEligibilityChecks.InlineOffsetTime.class)
@DomainModel(annotatedClasses = OffsetTimeMappingTests.EntityWithOffsetTime.class)
@SessionFactory
public class OffsetTimeMappingTests {

	@Test
	public void verifyMappings(SessionFactoryScope scope) {
		final SessionFactoryImplementor sf = scope.getSessionFactory();

		final MappingMetamodelImplementor mappingMetamodel = sf.getRuntimeMetamodels().getMappingMetamodel();
		final EntityPersister entityDescriptor = mappingMetamodel.findEntityDescriptor(EntityWithOffsetTime.class);

		final BasicAttributeMapping attributeMapping = (BasicAttributeMapping) entityDescriptor.findAttributeMapping("offsetTime");
		final JdbcMapping jdbcMapping = attributeMapping.getJdbcMapping();
		assertThat(jdbcMapping.getJavaTypeDescriptor().getJavaTypeClass(), equalTo(OffsetTime.class));

		final Matcher<Integer> expectedJdbcType = sf.getSessionFactoryOptions().isDirectJavaTimeJdbcAccessEnabled( OffsetTime.class )
				? equalTo( SqlTypes.OFFSET_TIME )
				: isOneOf( SqlTypes.TIME, SqlTypes.TIMESTAMP_WITH_TIMEZONE, SqlTypes.TIME_WITH_TIMEZONE );
		assertThat( jdbcMapping.getJdbcType().getJdbcTypeCode(), expectedJdbcType );

		scope.inTransaction(
				(session) -> {
					session.persist(new EntityWithOffsetTime(1, OffsetTime.parse( "12:34:56+02:00" )));
				}
		);

		scope.inTransaction(
				(session) -> {
					final var actual = session.find(EntityWithOffsetTime.class, 1);
					if ( sf.getSessionFactoryOptions().isTemporalPreservationEnabled() ) {
						assertThat( actual.offsetTime, equalTo( OffsetTime.parse( "12:34:56+02:00" ) ) );
					}
					session.remove( actual );
				}
		);
	}

	@Entity(name = "EntityWithOffsetTime")
	@Table(name = "EntityWithOffsetTime")
	public static class EntityWithOffsetTime {
		@Id
		private Integer id;

		//tag::basic-offsetTime-example[]
		// requires INLINE handling that preserves local time and offset
		private OffsetTime offsetTime;
		//end::basic-offsetTime-example[]

		public EntityWithOffsetTime() {
		}

		public EntityWithOffsetTime(Integer id, OffsetTime offsetTime) {
			this.id = id;
			this.offsetTime = offsetTime;
		}
	}
}
