/*
 * Copyright (c) 2007, Red Hat Middleware, LLC. All rights reserved.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, v. 2.1. This program is distributed in the
 * hope that it will be useful, but WITHOUT A WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. You should have received a
 * copy of the GNU Lesser General Public License, v.2.1 along with this
 * distribution; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * Red Hat Author(s): Brian Stansberry
 */

package org.hibernate.cache.jbc2.builder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.hibernate.cache.CacheException;
import org.jboss.cache.Cache;
import org.jboss.cache.CacheStatus;
import org.jboss.cache.DefaultCacheFactory;
import org.jboss.cache.config.Configuration;
import org.jboss.cache.config.ConfigurationException;
import org.jboss.cache.xml.XmlHelper;
import org.jgroups.ChannelFactory;
import org.jgroups.JChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;

/**
 * A JBossCacheConfigurationFactory. This is a basic prototype of a
 * JBCACHE-1156 solution; only in Hibernate code base for a very short
 * period.
 * 
 * @author <a href="brian.stansberry@jboss.com">Brian Stansberry</a>
 * @version $Revision: 1 $
 */
public class JBossCacheFactoryImpl implements JBossCacheFactory {
    
    private static final Logger log = LoggerFactory.getLogger(JBossCacheFactoryImpl.class);

    private static final String DOCUMENT_ROOT = "cache-configs";
    private static final String CONFIG_ROOT = "cache-config";
    private static final String CONFIG_NAME = "name";

    private static JBossCacheFactoryImpl sharedFactory;
    private static String sharedChannelFactoryCfg;
    
    private XmlConfigurationParser parser;
    private String configResource;
    private Map configs = new HashMap();
    private Map caches = new HashMap();
    private Map checkouts = new HashMap();
    private ChannelFactory channelFactory;
    private boolean started;
    
    public JBossCacheFactoryImpl(String configResource, ChannelFactory factory) {
        
        parser = new XmlConfigurationParser();
        this.configResource = configResource;
        this.channelFactory = factory;
    }
    
    public static synchronized JBossCacheFactory getSharedInstance(String cacheConfigResource, String channelFactoryConfigResource) {
        
        if (sharedFactory == null) {
            ChannelFactory cf = new JChannelFactory();
            try {
                cf.setMultiplexerConfig(channelFactoryConfigResource);
            }
            catch (Exception e) {
                throw new CacheException("Problem setting ChannelFactory config", e);
            }
            sharedFactory = new JBossCacheFactoryImpl(cacheConfigResource, cf);
            sharedChannelFactoryCfg = channelFactoryConfigResource;
        }
        else {
            // Validate that the provided resources match the existing singleton
            if (!sharedFactory.getConfigResource().equals(cacheConfigResource)) {
                throw new CacheException("Provided cacheConfigResource does " +
                		"not match the existing shared factory: provided = " + 
                		cacheConfigResource + "; existing = " + sharedFactory.getConfigResource());
            }
            else if (!sharedChannelFactoryCfg.equals(channelFactoryConfigResource)) {
                throw new IllegalStateException("Provided channelFactoryConfigResource does " +
                        "not match the existing shared factory: provided = " + 
                        channelFactoryConfigResource + "; existing = " + sharedChannelFactoryCfg);
                
            }
        }
        
        return sharedFactory;
    }
    
    public void start() {
        if (!started) {
            this.configs = parser.parseConfigs(configResource);
            started = true;
        }
    }
    
    public void stop() {
        if (started) {
            synchronized (caches) {
                for (Iterator it = caches.entrySet().iterator(); it.hasNext(); ) {
                    Map.Entry entry = (Entry) it.next();
                    destroyCache((Cache) entry.getValue());
                    it.remove();
                }
                caches.clear();
                checkouts.clear();
                configs.clear();
            }
            started = false;
        }
    }
    
    public String getConfigResource() {
        return configResource;
    }

    public ChannelFactory getChannelFactory() {
        return channelFactory;
    }

    public Set getConfigurationNames()
    {
        return new HashSet(configs.keySet());
    }
    
    public Cache getCache(String configName, boolean create) throws Exception
    {
        Cache cache = null;
        synchronized (caches) {
            cache = (Cache) caches.get(configName);
            if (cache == null && create) {
                Configuration config = getConfiguration(configName);
                cache = DefaultCacheFactory.getInstance().createCache(config, false);
                registerCache(cache, configName);
            }
            else if (cache != null) {
                incrementCheckout(configName);
            }
        }
        
        return cache;
    }

    private int incrementCheckout(String configName) {
        synchronized (checkouts) {
            Integer count = (Integer) checkouts.get(configName);
            if (count == null)
                count = new Integer(0);
            Integer newVal = new Integer(count.intValue() + 1);
            checkouts.put(configName, newVal);
            return newVal.intValue();
        }
    }

    private int decrementCheckout(String configName) {
        synchronized (checkouts) {
            Integer count = (Integer) checkouts.get(configName);
            if (count == null || count.intValue() < 1)
                throw new IllegalStateException("invalid count of " + count + " for " + configName);

            Integer newVal = new Integer(count.intValue() - 1);
            checkouts.put(configName, newVal);
            return newVal.intValue();
        }
    }
    
