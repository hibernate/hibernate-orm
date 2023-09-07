${pojo.getPackageDeclaration()}
// Generated ${date} by Hibernate Tools ${version}

<#assign classbody>
<#assign declarationName = pojo.importType(pojo.getDeclarationName())>/**
 * Home object for domain model class ${declarationName}.
 * @see ${pojo.getQualifiedDeclarationName()}
 * @author Hibernate Tools
 */
<#if ejb3>
@${pojo.importType("javax.ejb.Stateless")}
</#if>
public class ${declarationName}Home {

    private static final ${pojo.importType("java.util.logging.Logger")} logger = ${pojo.importType("Logger")}.getLogger(${pojo.getDeclarationName()}Home.class.getName());

<#if ejb3>
    @${pojo.importType("jakarta.persistence.PersistenceContext")} private ${pojo.importType("jakarta.persistence.EntityManager")} entityManager;
    
    public void persist(${declarationName} transientInstance) {
        logger.log(${pojo.importType("java.util.logging.Level")}.INFO, "persisting ${declarationName} instance");
        try {
            entityManager.persist(transientInstance);
            logger.log(${pojo.importType("java.util.logging.Level")}.INFO, "persist successful");
        }
        catch (RuntimeException re) {
            logger.log(${pojo.importType("java.util.logging.Level")}.SEVERE, "persist failed", re);
            throw re;
        }
    }
    
    public void remove(${declarationName} persistentInstance) {
        logger.log(${pojo.importType("java.util.logging.Level")}.INFO, "removing ${declarationName} instance");
        try {
            entityManager.remove(persistentInstance);
            logger.log(${pojo.importType("java.util.logging.Level")}.INFO, "remove successful");
        }
        catch (RuntimeException re) {
            logger.log(${pojo.importType("java.util.logging.Level")}.SEVERE, "remove failed", re);
            throw re;
        }
    }
    
    public ${declarationName} merge(${declarationName} detachedInstance) {
        logger.log(${pojo.importType("java.util.logging.Level")}.INFO, "merging ${declarationName} instance");
        try {
            ${declarationName} result = entityManager.merge(detachedInstance);
            logger.log(${pojo.importType("java.util.logging.Level")}.INFO, "merge successful");
            return result;
        }
        catch (RuntimeException re) {
            logger.log(${pojo.importType("java.util.logging.Level")}.SEVERE, "merge failed", re);
            throw re;
        }
    }
    
<#if clazz.identifierProperty?has_content>    
    public ${declarationName} findById( ${pojo.getJavaTypeName(clazz.identifierProperty, jdk5)} id) {
        logger.log(${pojo.importType("java.util.logging.Level")}.INFO, "getting ${declarationName} instance with id: " + id);
        try {
            ${declarationName} instance = entityManager.find(${pojo.getDeclarationName()}.class, id);
            logger.log(${pojo.importType("java.util.logging.Level")}.INFO, "get successful");
            return instance;
        }
        catch (RuntimeException re) {
            logger.log(${pojo.importType("java.util.logging.Level")}.SEVERE, "get failed", re);
            throw re;
        }
    }
</#if>
<#else>    
    private final ${pojo.importType("org.hibernate.SessionFactory")} sessionFactory = getSessionFactory();
    
    protected ${pojo.importType("org.hibernate.SessionFactory")} getSessionFactory() {
        try {
            return (${pojo.importType("org.hibernate.SessionFactory")}) new ${pojo.importType("javax.naming.InitialContext")}().lookup("${sessionFactoryName}");
        }
        catch (Exception e) {
            logger.log(${pojo.importType("java.util.logging.Level")}.SEVERE, "Could not locate SessionFactory in JNDI", e);
            throw new IllegalStateException("Could not locate SessionFactory in JNDI");
        }
    }
    
