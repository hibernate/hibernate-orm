/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2010-2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.internal.export.query;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.tool.internal.export.common.AbstractExporter;

/** 
 * exporter for query execution.
 * 
 **/
public class QueryExporter extends AbstractExporter {

    public void doStart() {
        Session session = null;
        SessionFactory sessionFactory = null;
        Transaction transaction = null;
        try {
            sessionFactory = buildMetadata().buildSessionFactory();
            session = sessionFactory.openSession();
            transaction = session.beginTransaction();
            for ( Object o : getQueryList() ) {
                String query = (String) o;

                List<?> list = session.createQuery( query, (Class<?>)null ).getResultList();

                if ( getFileName() != null ) {
                    PrintWriter pw = null;
                    try {
                        File file = new File( getOutputDirectory(), getFileName() );
                        getTemplateHelper().ensureExistence( file );
                        pw = new PrintWriter( new FileWriter( file, true ) );
                        getArtifactCollector().addFile( file, "query-output" );

                        for ( Object element : list ) {
                            pw.println( element );
                        }

                    }
                    catch (IOException e) {
                        throw new RuntimeException( "Could not write query output", e );
                    }
                    finally {
                        if ( pw != null ) {
                            pw.flush();
                            pw.close();
                        }
                    }
                }
            }
            transaction.commit();
        }
        catch(HibernateException he) {
            if(transaction!=null) {
                transaction.rollback();
            }
            throw new RuntimeException("Error occured while trying to execute query", he);
        }
        finally {
            if(session!=null) {
                session.close();
            }
            if(sessionFactory!=null) {
                sessionFactory.close();
            }

        }
    }

    private String getFileName() {
        return (String)getProperties().get(OUTPUT_FILE_NAME);
    }

    private List<?> getQueryList() {
        return (List<?>)getProperties().get(QUERY_LIST);
    }

    public void setQueries(List<String> queryStrings) {
        getProperties().put(QUERY_LIST, queryStrings);
    }

}
