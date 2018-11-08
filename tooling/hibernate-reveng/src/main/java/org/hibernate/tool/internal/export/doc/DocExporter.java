package org.hibernate.tool.internal.export.doc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.boot.Metadata;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Table;
import org.hibernate.tool.internal.export.common.AbstractExporter;
import org.hibernate.tool.internal.export.common.GenericExporter;
import org.hibernate.tool.internal.export.common.TemplateProducer;
import org.hibernate.tool.internal.export.pojo.POJOClass;

/**
 * Exporter implementation that creates Hibernate Documentation.
 * Creates Tables and Classes Details
 * 
 * @author Ricardo C. Moral
 * @author <a href="mailto:abhayani@jboss.org">Amit Bhayani</a>
 * 
 */
public class DocExporter extends AbstractExporter {

    /**
     * CSS Definition.
     */
    private static final String FILE_CSS_DEFINITION = "doc/doc-style.css";

    /**
     * Hibernate Image.
     */
    private static final String FILE_HIBERNATE_IMAGE = "doc/hibernate_logo.gif";

    /**
     * Extends Image.
     */
    private static final String FILE_EXTENDS_IMAGE = "doc/inherit.gif";

    /**
     * Main index page.
     */
    private static final String FILE_INDEX = "doc/index.html";
    
    /**
     * Template used for the index of the table documentation.
     */
    private static final String FTL_TABLES_INDEX = "doc/tables/index.ftl";
    
    /**
     * Template used for index of the entity documentation
     */
    private static final String FTL_ENTITIES_INDEX = "doc/entities/index.ftl";
    
    /**
     * Template used for the Classes Summary
     */
    private static final String FTL_ENTITIES_SUMMARY = "doc/entities/summary.ftl";
    
    /**
     * Template used for Class details
     */
    private static final String FTL_ENTITIES_ENTITY = "doc/entities/entity.ftl";
    
    /**
     * Template used to create the Package List
     */
    private static final String FTL_ENTITIES_PACKAGE_LIST = "doc/entities/package-list.ftl";
    
    /**
     * Template used to create the list of all Classes
     */
    private static final String FTL_ENTITIES_ENTITY_LIST = "doc/entities/allEntity-list.ftl";
    
    /**
     * Template used to create List of Classes specific to packages.
     */
    private static final String FTL_ENTITIES_PERPACKAGE_ENTITY_LIST = "doc/entities/perPackageEntity-list.ftl";
    
    /**
     * Template used to show the specific package details
     */
    private static final String FTL_ENTITIES_PACKAGE_SUMMARY = "doc/entities/package-summary.ftl";

    /**
     * Template used for the Tables Summary.
     */
    private static final String FTL_TABLES_SUMMARY = "doc/tables/summary.ftl";

    /**
     * Template used for table lists.
     */
    private static final String FTL_TABLES_TABLE_LIST = "doc/tables/table-list.ftl";

    /**
     * Template used for table lists for a specific schema.
     */
    private static final String FTL_TABLES_PERSCHEMA_TABLE_LIST = "doc/tables/schema-table-list.ftl";

    /**
     * Template used for schema lists.
     */
    private static final String FTL_TABLES_SCHEMA_LIST = "doc/tables/schema-list.ftl";

    /**
     * Template used for Schema Summary.
     */
    private static final String FTL_TABLES_SCHEMA_SUMMARY = "doc/tables/schema-summary.ftl";

    /**
     * Template used for the Table Details.
     */
    private static final String FTL_TABLES_TABLE = "doc/tables/table.ftl";

    /**
     * Doc helper.
     */
    private DocHelper docHelper;

    /**
     * Doc File Manager.
     */
    private DocFileManager docFileManager;
    
	public void doStart() {
        generateCommmonAndAssets();
        
        boolean graphsGenerated = generateDot();
        generateTablesIndex();
        generateTablesSummary(graphsGenerated);
        generateTablesDetails();
        generateTablesAllSchemasList();
        generateTablesAllTablesList();
        generateTablesSchemaTableList();
        generateTablesSchemaDetailedInfo();
        
        generateEntitiesIndex();
        generatePackageSummary(graphsGenerated);
        generateEntitiesDetails();
        generateEntitiesAllPackagesList();
        generateEntitiesAllEntitiesList();
        generateEntitiesPackageEntityList();
        generateEntitiesPackageDetailedInfo();
        
        
    }

