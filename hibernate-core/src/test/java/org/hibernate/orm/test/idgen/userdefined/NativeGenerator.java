package org.hibernate.orm.test.idgen.userdefined;

import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.ExportableProducer;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.generator.Generator;
import org.hibernate.generator.OnExecutionGenerator;
import org.hibernate.id.Configurable;
import org.hibernate.id.IdentityGenerator;
import org.hibernate.id.PostInsertIdentityPersister;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.id.factory.spi.CustomIdGeneratorCreationContext;
import org.hibernate.id.insert.InsertGeneratedIdentifierDelegate;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.Type;

import java.lang.reflect.Member;
import java.util.EnumSet;
import java.util.Properties;

public class NativeGenerator
        implements OnExecutionGenerator, BeforeExecutionGenerator, Configurable, ExportableProducer {

    private final Generator generator;

    public NativeGenerator(NativeId nativeId, Member member, CustomIdGeneratorCreationContext creationContext) {
		final String strategy = creationContext.getDatabase().getDialect().getNativeIdentifierGeneratorStrategy();
        switch (strategy) {
            case "sequence":
                generator = new SequenceStyleGenerator();
                break;
            case "identity":
                creationContext.getProperty().getValue().getColumns().get(0).setIdentity(true);
                generator = new IdentityGenerator();
                break;
            default:
                throw new IllegalArgumentException();
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
        if ( generator instanceof Configurable ) {
            ((Configurable) generator).configure( type, parameters, serviceRegistry );
        }
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
