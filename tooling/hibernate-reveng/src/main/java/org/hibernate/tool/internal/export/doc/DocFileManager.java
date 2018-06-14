package org.hibernate.tool.internal.export.doc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.hibernate.mapping.Table;
import org.hibernate.tool.internal.export.pojo.POJOClass;

/**
 * Class used to manage the files created during the documentation generation
 * process. This manager is needed to manage references between files.
 * 
 * @author Ricardo C. Moral
 * @author <a href="mailto:abhayani@jboss.org">Amit Bhayani</a>
 */
public class DocFileManager {
	
    /**
     * Root Documentation Folder.
     */
    private DocFolder rootDocFolder;

    /**
     * The main index file for the documentation.
     */
    private DocFile mainIndexDocFile;

    /**
     * Folder for the utility files.
     */
    private DocFolder assetsDocFolder;

    /**
     * The Hibernate image.
     */
    private DocFile hibernateImageDocFile;
    
    /**
     * The extends image.
     */
    private DocFile extendsImageDocFile;

    /**
     * The CSS stylesheet file.
     */
    private DocFile cssStylesDocFile;

    /**
     * Root Folder for the Table documentation.
     */
    private DocFolder rootTablesDocFolder;
    
    /**
     * Root Folder for Class doccumentation
     */
    private DocFolder rootEntitiesDocFolder;
    
    /**
     * Class index DocFile
     */
    private DocFile classIndexDocFile;
    
    /**
     * Class Summary DocFile
     */
    private DocFile entitySummaryDocFile;
    
    /**
     * All packages DocFile allpackages.html
     */
    private DocFile allPackagesDocFile;
    
    /**
     * All classes DocFile allclases.html
     */
    private DocFile allEntitiesDocFile;

    /**
     * Table index DocFile.
     */
    private DocFile tableIndexDocFile;

    /**
     * Table summary DocFile.
     */
    private DocFile tableSummaryDocFile;

    /**
     * All Schemas DocFile.
     */
    private DocFile allSchemasDocFile;

    /**
     * All Tables DocFile.
     */
    private DocFile allTablesDocFile;

    /**
     * Map with the doc files for the tables. The keys are the Table objects and
     * the values are the DocFile instances.
     */
    private Map<Table, DocFile> tableDocFiles = new HashMap<Table, DocFile>();
    
    /**
     * Map with the DocFile for classes. The keys are the POJOClass objects and 
     * the values are the DocFile instances.
     */
    private Map<POJOClass, DocFile> entityDocFiles = new HashMap<POJOClass, DocFile>();

    /**
     * Map with the schema summary DocFiles keyed by Schema FQN.
     */
    private Map<String, DocFile> schemaSummaryDocFiles = new HashMap<String, DocFile>();
    
    /**
     * Map with the package summary DocFiles keyed by package name
     */
    private Map<String, DocFile> packageSummaryDocFiles = new HashMap<String, DocFile>();

    /**
     * Map with the schema table lists DocFiles keyed by Schema FQN.
     */
    private Map<String, DocFile> schemaTableListDocFiles = new HashMap<String, DocFile>();
    
    /**
     * Map with package class lists DocFiles keyed by package name
     */
    private Map<String, DocFile> packageEntityListDocFile = new HashMap<String, DocFile>();

    public DocFolder getRootDocFolder() {
		return rootDocFolder;
	}
    
