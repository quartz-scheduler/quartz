/* All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.*/

package org.quartz.management.jaxrs;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

import org.codehaus.jackson.map.MapperConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.PropertyNamingStrategy;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.introspect.AnnotatedMethod;

/**
 * @author brandony
 * 
 *         The aim of this context resolver is to rename rootRepresentables
 *         elements in json to schedulers
 * 
 */
@Provider
public final class QuartzObjectMapperProvider implements ContextResolver<ObjectMapper> {
    private static final String ROOT_METHOD_NAME = "getRootRepresentables";

    private static final String QUARTZ_ROOT_NAME = "schedulers";

    private final ObjectMapper om;

    public QuartzObjectMapperProvider() {
        ObjectMapper objectMapper = new ObjectMapper();
        SerializationConfig dflt = objectMapper.getSerializationConfig();
        objectMapper.setSerializationConfig(dflt.withPropertyNamingStrategy(new PropertyNamingStrategy() {

            /**
             * @see org.codehaus.jackson.map.PropertyNamingStrategy#nameForGetterMethod(org.codehaus.jackson.map.MapperConfig,
             *      org.codehaus.jackson.map.introspect.AnnotatedMethod,
             *      java.lang.String)
             */
            @Override
            public String nameForGetterMethod(MapperConfig<?> config, AnnotatedMethod method, String defaultName) {
                if (ROOT_METHOD_NAME.equals(method.getName()))
                    return QUARTZ_ROOT_NAME;
                else
                    return super.nameForGetterMethod(config, method, defaultName);
            }
        }));

        this.om = objectMapper;
    }

    /**
     * @see javax.ws.rs.ext.ContextResolver#getContext(java.lang.Class)
     */
    public ObjectMapper getContext(Class<?> arg0) {
        return om;
    }
}
