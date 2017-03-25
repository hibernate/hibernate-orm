/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.tool.schema;

import java.sql.SQLSyntaxErrorException;
import java.util.Collections;
import java.util.EnumSet;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.transaction.SystemException;
import javax.transaction.Transaction;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.resource.transaction.backend.jta.internal.JtaTransactionCoordinatorBuilderImpl;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder;
import org.hibernate.tool.schema.SourceType;
import org.hibernate.tool.schema.TargetType;
import org.hibernate.tool.schema.spi.CommandAcceptanceException;
import org.hibernate.tool.schema.spi.SchemaCreator;
import org.hibernate.tool.schema.spi.SchemaDropper;
import org.hibernate.tool.schema.spi.SchemaManagementTool;
import org.hibernate.tool.schema.spi.ScriptSourceInput;
import org.hibernate.tool.schema.spi.ScriptTargetOutput;
import org.hibernate.tool.schema.spi.SourceDescriptor;
import org.hibernate.tool.schema.spi.TargetDescriptor;

import org.hibernate.testing.jta.TestingJtaBootstrap;
import org.hibernate.testing.jta.TestingJtaPlatformImpl;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Steve Ebersole
 */
public class SchemaToolTransactionHandlingTest extends BaseUnitTestCase {
	// for each case we want to run these tool delegates in a matrix of:
	//		1) JTA versus JDBC transaction handling
	//		2) existing transaction versus not
	//
	// cases:
	//		1) create-drop
	//		2) update
	//		3) validate
	//
	// so:
	//		1) create-drop
	//			1.1) JTA transaction handling
	//				1.1.1) inside an existing transaction
	//				1.1.2) outside any transaction
	//			1.1) JDBC transaction handling
	//				- there really cannot be an "existing transaction" case...

	@Test
	public void testDropCreateDropInExistingJtaTransaction() {
		// test for 1.1.1 - create-drop + JTA handling + existing

		// start a JTA transaction...
		try {
			TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		}
		catch (Exception e) {
			throw new RuntimeException( "Unable to being JTA transaction prior to starting test", e );
		}

		// hold reference to Transaction object
		final Transaction jtaTransaction;
		try {
			jtaTransaction = TestingJtaPlatformImpl.INSTANCE.getTransactionManager().getTransaction();
		}
		catch (SystemException e) {
			throw new RuntimeException( "Unable to access JTA Transaction prior to starting test", e );
		}

		final StandardServiceRegistry registry = buildJtaStandardServiceRegistry();
		// perform the test...
		try {
			final SchemaManagementTool smt = registry.getService( SchemaManagementTool.class );
			final SchemaDropper schemaDropper = smt.getSchemaDropper( Collections.emptyMap() );
			final SchemaCreator schemaCreator = smt.getSchemaCreator( Collections.emptyMap() );

			final Metadata mappings = buildMappings( registry );
			try {
				try {
					schemaDropper.doDrop(
							mappings,
							ExecutionOptionsTestImpl.INSTANCE,
							SourceDescriptorImpl.INSTANCE,
							TargetDescriptorImpl.INSTANCE
					);
				}catch (CommandAcceptanceException e){
					//ignore may happen if sql drop does not support if exist
				}
				schemaCreator.doCreation(
						mappings,
						ExecutionOptionsTestImpl.INSTANCE,
						SourceDescriptorImpl.INSTANCE,
						TargetDescriptorImpl.INSTANCE
				);
			}
			finally {
				try {
					schemaDropper.doDrop(
							mappings,
							ExecutionOptionsTestImpl.INSTANCE,
							SourceDescriptorImpl.INSTANCE,
							TargetDescriptorImpl.INSTANCE
					);
				}
				catch (Exception ignore) {
					// ignore
				}
			}
		}
		finally {
			try {
				jtaTransaction.commit();
				((StandardServiceRegistryImpl) registry).destroy();
			}
			catch (Exception e) {
				// not much we can do...
			}
		}

	}
	@Test
	public void testValidateInExistingJtaTransaction() {
		// test for 1.1.1 - create-drop + JTA handling + existing

		// start a JTA transaction...
		try {
			TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		}
		catch (Exception e) {
			throw new RuntimeException( "Unable to being JTA transaction prior to starting test", e );
		}

		// hold reference to Transaction object
		final Transaction jtaTransaction;
		try {
			jtaTransaction = TestingJtaPlatformImpl.INSTANCE.getTransactionManager().getTransaction();
		}
		catch (SystemException e) {
			throw new RuntimeException( "Unable to access JTA Transaction prior to starting test", e );
		}

		final StandardServiceRegistry registry = buildJtaStandardServiceRegistry();
		// perform the test...
		try {
			final SchemaManagementTool smt = registry.getService( SchemaManagementTool.class );

			final Metadata mappings = buildMappings( registry );

			// first make the schema exist...
			try {
				smt.getSchemaCreator( Collections.emptyMap() ).doCreation(
						mappings,
						ExecutionOptionsTestImpl.INSTANCE,
						SourceDescriptorImpl.INSTANCE,
						TargetDescriptorImpl.INSTANCE
				);
			}
			catch (Exception e) {
				throw new RuntimeException( "Unable to create schema to validation tests", e );
			}

			try {
				smt.getSchemaValidator( Collections.emptyMap() ).doValidation(
						mappings,
						ExecutionOptionsTestImpl.INSTANCE
				);
			}
			finally {
				try {
					smt.getSchemaDropper( Collections.emptyMap() ).doDrop(
							mappings,
							ExecutionOptionsTestImpl.INSTANCE,
							SourceDescriptorImpl.INSTANCE,
							TargetDescriptorImpl.INSTANCE
					);
				}
				catch (Exception ignore) {
					// ignore
				}
			}
		}
		finally {
			try {
				jtaTransaction.commit();
				((StandardServiceRegistryImpl) registry).destroy();
			}
			catch (Exception e) {
				// not much we can do...
			}
		}

	}

	private Metadata buildMappings(StandardServiceRegistry registry) {
		return new MetadataSources( registry )
				.addAnnotatedClass( MyEntity.class )
				.buildMetadata();
	}

	protected StandardServiceRegistry buildJtaStandardServiceRegistry() {
		StandardServiceRegistry registry = TestingJtaBootstrap.prepare().build();
		assertThat( registry.getService( TransactionCoordinatorBuilder.class ), instanceOf( JtaTransactionCoordinatorBuilderImpl.class ) );
		return registry;
	}

	@Entity( name = "MyEntity" )
	@Table( name = "MyEntity" )
	public static class MyEntity {
		@Id
		public Integer id;
		public String name;
	}

	private static class SourceDescriptorImpl implements SourceDescriptor {
		/**
		 * Singleton access
		 */
		public static final SourceDescriptorImpl INSTANCE = new SourceDescriptorImpl();

		@Override
		public SourceType getSourceType() {
			return SourceType.METADATA;
		}

		@Override
		public ScriptSourceInput getScriptSourceInput() {
			return null;
		}
	}

	private static class TargetDescriptorImpl implements TargetDescriptor {
		/**
		 * Singleton access
		 */
		public static final TargetDescriptorImpl INSTANCE = new TargetDescriptorImpl();

		@Override
		public EnumSet<TargetType> getTargetTypes() {
			return EnumSet.of( TargetType.DATABASE );
		}

		@Override
		public ScriptTargetOutput getScriptTargetOutput() {
			return null;
		}
	}
}
