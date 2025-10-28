/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.write;

import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.OptimisticLockType;
import org.hibernate.annotations.OptimisticLocking;
import org.hibernate.internal.util.StringHelper;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@DomainModel( annotatedClasses = {
		DynamicUpdateTests.AttachableJob.class,
		DynamicUpdateTests.VersionedJob.class,
		DynamicUpdateTests.DirtyJob.class,
		DynamicUpdateTests.AllJob.class,
} )
@SessionFactory( useCollectingStatementInspector = true )
public class DynamicUpdateTests {

	@Test
	public void testAttachableLocking(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.persist( new AttachableJob( 1, "job", "controller-1" ) );
		} );

		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();

		scope.inTransaction( (session) -> {
			final AttachableJob job1 = session.get( AttachableJob.class, 1 );

			statementInspector.clear();

			job1.setCode( "job-1" );
		} );

		assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
		// the first should look like
		// ```
		// 	update AttachableJob set code = ? where id = ?
		// ```
		assertThat( StringHelper.count( statementInspector.getSqlQueries().get( 0 ), "code" ) ).isEqualTo( 1 );
		assertThat( StringHelper.count( statementInspector.getSqlQueries().get( 0 ), "id" ) ).isEqualTo( 1 );
		assertThat( StringHelper.count( statementInspector.getSqlQueries().get( 0 ), "controller" ) ).isEqualTo( 0 );
	}

	@Test
	public void testVersionedAttachableLocking(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.persist( new VersionedJob( 1, "job", "controller-1" ) );
		} );

		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();

		scope.inTransaction( (session) -> {
			final VersionedJob job1 = session.get( VersionedJob.class, 1 );

			statementInspector.clear();

			job1.setCode( "job-1" );
		} );

		assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );

		// the first should look like
		// ```
		// 	update VersionedJob set code = ?, version = ? where id = ? and version = ?
		// ```
		assertThat( StringHelper.count( statementInspector.getSqlQueries().get( 0 ), "code" ) ).isEqualTo( 1 );
		assertThat( StringHelper.count( statementInspector.getSqlQueries().get( 0 ), "id" ) ).isEqualTo( 1 );
		assertThat( StringHelper.count( statementInspector.getSqlQueries().get( 0 ), "version" ) ).isEqualTo( 2 );
	}

	@Test
	public void testDirtyLocking(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.persist( new DirtyJob( 1, "job", "controller-1" ) );
			session.persist( new DirtyJob( 2, null, "controller-1" ) );
		} );

		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();

		scope.inTransaction( (session) -> {
			final DirtyJob job1 = session.get( DirtyJob.class, 1 );
			final DirtyJob job2 = session.get( DirtyJob.class, 2 );

			statementInspector.clear();

			job1.setCode( "job-1" );
			job2.setCode( "job-2" );
		} );

		assertThat( statementInspector.getSqlQueries() ).hasSize( 2 );

		// the first should look like
		// ```
		// 	update DirtyJob set code = ? where code = ? and id = ?
		// ```
		assertThat( StringHelper.count( statementInspector.getSqlQueries().get( 0 ), "code" ) ).isEqualTo( 2 );
		assertThat( StringHelper.count( statementInspector.getSqlQueries().get( 0 ), "id" ) ).isEqualTo( 1 );

		// the second should look like
		// ```
		// 	update DirtyJob set code = ? where code is null and id = ?
		// ```
		assertThat( StringHelper.count( statementInspector.getSqlQueries().get( 1 ), "code" ) ).isEqualTo( 2 );
		assertThat( StringHelper.count( statementInspector.getSqlQueries().get( 1 ), "id" ) ).isEqualTo( 1 );

	}

	@Test
	public void testAllLocking(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.persist( new AllJob( 1, "job", "controller-1" ) );
			session.persist( new AllJob( 2, null, "controller-1" ) );
		} );

		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();

		scope.inTransaction( (session) -> {
			final AllJob job1 = session.get( AllJob.class, 1 );
			final AllJob job2 = session.get( AllJob.class, 2 );

			statementInspector.clear();

			job1.setCode( "job-1" );
			job2.setCode( "job-2" );
		} );

		assertThat( statementInspector.getSqlQueries() ).hasSize( 2 );

		// the first should look like
		// ```
		// 	update AllJob set code = ? where code = ? and controller = ? and id = ?
		// ```
		assertThat( StringHelper.count( statementInspector.getSqlQueries().get( 0 ), "code" ) ).isEqualTo( 2 );
		assertThat( StringHelper.count( statementInspector.getSqlQueries().get( 0 ), "controller" ) ).isEqualTo( 1 );
		assertThat( StringHelper.count( statementInspector.getSqlQueries().get( 0 ), "id" ) ).isEqualTo( 1 );

		// the second should look like
		// ```
		// 	update AllJob set code = ? where code is null and controller = ? and id = ?
		// ```
		assertThat( StringHelper.count( statementInspector.getSqlQueries().get( 1 ), "code" ) ).isEqualTo( 2 );
		assertThat( StringHelper.count( statementInspector.getSqlQueries().get( 1 ), "controller" ) ).isEqualTo( 1 );
		assertThat( StringHelper.count( statementInspector.getSqlQueries().get( 1 ), "id" ) ).isEqualTo( 1 );

	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Entity( name = "DirtyJob" )
	@Table( name = "DirtyJob" )
	@DynamicUpdate
	@OptimisticLocking( type = OptimisticLockType.DIRTY )
	public static class DirtyJob {
		@Id
		private Integer id;
		@Basic
		private String code;
		@Basic
		private String controller;

		private DirtyJob() {
			// for use by Hibernate
		}

		public DirtyJob(Integer id, String code, String controller) {
			this.id = id;
			this.code = code;
			this.controller = controller;
		}

		public Integer getId() {
			return id;
		}

		public String getCode() {
			return code;
		}

		public void setCode(String code) {
			this.code = code;
		}

		public String getController() {
			return controller;
		}

		public void setController(String controller) {
			this.controller = controller;
		}
	}

	@Entity( name = "AllJob" )
	@Table( name = "AllJob" )
	@DynamicUpdate
	@OptimisticLocking( type = OptimisticLockType.ALL )
	public static class AllJob {
		@Id
		private Integer id;
		@Basic
		private String code;
		@Basic
		private String controller;

		private AllJob() {
			// for use by Hibernate
		}

		public AllJob(Integer id, String code, String controller) {
			this.id = id;
			this.code = code;
			this.controller = controller;
		}

		public Integer getId() {
			return id;
		}

		public String getCode() {
			return code;
		}

		public void setCode(String code) {
			this.code = code;
		}

		public String getController() {
			return controller;
		}

		public void setController(String controller) {
			this.controller = controller;
		}
	}

	@Entity( name = "VersionedJob" )
	@Table( name = "VersionedJob" )
	@DynamicUpdate
	public static class VersionedJob {
		@Id
		private Integer id;
		@Basic
		private String code;
		@Basic
		private String controller;
		@Version
		private int version;

		private VersionedJob() {
			// for use by Hibernate
		}

		public VersionedJob(Integer id, String code, String controller) {
			this.id = id;
			this.code = code;
			this.controller = controller;
		}

		public Integer getId() {
			return id;
		}

		public String getCode() {
			return code;
		}

		public void setCode(String code) {
			this.code = code;
		}

		public String getController() {
			return controller;
		}

		public void setController(String controller) {
			this.controller = controller;
		}

		public int getVersion() {
			return version;
		}

		public void setVersion(int version) {
			this.version = version;
		}
	}

	@Entity( name = "AttachableJob" )
	@Table( name = "AttachableJob" )
	@DynamicUpdate
	public static class AttachableJob {
		@Id
		private Integer id;
		@Basic
		private String code;
		@Basic
		private String controller;

		private AttachableJob() {
			// for use by Hibernate
		}

		public AttachableJob(Integer id, String code, String controller) {
			this.id = id;
			this.code = code;
			this.controller = controller;
		}

		public Integer getId() {
			return id;
		}

		public String getCode() {
			return code;
		}

		public void setCode(String code) {
			this.code = code;
		}

		public String getController() {
			return controller;
		}

		public void setController(String controller) {
			this.controller = controller;
		}
	}
}
