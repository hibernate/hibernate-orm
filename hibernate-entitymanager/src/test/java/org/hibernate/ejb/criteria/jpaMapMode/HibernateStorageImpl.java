package org.hibernate.ejb.criteria.jpaMapMode;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.EntityType;

/**
 *  Use JPA and DocumentManagerFactory instead.
 */
public class HibernateStorageImpl {

    private SessionFactory sessionFactory;

    public DocumentInstance find(Document document, List<Property> properties,
                                 Map<Relationship, List<Property>> relationships, UUID id) {

        Session session = getSessionFactory().getCurrentSession();
        Transaction transaction = session.beginTransaction();

        try {
            DocumentInstance instance = (DocumentInstance) session.get(
                    document.getName(), id);
            return instance;
        } finally {
            transaction.commit();
        }
    }

    public DocumentInstance find(Document document, String id) {
        CriteriaBuilder criteriaBuilder = null;
        CriteriaQuery<Object> criteriaQuery = criteriaBuilder.createQuery();
        EntityType<Object> entity = null;
        Root<Object> root = criteriaQuery.from(entity);
        root.join("name");
        return null;
    }

    public DocumentInstance store(DocumentInstance instance) {
        Session session = getSessionFactory().getCurrentSession();
        Transaction transaction = session.beginTransaction();

        try {
            session.saveOrUpdate(instance);
            session.flush();
            return instance;
        } finally {
            transaction.commit();
        }
    }

    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

}
