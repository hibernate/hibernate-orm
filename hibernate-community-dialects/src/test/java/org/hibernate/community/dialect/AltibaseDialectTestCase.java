package org.hibernate.community.dialect;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.pagination.spi.PaginationRequest;
import org.hibernate.dialect.type.spi.DirectJavaTimeJdbcSupport;
import org.hibernate.query.spi.Limit;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.DateTimeUtils;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.spi.TypeConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Unit test of the behavior of the AltibaseDialect utility methods
 *
 * @author Geoffrey Park
 */
@BaseUnitTest
public class AltibaseDialectTestCase {
	private Dialect dialect;

	@BeforeEach
	public void setUp() {
		dialect = new AltibaseDialect( DatabaseVersion.make( 7, 3 ) );
	}

	@AfterEach
	public void tearDown() {
		dialect = null;
	}

	@Test
	@Jira("https://hibernate.atlassian.net/browse/HHH-20874")
	public void testDirectJavaTimeJdbcSupport() {
		final DirectJavaTimeJdbcSupport support = dialect.getDirectJavaTimeJdbcSupport();

		assertThat( support.supports( LocalDate.class ) ).isTrue();
		assertThat( support.supports( LocalTime.class ) ).isTrue();
		assertThat( support.supports( LocalDateTime.class ) ).isTrue();
		assertThat( support.supports( OffsetTime.class ) ).isFalse();
		assertThat( support.supports( OffsetDateTime.class ) ).isFalse();
		assertThat( support.supports( ZonedDateTime.class ) ).isFalse();
		assertThat( support.supports( Instant.class ) ).isFalse();
	}

	@Test
	@Jira("https://hibernate.atlassian.net/browse/HHH-20874")
	public void testTimestampPrecisionTruncation() {
		assertThat( DateTimeUtils.adjustToDefaultPrecision(
				Instant.parse( "2026-09-14T12:34:56.294520789Z" ), dialect
		) ).isEqualTo( Instant.parse( "2026-09-14T12:34:56.294520Z" ) );
		assertThat( DateTimeUtils.adjustToDefaultPrecision(
				Instant.parse( "2026-09-14T12:34:56.999999999Z" ), dialect
		) ).isEqualTo( Instant.parse( "2026-09-14T12:34:56.999999Z" ) );
	}

	@Test
	public void testSupportLimits() {
		assertThat( dialect.getLimitHandler().supportsLimit() ).isTrue();
	}

	@Test
	public void testSelectWithLimitOnly() {
		assertThat( withLimit( "select c1, c2 from t1 order by c1, c2 desc",
				toRowSelection( null, 15 ) ).toLowerCase( Locale.ROOT ) )
				.isEqualTo( "select c1, c2 from t1 order by c1, c2 desc limit ?" );
	}

	@Test
	public void testSelectWithOffsetLimit() {
		assertThat( withLimit( "select c1, c2 from t1 order by c1, c2 desc",
				toRowSelection( 5, 15 ) ).toLowerCase( Locale.ROOT ) )
				.isEqualTo( "select c1, c2 from t1 order by c1, c2 desc limit 1+?,?" );
	}

	@Test
	public void testSelectWithNoLimit() {
		assertThat( withLimit( "select c1, c2 from t1 order by c1, c2 desc", null ).toLowerCase( Locale.ROOT ) )
				.isEqualTo( "select c1, c2 from t1 order by c1, c2 desc" );
	}

	@Test
	public void testJsonTypeNotRegisteredForAltibase71() {
		final Dialect dialect = new AltibaseDialect( DatabaseVersion.make( 7, 1 ) );
		final TypeConfiguration typeConfiguration = typeConfigurationFor( dialect );

		assertThat( typeConfiguration.getDdlTypeRegistry().getDescriptor( SqlTypes.JSON ) ).isNull();
		assertThat( typeConfiguration.getJdbcTypeRegistry().findDescriptor( SqlTypes.JSON ) ).isNull();
	}

	@Test
	public void testJsonTypeRegisteredForAltibase81() {
		final Dialect dialect = new AltibaseDialect( DatabaseVersion.make( 8, 1 ) );
		final TypeConfiguration typeConfiguration = typeConfigurationFor( dialect );

		assertThat( typeConfiguration.getDdlTypeRegistry().getTypeName( SqlTypes.JSON, dialect ) )
				.isEqualTo( "json" );
		assertThat( typeConfiguration.getDdlTypeRegistry().getSqlTypeCode( "json" ) )
				.isEqualTo( SqlTypes.JSON );

		final JdbcType jsonJdbcType = typeConfiguration.getJdbcTypeRegistry().findDescriptor( SqlTypes.JSON );
		assertThat( jsonJdbcType ).isNotNull();
		assertThat( jsonJdbcType.getDdlTypeCode() ).isEqualTo( SqlTypes.JSON );
		assertThat( dialect.resolveSqlTypeDescriptor(
				"json",
				Types.CLOB,
				0,
				0,
				typeConfiguration.getJdbcTypeRegistry()
		).getDdlTypeCode() ).isEqualTo( SqlTypes.JSON );
	}

	private String withLimit(String sql, Limit limit) {
		return dialect.getLimitHandler().processSql(
				new PaginationRequest(
						sql,
						limit == null ? null : limit.getFirstRow(),
						limit == null ? null : limit.getMaxRows(),
						-1,
						null
				)
		).sql();
	}

	private TypeConfiguration typeConfigurationFor(Dialect dialect) {
		final TypeConfiguration typeConfiguration = new TypeConfiguration();
		dialect.contributeTypes( () -> typeConfiguration, null );
		return typeConfiguration;
	}

	private Limit toRowSelection(Integer firstRow, Integer maxRows) {
		Limit selection = new Limit();
		selection.setFirstRow( firstRow );
		selection.setMaxRows( maxRows );
		return selection;
	}
}