    /**
     * Constructor.
     * 
     * @param docHelper the doc helper.
     * @param pRootFolder the root folder for the documentation.
     */
    public DocFileManager(DocHelper docHelper, File pRootFolder) {

        super();        

        rootDocFolder = new DocFolder(pRootFolder);

        mainIndexDocFile = new DocFile("index.html", rootDocFolder);

        assetsDocFolder = new DocFolder("assets", rootDocFolder);

        hibernateImageDocFile = new DocFile("hibernate_logo.gif",
                assetsDocFolder);
        
        extendsImageDocFile = new DocFile("inherit.gif", assetsDocFolder);

        cssStylesDocFile = new DocFile("doc-style.css", assetsDocFolder);
        
        rootEntitiesDocFolder = new DocFolder("entities", rootDocFolder);
        classIndexDocFile = new DocFile("index.html", rootEntitiesDocFolder);
        entitySummaryDocFile = new DocFile("summary.html", rootEntitiesDocFolder);
        allPackagesDocFile = new DocFile("allpackages.html", rootEntitiesDocFolder);
        allEntitiesDocFile = new DocFile("allentities.html", rootEntitiesDocFolder);
        

        rootTablesDocFolder = new DocFolder("tables", rootDocFolder);

        tableIndexDocFile = new DocFile("index.html", rootTablesDocFolder);

        tableSummaryDocFile = new DocFile("summary.html", rootTablesDocFolder);

        allSchemasDocFile = new DocFile("allschemas.html", rootTablesDocFolder);

        allTablesDocFile = new DocFile("alltables.html", rootTablesDocFolder);

        Map<String, DocFolder> schemaFolders = new HashMap<String, DocFolder>();
        
        Iterator<String> packages = docHelper.getPackages().iterator();

        while(packages.hasNext()){
        	String packageName = packages.next();
        	DocFolder packageFolder = null;
        	DocFolder theRoot = rootEntitiesDocFolder;
        	if(!packageName.equals(DocHelper.DEFAULT_NO_PACKAGE)){
        		String[] packagesArr = packageName.split("\\.");
        		
        		for(int count = 0 ; count < packagesArr.length ; count++){
        			packageFolder = new DocFolder(packagesArr[count], theRoot);
        			theRoot = packageFolder;
        		}
                
            	DocFile packageSummaryDocFile = new DocFile("summary.html", packageFolder);
            	packageSummaryDocFiles.put(packageName, packageSummaryDocFile);
            	
            	DocFile classListDocFile = new DocFile("entities.html", packageFolder);
            	packageEntityListDocFile.put(packageName, classListDocFile);            	
        	}
        	else{
        		packageFolder = rootEntitiesDocFolder;
        	}
        	
        	Iterator<POJOClass> classes = docHelper.getClasses(packageName).iterator();
        	while(classes.hasNext()){
        		POJOClass pc = classes.next();
        		String classFileName = pc.getDeclarationName();        		
        		classFileName = classFileName + ".html";
        		DocFile classDocFile = new DocFile(classFileName, packageFolder);        		
        		entityDocFiles.put(pc, classDocFile);
        	}        	
        }

        Iterator<String> schemas = docHelper.getSchemas().iterator();
        while (schemas.hasNext() ) {
            String schemaName = schemas.next();
            DocFolder schemaFolder = new DocFolder(schemaName,
                    rootTablesDocFolder);
            schemaFolders.put(schemaName, schemaFolder);
            DocFile schemaSummaryDocFile = new DocFile("summary.html",
                    schemaFolder);
            schemaSummaryDocFiles.put(schemaName, schemaSummaryDocFile);
            DocFile tableListDocFile = new DocFile("tables.html", schemaFolder);
            schemaTableListDocFiles.put(schemaName, tableListDocFile);

            Iterator<Table> tables = docHelper.getTables(schemaName).iterator();

            while (tables.hasNext() ) {
                Table table = (Table) tables.next();
                if(table.isPhysicalTable()) { 
                	String tableFileName = table.getName() + ".html";

                	DocFile tableDocFile = new DocFile(tableFileName, schemaFolder);

                	tableDocFiles.put(table, tableDocFile);
                }
            }
        }
    }

    /**
     * Returns the DocFolder for the helper files.
     * 
     * @return the value.
     */
    public DocFolder getAssetsDocFolder() {

        return assetsDocFolder;
    }

    /**
     * Returns the DocFile for the CSS definitions.
     * 
     * @return the value.
     */
    public DocFile getCssStylesDocFile() {

        return cssStylesDocFile;
    }

    /**
     * Returns the DocFile for the Hibernate Image.
     * 
     * @return the value.
     */
    public DocFile getHibernateImageDocFile() {

        return hibernateImageDocFile;
    }
    
    /**
     * Returns the DocFile for the extends Image.
     * 
     * @return the value.
     */
    public DocFile getExtendsImageDocFile() {

        return extendsImageDocFile;
    }

    /**
     * Returns the DocFile for the main index.
     * 
     * @return the value.
     */
    public DocFile getMainIndexDocFile() {

        return mainIndexDocFile;
    }

    /**
     * Return the table index DocFile.
     * 
     * @return the table index DocFile.
     */
    public DocFile getTableIndexDocFile() {
        return tableIndexDocFile;
    }
    
    /**
     * Returns the class index DocFile
     * @return class index DocFile
     */
    public DocFile getClassIndexDocFile(){
    	return classIndexDocFile;
    }
    
    /**
     * Returns the summary index DocFile
     * @return summary index DocFile
     */
    public DocFile getClassSummaryFile(){
    	return entitySummaryDocFile;
    }
    
    /**
     * Returns the DocFile responsible for generating allpackages.html
     * @return DocFile
     */
    public DocFile getAllPackagesDocFile(){
    	return allPackagesDocFile;
    }
    
    /**
     * Returns the DocFile responsible for generating allclasses.html
     * @return
     */
    public DocFile getAllEntitiesDocFile(){
    	return allEntitiesDocFile;
    }
    
    /**
     * Returns the DocFile responsible to generate classes.html corresponding to packageName passed 
     * @param packageName Package name which acts as key to get DocFile value object from packageEntityListDocFile
     * @return DocFile for classes.html
     */
    public DocFile getPackageEntityListDocFile(String packageName){
    	return packageEntityListDocFile.get(packageName);
    	
    }

    /**
     * Return the table summary DocFile.
     * 
     * @return the table summary DocFile.
     */
    public DocFile getTableSummaryDocFile() {
        return tableSummaryDocFile;
    }