	private boolean generateDot() {
		String cmd = getProperties().getProperty( "dot.executable" );
		boolean ignoreError = Boolean.parseBoolean(getProperties().getProperty("dot.ignoreerror", "false"));
		
		if(StringHelper.isNotEmpty( cmd )) {
			try {
				GenericExporter exporter = new GenericExporter();
				exporter.getProperties().putAll( getProperties() );
				exporter.getProperties().put(ARTIFACT_COLLECTOR, getArtifactCollector());
				exporter.getProperties().put(METADATA_DESCRIPTOR, getMetadataDescriptor());
				exporter.getProperties().put(OUTPUT_FOLDER, getOutputDirectory());
				String[] tp = (String[])exporter.getProperties().get(TEMPLATE_PATH);
				if (tp != null) {
					exporter.getProperties().put(TEMPLATE_PATH, tp);
				}
 
				exporter.setTemplateName( "dot/entitygraph.dot.ftl" );
				exporter.setFilePattern( "entities/entitygraph.dot" );
				exporter.start();

				exporter.setTemplateName( "dot/tablegraph.dot.ftl" );
				exporter.setFilePattern( "tables/tablegraph.dot" );
				exporter.start();
				
				
				File entityGraphDot = new File(getOutputDirectory(), "entities/entitygraph.dot");
				dotToFile( cmd, entityGraphDot.toString(), new File(getOutputDirectory(), "entities/entitygraph.png").toString());
				dotToFile( cmd, entityGraphDot.toString(), new File(getOutputDirectory(), "entities/entitygraph.svg").toString());
				dotToFile( cmd, entityGraphDot.toString(), new File(getOutputDirectory(), "entities/entitygraph.cmap").toString());
				
				File tableGraphDot = new File(getOutputDirectory(), "tables/tablegraph.dot");
				dotToFile( cmd, tableGraphDot.toString(), new File(getOutputDirectory(), "tables/tablegraph.png").toString());
				dotToFile( cmd, tableGraphDot.toString(), new File(getOutputDirectory(), "tables/tablegraph.svg").toString());
				dotToFile( cmd, tableGraphDot.toString(), new File(getOutputDirectory(), "tables/tablegraph.cmap").toString());
			
				return true;

			}
			catch (IOException e) {
				if(ignoreError) {
					log.warn( "Skipping entitygraph creation since dot.executable was not found and dot.ignoreerror=false." );
					return false;
				} else {
					throw new HibernateException("Problem while generating DOT graph for Configuration (set dot.ignoreerror=false to ignore)", e);
				}
			}
		} else {
			log.info( "Skipping entitygraph creation since dot.executable is empty or not-specified." );
			return false;
		}
	}

	public static final String OS_NAME = System.getProperty("os.name");    
    public static final boolean IS_LINUX = OS_NAME.startsWith("Linux");    
	
	private String escape(String fileName){
		
		// Linux does not need " " around file names
		if (IS_LINUX){
			return fileName;
		}
		
		// Windows needs " " around file names; actually we do not 
		// need it always, only when spaces are present;
		// but it does not hurt to usem them always
		return "\"" + fileName + "\"";
		
	}
	
	private void dotToFile(String dotExeFileName, String dotFileName, String outFileName) throws IOException {

		//
		// dot.exe works by taking *.dot file and piping 
		// results into another file, for example:
		// d:\graphviz-1.12\bin\dot.exe -Tgif c:\temp\ManualDraw.dot > c:\temp\ManualDraw.gif
		// so we follow that model here and read stdout until EOF
		// 
	
		final String exeCmd = 
			escape(dotExeFileName) + 
			" -T" + getFormatForFile(outFileName) + " " + 
			escape(dotFileName) +
			" -o " + 
			escape(outFileName);			
	
		Process p = Runtime.getRuntime().exec(exeCmd);
		//p.getErrorStream().
		try {
			log.debug( "Executing: " + exeCmd );
//			 Get the input stream and read from it
	        InputStream in = p.getErrorStream();
	        int c;
	        while ((c = in.read()) != -1) {
	            System.out.print((char)c);
	        }
	        in.close();
			int i = p.waitFor( );
			if(i!=0) {
				//TODO: dump system.err
				log.error("Error " + i + " while executing: " + exeCmd);				
			}
		} catch(Exception ie){
			log.error( "Error while executing: " + exeCmd, ie );
		}
	}		

