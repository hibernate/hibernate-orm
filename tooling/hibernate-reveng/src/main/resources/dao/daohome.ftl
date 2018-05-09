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

    private static final ${pojo.importType("org.apache.commons.logging.Log")} log = ${pojo.importType("org.apache.commons.logging.LogFactory")}.getLog(${pojo.getDeclarationName()}Home.class);

<#if ejb3>
    @${pojo.importType("javax.persistence.PersistenceContext")} private ${pojo.importType("javax.persistence.EntityManager")} entityManager;
    
    public void persist(${declarationName} transientInstance) {
        log.debug("persisting ${declarationName} instance");
        try {
            entityManager.persist(transientInstance);
            log.debug("persist successful");
        }
        catch (RuntimeException re) {
            log.error("persist failed", re);
            throw re;
        }
    }
    
    public void remove(${declarationName} persistentInstance) {
        log.debug("removing ${declarationName} instance");
        try {
            entityManager.remove(persistentInstance);
            log.debug("remove successful");
        }
        catch (RuntimeException re) {
            log.error("remove failed", re);
            throw re;
        }
    }
    
    public ${declarationName} merge(${declarationName} detachedInstance) {
        log.debug("merging ${declarationName} instance");
        try {
            ${declarationName} result = entityManager.merge(detachedInstance);
            log.debug("merge successful");
            return result;
        }
        catch (RuntimeException re) {
            log.error("merge failed", re);
            throw re;
        }
    }
    
<#if clazz.identifierProperty?has_content>    
    public ${declarationName} findById( ${pojo.getJavaTypeName(clazz.identifierProperty, jdk5)} id) {
        log.debug("getting ${declarationName} instance with id: " + id);
        try {
            ${declarationName} instance = entityManager.find(${pojo.getDeclarationName()}.class, id);
            log.debug("get successful");
            return instance;
        }
        catch (RuntimeException re) {
            log.error("get failed", re);
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
            log.error("Could not locate SessionFactory in JNDI", e);
            throw new IllegalStateException("Could not locate SessionFactory in JNDI");
        }
    }
    
    public void persist(${declarationName} transientInstance) {
        log.debug("persisting ${declarationName} instance");
        try {
            sessionFactory.getCurrentSession().persist(transientInstance);
            log.debug("persist successful");
        }
        catch (RuntimeException re) {
            log.error("persist failed", re);
            throw re;
        }
    }
    
    public void attachDirty(${declarationName} instance) {
        log.debug("attaching dirty ${declarationName} instance");
        try {
            sessionFactory.getCurrentSession().saveOrUpdate(instance);
            log.debug("attach successful");
        }
        catch (RuntimeException re) {
            log.error("attach failed", re);
            throw re;
        }
    }
    
    public void attachClean(${declarationName} instance) {
        log.debug("attaching clean ${declarationName} instance");
        try {
            sessionFactory.getCurrentSession().lock(instance, ${pojo.importType("org.hibernate.LockMode")}.NONE);
            log.debug("attach successful");
        }
        catch (RuntimeException re) {
            log.error("attach failed", re);
            throw re;
        }
    }
    
    public void delete(${declarationName} persistentInstance) {
        log.debug("deleting ${declarationName} instance");
        try {
            sessionFactory.getCurrentSession().delete(persistentInstance);
            log.debug("delete successful");
        }
        catch (RuntimeException re) {
            log.error("delete failed", re);
            throw re;
        }
    }
    
    public ${declarationName} merge(${declarationName} detachedInstance) {
        log.debug("merging ${declarationName} instance");
        try {
            ${declarationName} result = (${declarationName}) sessionFactory.getCurrentSession()
                    .merge(detachedInstance);
            log.debug("merge successful");
            return result;
        }
        catch (RuntimeException re) {
            log.error("merge failed", re);
            throw re;
        }
    }
    
<#if clazz.identifierProperty?has_content>
    public ${declarationName} findById( ${c2j.getJavaTypeName(clazz.identifierProperty, jdk5)} id) {
        log.debug("getting ${declarationName} instance with id: " + id);
        try {
            ${declarationName} instance = (${declarationName}) sessionFactory.getCurrentSession()
                    .get("${clazz.entityName}", id);
            if (instance==null) {
                log.debug("get successful, no instance found");
            }
            else {
                log.debug("get successful, instance found");
            }
            return instance;
        }
        catch (RuntimeException re) {
            log.error("get failed", re);
            throw re;
        }
    }
</#if>
    
<#if clazz.hasNaturalId()>
    public ${declarationName} findByNaturalId(${c2j.asNaturalIdParameterList(clazz)}) {
        log.debug("getting ${declarationName} instance by natural id");
        try {
            ${declarationName} instance = (${declarationName}) sessionFactory.getCurrentSession()
                    .createCriteria("${clazz.entityName}")
<#if jdk5>
                    .add( ${pojo.staticImport("org.hibernate.criterion.Restrictions", "naturalId")}()
<#else>
                   .add( ${pojo.importType("org.hibernate.criterion.Restrictions")}.naturalId()
</#if>                    
<#foreach property in pojo.getAllPropertiesIterator()>
<#if property.isNaturalIdentifier()>
                            .set("${property.name}", ${property.name})
</#if>
</#foreach>
                        )
                    .uniqueResult();
            if (instance==null) {
                log.debug("get successful, no instance found");
            }
            else {
                log.debug("get successful, instance found");
            }
            return instance;
        }
        catch (RuntimeException re) {
            log.error("query failed", re);
            throw re;
        }
    }
</#if>    
<#if jdk5>
    public ${pojo.importType("java.util.List")}<${declarationName}> findByExample(${declarationName} instance) {
<#else>
    public ${pojo.importType("java.util.List")} findByExample(${declarationName} instance) {
</#if>
        log.debug("finding ${declarationName} instance by example");
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
            log.debug("find by example successful, result size: " + results.size());
            return results;
        }
        catch (RuntimeException re) {
            log.error("find by example failed", re);
            throw re;
        }
    } 
<#foreach query in md.namedQueryDefinitions>
<#assign queryName = query.name>
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
        ${pojo.importType("org.hibernate.Query")} query = sessionFactory.getCurrentSession()
                .getNamedQuery("${queryName}");
<#foreach param in params.keySet()>
<#if param.equals("maxResults")>
		query.setMaxResults(maxResults);
<#elseif param.equals("firstResult")>
        query.setFirstResult(firstResult);
<#else>
        query.setParameter("${param}", ${param});
</#if>
</#foreach>
<#if jdk5 && methname.startsWith("find")>
        return (List<${declarationName}>) query.list();
<#elseif methname.startsWith("count")>
        return ( (Integer) query.uniqueResult() ).intValue();
<#else>
        return query.list();
</#if>
    }
</#if>
</#foreach></#if>
}
</#assign>

${pojo.generateImports()}
${classbody}
