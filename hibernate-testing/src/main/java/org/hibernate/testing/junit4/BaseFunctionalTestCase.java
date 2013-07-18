package org.hibernate.testing.junit4;

import java.util.Arrays;
import java.util.Iterator;

import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.Dialect;
import org.hibernate.metamodel.spi.MetadataImplementor;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.binding.PluralAttributeBinding;

/**
 * @author Strong Liu <stliu@hibernate.org>
 */
public class BaseFunctionalTestCase extends BaseUnitTestCase  {
	public static final String VALIDATE_DATA_CLEANUP = "hibernate.test.validateDataCleanup";

	protected static final String[] NO_MAPPINGS = new String[0];
	protected static final Class<?>[] NO_CLASSES = new Class[0];
	private TestServiceRegistryHelper testServiceRegistryHelper = new TestServiceRegistryHelper( getTestConfiguration() );

	protected void initialize(){
		//to keep compatibility, collect the static info here
		getTestConfiguration().setCreateSchema( createSchema() );
		getTestConfiguration().setSecondSchemaName( createSecondSchema() );

		String[] mappings = getMappings();
		if ( mappings != null ) {
			for ( String mapping : mappings ) {
				getTestConfiguration().getMappings().add(
						getBaseForMappings() + mapping
				);
			}
		}
		Class<?>[] annotatedClasses = getAnnotatedClasses();
		if ( annotatedClasses != null ) {
			getTestConfiguration().getAnnotatedClasses().addAll( Arrays.asList( annotatedClasses ) );
		}
		String[] annotatedPackages = getAnnotatedPackages();
		if ( annotatedPackages != null ) {
			getTestConfiguration().getAnnotatedPackages().addAll( Arrays.asList( annotatedPackages ) );
		}
		String[] xmlFiles = getXmlFiles();
		if ( xmlFiles != null ) {
			getTestConfiguration().getOrmXmlFiles().addAll( Arrays.asList( xmlFiles ) );
		}
		getTestServiceRegistryHelper().setCallback(
				new TestServiceRegistryHelper.Callback() {
					@Override
					public void prepareStandardServiceRegistryBuilder(
							final StandardServiceRegistryBuilder serviceRegistryBuilder) {
						BaseFunctionalTestCase.this.prepareStandardServiceRegistryBuilder( serviceRegistryBuilder );
					}

					@Override
					public void prepareBootstrapServiceRegistryBuilder(
							final BootstrapServiceRegistryBuilder builder) {
						BaseFunctionalTestCase.this.prepareBootstrapServiceRegistryBuilder( builder );
					}
				}
		);
	}

	protected  final Dialect getDialect() {
		return getTestConfiguration().getDialect();
	}
	//----------------------- configuration properties

	protected boolean createSchema() {
		return true;
	}

	protected final Configuration configuration() {
		return getTestConfiguration().getConfiguration();
	}

	protected final MetadataImplementor metadata() {
		return getTestConfiguration().getMetadata();
	}



	//----------------------- services and service registry

	protected StandardServiceRegistryImpl serviceRegistry() {
		return getTestServiceRegistryHelper().getServiceRegistry();
	}
	
	protected void prepareStandardServiceRegistryBuilder(StandardServiceRegistryBuilder serviceRegistryBuilder) {
	}

	protected void prepareBootstrapServiceRegistryBuilder(BootstrapServiceRegistryBuilder builder) {
	}



	/**
	 * Feature supported only by H2 dialect.
	 * @return Provide not empty name to create second schema.
	 */
	protected String createSecondSchema() {
		return null;
	}

	protected String[] getMappings() {
		return NO_MAPPINGS;
	}

	protected String getBaseForMappings() {
		return "org/hibernate/test/";
	}

	protected Class<?>[] getAnnotatedClasses() {
		return NO_CLASSES;
	}

	protected String[] getAnnotatedPackages() {
		return NO_MAPPINGS;
	}

	protected String[] getXmlFiles() {
		// todo : rename to getOrmXmlFiles()
		return NO_MAPPINGS;
	}
	//-------------------------------------------

	protected EntityBinding getEntityBinding(Class<?> clazz) {
		return getTestConfiguration().getMetadata().getEntityBinding( clazz.getName() );
	}

	protected EntityBinding getRootEntityBinding(Class<?> clazz) {
		return getTestConfiguration().getMetadata().getRootEntityBinding( clazz.getName() );
	}

	protected Iterator<PluralAttributeBinding> getCollectionBindings() {
		return getTestConfiguration().getMetadata().getCollectionBindings().iterator();
	}

	public TestServiceRegistryHelper getTestServiceRegistryHelper() {
		return testServiceRegistryHelper;
	}

	public void setTestServiceRegistryHelper(final TestServiceRegistryHelper testServiceRegistryHelper) {
		this.testServiceRegistryHelper = testServiceRegistryHelper;
	}
}
