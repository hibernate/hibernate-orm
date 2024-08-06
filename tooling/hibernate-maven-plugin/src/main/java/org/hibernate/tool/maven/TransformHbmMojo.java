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
import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.SourceType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmHibernateMapping;
import org.hibernate.boot.jaxb.hbm.transform.HbmXmlTransformer;
import org.hibernate.boot.jaxb.hbm.transform.UnsupportedFeatureHandling;
import org.hibernate.boot.jaxb.internal.MappingBinder;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.jaxb.spi.Binding;

import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;

@Mojo(name = "hbm2orm", defaultPhase = GENERATE_RESOURCES)
public class TransformHbmMojo extends AbstractMojo {
	
    @Parameter(defaultValue = "${project.basedir}/src/main/resources")
	private File inputFolder;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		MappingBinder mappingBinder = new MappingBinder(
				MappingBinder.class.getClassLoader()::getResourceAsStream,
				UnsupportedFeatureHandling.ERROR);
		Marshaller marshaller = createMarshaller(mappingBinder);
		List<File> hbmFiles = getHbmFiles(inputFolder);
		List<Binding<JaxbHbmHibernateMapping>> hbmMappings = getHbmMappings(hbmFiles, mappingBinder);
		List<Binding<JaxbEntityMappingsImpl>> transformed = 
				HbmXmlTransformer.transform(hbmMappings, UnsupportedFeatureHandling.ERROR);
		for (int i = 0; i < hbmFiles.size(); i++) {
			marshall(marshaller, transformed.get(i).getRoot(), hbmFiles.get(i));
		}
	}
	
	private List<Binding<JaxbHbmHibernateMapping>> getHbmMappings(List<File> hbmXmlFiles, MappingBinder mappingBinder) {
		List<Binding<JaxbHbmHibernateMapping>> result = new ArrayList<Binding<JaxbHbmHibernateMapping>>();
		hbmXmlFiles.forEach((hbmXmlFile) -> {
			final String fullPath = hbmXmlFile.getAbsolutePath();
			getLog().info("Adding file: '" + fullPath + "' to the list to be transformed.");
			Origin origin = new Origin(SourceType.FILE, hbmXmlFile.getAbsolutePath());
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
	
}
