package org.hibernate.envers.test.integration.reventity;

import javax.persistence.EntityManager;
import java.util.Arrays;
import java.util.Map;

import org.hibernate.cfg.Environment;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.StrTestEntity;

import org.junit.Test;

import org.hibernate.metamodel.spi.relational.TableSpecification;
import org.hibernate.testing.RequiresDialect;

/**
 * Tests simple auditing process (read and write operations) when <i>REVINFO</i> and audit tables
 * exist in a different database schema.
 *
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@RequiresDialect({H2Dialect.class})
public class DifferentDBSchemaTest extends BaseEnversJPAFunctionalTestCase {
	private static final String SCHEMA_NAME = "ENVERS_AUDIT";
	private Integer steId = null;

	@Override
	protected void addConfigOptions(Map options) {
		super.addConfigOptions( options );
		// Creates new schema after establishing connection
		options.putAll( Environment.getProperties() );
		options.put( EnversSettings.DEFAULT_SCHEMA, SCHEMA_NAME );
	}

	@Override
	protected String createSecondSchema() {
		return SCHEMA_NAME;
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {StrTestEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() throws InterruptedException {
		// Revision 1
		EntityManager em = getEntityManager();
		em.getTransaction().begin();
		StrTestEntity ste = new StrTestEntity( "x" );
		em.persist( ste );
		steId = ste.getId();
		em.getTransaction().commit();

		// Revision 2
		em.getTransaction().begin();
		ste = em.find( StrTestEntity.class, steId );
		ste.setStr( "y" );
		em.getTransaction().commit();
	}

	@Test
	public void testRevinfoSchemaName() {
		TableSpecification revisionTable = getMetadata().getEntityBinding( "org.hibernate.envers.enhanced.SequenceIdRevisionEntity" )
				.getPrimaryTable();
		assert SCHEMA_NAME.equals( revisionTable.getSchema().getName().getSchema().getText() );
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList( 1, 2 ).equals( getAuditReader().getRevisions( StrTestEntity.class, steId ) );
	}

	@Test
	public void testHistoryOfId1() {
		StrTestEntity ver1 = new StrTestEntity( "x", steId );
		StrTestEntity ver2 = new StrTestEntity( "y", steId );

		assert getAuditReader().find( StrTestEntity.class, steId, 1 ).equals( ver1 );
		assert getAuditReader().find( StrTestEntity.class, steId, 2 ).equals( ver2 );
	}
}
