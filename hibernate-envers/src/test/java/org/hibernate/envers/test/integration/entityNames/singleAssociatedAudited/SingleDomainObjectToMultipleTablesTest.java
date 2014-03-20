package org.hibernate.envers.test.integration.entityNames.singleAssociatedAudited;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

import org.hibernate.MappingException;
import org.hibernate.envers.test.AbstractOneSessionTest;
import org.hibernate.envers.test.Priority;

import org.junit.Assert;
import org.junit.Test;

import org.hibernate.testing.FailureExpectedWithNewMetamodel;
import org.hibernate.testing.TestForIssue;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@FailureExpectedWithNewMetamodel( message = "hbm.xml source not supported because it is not indexed." )
public class SingleDomainObjectToMultipleTablesTest extends AbstractOneSessionTest {
	private long carId = 0;
	private long ownerId = 0;
	private long driverId = 0;

	protected void initMappings() throws MappingException, URISyntaxException {
		URL url = Thread.currentThread().getContextClassLoader().getResource(
				"mappings/entityNames/singleAssociatedAudited/mappings.hbm.xml"
		);
		config.addFile( new File( url.toURI() ) );
	}


	@Test
	@Priority(10)
	public void initData() {
		initializeSession();

		//REV 1
		getSession().getTransaction().begin();
		Person owner = new Person( "Lukasz", 25 );
		Person driver = new Person( "Kinga", 24 );
		Car car = new Car( 1, owner, driver );
		getSession().persist( "Personaje", owner );
		getSession().persist( "Driveraje", driver );
		getSession().persist( car );
		getSession().getTransaction().commit();

		carId = car.getId();
		ownerId = owner.getId();
		driverId = driver.getId();
	}

	@Test
	@TestForIssue(jiraKey = "HHH-4648")
	public void testSingleDomainObjectToMultipleTablesMapping() {
		Car carVer1 = getAuditReader().find( Car.class, carId, 1 );
		Person ownerVer1 = getAuditReader().find( Person.class, "Personaje", ownerId, 1 );
		Person driverVer1 = getAuditReader().find( Person.class, "Driveraje", driverId, 1 );

        /* Check ids. */
		Assert.assertEquals( ownerVer1.getId(), carVer1.getOwner().getId() );
		Assert.assertEquals( driverVer1.getId(), carVer1.getDriver().getId() );

        /* Check object properties. */
		Assert.assertEquals( "Lukasz", ownerVer1.getName() );
		Assert.assertEquals( "Kinga", driverVer1.getName() );
		Assert.assertEquals( 1, carVer1.getNumber() );
	}
}
