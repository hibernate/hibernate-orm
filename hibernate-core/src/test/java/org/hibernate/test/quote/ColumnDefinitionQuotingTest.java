/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.quote;

import java.util.Iterator;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.mapping.PersistentClass;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author Steve Ebersole
 */
public class ColumnDefinitionQuotingTest extends BaseUnitTestCase {
	@Test
	@TestForIssue( jiraKey = "HHH-9491" )
	public void testExplicitQuoting() {
		withStandardServiceRegistry(
				false,
				false,
				new TestWork() {
					@Override
					public void doTestWork(StandardServiceRegistry ssr) {
						MetadataImplementor metadata = (MetadataImplementor) new MetadataSources( ssr )
								.addAnnotatedClass( E1.class )
								.buildMetadata();
						metadata.validate();

						PersistentClass entityBinding = metadata.getEntityBinding( E1.class.getName() );

						org.hibernate.mapping.Column idColumn = extractColumn( entityBinding.getIdentifier().getColumnIterator() );
						assertTrue( isQuoted( idColumn.getSqlType(), ssr ) );

						org.hibernate.mapping.Column otherColumn = extractColumn( entityBinding.getProperty( "other" ).getColumnIterator() );
						assertTrue( isQuoted( otherColumn.getSqlType(), ssr ) );
					}
				}
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9491" )
	public void testExplicitQuotingSkippingColumnDef() {
		withStandardServiceRegistry(
				false,
				true,
				new TestWork() {
					@Override
					public void doTestWork(StandardServiceRegistry ssr) {
						MetadataImplementor metadata = (MetadataImplementor) new MetadataSources( ssr )
								.addAnnotatedClass( E1.class )
								.buildMetadata();
						metadata.validate();

						PersistentClass entityBinding = metadata.getEntityBinding( E1.class.getName() );

						org.hibernate.mapping.Column idColumn = extractColumn( entityBinding.getIdentifier().getColumnIterator() );
						assertTrue( isQuoted( idColumn.getSqlType(), ssr ) );

						org.hibernate.mapping.Column otherColumn = extractColumn( entityBinding.getProperty( "other" ).getColumnIterator() );
						assertTrue( isQuoted( otherColumn.getSqlType(), ssr ) );
					}
				}
		);
	}

	private org.hibernate.mapping.Column extractColumn(Iterator columnIterator) {
		return (org.hibernate.mapping.Column) columnIterator.next();
	}

	private boolean isQuoted(String sqlType, StandardServiceRegistry ssr) {
		final Dialect dialect = ssr.getService( JdbcEnvironment.class ).getDialect();
		return sqlType.charAt( 0  ) == dialect.openQuote()
				&& sqlType.charAt( sqlType.length()-1 ) == dialect.closeQuote();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9491" )
	public void testGlobalQuotingNotSkippingColumnDef() {
		withStandardServiceRegistry(
				true,
				false,
				new TestWork() {
					@Override
					public void doTestWork(StandardServiceRegistry ssr) {
						MetadataImplementor metadata = (MetadataImplementor) new MetadataSources( ssr )
								.addAnnotatedClass( E2.class )
								.buildMetadata();
						metadata.validate();

						PersistentClass entityBinding = metadata.getEntityBinding( E2.class.getName() );

						org.hibernate.mapping.Column idColumn = extractColumn( entityBinding.getIdentifier().getColumnIterator() );
						assertTrue( isQuoted( idColumn.getSqlType(), ssr ) );

						org.hibernate.mapping.Column otherColumn = extractColumn( entityBinding.getProperty( "other" ).getColumnIterator() );
						assertTrue( isQuoted( otherColumn.getSqlType(), ssr ) );
					}
				}
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9491" )
	public void testGlobalQuotingSkippingColumnDef() {
		withStandardServiceRegistry(
				true,
				true,
				new TestWork() {
					@Override
					public void doTestWork(StandardServiceRegistry ssr) {
						MetadataImplementor metadata = (MetadataImplementor) new MetadataSources( ssr )
								.addAnnotatedClass( E2.class )
								.buildMetadata();
						metadata.validate();

						PersistentClass entityBinding = metadata.getEntityBinding( E2.class.getName() );

						org.hibernate.mapping.Column idColumn = extractColumn( entityBinding.getIdentifier().getColumnIterator() );
						assertTrue( !isQuoted( idColumn.getSqlType(), ssr ) );

						org.hibernate.mapping.Column otherColumn = extractColumn( entityBinding.getProperty( "other" ).getColumnIterator() );
						assertTrue( !isQuoted( otherColumn.getSqlType(), ssr ) );
					}
				}
		);
	}

	interface TestWork {
		void doTestWork(StandardServiceRegistry ssr);
	}

	void withStandardServiceRegistry(boolean globalQuoting, boolean skipColumnDefinitions, TestWork work) {
		final StandardServiceRegistry ssr = new StandardServiceRegistryBuilder()
				.applySetting( AvailableSettings.GLOBALLY_QUOTED_IDENTIFIERS, globalQuoting )
				.applySetting( AvailableSettings.GLOBALLY_QUOTED_IDENTIFIERS_SKIP_COLUMN_DEFINITIONS, skipColumnDefinitions )
				.build();

		try {
			work.doTestWork( ssr );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@Entity
	public static class E1 {
		@Id
		@Column( columnDefinition = "`explicitly quoted`" )
		private Integer id;

		@ManyToOne
		@JoinColumn( columnDefinition = "`explicitly quoted`" )
		private E1 other;
	}

	@Entity
	public static class E2 {
		@Id
		@Column( columnDefinition = "not explicitly quoted" )
		private Integer id;

		@ManyToOne
		@JoinColumn( columnDefinition = "not explicitly quoted" )
		private E2 other;
	}
}
