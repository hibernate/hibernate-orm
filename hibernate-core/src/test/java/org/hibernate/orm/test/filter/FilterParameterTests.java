/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.filter;

import java.sql.Types;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.hibernate.SharedSessionContract;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.ParamDef;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.community.dialect.AltibaseDialect;
import org.hibernate.community.dialect.FirebirdDialect;
import org.hibernate.dialect.GaussDBDialect;
import org.hibernate.dialect.HANADialect;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.community.dialect.DerbyDialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgresPlusDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.community.dialect.TiDBDialect;
import org.hibernate.type.NumericBooleanConverter;
import org.hibernate.type.YesNoConverter;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import jakarta.enterprise.inject.se.SeContainer;
import jakarta.enterprise.inject.se.SeContainerInitializer;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Steve Ebersole
 */
@DomainModel( annotatedClasses = {
		FilterParameterTests.EntityOne.class,
		FilterParameterTests.EntityTwo.class,
		FilterParameterTests.EntityThree.class,
		FilterParameterTests.EntityFour.class
} )
public class FilterParameterTests extends AbstractStatefulStatelessFilterTest {

	@ParameterizedTest
	@MethodSource("transactionKind")
	public void testYesNo(BiConsumer<SessionFactoryScope, Consumer<? extends SharedSessionContract>> inTransaction) {
		scope.inTransaction( (session) -> {
			session.disableFilter( "subDepartmentFilter" );
			final EntityOne loaded = session.byId( EntityOne.class ).load( 1 );
			assertThat( loaded ).isNotNull();
		} );

		inTransaction.accept( scope, session -> {
			session.disableFilter( "subDepartmentFilter" );
			session.enableFilter( "filterYesNoConverter" ).setParameter( "yesNo", Boolean.FALSE );

			final EntityOne loaded = session.createQuery( "from EntityOne e where e.id = :id", EntityOne.class )
					.setParameter( "id", 1 )
					.getSingleResultOrNull();
			assertThat( loaded ).isNull();
		} );
	}

	@ParameterizedTest
	@MethodSource("transactionKind")
	@SkipForDialect(dialectClass = H2Dialect.class, reason = "H2 silently converts a boolean to string types")
	@SkipForDialect(dialectClass = HSQLDialect.class, reason = "HSQL silently converts a boolean to string types")
	@SkipForDialect(dialectClass = DerbyDialect.class, reason = "Derby silently converts a boolean to string types")
	@SkipForDialect(dialectClass = DB2Dialect.class, reason = "DB2 silently converts a boolean to string types")
	@SkipForDialect(dialectClass = MySQLDialect.class, reason = "MySQL silently converts a boolean to string types")
	@SkipForDialect(dialectClass = MariaDBDialect.class, reason = "MariaDB silently converts a boolean to string types")
	@SkipForDialect(dialectClass = TiDBDialect.class, reason = "TiDB silently converts a boolean to string types")
	@SkipForDialect(dialectClass = SybaseDialect.class, matchSubTypes = true, reason = "Sybase silently converts a boolean to string types")
	@SkipForDialect(dialectClass = HANADialect.class, matchSubTypes = true, reason = "HANA silently converts a boolean to string types")
	@SkipForDialect(dialectClass = CockroachDialect.class, matchSubTypes = true, reason = "Cockroach silently converts a boolean to string types")
	@SkipForDialect(dialectClass = PostgresPlusDialect.class, reason = "PostgresPlus silently converts a boolean to string types")
	@SkipForDialect(dialectClass = FirebirdDialect.class, reason = "Firebird silently converts a boolean to string")
	@SkipForDialect(dialectClass = AltibaseDialect.class, reason = "Altibase silently converts a boolean to string")
	@SkipForDialect(dialectClass = OracleDialect.class, majorVersion = 23, reason = "Oracle 23 interprets Y and T as true and N and F as false, so this works")
	public void testYesNoMismatch(BiConsumer<SessionFactoryScope, Consumer<? extends SharedSessionContract>> inTransaction) {
		scope.inTransaction( (session) -> {
			session.disableFilter( "subDepartmentFilter" );
			final EntityOne loaded = session.byId( EntityOne.class ).load( 1 );
			assertThat( loaded ).isNotNull();
		} );

		inTransaction.accept( scope, session -> {
			session.disableFilter( "subDepartmentFilter" );
			session.enableFilter( "filterYesNoBoolean" ).setParameter( "yesNo", Boolean.FALSE );

			assertThatThrownBy( () -> session.createQuery( "from EntityOne e where e.id = :id", EntityOne.class )
					.setParameter( "id", 1 )
					.getSingleResultOrNull() )
					.isInstanceOf( Exception.class );
		} );
	}

