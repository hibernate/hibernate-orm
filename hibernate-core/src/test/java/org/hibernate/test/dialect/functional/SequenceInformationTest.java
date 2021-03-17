/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.dialect.functional;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.AbstractHANADialect;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.InformixDialect;
import org.hibernate.dialect.PostgreSQL81Dialect;
import org.hibernate.dialect.SQLServer2012Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;
import org.hibernate.tool.schema.extract.spi.SequenceInformation;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertFalse;

/**
 * @author Vlad Mihalcea
 */
@TestForIssue(jiraKey = "HHH-12973")
@RequiresDialectFeature(DialectChecks.SupportsSequences.class)
public class SequenceInformationTest extends
		BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Product.class,
				Vehicle.class
		};
	}

	protected ServiceRegistry serviceRegistry;
	protected MetadataImplementor metadata;

	@Override
	public void buildEntityManagerFactory() {
		serviceRegistry = new StandardServiceRegistryBuilder().build();
		metadata = (MetadataImplementor) new MetadataSources( serviceRegistry )
				.addAnnotatedClass( Product.class )
				.addAnnotatedClass( Vehicle.class )
				.buildMetadata();

		new SchemaExport().drop( EnumSet.of( TargetType.DATABASE ), metadata );
		new SchemaExport().create( EnumSet.of( TargetType.DATABASE ), metadata );
		super.buildEntityManagerFactory();
	}

	@Override
	public void releaseResources() {
		super.releaseResources();

		new SchemaExport().drop( EnumSet.of( TargetType.DATABASE ), metadata );
		StandardServiceRegistryBuilder.destroy( serviceRegistry );
	}

	@Override
	protected void addMappings(Map settings) {
		settings.put( AvailableSettings.HBM2DDL_AUTO, "none" );
	}

	@Test
	public void test() {

		SequenceInformation productSequenceInfo = sequenceInformation("product_sequence");

		assertNotNull( productSequenceInfo );
		assertEquals( "product_sequence", productSequenceInfo.getSequenceName().getSequenceName().getText().toLowerCase() );
		assertProductSequence( productSequenceInfo );

		SequenceInformation vehicleSequenceInfo = sequenceInformation("vehicle_sequence");

		assertNotNull( vehicleSequenceInfo );
		assertEquals( "vehicle_sequence", vehicleSequenceInfo.getSequenceName().getSequenceName().getText().toLowerCase() );
		assertVehicleSequenceInfo( vehicleSequenceInfo );
	}

	protected void assertProductSequence(SequenceInformation productSequenceInfo) {
		assertEquals( Long.valueOf( 10 ), productSequenceInfo.getIncrementValue() );
	}

	protected void assertVehicleSequenceInfo(SequenceInformation vehicleSequenceInfo) {
		assertEquals( Long.valueOf( 1 ), vehicleSequenceInfo.getIncrementValue() );
	}

	private SequenceInformation sequenceInformation(String sequenceName) {
		List<SequenceInformation> sequenceInformationList = entityManagerFactory().unwrap( SessionFactoryImplementor.class ).getJdbcServices().getExtractedMetaDataSupport().getSequenceInformationList();

		return sequenceInformationList.stream().filter(
				sequenceInformation -> sequenceName.equalsIgnoreCase( sequenceInformation.getSequenceName().getSequenceName().getText() )
		).findFirst().orElse( null );
	}

	@Entity(name = "Product")
	public static class Product {

		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "product_sequence")
		@SequenceGenerator( name = "product_sequence", sequenceName = "product_sequence", initialValue = 1, allocationSize = 10)
		private Long id;

		private String name;
	}

	@Entity(name = "Vehicle")
	public static class Vehicle {

		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "vehicle_sequence")
		@SequenceGenerator( name = "vehicle_sequence", sequenceName = "vehicle_sequence", initialValue = 1, allocationSize = 1)
		private Long id;

		private String name;
	}
}
