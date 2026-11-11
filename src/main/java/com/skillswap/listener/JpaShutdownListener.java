package com.skillswap.listener;

import com.skillswap.util.JpaUtil;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

/** Closes the Hibernate session factory when the web application stops. */
@WebListener
public class JpaShutdownListener implements ServletContextListener {
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        JpaUtil.shutdown();
    }
}
