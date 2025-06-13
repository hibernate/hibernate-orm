/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.type;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.NClob;
import java.sql.SQLException;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;

import org.hibernate.Hibernate;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.community.dialect.FirebirdDialect;
import org.hibernate.dialect.SybaseDialect;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Test;

@JiraKey("HHH-12555")
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsExpectedLobUsagePattern.class)
@DomainModel(
		annotatedClasses = {
			LobUnfetchedPropertyTest.FileBlob.class, LobUnfetchedPropertyTest.FileClob.class, LobUnfetchedPropertyTest.FileNClob.class
		}
)
@SessionFactory
@BytecodeEnhanced
public class LobUnfetchedPropertyTest {

	@Test
	public void testBlob(SessionFactoryScope scope) throws SQLException {
		final int id = scope.fromTransaction( s -> {
			FileBlob file = new FileBlob();
			file.setBlob( s.getLobHelper().createBlob( "TEST CASE".getBytes() ) );
			// merge transient entity
			file = (FileBlob) s.merge( file );
			return file.getId();
		} );

		scope.inTransaction( s -> {
			FileBlob file = s.get( FileBlob.class, id );
			assertFalse( Hibernate.isPropertyInitialized( file, "blob" ) );
			Blob blob = file.getBlob();
			try {
				assertArrayEquals( "TEST CASE".getBytes(), blob.getBytes( 1, (int) file.getBlob().length() ) );
			}
			catch (SQLException ex) {
				fail( "could not determine Lob length" );
			}
		});
	}

	@Test
	@SkipForDialect( dialectClass = FirebirdDialect.class, reason = "Driver cannot determine clob length" )
	public void testClob(SessionFactoryScope scope) throws SQLException {
		final int id = scope.fromTransaction( s -> {
			FileClob file = new FileClob();
			file.setClob( s.getLobHelper().createClob( "TEST CASE" ) );
			// merge transient entity
			file = (FileClob) s.merge( file );
			return file.getId();
		} );

		scope.inTransaction( s -> {
			FileClob file = s.get( FileClob.class, id );
			assertFalse( Hibernate.isPropertyInitialized( file, "clob" ) );
			Clob clob = file.getClob();
			try {
				final char[] chars = new char[(int) file.getClob().length()];
				clob.getCharacterStream().read( chars );
				assertArrayEquals( "TEST CASE".toCharArray(), chars );
			}
			catch (SQLException ex ) {
				fail( "could not determine Lob length" );
			}
			catch (IOException ex) {
				fail( "could not read Lob" );
			}
		});
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsNClob.class)
	@SkipForDialect(
			dialectClass = SybaseDialect.class, matchSubTypes = true,
			reason = "jConnect does not support Connection#createNClob which is ultimately used by LobHelper#createNClob" )
	public void testNClob(SessionFactoryScope scope) {
		final int id = scope.fromTransaction( s -> {
			FileNClob file = new FileNClob();
			file.setClob( s.getLobHelper().createNClob( "TEST CASE" ) );
			// merge transient entity
			file = (FileNClob) s.merge( file );
			return file.getId();
		});

		scope.inTransaction( s -> {
			FileNClob file = s.get( FileNClob.class, id );
			assertFalse( Hibernate.isPropertyInitialized( file, "clob" ) );
			NClob nClob = file.getClob();
			assertTrue( Hibernate.isPropertyInitialized( file, "clob" ) );
			try {
			final char[] chars = new char[(int) file.getClob().length()];
			nClob.getCharacterStream().read( chars );
				assertArrayEquals( "TEST CASE".toCharArray(), chars );
			}
			catch (SQLException ex ) {
			fail( "could not determine Lob length" );
			}
			catch (IOException ex) {
			fail( "could not read Lob" );
			}
		});
	}

	@Entity(name = "FileBlob")
	@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE, includeLazy = false)
	public static class FileBlob {

		private int id;

		private Blob blob;

		@Id
		@GeneratedValue
		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		@Column(name = "filedata", length = 1024 * 1024)
		@Lob
		@Basic(fetch = FetchType.LAZY)
		public Blob getBlob() {
			return blob;
		}

		public void setBlob(Blob blob) {
			this.blob = blob;
		}
	}

	@Entity(name = "FileClob")
	@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE, includeLazy = false)
	public static class FileClob {

		private int id;

		private Clob clob;

		@Id
		@GeneratedValue
		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		@Column(name = "filedata", length = 1024 * 1024)
		@Lob
		@Basic(fetch = FetchType.LAZY)
		public Clob getClob() {
			return clob;
		}

		public void setClob(Clob clob) {
			this.clob = clob;
		}
	}

	@Entity(name = "FileNClob")
	@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE, includeLazy = false)
	public static class FileNClob {

		private int id;

		private NClob clob;

		@Id
		@GeneratedValue
		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		@Column(name = "filedata", length = 1024 * 1024)
		@Lob
		@Basic(fetch = FetchType.LAZY)
		public NClob getClob() {
			return clob;
		}

		public void setClob(NClob clob) {
			this.clob = clob;
		}
	}

	@Entity(name = "FileNClob2")
	@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE, includeLazy = false)
	public static class FileNClob2 {

		private int id;

		private String clob;

		@Id
		@GeneratedValue
		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		@Column(name = "filedata", length = 1024 * 1024)
		@Lob
		@Basic(fetch = FetchType.LAZY)
		public String getClob() {
			return clob;
		}

		public void setClob(String clob) {
			this.clob = clob;
		}
	}
}
