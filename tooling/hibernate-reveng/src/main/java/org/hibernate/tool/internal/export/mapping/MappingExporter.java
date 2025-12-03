package org.hibernate.tool.internal.export.mapping;

import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import org.apache.commons.collections4.list.UnmodifiableList;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmHibernateMapping;
import org.hibernate.boot.jaxb.hbm.transform.HbmXmlTransformer;
import org.hibernate.boot.jaxb.hbm.transform.UnsupportedFeatureHandling;
import org.hibernate.boot.jaxb.internal.MappingBinder;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.api.export.Exporter;
import org.hibernate.tool.api.xml.XMLPrettyPrinter;
import org.hibernate.tool.internal.util.DummyDialect;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

public class MappingExporter implements Exporter {

    private static final Logger LOGGER = Logger.getLogger( MappingExporter.class.getName() );

    private UnmodifiableList<File> hbmXmlFiles = new UnmodifiableList<>(Collections.emptyList());
    private boolean formatResult = true;

    private final MappingBinder mappingBinder;

    public MappingExporter() {
        mappingBinder = createMappingBinder();
     }

    public void setHbmFiles(List<File> fileList) {
        hbmXmlFiles = new UnmodifiableList<>( fileList );
    }

    public void setFormatResult(boolean formatResult) {
        this.formatResult = formatResult;
    }

    @Override
    public Properties getProperties() {
        return null;
    }

    @Override
    public void start() {
        List<Binding<JaxbHbmHibernateMapping>> hbmBindings = getHbmMappings();
        Marshaller marshaller = createMarshaller(mappingBinder);
        MetadataSources metadataSources = new MetadataSources( createServiceRegistry() );
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
            marshall(marshaller, transformedBinding.getRoot(), origin.getHbmXmlFile());
        }
    }

    private ServiceRegistry createServiceRegistry() {
        StandardServiceRegistryBuilder ssrb = new StandardServiceRegistryBuilder();
        ssrb.clearSettings();
        ssrb.applySetting(JdbcSettings.ALLOW_METADATA_ON_BOOT, false);
        ssrb.applySetting(JdbcSettings.DIALECT, DummyDialect.class.getName());
        return ssrb.build();
    }

    private MappingBinder createMappingBinder() {
        return new MappingBinder(
                MappingBinder.class.getClassLoader()::getResourceAsStream,
                UnsupportedFeatureHandling.ERROR);
    }

    private List<Binding<JaxbHbmHibernateMapping>> getHbmMappings() {
        List<Binding<JaxbHbmHibernateMapping>> result = new ArrayList<>();
        hbmXmlFiles.forEach((hbmXmlFile) -> {
            final String fullPath = hbmXmlFile.getAbsolutePath();
            LOGGER.info("Adding file: '" + fullPath + "' to the list to be transformed.");
            HbmXmlOrigin origin = new HbmXmlOrigin( hbmXmlFile );
            Binding<JaxbHbmHibernateMapping> binding = bindMapping( mappingBinder, origin );
            result.add(binding);
        });
        return result;
    }

    private Binding<JaxbHbmHibernateMapping> bindMapping(
            MappingBinder mappingBinder, HbmXmlOrigin origin) {
        File hbmXmlFile = origin.getHbmXmlFile();
        try ( final FileInputStream fileStream = new FileInputStream(hbmXmlFile) ) {
            return mappingBinder.bind( fileStream, origin );
        }
        catch (IOException e) {
            LOGGER.info( "Unable to open hbm.xml file `" + hbmXmlFile.getAbsolutePath() + "` for transformation");
            return null;
        }
    }

    private Marshaller createMarshaller(MappingBinder mappingBinder) {
        try {
            return mappingBinder.mappingJaxbContext().createMarshaller();
        }
        catch (JAXBException e) {
            throw new RuntimeException("Unable to create JAXB Marshaller", e);
        }
    }

    private void marshall(
            Marshaller marshaller,
            JaxbEntityMappingsImpl mappings,
            File hbmXmlFile) {
        File mappingXmlFile =  new File(
                hbmXmlFile.getParentFile(),
                hbmXmlFile.getName().replace(".hbm.xml", ".mapping.xml"));
        LOGGER.info("Marshalling file: " + hbmXmlFile.getAbsolutePath() + " into " + mappingXmlFile.getAbsolutePath());
        try {
            marshaller.marshal( mappings, mappingXmlFile );
            if (formatResult) {
                XMLPrettyPrinter.prettyPrintFile(mappingXmlFile);
            }
        }
        catch (JAXBException e) {
            throw new RuntimeException(
                    "Unable to marshall mapping JAXB representation to file `" + mappingXmlFile.getAbsolutePath() + "`",
                    e);
        }
        catch (IOException e) {
            throw new RuntimeException(
                    "Unable to format XML file `" + mappingXmlFile.getAbsolutePath() + "`",
                    e);
        }
    }

}