	private String getFormatForFile(String outFileName){
		int idx = outFileName.lastIndexOf(".");
		if (idx == -1 || idx == outFileName.length() - 1){
			throw new IllegalArgumentException("Can't determine file name extention for file name " + outFileName); 
		}
		return outFileName.substring(idx + 1);
	}
	

	protected void setupContext() {
		if(!getProperties().contains( "jdk5" )) {
			getProperties().setProperty( "jdk5", "true" );
		}		
		super.setupContext();
		Metadata metadata = getMetadata();
		docHelper = new DocHelper( metadata, getProperties(), getCfg2JavaTool() );
        docFileManager = new DocFileManager(docHelper, getOutputDirectory() );

        getTemplateHelper().putInContext("dochelper", docHelper);
        getTemplateHelper().putInContext("docFileManager", docFileManager);
	}

    /**
     * Generate common files and copy assets.
     */
    public void generateCommmonAndAssets() {
        try {
            DocFile cssStylesDocFile = docFileManager.getCssStylesDocFile();

            processTemplate(new HashMap<String, Object>(0), FILE_CSS_DEFINITION, cssStylesDocFile.getFile());

            DocFile hibernateLogoDocFile = docFileManager.getHibernateImageDocFile();

            DocFileManager.copy(this.getClass().getClassLoader(), FILE_HIBERNATE_IMAGE,
                    hibernateLogoDocFile.getFile() );

            DocFile extendsImageDocFile = docFileManager.getExtendsImageDocFile();
                        
            DocFileManager.copy(this.getClass().getClassLoader(), FILE_EXTENDS_IMAGE, extendsImageDocFile.getFile());
            
            DocFile mainIndexDocFile = docFileManager.getMainIndexDocFile();

            processTemplate(new HashMap<String, Object>(0), FILE_INDEX, mainIndexDocFile.getFile() );
        } 
        catch (IOException ioe) {
            throw new RuntimeException("Error while copying files.", ioe);
        }
    }

    /**
     * Generate the index file of the table documentation.
     */
    public void generateTablesIndex() {
        DocFile docFile = docFileManager.getTableIndexDocFile();

        File file = docFile.getFile();

        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("docFile", docFile);

        processTemplate(parameters, FTL_TABLES_INDEX, file);
    }
    
    /**
     * Generate the index file of the class documentation
     */
    public void generateEntitiesIndex(){
    	DocFile docFile = docFileManager.getClassIndexDocFile();
    	File file = docFile.getFile();
    	Map<String, Object> parameters = new HashMap<String, Object>();
    	parameters.put("docFile", docFile);
    	processTemplate(parameters, FTL_ENTITIES_INDEX, file );
    }

    /**
     * Generate a file with an summary of all the tables.
     * @param graphsGenerated 
     */
    public void generateTablesSummary(boolean graphsGenerated) {
        DocFile docFile = docFileManager.getTableSummaryDocFile();

        File file = docFileManager.getTableSummaryDocFile().getFile();

        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("docFile", docFile);
        parameters.put( "graphsGenerated", Boolean.valueOf( graphsGenerated ) );
        if(graphsGenerated) {
        	StringBuffer sb = new StringBuffer();
        	String fileName = "tables/tablegraph.cmap";
        	appendFile( sb, fileName );
            parameters.put( "tablegrapharea", sb );
        }
        
        processTemplate(parameters, FTL_TABLES_SUMMARY, file);
    }

	private void appendFile(StringBuffer sb, String fileName) {
		try {
			BufferedReader in = new BufferedReader(new FileReader(new File(getOutputDirectory(), fileName)));
		    String str;
		    
		    while ((str = in.readLine()) != null) {
		        sb.append(str);
		        sb.append(System.getProperty("line.separator"));
		    }
		    
		    in.close();
		} catch (IOException e) {
		}
	}
    