	@ParameterizedTest
	@MethodSource("transactionKind")
	public void testNumeric(BiConsumer<SessionFactoryScope, Consumer<? extends SharedSessionContract>> inTransaction) {
		scope.inTransaction( (session) -> {
			session.disableFilter( "subDepartmentFilter" );
			final EntityTwo loaded = session.byId( EntityTwo.class ).load( 1 );
			assertThat( loaded ).isNotNull();
		} );

		inTransaction.accept( scope, session -> {
			session.disableFilter( "subDepartmentFilter" );
			session.enableFilter( "filterNumberConverter" ).setParameter( "zeroOne", Boolean.FALSE );

			final EntityTwo loaded = session.createQuery( "from EntityTwo e where e.id = :id", EntityTwo.class )
					.setParameter( "id", 1 )
					.getSingleResultOrNull();
			assertThat( loaded ).isNull();
		} );
	}

	@ParameterizedTest
	@MethodSource("transactionKind")
	@SkipForDialect(dialectClass = H2Dialect.class, reason = "H2 silently converts a boolean to integral types")
	@SkipForDialect(dialectClass = GaussDBDialect.class, reason = "type:resolved.gaussdb silently converts a boolean to integral types")
	@SkipForDialect(dialectClass = OracleDialect.class, reason = "Oracle silently converts a boolean to integral types")
	@SkipForDialect(dialectClass = HSQLDialect.class, reason = "HSQL silently converts a boolean to integral types")
	@SkipForDialect(dialectClass = DerbyDialect.class, reason = "Derby silently converts a boolean to integral types")
	@SkipForDialect(dialectClass = DB2Dialect.class, reason = "DB2 silently converts a boolean to integral types")
	@SkipForDialect(dialectClass = MySQLDialect.class, reason = "MySQL silently converts a boolean to integral types")
	@SkipForDialect(dialectClass = MariaDBDialect.class, reason = "MariaDB silently converts a boolean to integral types")
	@SkipForDialect(dialectClass = TiDBDialect.class, reason = "TiDB silently converts a boolean to integral types")
	@SkipForDialect(dialectClass = SQLServerDialect.class, reason = "SQL Server silently converts a boolean to integral types")
	@SkipForDialect(dialectClass = AltibaseDialect.class, reason = "Altibase silently converts a boolean to integral types")
	@SkipForDialect(dialectClass = SybaseDialect.class, matchSubTypes = true, reason = "Sybase silently converts a boolean to integral types")
	@SkipForDialect(dialectClass = HANADialect.class, matchSubTypes = true, reason = "HANA silently converts a boolean to integral types")
	@SkipForDialect(dialectClass = FirebirdDialect.class, matchSubTypes = true, reason = "Firebird silently converts a boolean to integral types")
	public void testNumericMismatch(BiConsumer<SessionFactoryScope, Consumer<? extends SharedSessionContract>> inTransaction) {
		scope.inTransaction( (session) -> {
			session.disableFilter( "subDepartmentFilter" );
			final EntityTwo loaded = session.byId( EntityTwo.class ).load( 1 );
			assertThat( loaded ).isNotNull();
		} );

		inTransaction.accept( scope, session -> {
			session.disableFilter( "subDepartmentFilter" );
			session.enableFilter( "filterNumberBoolean" ).setParameter( "zeroOne", Boolean.FALSE );

			assertThatThrownBy( () -> session.createQuery( "from EntityTwo e where e.id = :id", EntityTwo.class )
					.setParameter( "id", 1 )
					.getSingleResultOrNull() )
					.isInstanceOf( Exception.class );
		} );
	}

	@ParameterizedTest
	@MethodSource("transactionKind")
	@SkipForDialect(dialectClass = MySQLDialect.class, reason = "MySQL silently converts strings to integral types")
	@SkipForDialect(dialectClass = MariaDBDialect.class, reason = "MariaDB silently converts strings to integral types")
	@SkipForDialect(dialectClass = TiDBDialect.class, reason = "TiDB silently converts strings to integral types")
	@SkipForDialect(dialectClass = PostgresPlusDialect.class, reason = "PostgresPlus silently converts strings to integral types")
	public void testMismatch(BiConsumer<SessionFactoryScope, Consumer<? extends SharedSessionContract>> inTransaction) {
		scope.inTransaction( (session) -> {
			session.disableFilter( "subDepartmentFilter" );
			final EntityThree loaded = session.byId( EntityThree.class ).load( 1 );
			assertThat( loaded ).isNotNull();
		} );

		inTransaction.accept( scope, session -> {
			session.disableFilter( "subDepartmentFilter" );
			session.enableFilter( "filterMismatchConverter" ).setParameter( "mismatch", Boolean.FALSE );

			assertThatThrownBy( () -> session.createQuery( "from EntityThree e where e.id = :id", EntityThree.class )
					.setParameter( "id", 1 )
					.getSingleResultOrNull() )
					.isInstanceOf( Exception.class );
		} );
	}

