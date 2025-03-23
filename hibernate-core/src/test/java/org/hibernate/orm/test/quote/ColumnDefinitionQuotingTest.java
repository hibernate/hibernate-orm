/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.quote;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Selectable;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Steve Ebersole
 */
@BaseUnitTest
public class ColumnDefinitionQuotingTest {

	@Test
	@JiraKey( value = "HHH-9491" )
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
						metadata.orderColumns( false );
						metadata.validate();

						PersistentClass entityBinding = metadata.getEntityBinding( E1.class.getName() );

						org.hibernate.mapping.Column idColumn = extractColumn( entityBinding.getIdentifier().getSelectables() );
						assertTrue( isQuoted( idColumn.getSqlType(), ssr ) );

						org.hibernate.mapping.Column otherColumn = extractColumn( entityBinding.getProperty( "other" ).getSelectables() );
						assertTrue( isQuoted( otherColumn.getSqlType(), ssr ) );
					}
				}
		);
	}

	@Test
	@JiraKey( value = "HHH-9491" )
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
						metadata.orderColumns( false );
						metadata.validate();

						PersistentClass entityBinding = metadata.getEntityBinding( E1.class.getName() );

						org.hibernate.mapping.Column idColumn = extractColumn( entityBinding.getIdentifier().getSelectables() );
						assertTrue( isQuoted( idColumn.getSqlType(), ssr ) );

						org.hibernate.mapping.Column otherColumn = extractColumn( entityBinding.getProperty( "other" ).getSelectables() );
						assertTrue( isQuoted( otherColumn.getSqlType(), ssr ) );
					}
				}
		);
	}

	private org.hibernate.mapping.Column extractColumn(List<Selectable> columns) {
		Assert.assertEquals( 1, columns.size() );
		return (org.hibernate.mapping.Column) columns.get( 0 );
	}

	private boolean isQuoted(String sqlType, StandardServiceRegistry ssr) {
		final Dialect dialect = ssr.getService( JdbcEnvironment.class ).getDialect();
		return sqlType.charAt( 0  ) == dialect.openQuote()
				&& sqlType.charAt( sqlType.length()-1 ) == dialect.closeQuote();
	}

	@Test
	@JiraKey( value = "HHH-9491" )
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
						metadata.orderColumns( false );
						metadata.validate();

						PersistentClass entityBinding = metadata.getEntityBinding( E2.class.getName() );

						org.hibernate.mapping.Column idColumn = extractColumn( entityBinding.getIdentifier().getSelectables() );
						assertTrue( isQuoted( idColumn.getSqlType(), ssr ) );

						org.hibernate.mapping.Column otherColumn = extractColumn( entityBinding.getProperty( "other" ).getSelectables() );
						assertTrue( isQuoted( otherColumn.getSqlType(), ssr ) );
					}
				}
		);
	}

	@Test
	@JiraKey( value = "HHH-9491" )
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
						metadata.orderColumns( false );
						metadata.validate();

						PersistentClass entityBinding = metadata.getEntityBinding( E2.class.getName() );

						org.hibernate.mapping.Column idColumn = extractColumn( entityBinding.getIdentifier().getSelectables() );
						assertTrue( !isQuoted( idColumn.getSqlType(), ssr ) );

						org.hibernate.mapping.Column otherColumn = extractColumn( entityBinding.getProperty( "other" ).getSelectables() );
						assertTrue( !isQuoted( otherColumn.getSqlType(), ssr ) );
					}
				}
		);
	}

	interface TestWork {
		void doTestWork(StandardServiceRegistry ssr);
	}

	void withStandardServiceRegistry(boolean globalQuoting, boolean skipColumnDefinitions, TestWork work) {
		final StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistryBuilder()
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
