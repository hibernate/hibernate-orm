package org.hibernate.ejb.criteria.jpaMapMode;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.hibernate.SessionFactory;
import org.junit.Before;
import org.junit.Test;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import javax.sql.DataSource;

public class HibernateStorageImplTest  {

    private DataSource dataSource;

    private MetaModel metaModel;
    private DocumentSessionFactoryBean sessionFactoryBean;

    @Before
    public void setup() {
        metaModel = StorageImplTest.createSampleMetaModel();
        MultiTableSchemaGeneratorImpl multiTableSchemaGeneratorImpl = new MultiTableSchemaGeneratorImpl();
        multiTableSchemaGeneratorImpl.setSqlGenerator(SchemaSqlGeneratorFactory
                .createSchemaSqlGenerator());
        MetaModelMapping metaModelMapping = multiTableSchemaGeneratorImpl
                .createMetaModelMapping(metaModel);

        multiTableSchemaGeneratorImpl.setDataSource(dataSource);

        //multiTableSchemaGeneratorImpl.createDocumentTables(metaModelMapping);

        sessionFactoryBean = new DocumentSessionFactoryBean();
        sessionFactoryBean.setDataSource(dataSource);
        sessionFactoryBean.setMetaModelMapping(metaModelMapping);
    }

    @Test
    public void findById() {
        SessionFactory sessionFactory = sessionFactoryBean.getObject();
        HibernateStorageImpl impl = new HibernateStorageImpl();
        impl.setSessionFactory(sessionFactory);

        DocumentInstance result = impl.find(metaModel.getDocuments().iterator()
                .next(), null, null, UUID.randomUUID());
        assertNull(result);
    }

    @Test
    public void store() {
//        SessionFactory sessionFactory = sessionFactoryBean.getObject();
//        HibernateStorageImpl impl = new HibernateStorageImpl();
//        impl.setSessionFactory(sessionFactory);

        final DocumentManagerFactory entityManagerFactory = sessionFactoryBean.getEntityManagerFactory();

        Document document = metaModel.getDocuments().iterator().next();
        DocumentInstance documentInstance = new DocumentInstance(document);
        Property property = document.getProperties().iterator().next();
        PropertyInstance propertyInstance = new PropertyInstance(property, 4d);
        documentInstance.setPropertyInstance(property, propertyInstance);
        assertNull(documentInstance.getId());

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        entityManager.getTransaction().begin();
        entityManager.persist(documentInstance);
        entityManager.getTransaction().commit();
        assertNotNull(documentInstance.getId());

//        DocumentInstance result = retrieve(impl, document, documentInstance);
//        assertNotNull(result);
//        assertEquals(documentInstance.getId(), result.getId());
//        assertEquals(4d, documentInstance.getPropertyInstance(property)
//                .getValue());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void criteriaFind() {
        final DocumentManagerFactory entityManagerFactory = sessionFactoryBean.getEntityManagerFactory();

        final Document document = metaModel.getDocument("aDocument");    // any document

        final Document document2 = metaModel.getDocument("anotherDocument");

        final Relationship relationship = metaModel.getRelationships().iterator().next();


        final Object two = 2;
        final Double three = 3d;
        final Object four = 4;
        final Object six = 6;
        final Double ten = 10d;
        final Double fortyFour = 44d;

        final Iterator<Property> iterator = document.getProperties().iterator();
        final Property propertyOne = iterator.next();                            // any property
        final Property propertyTwo = iterator.next();                            // any property
        final EntityManager entityManager = entityManagerFactory.createEntityManager();
        final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();

        {
            final CriteriaQuery criteriaQuery = criteriaBuilder.createQuery();

            // relieving the caller from the burden of knowing a few JPA semantics
            // (e.g., that a Document maps to a JPA EntityType) is probably not worth the effort
            final Root documentRoot = criteriaQuery.from(entityManagerFactory.getEntityType(document));

            // the fact that both our metamodel and JPA uses a String to represent an Attribute name is convenient
            final Path propertyOnePath = documentRoot.get(propertyOne.getName());
            final Path propertyTwoPath = documentRoot.get(propertyTwo.getName());

            DocumentInstance dummyParam = new DocumentInstance(document);
            dummyParam.setId(UUID.randomUUID());

            criteriaQuery.select(
                    documentRoot // if there's only one root this is implied
            ).where(
                    criteriaBuilder.or(
                            criteriaBuilder.and(
                                    criteriaBuilder.notEqual(propertyOnePath, four),
                                    criteriaBuilder.lessThanOrEqualTo(propertyTwoPath, three)
                            ),
                            criteriaBuilder.and(
                                    propertyTwoPath.isNull(),
                                    propertyOnePath.isNotNull()
                            ),
                            criteriaBuilder.not(propertyTwoPath.in(six, four, three, two)),
                            criteriaBuilder.between(propertyTwoPath, ten, fortyFour),
                            criteriaBuilder.equal(propertyTwoPath, propertyOnePath),
                            documentRoot.in(dummyParam)
                    )
            );


            final TypedQuery<DocumentInstance> query = entityManager.createQuery(criteriaQuery);

            final List<DocumentInstance> results = query.getResultList();

            assertNotNull(results);
            //assertEquals(0, results.size());
        }

        {
            final CriteriaQuery criteriaQuery = criteriaBuilder.createQuery();

            // relieving the caller from the burden of knowing a few JPA semantics
            // (e.g., that a Document maps to a JPA EntityType) is probably not worth the effort
            final Root documentRoot = criteriaQuery.from(entityManagerFactory.getEntityType(document));
            documentRoot.fetch(relationship.getRoleName(Relationship.Side.FROM), JoinType.LEFT);   // fetch parent, if any

            final TypedQuery<DocumentInstance> query = entityManager.createQuery(criteriaQuery);

            final List<DocumentInstance> results = query.getResultList();
        }

        {
            final CriteriaQuery criteriaQuery = criteriaBuilder.createQuery();

            // relieving the caller from the burden of knowing a few JPA semantics
            // (e.g., that a Document maps to a JPA EntityType) is probably not worth the effort
            final Root document2Root = criteriaQuery.from(entityManagerFactory.getEntityType(document2));
            document2Root.fetch(relationship.getRoleName(Relationship.Side.TO), JoinType.LEFT); // fetch children, if any

            final TypedQuery<DocumentInstance> query = entityManager.createQuery(criteriaQuery);

            final List<DocumentInstance> results = query.getResultList();
        }
    }

    protected DocumentInstance retrieve(
        HibernateStorageImpl impl, Document document, DocumentInstance documentInstance) {
        return impl.find(document, null, null,
                documentInstance.getId());
    }

    protected void store(HibernateStorageImpl impl,
                         DocumentInstance documentInstance) {
        impl.store(documentInstance);
    }

}