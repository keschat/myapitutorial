package com.myapitutorial.rest;

import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApiApplication extends ResourceConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiApplication.class);

    public ApiApplication() {
        packages(ApiApplication.class.getPackageName());

         register(new AbstractBinder() {
            @Override
            protected void configure() {
                LOGGER.info("Configuring binder");
                // bindFactory(HttpSessionFactory.class).to(HttpSession.class);
            }
        });
    }
    
}
