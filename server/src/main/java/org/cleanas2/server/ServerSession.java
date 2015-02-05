package org.cleanas2.server;

import com.google.inject.Injector;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cleanas2.common.service.StoppableService;

import java.util.ArrayList;
import java.util.List;

import static org.cleanas2.util.AS2Util.ofType;

/**
 * Default context for the running server.  Event bus and others may hold weak references, so any
 * long lived services must be stored here or they will be disposed by the garbage collector.  This
 * also allows services access to other services that they may need (generally just partner/company/config)
 *
 * @author Andrew Backer {@literal awbacker@gmail.com / andrew.backer@powere2e.com}
 */
public class ServerSession {

    private static final Log logger = LogFactory.getLog(ServerSession.class.getSimpleName());
    private static ServerSession instance;
    private final Injector injector;
    private final List<Object> allServices = new ArrayList<>();

    private ServerSession(Injector i) {
        this.injector = i;
    }

    public static void initialize(Injector i) {
        if (instance == null) {
            instance = new ServerSession(i);
        }
    }

    public static ServerSession getSession() {
        return instance;
    }

    public List<Object> getAllServices() {
        return allServices;
    }

    public <T> T getInstance(Class<T> klass) {
        return getSession().injector.getInstance(klass);
    }

    public <T> void startService(Class<T> klass) {
        T svc = injector.getInstance(klass);
        allServices.add(svc);
        MessageBus.subscribe(svc);
    }

    public void shutdown() {
        stopAllServices();
        allServices.clear();
        ServerSession.instance = null;
    }

    public void stopAllServices() {
        logger.debug("Shutting down running services");
        for (StoppableService svc : ofType(allServices, StoppableService.class)) {
            logger.debug("   " + svc.getClass().getSimpleName());
            svc.stop();
        }
    }

}