    /**
     * Return the all schemas DocFile.
     * 
     * @return the all schemas DocFile.
     */
    public DocFile getAllSchemasDocFile() {
        return allSchemasDocFile;
    }

    /**
     * Return the all tables DocFile.
     * 
     * @return the all tables DocFile.
     */
    public DocFile getAllTablesDocFile() {
        return allTablesDocFile;
    }

    /**
     * Return the DocFile for the specified Table.
     * 
     * @param table the Table.
     * 
     * @return the DocFile.
     */
    public DocFile getTableDocFile(Table table) {
        return tableDocFiles.get(table);
    }
    
    
    /**
     * Get the DocFile corresponding to POJOClass. But if the POJOClass is ComponentPOJO, it is created on fly
     * and we are not implementing .equals method hence get by getQualifiedDeclarationName.
     * @param pc DocFile corresponding to this POJOClass
     * @return DocFile
     */
    public DocFile getEntityDocFileByDeclarationName(POJOClass pc){
    	DocFile df = getEntityDocFile(pc);
    	String pcQualifiedDeclarationName = pc.getQualifiedDeclarationName();
    	String pojoClassQualifiedDeclarationName ;
    	//TODO Can we implement equals method in BasicPOJO to avoid this loop?
    	if(df == null){
    		Iterator<POJOClass> itr = entityDocFiles.keySet().iterator();
    		while(itr.hasNext()){
    			POJOClass pojoClass = itr.next();
    			pojoClassQualifiedDeclarationName = pojoClass.getQualifiedDeclarationName();
    			
    			if( pcQualifiedDeclarationName.equals(pojoClassQualifiedDeclarationName )){
    				df = entityDocFiles.get(pojoClass);
    				break;
    			}
    		}
    			
    		
    		
    	}
    	return df;
    }    
    
    /**
     * Returns the DocFile responsible to generate the .html for each classes.
     * @param pc The DocFile corresponding to this pc is retrieved from  entityDocFiles
     * @return DocFile
     */
    public DocFile getEntityDocFile(POJOClass pc){
    	return entityDocFiles.get(pc);
    }

    /**
     * Return the summary DocFile for the specified schema FQN.
     * 
     * @param schemaName the name of the schema.
     * 
     * @return the DocFile.
     */
    public DocFile getSchemaSummaryDocFile(String schemaName) {
        return schemaSummaryDocFiles.get(schemaName);
    }
    
    /**
     * get DocFile responsible to generate summary.html for corresponding packageName passed
     * @param packageName DocFile corresponding to this packagename is retrieved from packageSummaryDocFiles
     * @return DocFile
     */
    public DocFile getPackageSummaryDocFile(String packageName){
    	return packageSummaryDocFiles.get(packageName);
    }

    /**
     * Return the Table List DocFile for the specified schema FQN.
     * 
     * @param schemaName the name of the schema.
     * 
     * @return the DocFile.
     */
    public DocFile getSchemaTableListDocFile(String schemaName) {
        return schemaTableListDocFiles.get(schemaName);
    }

    /**
     * Return the relative reference between the specified files.
     * 
     * @param from the origin.
     * @param to the target.
     * 
     * @throws IllegalArgumentException if any parameter is null.
     */
    public String getRef(DocFile from, DocFile to) {
        if (from == null) {
            throw new IllegalArgumentException("From cannot be null.");
        }

        if (to == null) {
            throw new IllegalArgumentException("To cannot be null.");
        }

        return from.buildRefTo(to);
    }

    /**
     * Copy a File.
     * 
     * TODO: this method ignores custom provided templatepath. Want to call freemarker to get the resourceloaders but they are hidden, so we need another way.
     * ..and if we use currentthread classloader you might conflict with the projects tools.jar
     * 
     * @param fileName the name of the file to copy.
     * @param to the target file.
     * 
     * @throws IOException in case of error.
     */
    public static void copy(ClassLoader loader, String fileName, File to) throws IOException {
        InputStream is = null;
        FileOutputStream out = null;
        try {
            /*ClassLoader classLoader = Thread.currentThread().getContextClassLoader(); 
            if (classLoader == null) {
                classLoader = DocFileManager.class.getClassLoader();			
            }
			
			is = classLoader.getResourceAsStream(fileName);*/
        	
            /*if (is == null && classLoader!=DocFileManager.class.getClassLoader() ) {
				is = DocFileManager.class.getClassLoader().getResourceAsStream(fileName); // HACK: workaround since eclipse for some reason doesnt provide the right classloader;
				
            } */
        	is = loader.getResourceAsStream( fileName );
			
			if(is==null) {
                throw new IllegalArgumentException("File not found: "
                        + fileName);
            }
            
            out = new FileOutputStream(to);

            int value;
            while ( (value = is.read() ) != -1) {
                out.write(value);
            }

        } 
        finally {
            if (is != null) {
                is.close();
            }
            if (out != null) {
                out.close();
            }
        }
    }

}