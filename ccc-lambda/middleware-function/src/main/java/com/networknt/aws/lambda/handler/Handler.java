package com.networknt.aws.lambda.handler;

import com.networknt.config.Config;
import com.networknt.handler.config.EndpointSource;
import com.networknt.handler.config.HandlerConfig;
import com.networknt.handler.config.PathChain;
import com.networknt.utility.ModuleRegistry;
import com.networknt.utility.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class Handler {
    private static final Logger LOG = LoggerFactory.getLogger(Handler.class);
    // Accessed directly.
    public static HandlerConfig config = HandlerConfig.load();
    public static final String REQUEST_CHAIN = "request";
    public static final String RESPONSE_CHAIN = "response";

    // each handler keyed by a name.
    static final Map<String, LambdaHandler> handlers = new HashMap<>();
    static final Map<String, List<LambdaHandler>> handlerListById = new HashMap<>();
    static List<LambdaHandler> defaultHandlers;
    // this is the last handler that need to be called when OrchestratorHandler is injected into the beginning of the chain
    static LambdaHandler lastHandler;

    public static void setLastHandler(LambdaHandler handler) {
        lastHandler = handler;
    }

    public static void init() {
        initHandlers();
        initChains();
        initPaths();
        initDefaultHandlers();
        ModuleRegistry.registerModule(HandlerConfig.CONFIG_NAME, Handler.class.getName(), Config.getNoneDecryptedInstance().getJsonMapConfigNoCache(HandlerConfig.CONFIG_NAME), null);
    }

    public ArrayList<LambdaHandler> getRequestChain() {
        return (ArrayList<LambdaHandler>) handlerListById.get(REQUEST_CHAIN);
    }

    public ArrayList<LambdaHandler> getResponseChain() {
        return (ArrayList<LambdaHandler>) handlerListById.get(RESPONSE_CHAIN);
    }

    /**
     * Construct the named map of handlers. Note: All handlers in use for this
     * microservice should be listed in this handlers list.
     */
    @SuppressWarnings("unchecked")
    static void initHandlers() {
        if (config != null && config.getHandlers() != null) {

            // initialize handlers
            for (var handler : config.getHandlers()) {
                // handler is a fully qualified class name with a default constructor.
                initStringDefinedHandler((String) handler);
            }
        }
    }

    /**
     * Construct chains of handlers, if any are configured NOTE: It is recommended
     * to define reusable chains of handlers
     */
    static void initChains() {

        if (config != null && config.getChains() != null) {

            // add the chains to the handler list by id list.
            for (var chainName : config.getChains().keySet()) {
                var chain = config.getChains().get(chainName);
                var handlerChain = new ArrayList<LambdaHandler>();

                for (var chainItemName : chain) {
                    var chainItem = handlers.get(chainItemName);

                    if (chainItem == null)
                        throw new RuntimeException("Chain " + chainName + " uses Unknown handler: " + chainItemName);

                    handlerChain.add(chainItem);
                }
                handlerListById.put(chainName, handlerChain);
            }
        }
    }

    /**
     * Build "handlerListById" and "reqTypeMatcherMap" from the paths in the config.
     */
    static void initPaths() {

        if (config != null && config.getPaths() != null) {

            for (var pathChain : config.getPaths()) {
                pathChain.validate(HandlerConfig.CONFIG_NAME + " config"); // raises exception on misconfiguration

                if (pathChain.getPath() == null)
                    addSourceChain(pathChain);

                else addPathChain(pathChain);
            }
        }
    }

    /**
     * Build "defaultHandlers" from the defaultHandlers in the config.
     */
    static void initDefaultHandlers() {

        if (config != null && config.getDefaultHandlers() != null) {
            defaultHandlers = getHandlersFromExecList(config.getDefaultHandlers());
            handlerListById.put("defaultHandlers", defaultHandlers);
        }
    }

    /**
     * Add PathChains crated from the EndpointSource given in sourceChain
     */
    private static void addSourceChain(PathChain sourceChain) {
        try {
            var sourceClass = Class.forName(sourceChain.getSource());
            var source = (EndpointSource) (sourceClass.getDeclaredConstructor().newInstance());

            for (var endpoint : source.listEndpoints()) {
                var sourcedPath = new PathChain();
                sourcedPath.setPath(endpoint.getPath());
                sourcedPath.setMethod(endpoint.getMethod());
                sourcedPath.setExec(sourceChain.getExec());
                sourcedPath.validate(sourceChain.getSource());
                addPathChain(sourcedPath);
            }
        } catch (Exception e) {

            if (LOG.isErrorEnabled())
                LOG.error("Failed to inject handler.yml paths from: " + sourceChain);

            if (e instanceof RuntimeException)
                throw (RuntimeException) e;

            else throw new RuntimeException(e);
        }
    }

    /**
     * Add a PathChain (having a non-null path) to the handler data structures.
     */
    private static void addPathChain(PathChain pathChain) {
        var method = pathChain.getMethod();

        // Use a random integer as the id for a given path.
        int randInt = new Random().nextInt();

        while (handlerListById.containsKey(Integer.toString(randInt)))
            randInt = new Random().nextInt();

        // Flatten out the execution list from a mix of middleware chains and handlers.
        var handlers = getHandlersFromExecList(pathChain.getExec());

        if (handlers.size() > 0) {
            handlerListById.put(Integer.toString(randInt), handlers);
        }
    }

    /**
     * Converts the list of chains and handlers to a flat list of handlers. If a
     * chain is named the same as a handler, the chain is resolved first.
     *
     * @param execs The list of names of chains and handlers.
     * @return A list containing references to the instantiated handlers
     */
    private static List<LambdaHandler> getHandlersFromExecList(List<String> execs) {
        var handlersFromExecList = new ArrayList<LambdaHandler>();

        if (execs != null) {

            for (var exec : execs) {
                var handlerList = handlerListById.get(exec);

                if (handlerList == null)
                    throw new RuntimeException("Unknown handler or chain: " + exec);

                for (LambdaHandler handler : handlerList) {

                    if (handler instanceof MiddlewareHandler) {

                        if (((MiddlewareHandler) handler).isEnabled())
                            handlersFromExecList.add(handler);

                    } else handlersFromExecList.add(handler);
                }
            }
        }
        return handlersFromExecList;
    }

    /**
     * Detect if the handler is a MiddlewareHandler instance. If yes, then register it.
     *
     * @param handler Object
     */
    private static void registerMiddlewareHandler(Object handler) {

        if (handler instanceof MiddlewareHandler) {

            // register the middleware handler if it is enabled.
            if (((MiddlewareHandler) handler).isEnabled())
                ((MiddlewareHandler) handler).register();
        }
    }

    /**
     * Helper method for generating the instance of a handler from its string
     * definition in config. Ie. No mapped values for setters, or list of
     * constructor fields. To note: It could either implement HttpHandler, or
     * HandlerProvider.
     *
     * @param handler
     */
    private static void initStringDefinedHandler(String handler) {

        // split the class name and its label, if defined
        Tuple<String, Class> namedClass = splitClassAndName(handler);

        // create an instance of the handler
        Object handlerOrProviderObject = null;
        try {
            handlerOrProviderObject = namedClass.second.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException |
                 InvocationTargetException e) {
            LOG.error("Could not instantiate handler class " + namedClass.second, e);
            throw new RuntimeException("Could not instantiate handler class: " + namedClass.second);
        }

        LambdaHandler resolvedHandler;

        if (handlerOrProviderObject instanceof LambdaHandler)
            resolvedHandler = (LambdaHandler) handlerOrProviderObject;
        else throw new RuntimeException("Unsupported type of handler provided: " + handlerOrProviderObject);

        registerMiddlewareHandler(resolvedHandler);
        handlers.put(namedClass.first, resolvedHandler);
        handlerListById.put(namedClass.first, Collections.singletonList(resolvedHandler));
    }


    /**
     * To support multiple instances of the same class, support a naming
     *
     * @param classLabel The label as seen in the config file.
     * @return A tuple where the first value is the name, and the second is the
     * class.
     * @throws Exception On invalid format of label.
     */
    static Tuple<String, Class> splitClassAndName(String classLabel) {
        String[] stringNameSplit = classLabel.split("@");
        // If i don't have a @, then no name is provided, use the class as the name.
        if (stringNameSplit.length == 1) {
            try {
                return new Tuple<>(classLabel, Class.forName(classLabel));
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Configured class: " + classLabel + " has not been found");
            }
        } else if (stringNameSplit.length > 1) { // Found a @, use that as the name, and
            try {
                return new Tuple<>(stringNameSplit[1], Class.forName(stringNameSplit[0]));
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Configured class: " + stringNameSplit[0]
                        + " has not been found. Declared label was: " + stringNameSplit[1]);
            }
        }
        throw new RuntimeException("Invalid format provided for class label: " + classLabel);
    }

    // Exposed for testing only.
    static void setConfig(String configName) throws Exception {
        config = HandlerConfig.load(configName);
        initHandlers();
        initChains();
        initPaths();
    }

    public static Map<String, LambdaHandler> getHandlers() {
        return handlers;
    }

}
