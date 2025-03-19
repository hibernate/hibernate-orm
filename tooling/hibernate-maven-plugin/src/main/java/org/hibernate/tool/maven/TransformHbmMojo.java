package org.hibernate.tool.maven;

import static org.apache.maven.plugins.annotations.LifecyclePhase.GENERATE_RESOURCES;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.SourceType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmHibernateMapping;
import org.hibernate.boot.jaxb.hbm.transform.HbmXmlTransformer;
import org.hibernate.boot.jaxb.hbm.transform.UnsupportedFeatureHandling;
import org.hibernate.boot.jaxb.internal.MappingBinder;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.service.ServiceRegistry;

import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;

@Mojo(
	name = "hbm2orm", 
	defaultPhase = GENERATE_RESOURCES,
	requiresDependencyResolution = ResolutionScope.RUNTIME)
public class TransformHbmMojo extends AbstractMojo {
	
    @Parameter(defaultValue = "${project.basedir}/src/main/resources")
	private File inputFolder;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		MappingBinder mappingBinder = new MappingBinder(
				MappingBinder.class.getClassLoader()::getResourceAsStream,
				UnsupportedFeatureHandling.ERROR);
		List<File> hbmFiles = getHbmFiles(inputFolder);
		List<Binding<JaxbHbmHibernateMapping>> hbmMappings = getHbmMappings(hbmFiles, mappingBinder);
		performTransformation(hbmMappings, mappingBinder, createServiceRegistry());
	}
	
	private ServiceRegistry createServiceRegistry() {
		StandardServiceRegistryBuilder ssrb = new StandardServiceRegistryBuilder();
		ssrb.clearSettings();
		ssrb.applySetting(JdbcSettings.ALLOW_METADATA_ON_BOOT, false);
		// Choose the H2 dialect by default, make this configurable
		ssrb.applySetting(JdbcSettings.DIALECT, H2Dialect.class.getName());
		return ssrb.build();
	}
	
	private void performTransformation(
			List<Binding<JaxbHbmHibernateMapping>> hbmBindings,
			MappingBinder mappingBinder,
			ServiceRegistry serviceRegistry) {
		Marshaller marshaller = createMarshaller(mappingBinder);
		MetadataSources metadataSources = new MetadataSources( serviceRegistry );
		hbmBindings.forEach( metadataSources::addHbmXmlBinding );
		List<Binding<JaxbEntityMappingsImpl>> transformedBindings = HbmXmlTransformer.transform(
				hbmBindings,
				(MetadataImplementor) metadataSources.buildMetadata(),
				UnsupportedFeatureHandling.ERROR
		);
		for (int i = 0; i < hbmBindings.size(); i++) {
			Binding<JaxbHbmHibernateMapping> hbmBinding = hbmBindings.get( i );
			Binding<JaxbEntityMappingsImpl> transformedBinding = transformedBindings.get( i );

			HbmXmlOrigin origin = (HbmXmlOrigin)hbmBinding.getOrigin();
			File hbmXmlFile = origin.getHbmXmlFile();

			marshall(marshaller, transformedBinding.getRoot(), hbmXmlFile);
		}
	}
	
	private List<Binding<JaxbHbmHibernateMapping>> getHbmMappings(List<File> hbmXmlFiles, MappingBinder mappingBinder) {
		List<Binding<JaxbHbmHibernateMapping>> result = new ArrayList<Binding<JaxbHbmHibernateMapping>>();
		hbmXmlFiles.forEach((hbmXmlFile) -> {
			final String fullPath = hbmXmlFile.getAbsolutePath();
			getLog().info("Adding file: '" + fullPath + "' to the list to be transformed.");
			Origin origin = new HbmXmlOrigin(hbmXmlFile);
			Binding<JaxbHbmHibernateMapping> binding = bindMapping( mappingBinder, hbmXmlFile, origin );
			result.add(binding);
		});
		return result;
	}
	
	private void marshall(Marshaller marshaller, JaxbEntityMappingsImpl mappings, File hbmXmlFile) {
		File mappingXmlFile =  new File(
				hbmXmlFile.getParentFile(), 
				hbmXmlFile.getName().replace(".hbm.xml", ".mapping.xml"));
        getLog().info("Marshalling file: " + hbmXmlFile.getAbsolutePath() + " into " + mappingXmlFile.getAbsolutePath());
		try {
			marshaller.marshal( mappings, mappingXmlFile );
		}
		catch (JAXBException e) {
			throw new RuntimeException(
					"Unable to marshall mapping JAXB representation to file `" + mappingXmlFile.getAbsolutePath() + "`",
					e
			);
		}
	}
	
	private Binding<JaxbHbmHibernateMapping> bindMapping(
			MappingBinder mappingBinder, File hbmXmlFile, Origin origin) {
		try ( final FileInputStream fileStream = new FileInputStream(hbmXmlFile) ) {
			return mappingBinder.bind( fileStream, origin );
		}
		catch (IOException e) {
			getLog().warn( "Unable to open hbm.xml file `" + hbmXmlFile.getAbsolutePath() + "` for transformation", e );
			return null;
		}
	}

	private Marshaller createMarshaller(MappingBinder mappingBinder) {
		try {
			return mappingBinder.mappingJaxbContext().createMarshaller();
		} catch (JAXBException e) {
			throw new RuntimeException("Unable to create JAXB Marshaller", e);
		}
	}
	
	private List<File> getHbmFiles(File f) {
		List<File> result = new ArrayList<File>();
		if (f.isFile()) {
			if (f.getName().endsWith("hbm.xml")) {
				result.add(f);
			}
		} else {
			for (File child : f.listFiles()) {
				result.addAll(getHbmFiles(child));
			}
		}
		return result;
	}
	
	private class HbmXmlOrigin extends Origin {

		private static final long serialVersionUID = 1L;

		private final File hbmXmlFile;

		public HbmXmlOrigin(File hbmXmlFile) {
			super( SourceType.FILE, hbmXmlFile.getAbsolutePath() );
			this.hbmXmlFile = hbmXmlFile;
		}

		public File getHbmXmlFile() {
			return hbmXmlFile;
		}

	}
	
}
