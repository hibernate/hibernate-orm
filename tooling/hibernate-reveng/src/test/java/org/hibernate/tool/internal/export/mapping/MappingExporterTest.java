package org.hibernate.tool.internal.export.mapping;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import org.apache.commons.collections4.list.UnmodifiableList;
import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.SourceType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmHibernateMapping;
import org.hibernate.boot.jaxb.hbm.transform.UnsupportedFeatureHandling;
import org.hibernate.boot.jaxb.internal.MappingBinder;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.jaxb.spi.JaxbBindableMappingDescriptor;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.internal.util.DummyDialect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.hibernate.tool.internal.export.mapping.MappingExporter.HbmXmlOrigin;
import static org.junit.jupiter.api.Assertions.*;

public class MappingExporterTest {

    private MappingExporter mappingExporter;

    @TempDir
    private File tempDir;

    @BeforeEach
    public void beforeEach() {
        mappingExporter = new MappingExporter();
    }

    @Test
    public void testConstructor() throws Exception {
        Field mappingBinderField = MappingExporter.class.getDeclaredField("mappingBinder");
        mappingBinderField.setAccessible(true);
        assertNotNull(mappingBinderField.get(mappingExporter));
        Field marshallerField = MappingExporter.class.getDeclaredField("marshaller");
        marshallerField.setAccessible(true);
        assertNotNull(marshallerField.get(mappingExporter));
    }

    @Test
    public void testSetHbmFiles() throws NoSuchFieldException, IllegalAccessException {
        Field hbmFilesField = MappingExporter.class.getDeclaredField("hbmXmlFiles");
        List<File> origin = new ArrayList<>();
        File fooFile = new File( tempDir, "foo.bar" );
        origin.add( fooFile );
        assertNotNull(hbmFilesField);
        hbmFilesField.setAccessible(true);
        assertTrue(((List<?>)hbmFilesField.get(mappingExporter)).isEmpty());
        mappingExporter.setHbmFiles(origin);
        List<?> destination = (List<?>) hbmFilesField.get(mappingExporter);
        assertEquals(1, destination.size());
        assertTrue(destination.contains(fooFile));
    }