    public void persist(${declarationName} transientInstance) {
        logger.log(${pojo.importType("java.util.logging.Level")}.INFO, "persisting ${declarationName} instance");
        try {
            sessionFactory.getCurrentSession().persist(transientInstance);
            logger.log(${pojo.importType("java.util.logging.Level")}.INFO, "persist successful");
        }
        catch (RuntimeException re) {
            logger.log(${pojo.importType("java.util.logging.Level")}.SEVERE, "persist failed", re);
            throw re;
        }
    }
    
    public void attachDirty(${declarationName} instance) {
        logger.log(${pojo.importType("java.util.logging.Level")}.INFO, "attaching dirty ${declarationName} instance");
        try {
            sessionFactory.getCurrentSession().saveOrUpdate(instance);
            logger.log(${pojo.importType("java.util.logging.Level")}.INFO, "attach successful");
        }
        catch (RuntimeException re) {
            logger.log(${pojo.importType("java.util.logging.Level")}.SEVERE, "attach failed", re);
            throw re;
        }
    }
    
    public void attachClean(${declarationName} instance) {
        logger.log(${pojo.importType("java.util.logging.Level")}.INFO, "attaching clean ${declarationName} instance");
        try {
            sessionFactory.getCurrentSession().lock(instance, ${pojo.importType("org.hibernate.LockMode")}.NONE);
            logger.log(${pojo.importType("java.util.logging.Level")}.INFO, "attach successful");
        }
        catch (RuntimeException re) {
            logger.log(${pojo.importType("java.util.logging.Level")}.SEVERE, "attach failed", re);
            throw re;
        }
    }
    
    public void delete(${declarationName} persistentInstance) {
        logger.log(${pojo.importType("java.util.logging.Level")}.INFO, "deleting ${declarationName} instance");
        try {
            sessionFactory.getCurrentSession().delete(persistentInstance);
            logger.log(${pojo.importType("java.util.logging.Level")}.INFO, "delete successful");
        }
        catch (RuntimeException re) {
            logger.log(${pojo.importType("java.util.logging.Level")}.SEVERE, "delete failed", re);
            throw re;
        }
    }
    
