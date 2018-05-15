package org.hibernate.boot.spi.metadatabuildercontributor;

import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.spi.MetadataBuilderContributor;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.type.StandardBasicTypes;

/**
 * @author Vlad Mihalcea
 */
//tag::bootstrap-jpa-compliant-MetadataBuilderContributor-example[]
public class SqlFunctionMetadataBuilderContributor
        implements MetadataBuilderContributor {

    @Override
    public void contribute(MetadataBuilder metadataBuilder) {
        metadataBuilder.applySqlFunction(
            "instr", new StandardSQLFunction( "instr", StandardBasicTypes.STRING )
        );
    }
}
//end::bootstrap-jpa-compliant-MetadataBuilderContributor-example[]