    @Test
    public void testCreateServiceRegistry()
            throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Method createServiceRegistryMethod = MappingExporter.class.getDeclaredMethod("createServiceRegistry");
        assertNotNull(createServiceRegistryMethod);
        createServiceRegistryMethod.setAccessible(true);
        Object object = createServiceRegistryMethod.invoke(mappingExporter);
        assertNotNull(object);
        assertInstanceOf(ServiceRegistry.class, object);
        try (ServiceRegistry serviceRegistry = (ServiceRegistry) object) {
			Dialect dialect = Objects.requireNonNull(serviceRegistry.getService(JdbcServices.class)).getDialect();
			assertInstanceOf(DummyDialect.class, dialect);
		}
    }

    @Test
    public void testCreateMappingBinder()
            throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Method createMappingBinderMethod = MappingExporter.class.getDeclaredMethod("createMappingBinder");
        assertNotNull(createMappingBinderMethod);
        createMappingBinderMethod.setAccessible(true);
        Object object = createMappingBinderMethod.invoke(mappingExporter);
        assertNotNull(object);
        assertInstanceOf(MappingBinder.class, object);
        MappingBinder mappingBinder = (MappingBinder) object;
        assertNotNull(mappingBinder);
    }

    @Test
    public void testBindHbmXml() throws Exception{
        Method bindMappingMethod = MappingExporter.class.getDeclaredMethod(
                "bindHbmXml",
                HbmXmlOrigin.class);
        assertNotNull(bindMappingMethod);
        bindMappingMethod.setAccessible(true);
        File file = new File(this.tempDir, "foo.bar");
        Files.writeString(file.toPath(), "foobar");
        final MappingBinder mappingBinder = new TestMappingBinder(file, "barfoo");
        Field mappingBinderField = MappingExporter.class.getDeclaredField("mappingBinder");
        mappingBinderField.setAccessible(true);
        mappingBinderField.set(mappingExporter, mappingBinder);
        assertNotEquals("barfoo", Files.readString(file.toPath()));
        Object object = bindMappingMethod.invoke(mappingExporter, new HbmXmlOrigin(file));
        assertInstanceOf(Binding.class, object);
        Origin origin = ((Binding<?>)object).getOrigin();
        assertInstanceOf(HbmXmlOrigin.class, origin);
        assertSame(file, ((HbmXmlOrigin)origin).getHbmXmlFile());
        assertEquals("barfoo", Files.readString(file.toPath()));
    }

    @Test
    public void testGetHbmBindings() throws Exception {
        Method getHbmMappingsMethod = MappingExporter.class.getDeclaredMethod("getHbmBindings");
        assertNotNull(getHbmMappingsMethod);
        getHbmMappingsMethod.setAccessible(true);
        Field hbmFilesField = MappingExporter.class.getDeclaredField("hbmXmlFiles");
        hbmFilesField.setAccessible(true);
        List<File> hbmXmlFiles = new ArrayList<>();
        File file = new File(this.tempDir, "foo.bar");
        Files.writeString(file.toPath(), "foobar");
        hbmXmlFiles.add(file);
        hbmFilesField.set(mappingExporter, new UnmodifiableList<>(hbmXmlFiles));
        Field mappingBinderField = MappingExporter.class.getDeclaredField("mappingBinder");
        mappingBinderField.setAccessible(true);
        mappingBinderField.set(mappingExporter, new TestMappingBinder(file, "barfoo"));
        Object object = getHbmMappingsMethod.invoke(mappingExporter);
        assertInstanceOf(List.class, object);
        assertEquals(1, ((List<?>)object).size());
        object = ((List<?>) object).get(0);
        assertInstanceOf(Binding.class, object);
        Origin origin = ((Binding<?>)object).getOrigin();
        assertInstanceOf(HbmXmlOrigin.class, origin);
        assertSame(file, ((HbmXmlOrigin)origin).getHbmXmlFile());
        assertEquals("barfoo", Files.readString(file.toPath()));
    }

    @Test
    public void testCreateMarshaller() throws Exception {
        Method createMarshallerMethod = MappingExporter.class.getDeclaredMethod("createMarshaller");
        assertNotNull(createMarshallerMethod);
        createMarshallerMethod.setAccessible(true);
        final MappingBinder mappingBinder = new TestMappingBinder(new File("foo"), "foobar");
        Field mappingBinderField = MappingExporter.class.getDeclaredField("mappingBinder");
        mappingBinderField.setAccessible(true);
        mappingBinderField.set(mappingExporter, mappingBinder);
        assertSame(
                DUMMY_MARSHALLER,
                createMarshallerMethod.invoke(mappingExporter));
    }

    @Test
    public void testMarshall() throws Exception {
        Method marshallMethod = MappingExporter.class.getDeclaredMethod(
                "marshall",
                JaxbEntityMappingsImpl.class,
                File.class);
        assertNotNull(marshallMethod);
        marshallMethod.setAccessible(true);
        Field marshallerField = MappingExporter.class.getDeclaredField("marshaller");
        marshallerField.setAccessible(true);
        marshallerField.set(mappingExporter, DUMMY_MARSHALLER);
        File hbmFile = new File(this.tempDir, "foo.hbm.xml");
        File mappingFile = new File(this.tempDir, "foo.mapping.xml");
        Files.writeString(mappingFile.toPath(), "<foo><bar>foobar</bar></foo>");
        List<String> lines = Files.readAllLines(mappingFile.toPath());
        assertEquals(1, lines.size());
        marshallMethod.invoke(mappingExporter, null, hbmFile);
        lines = Files.readAllLines(mappingFile.toPath());
        assertEquals(4, lines.size());
        Field formatResultField = MappingExporter.class.getDeclaredField("formatResult");
        formatResultField.setAccessible(true);
        formatResultField.set(mappingExporter, false);
        Files.writeString(mappingFile.toPath(), "<foo><bar>foobar</bar></foo>");
        lines = Files.readAllLines(mappingFile.toPath());
        assertEquals(1, lines.size());
        marshallMethod.invoke(mappingExporter, null, hbmFile);
        lines = Files.readAllLines(mappingFile.toPath());
        assertEquals(1, lines.size());
    }

    @Test
    public void testTransformBindings() throws Exception {
        FileInputStream simpleHbmXmlInputStream = null;
        try {
            File simpleHbmXmlFile = new File( this.tempDir, "simple.hbm.xml" );
            Files.writeString( simpleHbmXmlFile.toPath(), SIMPLE_HBM_XML );
            MappingBinder mappingBinder = new MappingBinder(
                    MappingBinder.class.getClassLoader()::getResourceAsStream,
                    UnsupportedFeatureHandling.ERROR );
            simpleHbmXmlInputStream = new FileInputStream( simpleHbmXmlFile );
            Binding<JaxbHbmHibernateMapping> hbmBinding = mappingBinder.bind(
                    simpleHbmXmlInputStream,
                    new HbmXmlOrigin( simpleHbmXmlFile ) );
            List<Binding<JaxbHbmHibernateMapping>> bindings = new ArrayList<>();
            bindings.add( hbmBinding );
            Method transformBindingsMethod = MappingExporter.class.getDeclaredMethod(
                    "transformBindings",
                    List.class );
            assertNotNull( transformBindingsMethod );
            transformBindingsMethod.setAccessible( true );
            List<?> transformedBindings = (List<?>) transformBindingsMethod.invoke( mappingExporter, bindings );
            assertNotNull( transformedBindings );
            assertEquals( 1, transformedBindings.size() );
            Object object = transformedBindings.get( 0 );
            assertInstanceOf( Binding.class, object );
            Binding<?> entityBinding = (Binding<?>) object;
            Origin origin = entityBinding.getOrigin();
            assertInstanceOf( HbmXmlOrigin.class, origin );
            assertSame( simpleHbmXmlFile, ((HbmXmlOrigin) origin).getHbmXmlFile() );
            Object root = entityBinding.getRoot();
            assertInstanceOf( JaxbEntityMappingsImpl.class, root );
            JaxbEntityMappingsImpl entityMappings = (JaxbEntityMappingsImpl) root;
            assertEquals( 1, entityMappings.getEntities().size() );
            assertEquals( "Foo", entityMappings.getEntities().get( 0 ).getClazz() );
        } finally {
            if ( simpleHbmXmlInputStream != null ) {
                simpleHbmXmlInputStream.close();
            }
        }
    }

    @Test
    public void testStart() throws Exception {
        File simpleHbmXmlFile = new File(this.tempDir, "simple.hbm.xml");
        File simpleMappingXmlFile = new File(this.tempDir, "simple.mapping.xml");
        Files.writeString(simpleHbmXmlFile.toPath(), SIMPLE_HBM_XML);
        Field hbmFilesField = MappingExporter.class.getDeclaredField("hbmXmlFiles");
        hbmFilesField.setAccessible(true);
        hbmFilesField.set(
                mappingExporter,
                new UnmodifiableList<>(List.of(simpleHbmXmlFile)));
        assertTrue(simpleHbmXmlFile.exists());
        assertFalse(simpleMappingXmlFile.exists());
        mappingExporter.start();
        assertTrue(simpleMappingXmlFile.exists());
        String mappingXml = Files.readString(simpleMappingXmlFile.toPath());
        assertTrue(mappingXml.contains("entity-mappings"));
    }

    @Test
    public void testHbmXmlOrigin() {
        File hbmXmlFile = new File(tempDir, "foo.hbm.xml");
        HbmXmlOrigin hxo = new HbmXmlOrigin(hbmXmlFile);
        assertNotNull(hxo);
        assertEquals(hbmXmlFile, hxo.getHbmXmlFile());
        assertEquals(hbmXmlFile.getAbsolutePath(), hxo.getName());
        assertEquals(SourceType.FILE, hxo.getType());
    }

    private static final Marshaller DUMMY_MARSHALLER = (Marshaller) Proxy.newProxyInstance(
            MappingExporterTest.class.getClassLoader(),
            new Class<?>[]{Marshaller.class},
            (proxy, method, args) -> {
                if ("marshall".equals(method.getName())) {
                    Files.writeString(((File)args[1]).toPath(), "foobar");
                }
                return proxy;
            }

    );

    private static class TestMappingBinder extends MappingBinder {
        private final File f;
        private final String s;
        public TestMappingBinder(File f, String s) {
            super(null, null, null);
            this.f = f;
            this.s = s;
        }
        @Override public <X extends JaxbBindableMappingDescriptor> org.hibernate.boot.jaxb.spi.Binding<X> bind(InputStream is, Origin o) {
            try {
                Files.writeString(f.toPath(), s);
            }
            catch (Exception ignore) {}
            return new Binding<>(null, new HbmXmlOrigin(f));
        }
        @Override public JAXBContext mappingJaxbContext() {
            return new JAXBContext() {
                @Override
                public Unmarshaller createUnmarshaller() {
                    return null;
                }
                @Override
                public Marshaller createMarshaller() {
                    return DUMMY_MARSHALLER;
                }
            };
        }
    }

    private static final String SIMPLE_HBM_XML =
                """
                        <hibernate-mapping>
                        	<class name="Foo">
                        		<id name="id" type="long"/>
                        		<property name="name" type="string"/>
                        	</class>
                        </hibernate-mapping>
                    """;

}
