
    @${helper.importType("jakarta.persistence.PersistenceContext")} private ${helper.importType("jakarta.persistence.EntityManager")} entityManager;

    public void persist(${declarationName} transientInstance) {
        logger.log(${helper.importType("java.util.logging.Level")}.INFO, "persisting ${declarationName} instance");
        try {
            entityManager.persist(transientInstance);
            logger.log(${helper.importType("java.util.logging.Level")}.INFO, "persist successful");
        }
        catch (RuntimeException re) {
            logger.log(${helper.importType("java.util.logging.Level")}.SEVERE, "persist failed", re);
            throw re;
        }
    }

    public void remove(${declarationName} persistentInstance) {
        logger.log(${helper.importType("java.util.logging.Level")}.INFO, "removing ${declarationName} instance");
        try {
            entityManager.remove(persistentInstance);
            logger.log(${helper.importType("java.util.logging.Level")}.INFO, "remove successful");
        }
        catch (RuntimeException re) {
            logger.log(${helper.importType("java.util.logging.Level")}.SEVERE, "remove failed", re);
            throw re;
        }
    }

    public ${declarationName} merge(${declarationName} detachedInstance) {
        logger.log(${helper.importType("java.util.logging.Level")}.INFO, "merging ${declarationName} instance");
        try {
            ${declarationName} result = entityManager.merge(detachedInstance);
            logger.log(${helper.importType("java.util.logging.Level")}.INFO, "merge successful");
            return result;
        }
        catch (RuntimeException re) {
            logger.log(${helper.importType("java.util.logging.Level")}.SEVERE, "merge failed", re);
            throw re;
        }
    }

<#if helper.hasIdentifier()>
    public ${declarationName} findById( ${helper.getIdTypeName()} id) {
        logger.log(${helper.importType("java.util.logging.Level")}.INFO, "getting ${declarationName} instance with id: " + id);
        try {
            ${declarationName} instance = entityManager.find(${helper.getDeclarationName()}.class, id);
            logger.log(${helper.importType("java.util.logging.Level")}.INFO, "get successful");
            return instance;
        }
        catch (RuntimeException re) {
            logger.log(${helper.importType("java.util.logging.Level")}.SEVERE, "get failed", re);
            throw re;
        }
    }
</#if>

    public void persistAll(${helper.importType("java.util.List")}<${declarationName}> entities, int batchSize) {
        logger.log(${helper.importType("java.util.logging.Level")}.INFO, "batch persisting " + entities.size() + " ${declarationName} instances");
        for (int i = 0; i < entities.size(); i++) {
            entityManager.persist(entities.get(i));
            if (i > 0 && i % batchSize == 0) {
                entityManager.flush();
                entityManager.clear();
            }
        }
    }

    public ${helper.importType("java.util.List")}<${declarationName}> mergeAll(${helper.importType("java.util.List")}<${declarationName}> entities, int batchSize) {
        logger.log(${helper.importType("java.util.logging.Level")}.INFO, "batch merging " + entities.size() + " ${declarationName} instances");
        ${helper.importType("java.util.List")}<${declarationName}> result = new ${helper.importType("java.util.ArrayList")}<>();
        for (int i = 0; i < entities.size(); i++) {
            result.add(entityManager.merge(entities.get(i)));
            if (i > 0 && i % batchSize == 0) {
                entityManager.flush();
                entityManager.clear();
            }
        }
        return result;
    }

    public void removeAll(${helper.importType("java.util.List")}<${declarationName}> entities, int batchSize) {
        logger.log(${helper.importType("java.util.logging.Level")}.INFO, "batch removing " + entities.size() + " ${declarationName} instances");
        for (int i = 0; i < entities.size(); i++) {
            entityManager.remove(entityManager.contains(entities.get(i)) ? entities.get(i) : entityManager.merge(entities.get(i)));
            if (i > 0 && i % batchSize == 0) {
                entityManager.flush();
                entityManager.clear();
            }
        }
    }

    public ${helper.importType("java.util.List")}<${declarationName}> findAll(int firstResult, int maxResults) {
        logger.log(${helper.importType("java.util.logging.Level")}.INFO, "finding ${declarationName} instances with pagination");
        try {
            ${helper.importType("jakarta.persistence.criteria.CriteriaBuilder")} criteriaBuilder = entityManager.getCriteriaBuilder();
            ${helper.importType("jakarta.persistence.criteria.CriteriaQuery")}<${declarationName}> criteriaQuery = criteriaBuilder.createQuery(${declarationName}.class);
            criteriaQuery.from(${declarationName}.class);
            return entityManager.createQuery(criteriaQuery)
                    .setFirstResult(firstResult)
                    .setMaxResults(maxResults)
                    .getResultList();
        }
        catch (RuntimeException re) {
            logger.log(${helper.importType("java.util.logging.Level")}.SEVERE, "find all failed", re);
            throw re;
        }
    }