    public void registerCache(Cache cache, String configName) {
        synchronized (caches) {
            if (caches.containsKey(configName))
                throw new IllegalStateException(configName + " already registered");
            caches.put(configName, cache);
            incrementCheckout(configName);
        }
    }
    
    public void releaseCache(String configName) {

        synchronized (caches) {
            if (!caches.containsKey(configName))
                throw new IllegalStateException(configName + " not registered");
            if (decrementCheckout(configName) == 0) {
                Cache cache = (Cache) caches.remove(configName);
                destroyCache(cache);
            }
        }
    }

    private void destroyCache(Cache cache) {
        if (cache.getCacheStatus() == CacheStatus.STARTED) {
            cache.stop();
        }
        if (cache.getCacheStatus() != CacheStatus.DESTROYED
                && cache.getCacheStatus() != CacheStatus.INSTANTIATED) {
            cache.destroy();
        }
    }
    
    public Configuration getConfiguration(String configName) throws Exception {
        Element element = (Element) configs.get(configName);
        if (element == null)
            throw new IllegalArgumentException("unknown config " + configName);
        Configuration config = parser.parseConfig(element);
        if (channelFactory != null && config.getMultiplexerStack() != null) {
            config.getRuntimeConfig().setMuxChannelFactory(channelFactory);
        }
        return config;
    }
    
    class XmlConfigurationParser extends org.jboss.cache.factories.XmlConfigurationParser {

        public Map parseConfigs(String configs) {
            InputStream is = getAsInputStreamFromClassLoader(configs);
            if (is == null)
            {
               if (log.isDebugEnabled())
                  log.debug("Unable to find configuration file " + configs + " in classpath; searching for this file on the filesystem instead.");
               try
               {
                  is = new FileInputStream(configs);
               }
               catch (FileNotFoundException e)
               {
                  throw new ConfigurationException("Unable to find config file " + configs + " either in classpath or on the filesystem!", e);
               }
            }

            return parseConfigs(is);
        }
        
        public Map parseConfigs(InputStream stream) {
            
            // loop through all elements in XML.
            Element root = XmlHelper.getDocumentRoot(stream);
            NodeList list = root.getElementsByTagName(CONFIG_ROOT);
            if (list == null || list.getLength() == 0) 
                throw new ConfigurationException("Can't find " + CONFIG_ROOT + " tag");
            
            Map result = new HashMap();
            
            for (int i = 0; i < list.getLength(); i++)
            {
               org.w3c.dom.Node node = list.item(i);
               if (node.getNodeType() != org.w3c.dom.Node.ELEMENT_NODE)
               {
                  continue;
               }
               
               Element element = (Element) node;
               String name = element.getAttribute(CONFIG_NAME);
               if (name == null || name.trim().length() == 0)
                   throw new ConfigurationException("Element " + element + " has no name attribute");
               
               result.put(name.trim(), element);
            }
            
            return result;
        }
        
        public Configuration parseConfig(Element config) throws Exception {

            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = docBuilderFactory.newDocumentBuilder();
            Document doc = builder.newDocument();
            Element root = doc.createElement(DOCUMENT_ROOT);
            doc.appendChild(root);
            Node imported = doc.importNode(config, true);
            root.appendChild(imported);
            
            DOMImplementation domImpl = doc.getImplementation();

            DOMImplementationLS impl = 
                (DOMImplementationLS)domImpl.getFeature("LS", "3.0");

            LSSerializer writer = impl.createLSSerializer();
            LSOutput output = impl.createLSOutput();
            output.setEncoding("UTF-8");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            output.setByteStream(baos);
            writer.write(doc, output);
            
            ByteArrayInputStream is = new ByteArrayInputStream(baos.toByteArray());            
            return parseStream(is);
        } 


        @Override
        protected Element getMBeanElement(Element root)
        {
           // This is following JBoss convention.
           NodeList list = root.getElementsByTagName(CONFIG_ROOT);
           if (list == null) throw new ConfigurationException("Can't find " + CONFIG_ROOT + " tag");

           if (list.getLength() > 1) throw new ConfigurationException("Has multiple " + CONFIG_ROOT + " tag");

           Node node = list.item(0);
           Element element = null;
           if (node.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE)
           {
              element = (Element) node;
           }
           else
           {
              throw new ConfigurationException("Can't find " + CONFIG_ROOT + " element");
           }
           return element;
        }
        
        
    }
    
    public static void main(String[] args)
    {
        try
        {
            JChannelFactory cf = new JChannelFactory();
            cf.setMultiplexerConfig("stacks.xml");
            JBossCacheFactoryImpl factory = new JBossCacheFactoryImpl("jbc2-configs.xml", cf);
            for (Iterator iter = factory.getConfigurationNames().iterator(); iter.hasNext(); )
            {
                String name = (String) iter.next();
                Cache c = factory.getCache(name, true);
                c.start();
                System.out.println(name + " == " + c);
                factory.releaseCache(name);
                System.out.println(name + " == " + c.getCacheStatus());
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace(System.out);
        }
    }
}
