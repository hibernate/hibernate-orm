/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.type;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.NClob;
import java.sql.SQLException;
import java.util.Arrays;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;

import org.hibernate.Hibernate;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;
import org.junit.runner.RunWith;

@TestForIssue(jiraKey = "HHH-12555")
@RequiresDialectFeature(DialectChecks.SupportsExpectedLobUsagePattern.class)
@RunWith(BytecodeEnhancerRunner.class)
public class LobUnfetchedPropertyTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{ FileBlob.class, FileClob.class, FileNClob.class };
	}

	@Test
	public void testBlob() throws SQLException {
		final int id = doInHibernate( this::sessionFactory, s -> {
			FileBlob file = new FileBlob();
			file.setBlob( s.getLobHelper().createBlob( "TEST CASE".getBytes() ) );
			// merge transient entity
			file = (FileBlob) s.merge( file );
			return file.getId();
		} );

		doInHibernate( this::sessionFactory, s -> {
			FileBlob file = s.get( FileBlob.class, id );
			assertFalse( Hibernate.isPropertyInitialized( file, "blob" ) );
			Blob blob = file.getBlob();
			try {
				assertTrue(
					Arrays.equals( "TEST CASE".getBytes(), blob.getBytes( 1, (int) file.getBlob().length() ) )
				);
			}
			catch (SQLException ex) {
				fail( "could not determine Lob length" );
			}
		});
	}

	@Test
	public void testClob() {
		final int id = doInHibernate( this::sessionFactory, s -> {
			FileClob file = new FileClob();
			file.setClob( s.getLobHelper().createClob( "TEST CASE" ) );
			// merge transient entity
			file = (FileClob) s.merge( file );
			return file.getId();
		} );

		doInHibernate( this::sessionFactory, s -> {
			FileClob file = s.get( FileClob.class, id );
			assertFalse( Hibernate.isPropertyInitialized( file, "clob" ) );
			Clob clob = file.getClob();
			try {
				final char[] chars = new char[(int) file.getClob().length()];
				clob.getCharacterStream().read( chars );
				assertTrue( Arrays.equals( "TEST CASE".toCharArray(), chars ) );
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
	@RequiresDialectFeature(DialectChecks.SupportsNClob.class)
	public void testNClob() {
		final int id = doInHibernate( this::sessionFactory, s -> {
			FileNClob file = new FileNClob();
			file.setClob( s.getLobHelper().createNClob( "TEST CASE" ) );
			// merge transient entity
			file = (FileNClob) s.merge( file );
			return file.getId();
		});

		doInHibernate( this::sessionFactory, s -> {
			FileNClob file = s.get( FileNClob.class, id );
			assertFalse( Hibernate.isPropertyInitialized( file, "clob" ) );
			NClob nClob = file.getClob();
			assertTrue( Hibernate.isPropertyInitialized( file, "clob" ) );
			try {
			   final char[] chars = new char[(int) file.getClob().length()];
			   nClob.getCharacterStream().read( chars );
			   assertTrue( Arrays.equals( "TEST CASE".toCharArray(), chars ) );
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
	@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE, include = "non-lazy")
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
	@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE, include = "non-lazy")
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
	@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE, include = "non-lazy")
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
}
