package org.hibernate.orm.test.idgen.userdefined;

import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.ExportableProducer;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.generator.Generator;
import org.hibernate.generator.GeneratorCreationContext;
import org.hibernate.generator.OnExecutionGenerator;
import org.hibernate.id.Configurable;
import org.hibernate.id.PostInsertIdentityPersister;
import org.hibernate.id.factory.IdentifierGeneratorFactory;
import org.hibernate.id.factory.spi.CustomIdGeneratorCreationContext;
import org.hibernate.id.insert.InsertGeneratedIdentifierDelegate;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.Type;

import java.lang.reflect.Member;
import java.util.EnumSet;
import java.util.Properties;

public class NativeGenerator
        implements OnExecutionGenerator, BeforeExecutionGenerator, Configurable, ExportableProducer {

    private final IdentifierGeneratorFactory factory;
    private final String strategy;
    private final CustomIdGeneratorCreationContext creationContext;

    private Generator generator;

    public NativeGenerator(NativeId nativeId, Member member, CustomIdGeneratorCreationContext creationContext) {
        this.creationContext = creationContext;
        factory = creationContext.getIdentifierGeneratorFactory();
		strategy = creationContext.getDatabase().getDialect().getNativeIdentifierGeneratorStrategy();
        if ( "identity".equals(strategy) ) {
            creationContext.getProperty().getValue().getColumns().get(0).setIdentity(true);
        }
    }

    @Override
    public EnumSet<EventType> getEventTypes() {
        return generator.getEventTypes();
    }

    @Override
    public boolean generatedOnExecution() {
        return generator.generatedOnExecution();
    }

    @Override
    public void configure(Type type, Properties parameters, ServiceRegistry serviceRegistry) {
        generator = factory.createIdentifierGenerator(
                strategy,
                type,
                creationContext,
                parameters
        );
        //TODO: should use this instead of the deprecated method, but see HHH-18135
//        GenerationType generationType;
//        switch (strategy) {
//            case "identity":
//                generationType = GenerationType.IDENTITY;
//                break;
//            case "sequence":
//                generationType = GenerationType.SEQUENCE;
//                break;
//            default:
//                throw new AssertionFailure("unrecognized strategy");
//        }
//        generator =
//                factory.createIdentifierGenerator( generationType, strategy, strategy, type.getJavaTypeDescriptor(),
//                        parameters, (a, b) -> null );
    }

    @Override
    public void registerExportables(Database database) {
        if ( generator instanceof ExportableProducer ) {
            ((ExportableProducer) generator).registerExportables(database);
        }
    }

    @Override
    public void initialize(SqlStringGenerationContext context) {
        if ( generator instanceof Configurable ) {
            ((Configurable) generator).initialize(context);
        }
    }

    @Override
    public Object generate(SharedSessionContractImplementor session, Object owner, Object currentValue, EventType eventType) {
        return ((BeforeExecutionGenerator) generator).generate(session, owner, currentValue, eventType);
    }

    @Override
    public boolean referenceColumnsInSql(Dialect dialect) {
        return ((OnExecutionGenerator) generator).referenceColumnsInSql(dialect);
    }

    @Override
    public boolean writePropertyValue() {
        return ((OnExecutionGenerator) generator).writePropertyValue();
    }

    @Override
    public String[] getReferencedColumnValues(Dialect dialect) {
        return ((OnExecutionGenerator) generator).getReferencedColumnValues(dialect);
    }

    @Override
    public InsertGeneratedIdentifierDelegate getGeneratedIdentifierDelegate(PostInsertIdentityPersister persister) {
        return ((OnExecutionGenerator) generator).getGeneratedIdentifierDelegate(persister);
    }
}
