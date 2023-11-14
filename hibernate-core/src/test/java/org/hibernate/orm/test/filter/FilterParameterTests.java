/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.filter;

import java.sql.Types;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.ParamDef;
import org.hibernate.community.dialect.AltibaseDialect;
import org.hibernate.community.dialect.FirebirdDialect;
import org.hibernate.dialect.AbstractHANADialect;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.DerbyDialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgresPlusDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.dialect.TiDBDialect;
import org.hibernate.type.NumericBooleanConverter;
import org.hibernate.type.YesNoConverter;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Steve Ebersole
 */
@DomainModel( annotatedClasses = {
		FilterParameterTests.EntityOne.class,
		FilterParameterTests.EntityTwo.class,
		FilterParameterTests.EntityThree.class
} )
@SessionFactory
public class FilterParameterTests {

	@Test
	public void testYesNo(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final EntityOne loaded = session.byId( EntityOne.class ).load( 1 );
			assertThat( loaded ).isNotNull();
		} );

		scope.inTransaction( (session) -> {
			session.enableFilter( "filterYesNoConverter" ).setParameter( "yesNo", Boolean.FALSE );

			final EntityOne loaded = session.createQuery( "from EntityOne e where e.id = :id", EntityOne.class )
					.setParameter( "id", 1 )
					.getSingleResultOrNull();
			assertThat( loaded ).isNull();
		} );
	}

	@Test
	@SkipForDialect(dialectClass = H2Dialect.class, reason = "H2 silently converts a boolean to string types")
	@SkipForDialect(dialectClass = HSQLDialect.class, reason = "HSQL silently converts a boolean to string types")
	@SkipForDialect(dialectClass = DerbyDialect.class, reason = "Derby silently converts a boolean to string types")
	@SkipForDialect(dialectClass = DB2Dialect.class, reason = "DB2 silently converts a boolean to string types")
	@SkipForDialect(dialectClass = MySQLDialect.class, reason = "MySQL silently converts a boolean to string types")
	@SkipForDialect(dialectClass = MariaDBDialect.class, reason = "MariaDB silently converts a boolean to string types")
	@SkipForDialect(dialectClass = TiDBDialect.class, reason = "TiDB silently converts a boolean to string types")
	@SkipForDialect(dialectClass = SybaseDialect.class, matchSubTypes = true, reason = "Sybase silently converts a boolean to string types")
	@SkipForDialect(dialectClass = AbstractHANADialect.class, matchSubTypes = true, reason = "HANA silently converts a boolean to string types")
	@SkipForDialect(dialectClass = CockroachDialect.class, matchSubTypes = true, reason = "Cockroach silently converts a boolean to string types")
	@SkipForDialect(dialectClass = PostgresPlusDialect.class, reason = "PostgresPlus silently converts a boolean to string types")
	@SkipForDialect(dialectClass = FirebirdDialect.class, reason = "Firebird silently converts a boolean to string")
	@SkipForDialect(dialectClass = AltibaseDialect.class, reason = "Altibase silently converts a boolean to string")
	@SkipForDialect(dialectClass = OracleDialect.class, majorVersion = 23, reason = "Oracle 23 interprets Y and T as true and N and F as false, so this works")
	public void testYesNoMismatch(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final EntityOne loaded = session.byId( EntityOne.class ).load( 1 );
			assertThat( loaded ).isNotNull();
		} );

		scope.inTransaction( (session) -> {
			session.enableFilter( "filterYesNoBoolean" ).setParameter( "yesNo", Boolean.FALSE );

			try {
				session.createQuery( "from EntityOne e where e.id = :id", EntityOne.class )
						.setParameter( "id", 1 )
						.getSingleResultOrNull();
				fail( "Expecting an exception" );
			}
			catch (Exception expected) {
				System.out.println(expected.getMessage());
			}
		} );
	}

	@Test
	public void testNumeric(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final EntityTwo loaded = session.byId( EntityTwo.class ).load( 1 );
			assertThat( loaded ).isNotNull();
		} );

		scope.inTransaction( (session) -> {
			session.enableFilter( "filterNumberConverter" ).setParameter( "zeroOne", Boolean.FALSE );

			final EntityTwo loaded = session.createQuery( "from EntityTwo e where e.id = :id", EntityTwo.class )
					.setParameter( "id", 1 )
					.getSingleResultOrNull();
			assertThat( loaded ).isNull();
		} );
	}

	@Test
	@SkipForDialect(dialectClass = H2Dialect.class, reason = "H2 silently converts a boolean to integral types")
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
	@SkipForDialect(dialectClass = AbstractHANADialect.class, matchSubTypes = true, reason = "HANA silently converts a boolean to integral types")
	@SkipForDialect(dialectClass = FirebirdDialect.class, matchSubTypes = true, reason = "Firebird silently converts a boolean to integral types")
	public void testNumericMismatch(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final EntityTwo loaded = session.byId( EntityTwo.class ).load( 1 );
			assertThat( loaded ).isNotNull();
		} );

		scope.inTransaction( (session) -> {
			session.enableFilter( "filterNumberBoolean" ).setParameter( "zeroOne", Boolean.FALSE );

			try {
				session.createQuery( "from EntityTwo e where e.id = :id", EntityTwo.class )
						.setParameter( "id", 1 )
						.getSingleResultOrNull();
				fail( "Expecting an exception" );
			}
			catch (Exception expected) {
				System.out.println(expected.getMessage());
			}
		} );
	}

	@Test
	@SkipForDialect(dialectClass = MySQLDialect.class, reason = "MySQL silently converts strings to integral types")
	@SkipForDialect(dialectClass = MariaDBDialect.class, reason = "MariaDB silently converts strings to integral types")
	@SkipForDialect(dialectClass = TiDBDialect.class, reason = "TiDB silently converts strings to integral types")
	@SkipForDialect(dialectClass = PostgresPlusDialect.class, reason = "PostgresPlus silently converts strings to integral types")
	public void testMismatch(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final EntityThree loaded = session.byId( EntityThree.class ).load( 1 );
			assertThat( loaded ).isNotNull();
		} );

		scope.inTransaction( (session) -> {
			session.enableFilter( "filterMismatchConverter" ).setParameter( "mismatch", Boolean.FALSE );

			try {
				session.createQuery( "from EntityThree e where e.id = :id", EntityThree.class )
						.setParameter( "id", 1 )
						.getSingleResultOrNull();
				fail( "Expecting an exception" );
			}
			catch (Exception expected) {
				System.out.println(expected.getMessage());
			}
		} );
	}


	@BeforeEach
	public void prepareTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.persist( new EntityOne( 1, "one" ) );
			session.persist( new EntityTwo( 1, "two" ) );
			session.persist( new EntityThree( 1, "three" ) );
		} );
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.createMutationQuery( "delete EntityOne" ).executeUpdate();
			session.createMutationQuery( "delete EntityTwo" ).executeUpdate();
			session.createMutationQuery( "delete EntityThree" ).executeUpdate();
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
}