	@ParameterizedTest
	@MethodSource("transactionKind")
	public void testAutoEnableWithResolver() {
		final SeContainerInitializer cdiInitializer = SeContainerInitializer.newInstance()
				.disableDiscovery()
				.addBeanClasses( EntityFourDepartmentResolver.class );
		try ( final SeContainer cdiContainer = cdiInitializer.initialize() ) {
			BootstrapServiceRegistry bsr = new BootstrapServiceRegistryBuilder().build();

			final StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistryBuilder( bsr )
					.applySetting( AvailableSettings.CDI_BEAN_MANAGER, cdiContainer.getBeanManager() )
					.build();

			try {
				scope.inTransaction( (session) -> {
					session.getEnabledFilter("subDepartmentFilter").setParameter("subdepartment", "FIRST_A"  );
					final EntityFour first_a = session.createQuery( "from EntityFour e where e.id = :id", EntityFour.class )
							.setParameter( "id", 1 )
							.getSingleResultOrNull();
					assertThat( first_a ).isNotNull();
					assertThat( first_a.getDepartment() ).isEqualTo( "FIRST" );
					session.getEnabledFilter("subDepartmentFilter").setParameter("subdepartment", "SECOND_A"  );
					final EntityFour second = session.createQuery( "from EntityFour e where e.id = :id", EntityFour.class )
							.setParameter( "id", 3 )
							.getSingleResultOrNull();
					assertThat( second ).isNull();
				} );
			}
			finally {
				StandardServiceRegistryBuilder.destroy( ssr );
			}
		}
	}

	@ParameterizedTest
	@MethodSource("transactionKind")
	public void testAutoEnableWithoutResolver() {
		final SeContainerInitializer cdiInitializer = SeContainerInitializer.newInstance()
				.disableDiscovery()
				.addBeanClasses( EntityFourDepartmentResolver.class );
		try ( final SeContainer cdiContainer = cdiInitializer.initialize() ) {
			BootstrapServiceRegistry bsr = new BootstrapServiceRegistryBuilder().build();

			final StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistryBuilder( bsr )
					.applySetting( AvailableSettings.CDI_BEAN_MANAGER, cdiContainer.getBeanManager() )
					.build();

			try {
				scope.inTransaction( (session) -> {
					session.getEnabledFilter("subDepartmentFilter").setParameter("subdepartment", "FIRST_A"  );
					final EntityFour first_a = session.createQuery( "from EntityFour e where e.id = :id", EntityFour.class )
							.setParameter( "id", 1 )
							.getSingleResultOrNull();
					assertThat( first_a ).isNotNull();
					assertThat( first_a.getDepartment() ).isEqualTo( "FIRST" );
					final EntityFour first_b = session.createQuery( "from EntityFour e where e.id = :id", EntityFour.class )
							.setParameter( "id", 2 )
							.getSingleResultOrNull();
					assertThat( first_b ).isNull();
				} );
			}
			finally {
				StandardServiceRegistryBuilder.destroy( ssr );
			}
		}
	}


	@BeforeEach
	public void prepareTestData() {
		scope.inTransaction( (session) -> {
			session.persist( new EntityOne( 1, "one" ) );
			session.persist( new EntityTwo( 1, "two" ) );
			session.persist( new EntityThree( 1, "three" ) );
			session.persist( new EntityFour( 1, "four", "FIRST", "FIRST_A" ) );
			session.persist( new EntityFour( 2, "four", "FIRST", "FIRST_B" ) );
			session.persist( new EntityFour( 3, "four", "SECOND", "SECOND_A" ) );
		} );
	}

	@AfterEach
	public void dropTestData() {
		scope.inTransaction( (session) -> {
			session.disableFilter( "subDepartmentFilter" );
			session.disableFilter( "departmentFilter" );
			session.createMutationQuery( "delete EntityOne" ).executeUpdate();
			session.createMutationQuery( "delete EntityTwo" ).executeUpdate();
			session.createMutationQuery( "delete EntityThree" ).executeUpdate();
			session.createMutationQuery( "delete EntityFour" ).executeUpdate();
		} );
	}

