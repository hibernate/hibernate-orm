package org.hibernate.tool.internal.export.mapping;

import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import org.apache.commons.collections4.list.UnmodifiableList;
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
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.api.export.Exporter;
import org.hibernate.tool.api.xml.XMLPrettyPrinter;
import org.hibernate.tool.internal.util.DummyDialect;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serial;
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
    private final Marshaller marshaller;

    public MappingExporter() {
        mappingBinder = createMappingBinder();
        marshaller = createMarshaller();
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
        List<Binding<JaxbHbmHibernateMapping>> hbmBindings = getHbmBindings();
        List<Binding<JaxbEntityMappingsImpl>> transformedBindings = transformBindings(hbmBindings);
        for (int i = 0; i < hbmBindings.size(); i++) {
            marshall(
                    transformedBindings.get(i).getRoot(),
                    ((HbmXmlOrigin)hbmBindings.get(i).getOrigin()).getHbmXmlFile());
        }
    }

    private List<Binding<JaxbEntityMappingsImpl>> transformBindings(
            List<Binding<JaxbHbmHibernateMapping>> hbmBindings) {
        MetadataSources metadataSources = new MetadataSources( createServiceRegistry() );
        hbmBindings.forEach( metadataSources::addHbmXmlBinding );
        return HbmXmlTransformer.transform(
                hbmBindings,
                (MetadataImplementor) metadataSources.buildMetadata(),
                UnsupportedFeatureHandling.ERROR);
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

    private List<Binding<JaxbHbmHibernateMapping>> getHbmBindings() {
        List<Binding<JaxbHbmHibernateMapping>> result = new ArrayList<>();
        hbmXmlFiles.forEach((hbmXmlFile) -> {
            final String fullPath = hbmXmlFile.getAbsolutePath();
            LOGGER.info("Adding file: '" + fullPath + "' to the list to be transformed.");
            HbmXmlOrigin origin = new HbmXmlOrigin( hbmXmlFile );
            Binding<JaxbHbmHibernateMapping> binding = bindHbmXml( origin );
            result.add(binding);
        });
        return result;
    }

    private Binding<JaxbHbmHibernateMapping> bindHbmXml(HbmXmlOrigin origin) {
        File hbmXmlFile = origin.getHbmXmlFile();
        try ( final FileInputStream fileStream = new FileInputStream(hbmXmlFile) ) {
            return mappingBinder.bind( fileStream, origin );
        }
        catch (IOException e) {
            LOGGER.info( "Unable to open hbm.xml file `" + hbmXmlFile.getAbsolutePath() + "` for transformation");
            return null;
        }
    }

    private Marshaller createMarshaller() {
        try {
            return mappingBinder.mappingJaxbContext().createMarshaller();
        }
        catch (JAXBException e) {
            throw new RuntimeException("Unable to create JAXB Marshaller", e);
        }
    }

    private void marshall(
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

    static class  HbmXmlOrigin extends Origin {

        @Serial
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
