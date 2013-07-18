package org.hibernate.testing.junit4;

import java.util.Properties;

import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.config.ConfigurationHelper;

/**
 * @author Strong Liu <stliu@hibernate.org>
 */
public class TestServiceRegistryHelper {
	private static final DefaultCallback DEFAULT_REGISTERY_BUILDER_CALLBACK = new DefaultCallback();
	private final TestConfigurationHelper configuration;
	private StandardServiceRegistryImpl serviceRegistry;
	private BootstrapServiceRegistry bootstrapServiceRegistry;
	private Callback callback = DEFAULT_REGISTERY_BUILDER_CALLBACK;

	public TestServiceRegistryHelper(final TestConfigurationHelper configuration) {
		this.configuration = configuration;
	}

	public TestConfigurationHelper getConfiguration() {
		return configuration;
	}

	public Callback getCallback() {
		return callback;
	}

	public void setCallback(final Callback callback) {
		this.callback = callback;
	}

	public BootstrapServiceRegistry buildBootstrapServiceRegistry() {
		final BootstrapServiceRegistryBuilder builder = new BootstrapServiceRegistryBuilder();
		getCallback().prepareBootstrapServiceRegistryBuilder( builder );
		return builder.build();
	}

	public StandardServiceRegistryImpl getServiceRegistry() {
		if ( serviceRegistry == null ) {
			serviceRegistry = buildServiceRegistry( getBootstrapServiceRegistry() );
		}
		return serviceRegistry;
	}

	public void setServiceRegistry(final StandardServiceRegistryImpl serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	public BootstrapServiceRegistry getBootstrapServiceRegistry() {
		if ( bootstrapServiceRegistry == null ) {
			bootstrapServiceRegistry = buildBootstrapServiceRegistry();
		}
		return bootstrapServiceRegistry;
	}


	private StandardServiceRegistryImpl buildServiceRegistry(
			BootstrapServiceRegistry bootRegistry) {
		final Properties properties = getConfiguration().getProperties();
		Environment.verifyProperties( properties );
		ConfigurationHelper.resolvePlaceHolders( properties );

		StandardServiceRegistryBuilder registryBuilder = new StandardServiceRegistryBuilder( bootRegistry ).applySettings(
				properties
		);
		getCallback().prepareStandardServiceRegistryBuilder( registryBuilder );

		// TODO: StandardServiceRegistryBuilder#applySettings sets up the
		// Environment.URL property.  Rather than rely on that, createH2Schema
		// could be refactored and this whole block put back in
		// BaseCoreFunctionalTestCase#constructProperties
		if ( getConfiguration().isCreateSchema() ) {
			registryBuilder.applySetting( Environment.HBM2DDL_AUTO, "create-drop" );
			final String secondSchemaName = getConfiguration().getSecondSchemaName();
			if ( StringHelper.isNotEmpty( secondSchemaName ) ) {
				if ( !( getConfiguration().getDialect() instanceof H2Dialect ) ) {
					throw new UnsupportedOperationException( "Only H2 dialect supports creation of second schema." );
				}
				Helper.createH2Schema( secondSchemaName, registryBuilder.getSettings() );
			}
		}

		return (StandardServiceRegistryImpl) registryBuilder.build();
	}

	public void destroy() {
		if ( getServiceRegistry() != null ) {
			StandardServiceRegistryBuilder.destroy( getServiceRegistry() );
			setServiceRegistry( null );
		}
		if ( getBootstrapServiceRegistry() != null ) {
			bootstrapServiceRegistry = null;
		}
	}


	public static interface Callback {
		void prepareStandardServiceRegistryBuilder(StandardServiceRegistryBuilder serviceRegistryBuilder);

		void prepareBootstrapServiceRegistryBuilder(BootstrapServiceRegistryBuilder builder);
	}

	public static class DefaultCallback implements Callback {
		@Override
		public void prepareBootstrapServiceRegistryBuilder(
				final BootstrapServiceRegistryBuilder builder) {
		}

		@Override
		public void prepareStandardServiceRegistryBuilder(
				final StandardServiceRegistryBuilder serviceRegistryBuilder) {
		}
	}
}