	@FilterDef(
			name = "filterYesNoConverter",
			defaultCondition = "yes_no = :yesNo",
			parameters = @ParamDef( name = "yesNo", type = YesNoConverter.class )
	)
	@FilterDef(
			name = "filterYesNoBoolean",
			defaultCondition = "yes_no = :yesNo",
			parameters = @ParamDef( name = "yesNo", type = Boolean.class )
	)
	@Filter( name = "filterYesNoConverter" )
	@Filter( name = "filterYesNoBoolean" )
	@Entity( name = "EntityOne" )
	@Table( name = "EntityOne" )
	public static class EntityOne {
		@Id
		private Integer id;
		@Basic
		private String name;
		@Column( name = "yes_no" )
		@JdbcTypeCode( Types.CHAR )
		@Convert( converter = YesNoConverter.class )
		private boolean yesNo = true;

		private EntityOne() {
			// for use by Hibernate
		}

		public EntityOne(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public boolean isYesNo() {
			return yesNo;
		}

		public void setYesNo(boolean yesNo) {
			this.yesNo = yesNo;
		}
	}


	@FilterDef(
			name = "filterNumberConverter",
			defaultCondition = "zero_one = :zeroOne",
			parameters = @ParamDef( name = "zeroOne", type = NumericBooleanConverter.class )
	)
	@FilterDef(
			name = "filterNumberBoolean",
			defaultCondition = "zero_one = :zeroOne",
			parameters = @ParamDef( name = "zeroOne", type = Boolean.class )
	)
	@Filter( name = "filterNumberConverter" )
	@Filter( name = "filterNumberBoolean" )
	@Entity( name = "EntityTwo" )
	@Table( name = "EntityTwo" )
	public static class EntityTwo {
		@Id
		private Integer id;
		@Basic
		private String name;
		@Column( name = "zero_one" )
		@JdbcTypeCode( Types.TINYINT )
		@Convert( converter = NumericBooleanConverter.class )
		private boolean zeroOne = true;

		private EntityTwo() {
			// for use by Hibernate
		}

		public EntityTwo(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public boolean isZeroOne() {
			return zeroOne;
		}

		public void setZeroOne(boolean zeroOne) {
			this.zeroOne = zeroOne;
		}
	}

	@FilterDef(
			name = "filterMismatchConverter",
			defaultCondition = "mismatch = :mismatch",
			parameters = @ParamDef( name = "mismatch", type = YesNoConverter.class )
	)
	@Filter( name = "filterMismatchConverter" )
	@Entity( name = "EntityThree" )
	@Table( name = "EntityThree" )
	public static class EntityThree {
		@Id
		private Integer id;
		@Basic
		private String name;
		@Column( name = "mismatch" )
		@JdbcTypeCode( Types.TINYINT )
		@Convert( converter = NumericBooleanConverter.class )
		private boolean mismatch = true;

		private EntityThree() {
			// for use by Hibernate
		}

		public EntityThree(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public boolean isMismatch() {
			return mismatch;
		}

		public void setMismatch(boolean mismatch) {
			this.mismatch = mismatch;
		}
	}

	@FilterDef(
			name = "departmentFilter",
			defaultCondition = "department = :department",
			parameters = @ParamDef( name = "department", type = String.class, resolver = EntityFourDepartmentResolver.class),
			autoEnabled = true
	)
	@Filter( name = "departmentFilter" )
	@FilterDef(
			name = "subDepartmentFilter",
			defaultCondition = "subdepartment = :subdepartment",
			parameters = @ParamDef( name = "subdepartment", type = String.class ),
			autoEnabled = true
	)
	@Filter( name = "subDepartmentFilter" )
	@Entity( name = "EntityFour" )
	@Table( name = "EntityFour" )
	public static class EntityFour {
		@Id
		private Integer id;
		@Basic
		private String name;
		private String department;
		private String subdepartment;

		private EntityFour() {
			// for use by Hibernate
		}

		public EntityFour(Integer id, String name, String department, String subdepartment) {
			this.id = id;
			this.name = name;
			this.department = department;
			this.subdepartment = subdepartment;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getDepartment() {
			return department;
		}

		public void setDepartment(String department) {
			this.department = department;
		}

		public String getSubdepartment() {
			return subdepartment;
		}

		public void setSubdepartment(String subdepartment) {
			this.subdepartment = subdepartment;
		}
	}

	public static class EntityFourDepartmentResolver implements Supplier<String> {
		@Override
		public String get() {
			return "FIRST";
		}
	}
}