    /**
     * Generate summary (summaty.html) to show all the packages 
     *
     */
    public void generatePackageSummary(boolean graphsGenerated){
    	DocFile docFile = docFileManager.getClassSummaryFile(); 
    	File file = docFile.getFile();
    	
    	Map<String, Object> parameters = new HashMap<String, Object>();
    	parameters.put("docFile", docFile);
    	
        List<String> list = docHelper.getPackages();
        if (list.size() > 0){
    		//Remove All Classes
        	list.remove(0);
    	}
        parameters.put("packageList", list );
        parameters.put( "graphsGenerated", Boolean.valueOf( graphsGenerated ) );
        if(graphsGenerated) {
        	StringBuffer sb = new StringBuffer();
        	String fileName = "entities/entitygraph.cmap";
        	appendFile( sb, fileName );
            parameters.put( "entitygrapharea", sb );
        }

    	processTemplate(parameters, FTL_ENTITIES_SUMMARY, file);
    }

    /**
     * Generate one file per table with detail information.
     */
    public void generateTablesDetails() {
    		Metadata metadata = getMetadata();
        Iterator<Table> tables = metadata.collectTableMappings().iterator();
        while (tables.hasNext() ) {
            Table table = tables.next();

            DocFile docFile = docFileManager.getTableDocFile(table);
            if(docFile!=null) {
            	File file = docFile.getFile();

            	Map<String, Object> parameters = new HashMap<String, Object>();
            	parameters.put("docFile", docFile);
            	parameters.put("table", table);

            	processTemplate(parameters, FTL_TABLES_TABLE, file);
            }
        }
    }
   
    /**
     * generates one html file for each class containing detail information of class
     *
     */
    public void generateEntitiesDetails(){
    	Iterator<POJOClass> classes = docHelper.getClasses().iterator();
    	while(classes.hasNext()){
    		POJOClass pcObj = classes.next();  
    		
    		pcObj.getPropertiesForMinimalConstructor();		
    		DocFile docFile = docFileManager.getEntityDocFile(pcObj);
    		File file = docFile.getFile();
    		
    		Map<String, Object> parameters = new HashMap<String, Object>();
    		parameters.put("docFile", docFile);
    		parameters.put("class", pcObj);    		
    		processTemplate(parameters, FTL_ENTITIES_ENTITY, file);
    	}
    }
    
    /**
     * Generates the html file containig list of packages (allpackages.html)
     *
     */
    public void generateEntitiesAllPackagesList() {
        DocFile docFile = docFileManager.getAllPackagesDocFile();

        File file = docFile.getFile();

        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("docFile", docFile);
        List<String> list = docHelper.getPackages();
        if (list.size() > 0){
        	 //Remove All Classes
            list.remove(0);
        }
        parameters.put("packageList", list );

        processTemplate(parameters, FTL_ENTITIES_PACKAGE_LIST, file);
    } 
    
    /**
     * Generates the html file containing list of classes (allclases.html)
     *
     */
    public void generateEntitiesAllEntitiesList() {
        DocFile docFile = docFileManager.getAllEntitiesDocFile();

        File file = docFile.getFile();   

        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("docFile", docFile);
        parameters.put("classList", docHelper.getClasses());  

        processTemplate(parameters, FTL_ENTITIES_ENTITY_LIST, file);
    }
    
    /**
     * generates the list of classes sepcific to package
     *
     */
    public void generateEntitiesPackageEntityList() {
        Iterator<String> packages = docHelper.getPackages().iterator();

        while (packages.hasNext() ) {
            String packageName = packages.next();
            
            if(!packageName.equals(DocHelper.DEFAULT_NO_PACKAGE)){
                DocFile docFile = docFileManager.getPackageEntityListDocFile(packageName);
                File file = docFile.getFile();

                Map<String, Object> parameters = new HashMap<String, Object>();
                parameters.put("docFile", docFile);
                parameters.put("title", packageName);
                parameters.put("classList", docHelper.getClasses(packageName));
                processTemplate(parameters, FTL_ENTITIES_PERPACKAGE_ENTITY_LIST, file);
            	
            }

        }
    }
    
