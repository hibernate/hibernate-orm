package org.hibernate.test.insertordering;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToMany;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.SortNatural;
import org.hibernate.annotations.Where;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

/**
 * @author Normunds Gavars
 * @author Nathan Xu
 */
@TestForIssue( jiraKey = "HHH-14227" )
@RequiresDialectFeature(DialectChecks.SupportsJdbcDriverProxying.class)
public class InsertOrderingSelfReferenceTest extends BaseInsertOrderingTest {

    @Override
    protected Class<?>[] getAnnotatedClasses() {
        return new Class<?>[] {
                Parameter.class,
                InputParameter.class,
                OutputParameter.class,
                Placeholder.class,
        };
    }

    @Test
    public void testReferenceItself() {
        doInHibernate( this::sessionFactory, session -> {
            Placeholder placeholder = new Placeholder();
            session.save( placeholder );

            OutputParameter outputParameter1 = new OutputParameter();

            OutputParameter childOutputParameter = new OutputParameter();
            outputParameter1.children.add( childOutputParameter );
            childOutputParameter.parent = outputParameter1;

            session.save( outputParameter1 );

            Placeholder placeholder2 = new Placeholder();
            session.save( placeholder2 );

            InputParameter inputParameter = new InputParameter();
            session.save( inputParameter );

            OutputParameter outputParameter2 = new OutputParameter();
            session.save( outputParameter2 );

            clearBatches();
        } );

        verifyContainsBatches(
                new Batch( "insert into Placeholder (id) values (?)", 2 ),
                new Batch( "insert into Parameter (parent_id, TYPE, id) values (?, 'INPUT', ?)" ),
                new Batch( "insert into Parameter (parent_id, TYPE, id) values (?, 'OUTPUT', ?)", 3 )
        );
    }

    @MappedSuperclass
    static class AbstractEntity {

        @Id
        @GeneratedValue
        Long id;

    }

    @Entity(name = "Placeholder")
    static class Placeholder extends AbstractEntity {
    }

    @Entity(name = "Parameter")
    @DiscriminatorColumn(name = "TYPE")
    @Inheritance(strategy = InheritanceType.SINGLE_TABLE)
    static abstract class Parameter extends AbstractEntity {
    }

    @Entity(name = "InputParameter")
    @DiscriminatorValue("INPUT")
    static class InputParameter extends Parameter {

        @ManyToOne(fetch = FetchType.LAZY)
        InputParameter parent;

        @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "parent")
        @SortNatural
        @Where(clause = "TYPE = 'INPUT'")
        @Fetch(FetchMode.SUBSELECT)
        List<InputParameter> children = new ArrayList<>();
    }

    @Entity(name = "OutputParameter")
    @DiscriminatorValue("OUTPUT")
    static class OutputParameter extends Parameter {

        @ManyToOne(fetch = FetchType.LAZY)
        OutputParameter parent;

        @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "parent")
        @SortNatural
        @Where(clause = "TYPE = 'OUTPUT'")
        @Fetch(FetchMode.SUBSELECT)
        @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
        List<OutputParameter> children = new ArrayList<>();

    }
}
