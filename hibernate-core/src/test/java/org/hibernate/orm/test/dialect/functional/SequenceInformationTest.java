/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect.functional;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;
import org.hibernate.tool.schema.extract.spi.SequenceInformation;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryBasedFunctionalTest;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Vlad Mihalcea
 */
@JiraKey(value = "HHH-12973")
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsSequences.class)
public class SequenceInformationTest extends
		EntityManagerFactoryBasedFunctionalTest {

	protected ServiceRegistry serviceRegistry;
	protected MetadataImplementor metadata;

	@Override
	public EntityManagerFactory produceEntityManagerFactory() {
		serviceRegistry = ServiceRegistryUtil.serviceRegistry();
		metadata = (MetadataImplementor) new MetadataSources( serviceRegistry )
				.addAnnotatedClass( Product.class )
				.addAnnotatedClass( Vehicle.class )
				.buildMetadata();

		new SchemaExport().drop( EnumSet.of( TargetType.DATABASE ), metadata );
		new SchemaExport().create( EnumSet.of( TargetType.DATABASE ), metadata );
		return super.produceEntityManagerFactory();
	}

	@AfterAll
	public void releaseResources() {
		new SchemaExport().drop( EnumSet.of( TargetType.DATABASE ), metadata );
		StandardServiceRegistryBuilder.destroy( serviceRegistry );
	}

	@Override
	protected void addConfigOptions(Map options) {
		options.put( AvailableSettings.HBM2DDL_AUTO, "none" );
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
		assertEquals( Long.valueOf( 10 ), productSequenceInfo.getIncrementValue().longValue() );
	}

	protected void assertVehicleSequenceInfo(SequenceInformation vehicleSequenceInfo) {
		assertEquals( Long.valueOf( 1 ), vehicleSequenceInfo.getIncrementValue().longValue() );
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
