<#--
  ~ Copyright 2004 - 2025 Red Hat, Inc.
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~     http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" basis,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  -->
<#assign classbody>
<#assign declarationName = helper.importType(helper.getQualifiedDeclarationName())>/**
 * Home object for domain model class ${declarationName}.
 * @see ${helper.getQualifiedDeclarationName()}
 * @author Hibernate Tools
 */
<#if helper.isEjb3()>
@${helper.importType("jakarta.ejb.Stateless")}
</#if>
public class ${declarationName}Home {

    private static final ${helper.importType("java.util.logging.Logger")} logger = ${helper.importType("java.util.logging.Logger")}.getLogger(${helper.getDeclarationName()}Home.class.getName());

<#if helper.isEjb3()>
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
<#else>
    private final ${helper.importType("org.hibernate.SessionFactory")} sessionFactory = getSessionFactory();

    protected ${helper.importType("org.hibernate.SessionFactory")} getSessionFactory() {
        try {
            return (${helper.importType("org.hibernate.SessionFactory")}) new ${helper.importType("javax.naming.InitialContext")}().lookup("${helper.getSessionFactoryName()}");
        }
        catch (Exception e) {
            logger.log(${helper.importType("java.util.logging.Level")}.SEVERE, "Could not locate SessionFactory in JNDI", e);
            throw new IllegalStateException("Could not locate SessionFactory in JNDI");
        }
    }

    public void persist(${declarationName} transientInstance) {
        logger.log(${helper.importType("java.util.logging.Level")}.INFO, "persisting ${declarationName} instance");
        try {
            sessionFactory.getCurrentSession().persist(transientInstance);
            logger.log(${helper.importType("java.util.logging.Level")}.INFO, "persist successful");
        }
        catch (RuntimeException re) {
            logger.log(${helper.importType("java.util.logging.Level")}.SEVERE, "persist failed", re);
            throw re;
        }
    }

    public void attachDirty(${declarationName} instance) {
        logger.log(${helper.importType("java.util.logging.Level")}.INFO, "attaching dirty ${declarationName} instance");
        try {
            sessionFactory.getCurrentSession().merge(instance);
            logger.log(${helper.importType("java.util.logging.Level")}.INFO, "attach successful");
        }
        catch (RuntimeException re) {
            logger.log(${helper.importType("java.util.logging.Level")}.SEVERE, "attach failed", re);
            throw re;
        }
    }

    public void attachClean(${declarationName} instance) {
        logger.log(${helper.importType("java.util.logging.Level")}.INFO, "attaching clean ${declarationName} instance");
        try {
            sessionFactory.getCurrentSession().lock(instance, ${helper.importType("org.hibernate.LockMode")}.NONE);
            logger.log(${helper.importType("java.util.logging.Level")}.INFO, "attach successful");
        }
        catch (RuntimeException re) {
            logger.log(${helper.importType("java.util.logging.Level")}.SEVERE, "attach failed", re);
            throw re;
        }
    }

    public void remove(${declarationName} persistentInstance) {
        logger.log(${helper.importType("java.util.logging.Level")}.INFO, "removing ${declarationName} instance");
        try {
            sessionFactory.getCurrentSession().remove(persistentInstance);
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
            ${declarationName} result = (${declarationName}) sessionFactory.getCurrentSession()
                    .merge(detachedInstance);
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
            ${declarationName} instance = (${declarationName}) sessionFactory.getCurrentSession()
                    .get("${helper.getEntityName()}", id);
            if (instance==null) {
                logger.log(${helper.importType("java.util.logging.Level")}.INFO, "get successful, no instance found");
            }
            else {
                logger.log(${helper.importType("java.util.logging.Level")}.INFO, "get successful, instance found");
            }
            return instance;
        }
        catch (RuntimeException re) {
            logger.log(${helper.importType("java.util.logging.Level")}.SEVERE, "get failed", re);
            throw re;
        }
    }
</#if>

<#if helper.hasNaturalId()>
    public ${declarationName} findByNaturalId(${helper.getNaturalIdParameterList()}) {
        logger.log(${helper.importType("java.util.logging.Level")}.INFO, "getting ${declarationName} instance by natural id");
        try {
            ${helper.importType("jakarta.persistence.criteria.CriteriaBuilder")} criteriaBuilder = sessionFactory.getCriteriaBuilder();
            ${helper.importType("jakarta.persistence.criteria.CriteriaQuery")}<${declarationName}> criteriaQuery = criteriaBuilder.createQuery(${declarationName}.class);
            ${helper.importType("jakarta.persistence.criteria.Root")}<${declarationName}> root = criteriaQuery.from(${declarationName}.class);
            criteriaQuery.where(
<#assign notFirst = false/>
<#list helper.getNaturalIdFields() as field>
                    <#if notFirst>,</#if>criteriaBuilder.equal(root.get("${field.getName()}"), ${field.getName()})
<#assign notFirst = true/>
</#list>
            );
            ${declarationName} instance = sessionFactory
                    .getCurrentSession()
                    .createQuery(criteriaQuery)
                    .getSingleResult();
            if (instance==null) {
                logger.log(${helper.importType("java.util.logging.Level")}.INFO, "get successful, no instance found");
            }
            else {
                logger.log(${helper.importType("java.util.logging.Level")}.INFO, "get successful, instance found");
            }
            return instance;
        }
        catch (RuntimeException re) {
            logger.log(${helper.importType("java.util.logging.Level")}.SEVERE, "query failed", re);
            throw re;
        }
    }
</#if>

<#list helper.getEntityNamedQueries() as query>
<#assign methname = helper.unqualify(query.name())>
<#assign paramList = helper.getQueryParameterList(query)>
<#assign paramNames = helper.getQueryParameterNames(query)>
<#if methname?starts_with("find")>
    public ${helper.importType("java.util.List")}<${declarationName}> ${methname}(${paramList}) {
<#elseif methname?starts_with("count")>
    public int ${methname}(${paramList}) {
<#else>
    public ${helper.importType("java.util.List")} ${methname}(${paramList}) {
</#if>
        ${helper.importType("org.hibernate.query.Query")} query = sessionFactory.getCurrentSession()
                .createNamedQuery("${query.name()}");
<#list paramNames as param>
        query.setParameter("${param}", ${param});
</#list>
<#if methname?starts_with("find")>
        return (List<${declarationName}>) query.list();
<#elseif methname?starts_with("count")>
        return ( (Integer) query.uniqueResult() ).intValue();
<#else>
        return query.list();
</#if>
    }
</#list></#if>
}
</#assign>

${helper.getPackageDeclaration()}
// Generated ${date?datetime} by Hibernate Tools ${version}

${helper.generateImports()}
${classbody}