    public ${declarationName} merge(${declarationName} detachedInstance) {
        logger.log(${pojo.importType("java.util.logging.Level")}.INFO, "merging ${declarationName} instance");
        try {
            ${declarationName} result = (${declarationName}) sessionFactory.getCurrentSession()
                    .merge(detachedInstance);
            logger.log(${pojo.importType("java.util.logging.Level")}.INFO, "merge successful");
            return result;
        }
        catch (RuntimeException re) {
            logger.log(${pojo.importType("java.util.logging.Level")}.SEVERE, "merge failed", re);
            throw re;
        }
    }
    
<#if clazz.identifierProperty?has_content>
    public ${declarationName} findById( ${c2j.getJavaTypeName(clazz.identifierProperty, jdk5)} id) {
        logger.log(${pojo.importType("java.util.logging.Level")}.INFO, "getting ${declarationName} instance with id: " + id);
        try {
            ${declarationName} instance = (${declarationName}) sessionFactory.getCurrentSession()
                    .get("${clazz.entityName}", id);
            if (instance==null) {
                logger.log(${pojo.importType("java.util.logging.Level")}.INFO, "get successful, no instance found");
            }
            else {
                logger.log(${pojo.importType("java.util.logging.Level")}.INFO, "get successful, instance found");
            }
            return instance;
        }
        catch (RuntimeException re) {
            logger.log(${pojo.importType("java.util.logging.Level")}.SEVERE, "get failed", re);
            throw re;
        }
    }
</#if>
    
<#if clazz.hasNaturalId()>
    public ${declarationName} findByNaturalId(${c2j.asNaturalIdParameterList(clazz)}) {
        logger.log(${pojo.importType("java.util.logging.Level")}.INFO, "getting ${declarationName} instance by natural id");
        try {
            ${pojo.importType("jakarta.persistence.criteria.CriteriaBuilder")} criteriaBuilder = sessionFactory.getCriteriaBuilder();
            ${pojo.importType("jakarta.persistence.criteria.CriteriaQuery")}<${declarationName}> criteriaQuery = criteriaBuilder.createQuery(${declarationName}.class);
            ${pojo.importType("jakarta.persistence.criteria.Root")}<${declarationName}> root = criteriaQuery.from(${declarationName}.class);
            criteriaQuery.where(
<#assign notFirst = false/>
<#list pojo.getAllPropertiesIterator() as property>
<#if property.isNaturalIdentifier()>
                    <#if notFirst>,</#if>criteriaBuilder.equal(root.get("${property.name}"), ${property.name})
<#assign notFirst = true/>
</#if>
</#list>
            );
            ${declarationName} instance = sessionFactory
                    .getCurrentSession()
                    .createQuery(criteriaQuery)
                    .getSingleResult();                    
            if (instance==null) {
                logger.log(${pojo.importType("java.util.logging.Level")}.INFO, "get successful, no instance found");
            }
            else {
                logger.log(${pojo.importType("java.util.logging.Level")}.INFO, "get successful, instance found");
            }
            return instance;
        }
        catch (RuntimeException re) {
            logger.log(${pojo.importType("java.util.logging.Level")}.SEVERE, "query failed", re);
            throw re;
        }
    }
</#if>  

<#if false>
/**  
TODO: 
<#if jdk5>
    public ${pojo.importType("java.util.List")}<${declarationName}> findByExample(${declarationName} instance) {
<#else>
    public ${pojo.importType("java.util.List")} findByExample(${declarationName} instance) {
</#if>
        logger.log(${pojo.importType("java.util.logging.Level")}.INFO, "finding ${declarationName} instance by example");
        try {
<#if jdk5>
            ${pojo.importType("java.util.List")}<${declarationName}> results = (List<${declarationName}>) sessionFactory.getCurrentSession()
<#else>
            ${pojo.importType("java.util.List")} results = sessionFactory.getCurrentSession()
</#if>
                    .createCriteria("${clazz.entityName}")
<#if jdk5>
                    .add( ${pojo.staticImport("org.hibernate.criterion.Example", "create")}(instance) )
<#else>
                    .add(${pojo.importType("org.hibernate.criterion.Example")}.create(instance))
</#if>
            .list();
            logger.log(${pojo.importType("java.util.logging.Level")}.INFO, "find by example successful, result size: " + results.size());
            return results;
        }
        catch (RuntimeException re) {
            logger.log(${pojo.importType("java.util.logging.Level")}.SEVERE, "find by example failed", re);
            throw re;
        }
    } 
**/ 
</#if>   
    
<#list daoHelper.getNamedHqlQueryDefinitions(md) as query>
<#assign queryName = query.registrationName>
<#if queryName.startsWith(clazz.entityName + ".")>
<#assign methname = c2j.unqualify(queryName)>
<#assign params = c2j.getParameterTypes(query)>
<#assign argList = c2j.asFinderArgumentList(params, pojo)>
<#if jdk5 && methname.startsWith("find")>
    public ${pojo.importType("java.util.List")}<${declarationName}> ${methname}(${argList}) {
<#elseif methname.startsWith("count")>
    public int ${methname}(${argList}) {
<#else>
    public ${pojo.importType("java.util.List")} ${methname}(${argList}) {
</#if>
        ${pojo.importType("org.hibernate.query.Query")} query = sessionFactory.getCurrentSession()
                .getNamedQuery("${queryName}");
<#if jdk5 && methname.startsWith("find")>
        return (List<${declarationName}>) query.list();
<#elseif methname.startsWith("count")>
        return ( (Integer) query.uniqueResult() ).intValue();
<#else>
        return query.list();
</#if>
    }
</#if>
</#list></#if>
}
</#assign>

${pojo.generateImports()}
${classbody}