    /**
     * Generates the html file containing list of classes and interfaces for given package
     *
     */
    public void generateEntitiesPackageDetailedInfo() {
    	List<String> packageList = docHelper.getPackages();
    	if (packageList.size() > 0){
    		//Remove All Classes
    		packageList.remove(0);
    	}
        Iterator<String> packages = packageList.iterator();
        
        while (packages.hasNext() ) {
            String packageName = packages.next();

            DocFile summaryDocFile = docFileManager.getPackageSummaryDocFile(packageName);
            Map<String, Object> parameters = new HashMap<String, Object>();
            parameters.put("docFile", summaryDocFile);
            parameters.put("package", packageName);           
            parameters.put("classList", docHelper.getClasses(packageName));                       

            processTemplate(parameters, FTL_ENTITIES_PACKAGE_SUMMARY,
                    summaryDocFile.getFile() );
        }
    }    

    /**
     * Generate a file with a list of all the schemas in the configuration.
     */
    public void generateTablesAllSchemasList() {
        DocFile docFile = docFileManager.getAllSchemasDocFile();

        File file = docFile.getFile();

        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("docFile", docFile);
        parameters.put("schemaList", docHelper.getSchemas() );

        processTemplate(parameters, FTL_TABLES_SCHEMA_LIST, file);
    }

    /**
     * Generate a file with a list of all the tables in the configuration.
     */
    public void generateTablesAllTablesList() {
        DocFile docFile = docFileManager.getAllTablesDocFile();

        File file = docFile.getFile();

        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("docFile", docFile);
        parameters.put("tableList", docHelper.getTables() );

        processTemplate(parameters, FTL_TABLES_TABLE_LIST, file);
    }

    public void generateTablesSchemaTableList() {
        Iterator<String> schemas = docHelper.getSchemas().iterator();

        while (schemas.hasNext() ) {
            String schemaName = schemas.next();

            DocFile docFile = docFileManager.getSchemaTableListDocFile(schemaName);

            File file = docFile.getFile();

            Map<String, Object> parameters = new HashMap<String, Object>();
            parameters.put("docFile", docFile);
            parameters.put("title", schemaName);
            parameters.put("tableList", docHelper.getTables(schemaName) );

            processTemplate(parameters, FTL_TABLES_PERSCHEMA_TABLE_LIST, file);
        }
    }

    /**
     * Generate two files per schema. One with a summary of the tables in the
     * schema and another one with a list of tables.
     */
    public void generateTablesSchemaDetailedInfo() {
        Iterator<String> schemas = docHelper.getSchemas().iterator();
        while (schemas.hasNext() ) {
            String schemaName = schemas.next();

            DocFile summaryDocFile = docFileManager.getSchemaSummaryDocFile(schemaName);

            Map<String, Object> parameters = new HashMap<String, Object>();
            parameters.put("docFile", summaryDocFile);
            parameters.put("schema", schemaName);

            processTemplate(parameters, FTL_TABLES_SCHEMA_SUMMARY,
                    summaryDocFile.getFile() );

            DocFile tableListDocFile = docFileManager.getSchemaSummaryDocFile(schemaName);

            parameters = new HashMap<String, Object>();
            parameters.put("docFile", tableListDocFile);
            parameters.put("schema", schemaName);

            processTemplate(parameters, FTL_TABLES_SCHEMA_SUMMARY,
                    tableListDocFile.getFile() );
            
            //processTemplate( new HashMap(), templateName, outputFile );
        }
    }

    /**
     * Run templates.
     * 
     * @param parameters the parameters to pass to the templates template.
     * @param templateName the template to use.
     * @param outputFile the output file.
     */
    protected void processTemplate(Map<String, Object> parameters, String templateName,
            File outputFile) {
    	
    	TemplateProducer producer = new TemplateProducer(getTemplateHelper(), getArtifactCollector() );
    	producer.produce(parameters, templateName, outputFile, templateName);
     }

    public String getName() {
    	return "hbm2doc";
    }
       
}